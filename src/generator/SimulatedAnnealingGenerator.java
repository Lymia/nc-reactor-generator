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
import multiblock.overhaul.turbine.OverhaulTurbine;
import multiblock.ppe.PostProcessingEffect;
import multiblock.symmetry.Symmetry;
import multiblock.underhaul.fissionsfr.UnderhaulSFR;
import planner.exception.MissingConfigurationEntryException;
import planner.menu.component.*;
import planner.menu.component.generator.MenuComponentPostProcessingEffect;
import planner.menu.component.generator.MenuComponentPriority;
import planner.menu.component.generator.MenuComponentSymmetry;
import simplelibrary.opengl.gui.components.MenuComponent;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A generator based on the simulated annealing algorithm.
 */
public class SimulatedAnnealingGenerator extends MultiblockGenerator {
    MenuComponentMinimaList prioritiesList;
    MenuComponentMinimalistButton moveUp;
    MenuComponentMinimalistButton moveDown;
    MenuComponentMinimaList symmetriesList;
    MenuComponentMinimaList postProcessingEffectsList;
    MenuComponentMinimalistTextBox changeChance;
    MenuComponentToggleBox lockCore;

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
        return new ArrayList[]{ displayList, new ArrayList() };
    }

    @Override
    public boolean canGenerateFor(Multiblock multiblock) {
        return multiblock instanceof CuboidalMultiblock && !(multiblock instanceof OverhaulTurbine);
    }

    @Override
    public String getName() {
        return "Simulated Annealing";
    }

    @Override
    public void addSettings(MenuComponentMinimaList generatorSettings, Multiblock multi) {
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Priorities", true));
        prioritiesList = generatorSettings.add(new MenuComponentMinimaList(0, 0, 0, priorities.size() * 32, 24) {
            @Override
            public void render(int millisSinceLastTick) {
                for (simplelibrary.opengl.gui.components.MenuComponent c : components) {
                    c.width = width - (hasVertScrollbar() ? vertScrollbarWidth : 0);
                }
                super.render(millisSinceLastTick);
            }
        });
        refreshPriorities();
        MenuComponent priorityButtonHolder = generatorSettings.add(new MenuComponent(0, 0, 0, 32) {
            @Override
            public void renderBackground() {
                components.get(1).x = width / 2;
                components.get(0).width = components.get(1).width = width / 2;
                components.get(0).height = components.get(1).height = height;
            }

            @Override
            public void render() {}
        });
        moveUp =
                priorityButtonHolder.add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Move Up", true, true).setTooltip("Move the selected priority up so it is more important"));
        moveUp.addActionListener((e) -> {
            int index = prioritiesList.getSelectedIndex();
            if (index == -1 || index == 0) return;
            priorities.add(index - 1, priorities.remove(index));
            refreshPriorities();
            prioritiesList.setSelectedIndex(index - 1);
        });
        moveDown =
                priorityButtonHolder.add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Move Down", true, true).setTooltip("Move the selected priority down so it is less important"));
        moveDown.addActionListener((e) -> {
            int index = prioritiesList.getSelectedIndex();
            if (index == -1 || index == priorities.size() - 1) return;
            priorities.add(index + 1, priorities.remove(index));
            refreshPriorities();
            prioritiesList.setSelectedIndex(index + 1);
        });
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Generator Settings", true));
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 24, "Change Chance", true));
        changeChance =
                generatorSettings.add(new MenuComponentMinimalistTextBox(0, 0, 0, 32, "1", true).setFloatFilter(0f,
                        100f).setSuffix("%")).setTooltip("If variable rate is on: Each iteration, each block in the " +
                        "reactor has an x% chance of changing\nIf variable rate is off: Each iteration, exactly x% of" +
                        " the blocks in the reactor will change (minimum of 1)");
        lockCore = generatorSettings.add(new MenuComponentToggleBox(0, 0, 0, 32, "Lock Core", false));
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Symmetry Settings", true));
        ArrayList<Symmetry> symmetries = multi.getSymmetries();
        symmetriesList = generatorSettings.add(new MenuComponentMinimaList(0, 0, 0, symmetries.size() * 32, 24));
        for (Symmetry symmetry : symmetries) {
            symmetriesList.add(new MenuComponentSymmetry(symmetry));
        }
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Post-Processing", true));
        ArrayList<PostProcessingEffect> postProcessingEffects = multi.getPostProcessingEffects();
        postProcessingEffectsList = generatorSettings.add(new MenuComponentMinimaList(0, 0, 0,
                postProcessingEffects.size() * 32, 24));
        for (PostProcessingEffect postProcessingEffect : postProcessingEffects) {
            postProcessingEffectsList.add(new MenuComponentPostProcessingEffect(postProcessingEffect));
        }
    }

    @Override
    public MultiblockGenerator newInstance(Multiblock multi) {
        return new SimulatedAnnealingGenerator(multi);
    }

    private void refreshPriorities() {
        prioritiesList.components.clear();
        for (Priority priority : priorities) {
            prioritiesList.add(new MenuComponentPriority(priority));
        }
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

    /**
     * @return A copy of the current multiblock.
     */
    private CuboidalMultiblock getCopyOfCurrent() {
        synchronized (currentBlockLock) {
            return (CuboidalMultiblock) currentBlock.copy();
        }
    }

    /**
     * Mutates a multiblock, randomly changing the contents.
     */
    private void mutateMultiblock(CuboidalMultiblock cm) {
        final var changeChance = (rand.nextDouble() * 0.75 + 0.25) * settings.getChangeChance();
        cm.forEachInternalPosition((x, y, z) -> {
            var b = cm.getBlock(x, y, z);
            if (settings.lockCore && b != null && b.isCore()) return;
            if (rand.nextDouble() < changeChance) {
                var randBlock = rand(cm, settings.allowedBlocks);
                if (randBlock == null || settings.lockCore && randBlock.isCore() || !cm.canBePlacedWithinCasing(randBlock))
                    return;
                cm.queueAction(new SetblockAction(x, y, z, applyMultiblockSpecificSettings(cm,
                        randBlock.newInstance(x, y, z))));
            }
        });
    }

    /**
     * Applies post-processing passes to the generated multiblock.
     */
    private void postProcessMultiblock(CuboidalMultiblock cm) {
        cm.buildDefaultCasing();
        cm.performActions(false);
        for (var effect : settings.postProcessingEffects) {
            if (effect.preSymmetry) cm.action(new PostProcessingAction(effect, settings), true, false);
        }
        for (var symmetry : settings.symmetries) {
            cm.queueAction(new SymmetryAction(symmetry));
        }
        cm.performActions(false);
        cm.recalculate();
        for (var effect : settings.postProcessingEffects) {
            if (effect.postSymmetry) cm.action(new PostProcessingAction(effect, settings), true, false);
        }
    }

    /**
     * @return Returns a neighbor of the current multiblock.
     */
    private CuboidalMultiblock getNeighbor() {
        var cm = getCopyOfCurrent();
        mutateMultiblock(cm);
        postProcessMultiblock(cm);
        return cm;
    }

    /**
     * Replaces the current multiblock with the parameter.
     */
    private void acceptBlock(CuboidalMultiblock cm) {
        currentBlock = cm;
        displayList.set(0, cm);
    }

    @Override
    public void tick() {
        CuboidalMultiblock neighbor = getNeighbor();
        synchronized (currentBlockLock) {
            acceptBlock(neighbor);
        }
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
