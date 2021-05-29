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
    public ArrayList<Priority> priorities = new ArrayList<>();
    public ArrayList<Symmetry> symmetries = new ArrayList<>();
    public ArrayList<PostProcessingEffect> postProcessingEffects = new ArrayList<>();
    public ArrayList<Range<Block>> allowedBlocks = new ArrayList<>();
    public float changeChancePercent;
    public boolean lockCore;
    private final SimulatedAnnealingGenerator generator;

    public SimulatedAnnealingGeneratorSettings(SimulatedAnnealingGenerator generator) {
        this.generator = generator;
    }

    public void refresh(SimulatedAnnealingGeneratorSettings settings) {
        allowedBlocks = settings.allowedBlocks;
        priorities = settings.priorities;
        symmetries = settings.symmetries;
        postProcessingEffects = settings.postProcessingEffects;
        allowedBlocks = settings.allowedBlocks;
        changeChancePercent = settings.changeChancePercent;
        lockCore = settings.lockCore;
    }

    public void refresh(ArrayList<Range<Block>> allowedBlocks) {
        this.allowedBlocks = allowedBlocks;
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
        lockCore = generator.lockCore.isToggledOn;
    }

    public float getChangeChance() {
        return changeChancePercent / 100;
    }

    @Override
    public ArrayList<Range<Block>> getAllowedBlocks() {
        return allowedBlocks;
    }
}