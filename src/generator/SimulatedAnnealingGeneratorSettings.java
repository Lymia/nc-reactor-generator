package generator;

import multiblock.Block;
import multiblock.Range;
import multiblock.ppe.PostProcessingEffect;
import multiblock.symmetry.Symmetry;
import planner.menu.component.generator.MenuComponentPostProcessingEffect;
import planner.menu.component.generator.MenuComponentPriority;
import planner.menu.component.generator.MenuComponentSymmetry;

import java.util.ArrayList;

public class SimulatedAnnealingGeneratorSettings implements Settings {
    public int finalMultiblocks, workingMultiblocks, timeout;
    public ArrayList<Priority> priorities = new ArrayList<>();
    public ArrayList<Symmetry> symmetries = new ArrayList<>();
    public ArrayList<PostProcessingEffect> postProcessingEffects = new ArrayList<>();
    public ArrayList<Range<Block>> allowedBlocks = new ArrayList<>();
    public float changeChancePercent;
    public boolean variableRate, lockCore, fillAir;
    private final SimulatedAnnealingGenerator generator;

    public SimulatedAnnealingGeneratorSettings(SimulatedAnnealingGenerator generator) {
        this.generator = generator;
    }

    public void refresh(SimulatedAnnealingGeneratorSettings settings) {
        allowedBlocks = settings.allowedBlocks;
        finalMultiblocks = settings.finalMultiblocks;
        workingMultiblocks = settings.workingMultiblocks;
        timeout = settings.timeout;
        priorities = settings.priorities;
        symmetries = settings.symmetries;
        postProcessingEffects = settings.postProcessingEffects;
        allowedBlocks = settings.allowedBlocks;
        changeChancePercent = settings.changeChancePercent;
        variableRate = settings.variableRate;
        lockCore = settings.lockCore;
        fillAir = settings.fillAir;
    }

    public void refresh(ArrayList<Range<Block>> allowedBlocks) {
        this.allowedBlocks = allowedBlocks;
        finalMultiblocks = 1;//Integer.parseInt(generator.finalMultiblockCount.text);
        workingMultiblocks = Integer.parseInt(generator.workingMultiblockCount.text);
        timeout = Integer.parseInt(generator.timeout.text);
        ArrayList<Symmetry> newSymmetries = new ArrayList<>();
        for (simplelibrary.opengl.gui.components.MenuComponent comp : generator.symmetriesList.components) {
            if (((MenuComponentSymmetry) comp).enabled) newSymmetries.add(((MenuComponentSymmetry) comp).symmetry);
        }
        symmetries = newSymmetries;
        ArrayList<Priority> newPriorities = new ArrayList<>();
        for (simplelibrary.opengl.gui.components.MenuComponent comp : generator.prioritiesList.components) {
            newPriorities.add(((MenuComponentPriority) comp).priority);
        }
        priorities = newPriorities;//to avoid concurrentModification
        ArrayList<PostProcessingEffect> newEffects = new ArrayList<>();
        for (simplelibrary.opengl.gui.components.MenuComponent comp : generator.postProcessingEffectsList.components) {
            if (((MenuComponentPostProcessingEffect) comp).enabled)
                newEffects.add(((MenuComponentPostProcessingEffect) comp).postProcessingEffect);
        }
        postProcessingEffects = newEffects;
        changeChancePercent = Float.parseFloat(generator.changeChance.text);
        variableRate = generator.variableRate.isToggledOn;
        lockCore = generator.lockCore.isToggledOn;
        fillAir = generator.fillAir.isToggledOn;
    }

    public float getChangeChance() {
        return changeChancePercent / 100;
    }

    @Override
    public ArrayList<Range<Block>> getAllowedBlocks() {
        return allowedBlocks;
    }
}