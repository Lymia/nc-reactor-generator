package generator.search;

import generator.Settings;
import multiblock.Block;
import multiblock.Range;
import multiblock.symmetry.Symmetry;
import planner.menu.component.generator.MenuComponentSymmetry;

import java.util.ArrayList;

public class NoveltySearchGeneratorSettings implements Settings {
    public ArrayList<Symmetry> symmetries = new ArrayList<>();
    public ArrayList<Range<Block>> allowedBlocks = new ArrayList<>();
    public int populationSize;
    public double archiveThreshold, changeChance, createAdjacentChance, createChance;

    public ArrayList<Block> fuelCell = new ArrayList<>();
    public ArrayList<Block> moderators = new ArrayList<>();
    public ArrayList<Block> heatSinks = new ArrayList<>();
    public ArrayList<Block> reflectors = new ArrayList<>();
    public ArrayList<Block> casingSources = new ArrayList<>();
    public ArrayList<Block> casingGlass = new ArrayList<>();
    public ArrayList<Block> conductors = new ArrayList<>();

    private final NoveltySearchGenerator generator;

    public NoveltySearchGeneratorSettings(NoveltySearchGenerator generator) {
        this.generator = generator;
    }

    public void refresh(NoveltySearchGeneratorSettings settings) {
        allowedBlocks = settings.allowedBlocks;
        symmetries = settings.symmetries;

        populationSize = settings.populationSize;
        archiveThreshold = settings.archiveThreshold;

        changeChance = settings.changeChance;
        createAdjacentChance = settings.createAdjacentChance;
        createChance = settings.createChance;

        fuelCell = settings.fuelCell;
        moderators = settings.moderators;
        heatSinks = settings.heatSinks;
        reflectors = settings.reflectors;
        casingSources = settings.casingSources;
        casingGlass = settings.casingGlass;
        conductors = settings.conductors;
    }

    public void refresh(ArrayList<Range<Block>> allowedBlocks) {
        // Sets the allowed blocks
        this.allowedBlocks = allowedBlocks;
        fuelCell.clear();
        moderators.clear();
        heatSinks.clear();
        reflectors.clear();
        casingSources.clear();
        casingGlass.clear();
        conductors.clear();
        for (var range : allowedBlocks) {
            var block = (multiblock.overhaul.fissionsfr.Block) range.obj;

            if (block.isFuelCell() || block.isFuelCellActive()) fuelCell.add(block);
            if ((block.isModerator() || block.isModeratorActive()) && !block.isShield()) moderators.add(block);
            if (block.isHeatsink() || block.isHeatsinkActive()) heatSinks.add(block);
            if (block.isReflector()) reflectors.add(block);
            if (block.template.casing && block.template.source) casingSources.add(block);
            if (block.template.casing && block.getName().contains("Glass")) casingGlass.add(block);
            if (block.template.cluster && block.getName().contains("Conductor")) conductors.add(block);
        }

        // Sets up post-processing effects.
        ArrayList<Symmetry> newSymmetries = new ArrayList<>();
        for (simplelibrary.opengl.gui.components.MenuComponent comp : generator.symmetriesList.components) {
            if (((MenuComponentSymmetry) comp).enabled) newSymmetries.add(((MenuComponentSymmetry) comp).symmetry);
        }
        symmetries = newSymmetries;
        changeChance = Float.parseFloat(generator.changeChancePercent.text) / 100;
        archiveThreshold = Float.parseFloat(generator.archiveNoveltyThreshold.text);
        createAdjacentChance = Float.parseFloat(generator.createAdjacentChancePercent.text) / 100;
        createChance = Float.parseFloat(generator.createChancePercent.text) / 100;
        populationSize = Integer.parseInt(generator.populationSize.text);
    }

    @Override
    public ArrayList<Range<Block>> getAllowedBlocks() {
        return allowedBlocks;
    }
}