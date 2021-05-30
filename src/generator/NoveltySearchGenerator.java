package generator;

import multiblock.Block;
import multiblock.CuboidalMultiblock;
import multiblock.Multiblock;
import multiblock.Range;
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
import java.util.Arrays;
import java.util.Comparator;

public class NoveltySearchGenerator extends MultiblockGenerator {
    private static final class NoveltyVector {
        private final double[] components;
        public NoveltyVector(double... components) {
            this.components = components;
        }
        public double dist(NoveltyVector other) {
            if (components.length != other.components.length) return Float.NaN;
            else {
                double accum = 0;
                for (int i = 0; i < components.length; i++) {
                    var component = components[i] - other.components[i];
                    accum += component * component;
                }
                return Math.sqrt(accum);
            }
        }

        @Override
        public String toString() {
            return "NoveltyVector{" + "components=" + Arrays.toString(components) + '}';
        }
    }
    private static final class PoolEntry {
        public final CuboidalMultiblock block;
        public final NoveltyVector vector;
        public boolean hasOutput = false;
        public double currentNovelty = 0.0;
        public PoolEntry(CuboidalMultiblock block, NoveltyVector vector) {
            this.block = block;
            this.vector = vector;
        }
    }
    private static final class CalculateNovelty {
        private final NoveltyVector vec;
        private final double[] closest;
        private int maxIndex = -1;

        public CalculateNovelty(NoveltyVector vec, int closestCount) {
            this.vec = vec;
            this.closest = new double[closestCount];
        }
        private void addVector(NoveltyVector vec) {
            var dist = this.vec.dist(vec);
            if (maxIndex == -1) {
                Arrays.fill(closest, dist);
                maxIndex = 1;
            } else if (dist < closest[maxIndex]) {
                closest[maxIndex] = dist;

                int maxAt = 0;
                for (int i = 0; i < closest.length; i++) {
                    maxAt = closest[i] > closest[maxAt] ? i : maxAt;
                }
                maxIndex = maxAt;
            }
        }
        public void addPool(ArrayList<PoolEntry> pool) {
            // this can, in fact, change on us! we use this to avoid iteration errors
            var len = pool.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < len; i++) addVector(pool.get(i).vector);
        }
        public double calc() {
            double accum = 0;
            for (var d : closest) accum += d;
            return accum / closest.length;
        }
    }

    MenuComponentMinimaList symmetriesList;
    MenuComponentMinimalistTextBox populationSize;
    MenuComponentMinimalistTextBox archiveNoveltyThreshold;
    MenuComponentMinimalistTextBox changeChancePercent;
    MenuComponentMinimalistTextBox createAdjacentChancePercent;
    MenuComponentMinimalistTextBox createChancePercent;

    private final NoveltySearchGeneratorSettings settings = new NoveltySearchGeneratorSettings(this);

    // TODO: Use spatial hashes for these? maybe not worth the effort, rewrite later in Rust
    private int generation = 1;
    private CuboidalMultiblock archiveBest;
    private final ArrayList<Multiblock> displayBest = new ArrayList<>();
    private ArrayList<PoolEntry> noveltyPool = new ArrayList<>();
    private final ArrayList<PoolEntry> newGeneration = new ArrayList<>();
    private final ArrayList<PoolEntry> archive = new ArrayList<>();

    public NoveltySearchGenerator(Multiblock multiblock) {
        super(multiblock);
    }

    @Override
    public ArrayList<Multiblock>[] getMultiblockLists() {
        return new ArrayList[]{displayBest, new ArrayList()};
    }

    @Override
    public boolean canGenerateFor(Multiblock multiblock) {
        return multiblock instanceof OverhaulSFR;
    }

    @Override
    public String getName() {
        return "Novelty Search";
    }

    @Override
    public void addSettings(MenuComponentMinimaList generatorSettings, Multiblock multi) {
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Population Settings", true));
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 24, "Population Size", true));
        populationSize = generatorSettings.add(
                new MenuComponentMinimalistTextBox(0, 0, 0, 32, "750", true).setIntFilter())
                .setTooltip("The size of the internal population.");
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 24, "Archive Novelty Threshold", true));
        archiveNoveltyThreshold = generatorSettings.add(
                new MenuComponentMinimalistTextBox(0, 0, 0, 32, "1.25", true).setFloatFilter())
                .setTooltip("The size of the internal population.");

        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Mutation Settings", true));
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 24, "Change Chance", true));
        changeChancePercent = generatorSettings.add(
                new MenuComponentMinimalistTextBox(0, 0, 0, 32, "10", true).setFloatFilter(0f, 100f).setSuffix("%"))
                .setTooltip("The maximum percentage of blocks to change every iteration.");
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 24, "Fill Air Chance", true));
        createAdjacentChancePercent = generatorSettings.add(
                new MenuComponentMinimalistTextBox(0, 0, 0, 32, "0.5", true).setFloatFilter(0f, 100f).setSuffix("%"))
                .setTooltip("The maximum percentage of spaces adjacent to blocks to fill every iteration.");
        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 24, "Create Tile Change", true));
        createChancePercent = generatorSettings.add(
                new MenuComponentMinimalistTextBox(0, 0, 0, 32, "0.001", true).setFloatFilter(0f, 100f).setSuffix("%"))
                .setTooltip("The maximum percentage of spaces to fill every iteration.");

        generatorSettings.add(new MenuComponentLabel(0, 0, 0, 32, "Symmetry Settings", true));
        ArrayList<Symmetry> symmetries = multi.getSymmetries();
        symmetriesList = generatorSettings.add(new MenuComponentMinimaList(0, 0, 0, symmetries.size() * 32, 24));
        for (Symmetry symmetry : symmetries) {
            symmetriesList.add(new MenuComponentSymmetry(symmetry));
        }
    }

    @Override
    public MultiblockGenerator newInstance(Multiblock multi) {
        return new NoveltySearchGenerator(multi);
    }

    @Override
    public void refreshSettingsFromGUI(ArrayList<Range<Block>> allowedBlocks) {
        settings.refresh(allowedBlocks);
    }

    @Override
    public void refreshSettings(Settings settings) {
        if (settings instanceof NoveltySearchGeneratorSettings) {
            this.settings.refresh((NoveltySearchGeneratorSettings) settings);
        } else throw new IllegalArgumentException("Passed invalid settings to Simulated Annealing generator!");
    }

    private double clampSqrt(double x) {
        if (x < 0) return Math.max(-Math.sqrt(-x), -50.0);
        else return Math.min(Math.sqrt(x), 50.0);
    }
    private NoveltyVector computeNoveltyVector(CuboidalMultiblock cm) {
        if (cm instanceof OverhaulSFR) {
            var sfr = (OverhaulSFR) cm;

            return new NoveltyVector(
                    clampSqrt(sfr.totalOutput) * 0.25,
                    clampSqrt(sfr.totalHeat) * 0.15,
                    clampSqrt(sfr.totalCooling) * 0.15,
                    sfr.totalEfficiency * 2.5,
                    clampSqrt(sfr.totalIrradiation),
                    clampSqrt(sfr.clusters.size()) * 1.5
            );
        } else {
            throw new RuntimeException();
        }
    }

    private synchronized double computeNovelty(NoveltyVector vec) {
        var calc = new CalculateNovelty(vec, 2); // TODO: Parameterize
        calc.addPool(archive);
        calc.addPool(noveltyPool);
        return calc.calc();
    }
    private synchronized double finalScore(CuboidalMultiblock cm) {
        if (cm instanceof OverhaulSFR) {
            var sfr = (OverhaulSFR) cm;
            for (var cluster : sfr.clusters)
                if (cluster.netHeat > 0) return 0;
            return Math.log(sfr.totalOutput + 1) * sfr.totalEfficiency;
        } else {
            return 0;
        }
    }
    private synchronized void newGeneration() {
        System.err.println("Running generation "+generation+"... (archive size = "+archive.size()+")");
        generation++;

        // Computes the new novelty set.
        var all = new ArrayList<PoolEntry>();
        all.addAll(noveltyPool);
        all.addAll(newGeneration);
        for (var design : all) {
            var nov = computeNovelty(design.vector);
            nov += rand.nextGaussian() * 1.25; // add some randomness to the novelty search
            design.currentNovelty = nov;
        }

        // sets up the best designs in the pool
        double bestEfficiencyFactor = 0;
        CuboidalMultiblock best = null;
        CuboidalMultiblock best2 = null;
        outer: for (var design : all) {
            var score = finalScore(design.block);
            if (score > bestEfficiencyFactor) {
                bestEfficiencyFactor = score;
                best2 = best;
                best = design.block;
            }
        }

        // updates the archive best
        if (best != null && (archiveBest == null || finalScore(best) > finalScore(archiveBest)))
            archiveBest = best;

        // copies best designs to the output
        var arr = new CuboidalMultiblock[] { archiveBest, best, best2 };
        for (int i = 0; i < arr.length; i ++) {
            if (arr[i] != null) {
                var copy = (CuboidalMultiblock) arr[i];
                prepareForBest(copy);
                displayBest.set(i, copy);
            } else {
                displayBest.set(i, all.get(i).block.copy());
            }
        }

        // creates the novelty pool
        all.sort(Comparator.comparing(c -> -c.currentNovelty));
        newGeneration.clear();
        noveltyPool = new ArrayList<>(all.subList(0, settings.populationSize));
    }
    private synchronized void pushNewSync(PoolEntry entry) {
        if (entry.currentNovelty > settings.archiveThreshold) {
            System.err.println(" - New novel entry: "+entry.vector+", novelty = "+entry.currentNovelty);
            archive.add(entry);
        }
        newGeneration.add(entry);
        if (newGeneration.size() == settings.populationSize) newGeneration();
    }
    private void pushNew(CuboidalMultiblock cm) {
        var entry = new PoolEntry(cm, computeNoveltyVector(cm));
        entry.currentNovelty = computeNovelty(entry.vector);
        if (cm instanceof OverhaulSFR) {
            var sfr = (OverhaulSFR) cm;
            if (sfr.totalOutput >= 0.5) entry.hasOutput = true;
        }
        pushNewSync(entry);
    }

    private void mutateMultiblock(CuboidalMultiblock cm, boolean internal) {
        // TODO: Implement new parameters
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
        if(internal) cm.forEachInternalPosition((x, y, z) -> {
            var b = cm.getBlock(x, y, z);
            if (rand.nextDouble() < changeChance) {
                Block randBlock = null;
                var typeChance = rand.nextDouble();
                if (typeChance <= 0.4) {
                    // Place moderator 40% of the time.
                    randBlock = choice(settings.moderators);
                } else if (typeChance <= 0.65) {
                    // Place heat sinks 25% of the time.
                    randBlock = choice(settings.heatSinks);
                } else if (typeChance <= 0.75) {
                    // Place reflectors 10% of the time.
                    randBlock = choice(settings.reflectors);
                } else if (typeChance <= 0.95) {
                    // Place nothing 20% of the time.
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
    private void crossoverMultiblock(CuboidalMultiblock cm1, CuboidalMultiblock cm2) {
        var diffX = rand.nextDouble() < 0.15 ? rand.nextInt(3) - 1 : 0;
        var diffY = rand.nextDouble() < 0.15 ? rand.nextInt(3) - 1 : 0;
        var diffZ = rand.nextDouble() < 0.15 ? rand.nextInt(3) - 1 : 0;
        var proportion = rand.nextDouble();
        cm1.forEachInternalPosition((x, y, z) -> {
            if (rand.nextDouble() < proportion) {
                var other = cm2.getBlock(x + diffX, y + diffY, z + diffZ);
                if (other == null || cm1.canBePlacedWithinCasing(other))
                    cm1.queueAction(new SetblockAction(x, y, z, other));
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
    }

    private void prepareForBest(CuboidalMultiblock cm) {
        removeInvalidPass(cm);
        cm.performActions(false);
        cm.recalculate();
    }

    private CuboidalMultiblock getRandom() {
        CuboidalMultiblock cm;
        if (rand.nextDouble() < 0.2 && archive.size() != 0) {
            var idx = rand.nextInt(archive.size());
            cm = (CuboidalMultiblock) archive.get(idx).block.copy();
        } else {
            var idx = rand.nextInt(rand.nextInt(settings.populationSize) + 1);
            cm = (CuboidalMultiblock) noveltyPool.get(idx).block.copy();
        }
        return cm;
    }
    private CuboidalMultiblock getMutated() {
        CuboidalMultiblock cm = getRandom();
        mutateMultiblock(cm, true);
        postProcessMultiblock(cm);
        return cm;
    }
    private CuboidalMultiblock getCrossover() {
        CuboidalMultiblock cm = getRandom();
        crossoverMultiblock(cm, getRandom());
        mutateMultiblock(cm, false);
        postProcessMultiblock(cm);
        return cm;
    }

    @Override
    public void tick() {
        CuboidalMultiblock neighbor = rand.nextDouble() < 0.005 ? getCrossover() : getMutated();
        pushNew(neighbor);
        countIteration();
    }

    private Block applyMultiblockSpecificSettings(Multiblock currentMultiblock, Block randBlock) {
        if (multiblock instanceof OverhaulSFR) {
            multiblock.overhaul.fissionsfr.Block block = (multiblock.overhaul.fissionsfr.Block) randBlock;
            if (!block.template.allRecipes.isEmpty()) {
                ArrayList<Range<multiblock.configuration.overhaul.fissionsfr.BlockRecipe>> validRecipes =
                        new ArrayList<>(((OverhaulSFR) multiblock).getValidRecipes());
                validRecipes.removeIf(next -> !block.template.allRecipes.contains(next.obj));
                if (!validRecipes.isEmpty()) block.recipe = rand(currentMultiblock, validRecipes);
            }
            return randBlock;
        }
        if (multiblock instanceof OverhaulMSR) {
            multiblock.overhaul.fissionmsr.Block block = (multiblock.overhaul.fissionmsr.Block) randBlock;
            if (!block.template.allRecipes.isEmpty()) {
                ArrayList<Range<multiblock.configuration.overhaul.fissionmsr.BlockRecipe>> validRecipes =
                        new ArrayList<>(((OverhaulMSR) multiblock).getValidRecipes());
                validRecipes.removeIf(next -> !block.template.allRecipes.contains(next.obj));
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

        // setup state
        synchronized (this) {
            var cm = (CuboidalMultiblock) multiblock.blankCopy();
            var vec = computeNoveltyVector(cm);
            noveltyPool.clear();
            for (int i = 0; i < settings.populationSize; i++) {
                var cur = (CuboidalMultiblock) cm.copy();
                for (int j = 0; j < 5; j++) mutateMultiblock(cm, true);
                noveltyPool.add(new PoolEntry(cur, vec));
            }

            displayBest.clear();
            displayBest.add(cm);
            displayBest.add(cm);
            displayBest.add(cm);
            archiveBest = cm;
        }
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
