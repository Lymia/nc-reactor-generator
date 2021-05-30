package generator;

import multiblock.Block;
import multiblock.CuboidalMultiblock;
import multiblock.Multiblock;
import multiblock.Range;
import multiblock.action.PostProcessingAction;
import multiblock.action.SetblockAction;
import multiblock.action.SymmetryAction;
import multiblock.configuration.overhaul.fissionsfr.BlockRecipe;
import multiblock.overhaul.fissionmsr.OverhaulMSR;
import multiblock.overhaul.fissionsfr.OverhaulSFR;
import multiblock.symmetry.Symmetry;
import multiblock.underhaul.fissionsfr.UnderhaulSFR;
import planner.exception.MissingConfigurationEntryException;
import planner.menu.component.MenuComponentLabel;
import planner.menu.component.MenuComponentMinimaList;
import planner.menu.component.MenuComponentMinimalistTextBox;
import planner.menu.component.generator.MenuComponentSymmetry;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A generator based on the simulated annealing algorithm.
 */
public class SimulatedAnnealingGenerator extends MultiblockGenerator {
    MenuComponentMinimaList symmetriesList;
    MenuComponentMinimalistTextBox maxIterations;
    MenuComponentMinimalistTextBox changeChancePercent;

    private final SimulatedAnnealingGeneratorSettings settings = new SimulatedAnnealingGeneratorSettings(this);
    private final Object currentBlockLock = new Object();
    private CuboidalMultiblock currentBlock;
    private final ArrayList<Multiblock> displayList = new ArrayList<>();

    public SimulatedAnnealingGenerator(Multiblock multiblock) {
        super(multiblock);

        if (multiblock != null) {
            currentBlock = (CuboidalMultiblock) multiblock.blankCopy();
            displayList.add(currentBlock);
        }
    }

    @Override
    public ArrayList<Multiblock>[] getMultiblockLists() {
        return new ArrayList[]{displayList, new ArrayList()};
    }

    @Override
    public boolean canGenerateFor(Multiblock multiblock) {
        return multiblock instanceof OverhaulSFR;
    }

    @Override
    public String getName() {
        return "Simulated Annealing";
    }

    @Override
    public void addSettings(MenuComponentMinimaList generatorSettings, Multiblock multi) {
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Generator Settings", true));
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 24, "Max Iterations", true));
        maxIterations =
                generatorSettings.add(new MenuComponentMinimalistTextBox(0, 0, 0, 32, "100000", true).setIntFilter())
                        .setTooltip("The number of iterations to execute the annealing schedule over.");
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 24, "Change Chance", true));
        changeChancePercent =
                generatorSettings.add(new MenuComponentMinimalistTextBox(0, 0, 0, 32, "3", true).setFloatFilter(0f,
                        100f).setSuffix("%")).setTooltip("The maximum percentage of blocks to change every iteration.");
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Symmetry Settings", true));
        ArrayList<Symmetry> symmetries = multi.getSymmetries();
        symmetriesList = generatorSettings.add(new MenuComponentMinimaList(0, 0, 0, symmetries.size() * 32, 24));
        for (Symmetry symmetry : symmetries) {
            symmetriesList.add(new MenuComponentSymmetry(symmetry));
        }
    }

    @Override
    public MultiblockGenerator newInstance(Multiblock multi) {
        return new SimulatedAnnealingGenerator(multi);
    }

    @Override
    public void refreshSettingsFromGUI(ArrayList<Range<Block>> allowedBlocks) {
        settings.refresh(allowedBlocks);
    }

    @Override
    public void refreshSettings(Settings settings) {
        if (settings instanceof SimulatedAnnealingGeneratorSettings) {
            this.settings.refresh((SimulatedAnnealingGeneratorSettings) settings);
        } else throw new IllegalArgumentException("Passed invalid settings to Simulated Annealing generator!");
    }

    private CuboidalMultiblock getCopyOfCurrent() {
        synchronized (currentBlockLock) {
            return (CuboidalMultiblock) currentBlock.copy();
        }
    }

    private void mutateMultiblock(CuboidalMultiblock cm) {
        final var changeChance = rand.nextDouble() * settings.changeChance;
        cm.forEachCasingPosition((x, y, z) -> {
            if (rand.nextDouble() < changeChance) {
                Block randBlock = null;
                var typeChance = rand.nextDouble();
                if (typeChance <= 0.1) {
                    randBlock = choice(settings.casingSources);
                } else if (typeChance <= 1.0) {
                    randBlock = choice(settings.casingGlass);
                };

                if (randBlock == null || cm.canBePlacedWithinCasing(randBlock)) return;
                var out = applyMultiblockSpecificSettings(cm, randBlock.newInstance(x, y, z));
                cm.queueAction(new SetblockAction(x, y, z, out));
            }
        });
        cm.forEachInternalPosition((x, y, z) -> {
            var b = cm.getBlock(x, y, z);
            if (rand.nextDouble() < changeChance) {
                Block randBlock = null;
                var typeChance = rand.nextDouble();
                if (typeChance <= 0.5) {
                    // Place moderator 50% of the time.
                    randBlock = choice(settings.moderators);
                } else if (typeChance <= 0.8) {
                    // Place heat sinks 30% of the time.
                    randBlock = choice(settings.heatSinks);
                } else if (typeChance <= 0.85) {
                    // Place heat sinks 5% of the time.
                    randBlock = choice(settings.conductors);
                } else if (typeChance <= 0.9) {
                    // Place reflectors 5% of the time.
                    randBlock = choice(settings.reflectors);
                } else if (typeChance <= 0.95) {
                    // Place nothing 5% of the time.
                    randBlock = null;
                } else if (typeChance <= 1.0) {
                    // Place fuel cells 5% of the time.
                    randBlock = choice(settings.fuelCell);
                };

                if (randBlock == null) cm.queueAction(new SetblockAction(x, y, z, null));
                else if (cm.canBePlacedWithinCasing(randBlock)) {
                    var out = applyMultiblockSpecificSettings(cm, randBlock.newInstance(x, y, z));
                    cm.queueAction(new SetblockAction(x, y, z, out));
                }
            }
        });
    }

    private void removeInvalidPass(CuboidalMultiblock cm) {
        cm.forEachCasingPosition((x, y, z) -> {
            var block = cm.getBlock(x, y, z);
            if (block != null && !cm.getBlock(x, y, z).isValid())
                cm.queueAction(new SetblockAction(x, y, z, settings.casingGlass.get(0)));
        });
        cm.forEachInternalPosition((x, y, z) -> {
            var block = cm.getBlock(x, y, z);
            if (block != null && !block.isValid())
                cm.queueAction(new SetblockAction(x, y, z, null));
        });
    }

    private void postProcessMultiblock(CuboidalMultiblock cm) {
        cm.buildDefaultCasing();
        cm.performActions(false);
        for (var symmetry : settings.symmetries) {
            cm.queueAction(new SymmetryAction(symmetry));
        }
        cm.performActions(false);
        cm.recalculate();
        if (iterations > settings.maxIterations) {
            removeInvalidPass(cm);
            cm.performActions(false);
            cm.recalculate();
        }
    }

    private CuboidalMultiblock getNeighbor() {
        var cm = getCopyOfCurrent();
        mutateMultiblock(cm);
        postProcessMultiblock(cm);
        return cm;
    }

    private static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
    private double temperature() {
        return 1.0 - Math.min((double) iterations / (double) settings.maxIterations, 1.0);
    }
    private double scoreAcceptance(double oldScore, double newScore) {
        var temperature = temperature();
        var modifiedTemperature = 1.0 - ((1.0 - temperature) * (1.0 - temperature));
        if (oldScore <= newScore) return 1.0;
        else return Math.max(0.0, 1.0 - (oldScore - newScore) / lerp(2, 6, modifiedTemperature)) * temperature;
    }

    private double scoreMultiblock(CuboidalMultiblock cm) {
        if (cm instanceof OverhaulSFR) {
            var sfr = (OverhaulSFR) cm;

            var outputFactor = Math.log(sfr.totalOutput + 1) + sfr.totalEfficiency * 7.5;
            var noOutputPenalty = 1 / (sfr.totalOutput + 0.0005);
            var heatPenalty = sfr.netHeat > 0 ? 10.0 + Math.sqrt(sfr.netHeat) : 0;

            return outputFactor - noOutputPenalty - heatPenalty;
        } else {
            throw new RuntimeException("Cannot use Simulated Annealing for type: " + cm.getClass().getName());
        }
    }

    private void checkAccepted(CuboidalMultiblock cm) {
        synchronized (currentBlockLock) {
            double acceptance = scoreAcceptance(scoreMultiblock(currentBlock), scoreMultiblock(cm));
            System.out.println("t: "+temperature()+", ns: "+scoreMultiblock(cm)+", os: "+scoreMultiblock(currentBlock)+", acc: "+acceptance);
            if (rand.nextDouble() <= acceptance) {
                currentBlock = cm;
                displayList.set(0, cm);
            }
        }
    }

    @Override
    public void tick() {
        CuboidalMultiblock neighbor = getNeighbor();
        checkAccepted(neighbor);
        countIteration();
    }

    private Block applyMultiblockSpecificSettings(Multiblock currentMultiblock, Block randBlock) {
        if (multiblock instanceof OverhaulSFR) {
            multiblock.overhaul.fissionsfr.Block block = (multiblock.overhaul.fissionsfr.Block) randBlock;
            if (!block.template.allRecipes.isEmpty()) {
                ArrayList<Range<multiblock.configuration.overhaul.fissionsfr.BlockRecipe>> validRecipes =
                        new ArrayList<>(((OverhaulSFR) multiblock).getValidRecipes());
                for (Iterator<Range<BlockRecipe>> it = validRecipes.iterator(); it.hasNext(); ) {
                    Range<multiblock.configuration.overhaul.fissionsfr.BlockRecipe> next = it.next();
                    if (!block.template.allRecipes.contains(next.obj)) it.remove();
                }
                if (!validRecipes.isEmpty()) block.recipe = rand(currentMultiblock, validRecipes);
            }
            return randBlock;
        }
        if (multiblock instanceof OverhaulMSR) {
            multiblock.overhaul.fissionmsr.Block block = (multiblock.overhaul.fissionmsr.Block) randBlock;
            if (!block.template.allRecipes.isEmpty()) {
                ArrayList<Range<multiblock.configuration.overhaul.fissionmsr.BlockRecipe>> validRecipes =
                        new ArrayList<>(((OverhaulMSR) multiblock).getValidRecipes());
                for (Iterator<Range<multiblock.configuration.overhaul.fissionmsr.BlockRecipe>> it =
                     validRecipes.iterator(); it.hasNext(); ) {
                    Range<multiblock.configuration.overhaul.fissionmsr.BlockRecipe> next = it.next();
                    if (!block.template.allRecipes.contains(next.obj)) it.remove();
                }
                if (!validRecipes.isEmpty()) block.recipe = rand(currentMultiblock, validRecipes);
            }
            return randBlock;
        }
        return randBlock;
    }

    @Override
    public void importMultiblock(Multiblock multiblock) throws MissingConfigurationEntryException {
        multiblock.convertTo(this.multiblock.getConfiguration());
        if (multiblock instanceof UnderhaulSFR) {
            multiblock = multiblock.copy();
            ((UnderhaulSFR) multiblock).fuel = ((UnderhaulSFR) this.multiblock).fuel;
            multiblock.recalculate();
        }
        if (!multiblock.isShapeEqual(this.multiblock)) return;
        for (Range<Block> range : settings.allowedBlocks) {
            for (Block block : ((Multiblock<Block>) multiblock).getBlocks()) {
                if (multiblock.count(block) > range.max)
                    multiblock.action(new SetblockAction(block.x, block.y, block.z, null), true, false);
            }
        }
        ALLOWED:
        for (Block block : ((Multiblock<Block>) multiblock).getBlocks()) {
            for (Range<Block> range : settings.allowedBlocks) {
                if (range.obj.isEqual(block)) continue ALLOWED;
            }
            multiblock.action(new SetblockAction(block.x, block.y, block.z, null), true, false);
        }
        currentBlock = (CuboidalMultiblock) multiblock.copy();
    }

    private <T extends Object> T choice(ArrayList<T> array) {
        return array.get(rand.nextInt(array.size()));
    }

    private <T extends Object> T rand(Multiblock multiblock, ArrayList<Range<T>> ranges) {
        if (ranges.isEmpty()) return null;
        for (Range<T> range : ranges) {
            if (range.min == 0 && range.max == Integer.MAX_VALUE) continue;
            if (multiblock.count(range.obj) < range.min) return range.obj;
        }
        Range<T> randRange = ranges.get(rand.nextInt(ranges.size()));
        if ((randRange.min != 0 || randRange.max != Integer.MAX_VALUE) && randRange.max != 0 && multiblock.count(randRange.obj) >= randRange.max) {
            return null;
        }
        return randRange.obj;
    }
}
