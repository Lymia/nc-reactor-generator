package generator;

import multiblock.Block;
import multiblock.Multiblock;
import multiblock.Range;
import multiblock.action.SetblockAction;
import multiblock.overhaul.fissionmsr.OverhaulMSR;
import multiblock.overhaul.fissionsfr.OverhaulSFR;
import multiblock.overhaul.turbine.OverhaulTurbine;
import multiblock.underhaul.fissionsfr.UnderhaulSFR;
import planner.exception.MissingConfigurationEntryException;

import java.util.ArrayList;

/**
 * Implements some functions used across all reactor generators.
 */
public abstract class ReactorGeneratorCommon extends MultiblockGenerator {
    public ReactorGeneratorCommon(Multiblock multiblock) {
        super(multiblock);
    }

    protected abstract void finalize(Multiblock worst);

    protected Block applyMultiblockSpecificSettings(Multiblock currentMultiblock, Block randBlock) {
        if (multiblock instanceof UnderhaulSFR) return randBlock;//no block-specifics here!
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
        if (multiblock instanceof OverhaulTurbine) return randBlock;//also no block-specifics!
        throw new IllegalArgumentException("Unknown multiblock: " + multiblock.getDefinitionName());
    }

    @Override
    public void importMultiblock(Multiblock multiblock) throws MissingConfigurationEntryException {
        var settings = getSettings();
        multiblock.convertTo(this.multiblock.getConfiguration());
        if(multiblock instanceof UnderhaulSFR){
            multiblock = multiblock.copy();
            ((UnderhaulSFR)multiblock).fuel = ((UnderhaulSFR)this.multiblock).fuel;
            multiblock.recalculate();
        }
        if(!multiblock.isShapeEqual(this.multiblock))return;
        for(Range<Block> range : settings.getAllowedBlocks()){
            for(Block block : ((Multiblock<Block>)multiblock).getBlocks()){
                if(multiblock.count(block)>range.max)multiblock.action(new SetblockAction(block.x, block.y, block.z, null), true, false);
            }
        }
        ALLOWED:for(Block block : ((Multiblock<Block>)multiblock).getBlocks()){
            for(Range<Block> range : settings.getAllowedBlocks()){
                if(range.obj.isEqual(block))continue ALLOWED;
            }
            multiblock.action(new SetblockAction(block.x, block.y, block.z, null), true, false);
        }
        finalize(multiblock);
    }
}
