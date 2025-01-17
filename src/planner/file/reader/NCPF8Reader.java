package planner.file.reader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import multiblock.CuboidalMultiblock;
import multiblock.Multiblock;
import multiblock.configuration.Configuration;
import multiblock.configuration.PartialConfiguration;
import multiblock.configuration.overhaul.OverhaulConfiguration;
import multiblock.configuration.underhaul.UnderhaulConfiguration;
import multiblock.overhaul.fissionmsr.OverhaulMSR;
import multiblock.overhaul.fissionsfr.OverhaulSFR;
import multiblock.overhaul.fusion.OverhaulFusionReactor;
import multiblock.overhaul.turbine.OverhaulTurbine;
import multiblock.underhaul.fissionsfr.UnderhaulSFR;
import planner.file.FormatReader;
import planner.file.NCPFFile;
import simplelibrary.config2.Config;
import simplelibrary.config2.ConfigList;
import simplelibrary.config2.ConfigNumberList;
import simplelibrary.image.Image;
public class NCPF8Reader implements FormatReader{
    @Override
    public boolean formatMatches(InputStream in){
        try{
            Config header = Config.newConfig();
            header.load(in);
            in.close();
            return header.get("version", (byte)0)==(byte)8;
        }catch(Throwable t){
            return false;
        }
    }
    HashMap<multiblock.configuration.underhaul.fissionsfr.PlacementRule, Byte> underhaulPostLoadMap = new HashMap<>();
    HashMap<multiblock.configuration.overhaul.fissionsfr.PlacementRule, Byte> overhaulSFRPostLoadMap = new HashMap<>();
    HashMap<multiblock.configuration.overhaul.fissionmsr.PlacementRule, Byte> overhaulMSRPostLoadMap = new HashMap<>();
    HashMap<multiblock.configuration.overhaul.turbine.PlacementRule, Byte> overhaulTurbinePostLoadMap = new HashMap<>();
    HashMap<multiblock.configuration.overhaul.fusion.PlacementRule, Byte> overhaulFusionPostLoadMap = new HashMap<>();
    HashMap<OverhaulTurbine, ArrayList<Integer>> overhaulTurbinePostLoadInputsMap = new HashMap<>();
    @Override
    public synchronized NCPFFile read(InputStream in){
        overhaulTurbinePostLoadInputsMap.clear();
        try{
            NCPFFile ncpf = new NCPFFile();
            Config header = Config.newConfig();
            header.load(in);
            int multiblocks = header.get("count");
            if(header.hasProperty("metadata")){
                Config metadata = header.get("metadata");
                for(String key : metadata.properties()){
                    ncpf.metadata.put(key, metadata.get(key));
                }
            }
            Config config = Config.newConfig();
            config.load(in);
            ncpf.configuration = loadConfiguration(config);
            for(int i = 0; i<multiblocks; i++){
                Config data = Config.newConfig();
                data.load(in);
                Multiblock multiblock;
                int id = data.get("id");
                switch(id){
                    case 0:
                        //<editor-fold defaultstate="collapsed" desc="Underhaul SFR">
                        ConfigNumberList size = data.get("size");
                        UnderhaulSFR underhaulSFR = new UnderhaulSFR(ncpf.configuration, (int)size.get(0),(int)size.get(1),(int)size.get(2),ncpf.configuration.underhaul.fissionSFR.allFuels.get(data.get("fuel", (byte)-1)));
                        boolean compact = data.get("compact");
                        ConfigNumberList blocks = data.get("blocks");
                        if(compact){
                            int[] index = new int[1];
                            underhaulSFR.forEachInternalPosition((x, y, z) -> {
                                int bid = (int) blocks.get(index[0]);
                                if(bid>0)underhaulSFR.setBlockExact(x, y, z, new multiblock.underhaul.fissionsfr.Block(ncpf.configuration, x, y, z, ncpf.configuration.underhaul.fissionSFR.allBlocks.get(bid-1)));
                                index[0]++;
                            });
                        }else{
                            for(int j = 0; j<blocks.size(); j+=4){
                                int x = (int) blocks.get(j)+1;
                                int y = (int) blocks.get(j+1)+1;
                                int z = (int) blocks.get(j+2)+1;
                                int bid = (int) blocks.get(j+3);
                                underhaulSFR.setBlockExact(x, y, z, new multiblock.underhaul.fissionsfr.Block(ncpf.configuration, x, y, z, ncpf.configuration.underhaul.fissionSFR.allBlocks.get(bid-1)));
                            }
                        }
                        multiblock = underhaulSFR;
//</editor-fold>
                        break;
                    case 1:
                        //<editor-fold defaultstate="collapsed" desc="Overhaul SFR">
                        size = data.get("size");
                        OverhaulSFR overhaulSFR = new OverhaulSFR(ncpf.configuration, (int)size.get(0),(int)size.get(1),(int)size.get(2),ncpf.configuration.overhaul.fissionSFR.allCoolantRecipes.get(data.get("coolantRecipe", (byte)-1)));
                        compact = data.get("compact");
                        blocks = data.get("blocks");
                        if(compact){
                            int[] index = new int[1];
                            overhaulSFR.forEachInternalPosition((x, y, z) -> {
                                int bid = (int) blocks.get(index[0]);
                                if(bid>0){
                                    overhaulSFR.setBlockExact(x, y, z, new multiblock.overhaul.fissionsfr.Block(ncpf.configuration, x, y, z, ncpf.configuration.overhaul.fissionSFR.allBlocks.get(bid-1)));
                                }
                                index[0]++;
                            });
                        }else{
                            for(int j = 0; j<blocks.size(); j+=4){
                                int x = (int) blocks.get(j)+1;
                                int y = (int) blocks.get(j+1)+1;
                                int z = (int) blocks.get(j+2)+1;
                                int bid = (int) blocks.get(j+3);
                                overhaulSFR.setBlockExact(x, y, z, new multiblock.overhaul.fissionsfr.Block(ncpf.configuration, x, y, z, ncpf.configuration.overhaul.fissionSFR.allBlocks.get(bid-1)));
                            }
                        }
                        ConfigNumberList fuels = data.get("fuels");
                        ConfigNumberList sources = data.get("sources");
                        ConfigNumberList irradiatorRecipes = data.get("irradiatorRecipes");
                        int fuelIndex = 0;
                        int sourceIndex = 0;
                        int recipeIndex = 0;
                        ArrayList<multiblock.configuration.overhaul.fissionsfr.Block> srces = new ArrayList<>();
                        for(multiblock.configuration.overhaul.fissionsfr.Block bl : ncpf.configuration.overhaul.fissionSFR.allBlocks){
                            if(bl.source)srces.add(bl);
                        }
                        for(multiblock.overhaul.fissionsfr.Block block : overhaulSFR.getBlocks()){
                            if(block.template.fuelCell){
                                block.recipe = block.template.allRecipes.get((int)fuels.get(fuelIndex));
                                fuelIndex++;
                                int sid = (int) sources.get(sourceIndex);
                                if(sid>0)block.addNeutronSource(overhaulSFR, srces.get(sid-1));
                                sourceIndex++;
                            }
                            if(block.template.irradiator){
                                int rid = (int) irradiatorRecipes.get(recipeIndex);
                                if(rid>0)block.recipe = block.template.allRecipes.get(rid-1);
                                recipeIndex++;
                            }
                        }
                        multiblock = overhaulSFR;
//</editor-fold>
                        break;
                    case 2:
                        //<editor-fold defaultstate="collapsed" desc="Overhaul MSR">
                        size = data.get("size");
                        OverhaulMSR overhaulMSR = new OverhaulMSR(ncpf.configuration, (int)size.get(0),(int)size.get(1),(int)size.get(2));
                        compact = data.get("compact");
                        blocks = data.get("blocks");
                        if(compact){
                            int[] index = new int[1];
                            overhaulMSR.forEachInternalPosition((x, y, z) -> {
                                int bid = (int) blocks.get(index[0]);
                                if(bid>0){
                                    overhaulMSR.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(ncpf.configuration, x, y, z, ncpf.configuration.overhaul.fissionMSR.allBlocks.get(bid-1)));
                                }
                                index[0]++;
                            });
                        }else{
                            for(int j = 0; j<blocks.size(); j+=4){
                                int x = (int) blocks.get(j)+1;
                                int y = (int) blocks.get(j+1)+1;
                                int z = (int) blocks.get(j+2)+1;
                                int bid = (int) blocks.get(j+3);
                                overhaulMSR.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(ncpf.configuration, x, y, z, ncpf.configuration.overhaul.fissionMSR.allBlocks.get(bid-1)));
                            }
                        }
                        fuels = data.get("fuels");
                        sources = data.get("sources");
                        irradiatorRecipes = data.get("irradiatorRecipes");
                        fuelIndex = 0;
                        sourceIndex = 0;
                        recipeIndex = 0;
                        ArrayList<multiblock.configuration.overhaul.fissionmsr.Block> msrces = new ArrayList<>();
                        for(multiblock.configuration.overhaul.fissionmsr.Block bl : ncpf.configuration.overhaul.fissionMSR.allBlocks){
                            if(bl.source)msrces.add(bl);
                        }
                        for(multiblock.overhaul.fissionmsr.Block block : overhaulMSR.getBlocks()){
                            if(block.template.fuelVessel){
                                block.recipe = block.template.allRecipes.get((int)fuels.get(fuelIndex));
                                fuelIndex++;
                                int sid = (int) sources.get(sourceIndex);
                                if(sid>0)block.addNeutronSource(overhaulMSR, msrces.get(sid-1));
                                sourceIndex++;
                            }
                            if(block.template.irradiator){
                                int rid = (int) irradiatorRecipes.get(recipeIndex);
                                if(rid>0)block.recipe = block.template.allRecipes.get(rid-1);
                                recipeIndex++;
                            }
                            if(block.template.heater&&!block.template.allRecipes.isEmpty())block.recipe = block.template.allRecipes.get(0);
                        }
                        multiblock = overhaulMSR;
//</editor-fold>
                        break;
                    case 3:
                        //<editor-fold defaultstate="collapsed" desc="Overhaul Turbine">
                        size = data.get("size");
                        OverhaulTurbine overhaulTurbine = new OverhaulTurbine(ncpf.configuration, (int)size.get(0), (int)size.get(1), ncpf.configuration.overhaul.turbine.allRecipes.get(data.get("recipe", (byte)-1)));
                        overhaulTurbine.setBearing((int)size.get(2));
                        if(data.hasProperty("inputs")){
                            overhaulTurbinePostLoadInputsMap.put(overhaulTurbine, new ArrayList<>());
                            ConfigNumberList inputs = data.get("inputs");
                            for(Number number : inputs.iterable()){
                                overhaulTurbinePostLoadInputsMap.get(overhaulTurbine).add(number.intValue());
                            }
                        }
                        ArrayList<multiblock.configuration.overhaul.turbine.Block> allCoils = new ArrayList<>();
                        ArrayList<multiblock.configuration.overhaul.turbine.Block> allBlades = new ArrayList<>();
                        for(multiblock.configuration.overhaul.turbine.Block b : ncpf.configuration.overhaul.turbine.allBlocks){
                            if(b.blade)allBlades.add(b);
                            else allCoils.add(b);
                        }
                        ConfigNumberList coils = data.get("coils");
                        int index = 0;
                        for(int z = 0; z<2; z++){
                            if(z==1)z = overhaulTurbine.getExternalDepth()-1;
                            for(int x = 1; x<=overhaulTurbine.getInternalWidth(); x++){
                                for(int y = 1; y<=overhaulTurbine.getInternalHeight(); y++){
                                    int bid = (int) coils.get(index);
                                    if(bid>0){
                                        overhaulTurbine.setBlockExact(x, y, z, new multiblock.overhaul.turbine.Block(ncpf.configuration, x, y, z, allCoils.get(bid-1)));
                                    }
                                    index++;
                                }
                            }
                        }
                        ConfigNumberList blades = data.get("blades");
                        index = 0;
                        for(int z = 1; z<=overhaulTurbine.getInternalDepth(); z++){
                            int bid = (int) blades.get(index);
                            if(bid>0){
                                overhaulTurbine.setBlade((int)size.get(2), z, allBlades.get(bid-1));
                            }
                            index++;
                        }
                        multiblock = overhaulTurbine;
//</editor-fold>
                        break;
                    case 4:
                        //<editor-fold defaultstate="collapsed" desc="Overhaul Fusion Reactor">
                        size = data.get("size");
                        OverhaulFusionReactor overhaulFusionReactor = new OverhaulFusionReactor(ncpf.configuration, (int)size.get(0),(int)size.get(1),(int)size.get(2),(int)size.get(3),ncpf.configuration.overhaul.fusion.allRecipes.get(data.get("recipe", (byte)-1)),ncpf.configuration.overhaul.fusion.allCoolantRecipes.get(data.get("coolantRecipe", (byte)-1)));
                        blocks = data.get("blocks");
                        int[] findex = new int[1];
                        overhaulFusionReactor.forEachPosition((X, Y, Z) -> {
                            int bid = (int)blocks.get(findex[0]);
                            if(bid>0)overhaulFusionReactor.setBlockExact(X, Y, Z, new multiblock.overhaul.fusion.Block(ncpf.configuration, X, Y, Z, ncpf.configuration.overhaul.fusion.allBlocks.get(bid-1)));
                            findex[0]++;
                        });
                        ConfigNumberList breedingBlanketRecipes = data.get("breedingBlanketRecipes");
                        recipeIndex = 0;
                        for(multiblock.overhaul.fusion.Block block : overhaulFusionReactor.getBlocks()){
                            if(block.template.breedingBlanket){
                                int rid = (int) breedingBlanketRecipes.get(recipeIndex);
                                if(rid>0)block.recipe = block.template.allRecipes.get(rid-1);
                                recipeIndex++;
                            }
                        }
                        multiblock = overhaulFusionReactor;
//</editor-fold>
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown Multiblock ID: "+id);
                }
                if(multiblock instanceof CuboidalMultiblock)((CuboidalMultiblock)multiblock).buildDefaultCasingOnConvert();
                if(data.hasProperty("metadata")){
                    Config metadata = data.get("metadata");
                    for(String key : metadata.properties()){
                        multiblock.metadata.put(key, metadata.get(key));
                    }
                }
                ncpf.multiblocks.add(multiblock);
            }
            for(OverhaulTurbine turbine : overhaulTurbinePostLoadInputsMap.keySet()){
                for(int i : overhaulTurbinePostLoadInputsMap.get(turbine)){
                    turbine.inputs.add(ncpf.multiblocks.get(i));
                }
            }
            in.close();
            return ncpf;
        }catch(IOException ex){
            throw new RuntimeException(ex);
        }
    }
    private multiblock.configuration.underhaul.fissionsfr.PlacementRule readUnderRule(Config ruleCfg){
        multiblock.configuration.underhaul.fissionsfr.PlacementRule rule = new multiblock.configuration.underhaul.fissionsfr.PlacementRule();
        byte type = ruleCfg.get("type");
        switch(type){
            case 0:
                rule.ruleType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.BETWEEN;
                underhaulPostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 1:
                rule.ruleType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.AXIAL;
                underhaulPostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 2:
                rule.ruleType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.VERTEX;
                underhaulPostLoadMap.put(rule, ruleCfg.get("block"));
                break;
            case 3:
                rule.ruleType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.BETWEEN_GROUP;
                byte blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.COOLER;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.FUEL_CELL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.MODERATOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 4:
                rule.ruleType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.AXIAL_GROUP;
                blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.COOLER;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.FUEL_CELL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.MODERATOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 5:
                rule.ruleType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.VERTEX_GROUP;
                blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.COOLER;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.FUEL_CELL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.MODERATOR;
                        break;
                }
                break;
            case 6:
                rule.ruleType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.OR;
                ConfigList rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readUnderRule(rulC));
                }
                break;
            case 7:
                rule.ruleType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.AND;
                rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readUnderRule(rulC));
                }
                break;
        }
        return rule;
    }
    private multiblock.configuration.overhaul.fissionsfr.PlacementRule readOverSFRRule(Config ruleCfg){
        multiblock.configuration.overhaul.fissionsfr.PlacementRule rule = new multiblock.configuration.overhaul.fissionsfr.PlacementRule();
        byte type = ruleCfg.get("type");
        switch(type){
            case 0:
                rule.ruleType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.BETWEEN;
                overhaulSFRPostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 1:
                rule.ruleType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.AXIAL;
                overhaulSFRPostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 2:
                rule.ruleType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.VERTEX;
                overhaulSFRPostLoadMap.put(rule, ruleCfg.get("block"));
                break;
            case 3:
                rule.ruleType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.BETWEEN_GROUP;
                byte blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.HEATSINK;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.FUEL_CELL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.MODERATOR;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.SHIELD;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.IRRADIATOR;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.CONDUCTOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 4:
                rule.ruleType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.AXIAL_GROUP;
                blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.HEATSINK;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.FUEL_CELL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.MODERATOR;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.SHIELD;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.IRRADIATOR;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.CONDUCTOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 5:
                rule.ruleType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.VERTEX_GROUP;
                blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.HEATSINK;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.FUEL_CELL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.MODERATOR;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.SHIELD;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.IRRADIATOR;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.CONDUCTOR;
                        break;
                }
                break;
            case 6:
                rule.ruleType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.OR;
                ConfigList rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readOverSFRRule(rulC));
                }
                break;
            case 7:
                rule.ruleType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.AND;
                rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readOverSFRRule(rulC));
                }
                break;
        }
        return rule;
    }
    private multiblock.configuration.overhaul.fissionmsr.PlacementRule readOverMSRRule(Config ruleCfg){
        multiblock.configuration.overhaul.fissionmsr.PlacementRule rule = new multiblock.configuration.overhaul.fissionmsr.PlacementRule();
        byte type = ruleCfg.get("type");
        switch(type){
            case 0:
                rule.ruleType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.BETWEEN;
                overhaulMSRPostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 1:
                rule.ruleType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.AXIAL;
                overhaulMSRPostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 2:
                rule.ruleType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.VERTEX;
                overhaulMSRPostLoadMap.put(rule, ruleCfg.get("block"));
                break;
            case 3:
                rule.ruleType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.BETWEEN_GROUP;
                byte blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.HEATER;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.VESSEL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.MODERATOR;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.SHIELD;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.IRRADIATOR;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.CONDUCTOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 4:
                rule.ruleType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.AXIAL_GROUP;
                blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.HEATER;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.VESSEL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.MODERATOR;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.SHIELD;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.IRRADIATOR;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.CONDUCTOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 5:
                rule.ruleType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.VERTEX_GROUP;
                blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.CASING;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.HEATER;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.VESSEL;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.MODERATOR;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.SHIELD;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.IRRADIATOR;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.CONDUCTOR;
                        break;
                }
                break;
            case 6:
                rule.ruleType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.OR;
                ConfigList rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readOverMSRRule(rulC));
                }
                break;
            case 7:
                rule.ruleType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.AND;
                rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readOverMSRRule(rulC));
                }
                break;
        }
        return rule;
    }
    private multiblock.configuration.overhaul.turbine.PlacementRule readOverTurbineRule(Config ruleCfg){
        multiblock.configuration.overhaul.turbine.PlacementRule rule = new multiblock.configuration.overhaul.turbine.PlacementRule();
        byte type = ruleCfg.get("type");
        switch(type){
            case 0:
                rule.ruleType = multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.BETWEEN;
                overhaulTurbinePostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 1:
                rule.ruleType = multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.AXIAL;
                overhaulTurbinePostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 2:
                rule.ruleType = multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.EDGE;
                overhaulTurbinePostLoadMap.put(rule, ruleCfg.get("block"));
                break;
            case 3:
                rule.ruleType = multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.BETWEEN_GROUP;
                byte coilType = ruleCfg.get("block");
                switch(coilType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.CASING;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.COIL;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.BEARING;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.CONNECTOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 4:
                rule.ruleType = multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.AXIAL_GROUP;
                coilType = ruleCfg.get("block");
                switch(coilType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.CASING;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.COIL;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.BEARING;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.CONNECTOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 5:
                rule.ruleType = multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.EDGE_GROUP;
                coilType = ruleCfg.get("block");
                switch(coilType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.CASING;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.COIL;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.BEARING;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.CONNECTOR;
                        break;
                }
                break;
            case 6:
                rule.ruleType = multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.OR;
                ConfigList rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readOverTurbineRule(rulC));
                }
                break;
            case 7:
                rule.ruleType = multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.AND;
                rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readOverTurbineRule(rulC));
                }
                break;
        }
        return rule;
    }
    private multiblock.configuration.overhaul.fusion.PlacementRule readOverFusionRule(Config ruleCfg){
        multiblock.configuration.overhaul.fusion.PlacementRule rule = new multiblock.configuration.overhaul.fusion.PlacementRule();
        byte type = ruleCfg.get("type");
        switch(type){
            case 0:
                rule.ruleType = multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.BETWEEN;
                overhaulFusionPostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 1:
                rule.ruleType = multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.AXIAL;
                overhaulFusionPostLoadMap.put(rule, ruleCfg.get("block"));
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 2:
                rule.ruleType = multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.VERTEX;
                overhaulFusionPostLoadMap.put(rule, ruleCfg.get("block"));
                break;
            case 3:
                rule.ruleType = multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.BETWEEN_GROUP;
                byte blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.TOROIDAL_ELECTROMAGNET;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.POLOIDAL_ELECTROMAGNET;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.HEATING_BLANKET;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.BREEDING_BLANKET;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.HEAT_SINK;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.SHIELDING;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.CONDUCTOR;
                        break;
                    case 9:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.CONNECTOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 4:
                rule.ruleType = multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.AXIAL_GROUP;
                blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.TOROIDAL_ELECTROMAGNET;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.POLOIDAL_ELECTROMAGNET;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.HEATING_BLANKET;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.BREEDING_BLANKET;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.HEAT_SINK;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.SHIELDING;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.CONDUCTOR;
                        break;
                    case 9:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.CONNECTOR;
                        break;
                }
                rule.min = ruleCfg.get("min");
                rule.max = ruleCfg.get("max");
                break;
            case 5:
                rule.ruleType = multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.VERTEX_GROUP;
                blockType = ruleCfg.get("block");
                switch(blockType){
                    case 0:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.AIR;
                        break;
                    case 1:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.TOROIDAL_ELECTROMAGNET;
                        break;
                    case 2:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.POLOIDAL_ELECTROMAGNET;
                        break;
                    case 3:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.HEATING_BLANKET;
                        break;
                    case 4:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.BREEDING_BLANKET;
                        break;
                    case 5:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.REFLECTOR;
                        break;
                    case 6:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.HEAT_SINK;
                        break;
                    case 7:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.SHIELDING;
                        break;
                    case 8:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.CONDUCTOR;
                        break;
                    case 9:
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.CONNECTOR;
                        break;
                }
                break;
            case 6:
                rule.ruleType = multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.OR;
                ConfigList rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readOverFusionRule(rulC));
                }
                break;
            case 7:
                rule.ruleType = multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.AND;
                rules = ruleCfg.get("rules");
                for(Iterator rit = rules.iterator(); rit.hasNext();){
                    Config rulC = (Config)rit.next();
                    rule.rules.add(readOverFusionRule(rulC));
                }
                break;
        }
        return rule;
    }
    private Configuration loadConfiguration(Config config){
        boolean partial = config.get("partial");
        Configuration configuration;
        if(partial)configuration = new PartialConfiguration(config.get("name"), config.get("version"), config.get("underhaulVersion"));
        else configuration = new Configuration(config.get("name"), config.get("version"), config.get("underhaulVersion"));
        configuration.addon = config.get("addon");
        //<editor-fold defaultstate="collapsed" desc="Underhaul Configuration">
        if(config.hasProperty("underhaul")){
            configuration.underhaul = new UnderhaulConfiguration();
            Config underhaul = config.get("underhaul");
            if(underhaul.hasProperty("fissionSFR")){
                configuration.underhaul.fissionSFR = new multiblock.configuration.underhaul.fissionsfr.FissionSFRConfiguration();
                Config fissionSFR = underhaul.get("fissionSFR");
                if(!partial&&!configuration.addon){
                    configuration.underhaul.fissionSFR.minSize = fissionSFR.get("minSize");
                    configuration.underhaul.fissionSFR.maxSize = fissionSFR.get("maxSize");
                    configuration.underhaul.fissionSFR.neutronReach = fissionSFR.get("neutronReach");
                    configuration.underhaul.fissionSFR.moderatorExtraPower = fissionSFR.get("moderatorExtraPower");
                    configuration.underhaul.fissionSFR.moderatorExtraHeat = fissionSFR.get("moderatorExtraHeat");
                    configuration.underhaul.fissionSFR.activeCoolerRate = fissionSFR.get("activeCoolerRate");
                }
                ConfigList blocks = fissionSFR.get("blocks");
                underhaulPostLoadMap.clear();
                for(Iterator bit = blocks.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.underhaul.fissionsfr.Block block = new multiblock.configuration.underhaul.fissionsfr.Block(blockCfg.get("name"));
                    block.active = blockCfg.get("active");
                    block.cooling = blockCfg.get("cooling", 0);
                    block.fuelCell = blockCfg.get("fuelCell", false);
                    block.moderator = blockCfg.get("moderator", false);
                    if(blockCfg.hasProperty("texture"))block.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            block.rules.add(readUnderRule(ruleCfg));
                        }
                    }
                    configuration.underhaul.fissionSFR.allBlocks.add(block);configuration.underhaul.fissionSFR.blocks.add(block);
                }
                for(multiblock.configuration.underhaul.fissionsfr.PlacementRule rule : underhaulPostLoadMap.keySet()){
                    byte index = underhaulPostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.AIR;
                    }else{
                        rule.block = configuration.underhaul.fissionSFR.allBlocks.get(index-1);
                    }
                }
                ConfigList fuels = fissionSFR.get("fuels");
                for(Iterator fit = fuels.iterator(); fit.hasNext();){
                    Config fuelCfg = (Config)fit.next();
                    multiblock.configuration.underhaul.fissionsfr.Fuel fuel = new multiblock.configuration.underhaul.fissionsfr.Fuel(fuelCfg.get("name"), fuelCfg.get("power"), fuelCfg.get("heat"), fuelCfg.get("time"));
                    configuration.underhaul.fissionSFR.allFuels.add(fuel);configuration.underhaul.fissionSFR.fuels.add(fuel);
                }
            }
        }
//</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="Overhaul Configuration">
        if(config.hasProperty("overhaul")){
            configuration.overhaul = new OverhaulConfiguration();
            Config overhaul = config.get("overhaul");
            //<editor-fold defaultstate="collapsed" desc="Fission SFR Configuration">
            if(overhaul.hasProperty("fissionSFR")){
                configuration.overhaul.fissionSFR = new multiblock.configuration.overhaul.fissionsfr.FissionSFRConfiguration();
                Config fissionSFR = overhaul.get("fissionSFR");
                if(!partial&&!configuration.addon){
                    configuration.overhaul.fissionSFR.minSize = fissionSFR.get("minSize");
                    configuration.overhaul.fissionSFR.maxSize = fissionSFR.get("maxSize");
                    configuration.overhaul.fissionSFR.neutronReach = fissionSFR.get("neutronReach");
                    configuration.overhaul.fissionSFR.coolingEfficiencyLeniency = fissionSFR.get("coolingEfficiencyLeniency");
                    configuration.overhaul.fissionSFR.sparsityPenaltyMult = fissionSFR.get("sparsityPenaltyMult");
                    configuration.overhaul.fissionSFR.sparsityPenaltyThreshold = fissionSFR.get("sparsityPenaltyThreshold");
                }
                ConfigList blocks = fissionSFR.get("blocks");
                overhaulSFRPostLoadMap.clear();
                for(Iterator bit = blocks.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.fissionsfr.Block block = new multiblock.configuration.overhaul.fissionsfr.Block(blockCfg.get("name"));
                    int cooling = blockCfg.get("cooling", 0);
                    if(cooling!=0){
                        block.heatsink = true;
                        block.heatsinkHasBaseStats = true;
                        block.heatsinkCooling = cooling;
                    }
                    block.cluster = blockCfg.get("cluster", false);
                    block.createCluster = blockCfg.get("createCluster", false);
                    block.conductor = blockCfg.get("conductor", false);
                    block.fuelCell = blockCfg.get("fuelCell", false);
                    if(blockCfg.get("reflector", false)){
                        block.reflector = true;
                        block.reflectorHasBaseStats = true;
                        block.reflectorEfficiency = blockCfg.get("efficiency");
                        block.reflectorReflectivity = blockCfg.get("reflectivity");
                    }
                    block.irradiator = blockCfg.get("irradiator", false);
                    if(blockCfg.get("moderator", false)){
                        block.moderator = true;
                        block.moderatorHasBaseStats = true;
                        block.moderatorActive = blockCfg.get("activeModerator", false);
                        block.moderatorFlux = blockCfg.get("flux");
                        block.moderatorEfficiency = blockCfg.get("efficiency");
                    }
                    if(blockCfg.get("shield", false)){
                        block.shield = true;
                        block.shieldHasBaseStats = true;
                        block.shieldHeat = blockCfg.get("heatMult");
                        block.shieldEfficiency = blockCfg.get("efficiency");
                    }
                    block.blocksLOS = blockCfg.get("blocksLOS", false);
                    block.functional = blockCfg.get("functional");
                    if(blockCfg.hasProperty("texture"))block.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("closedTexture"))block.setShieldClosedTexture(loadNCPFTexture(blockCfg.get("closedTexture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            block.rules.add(readOverSFRRule(ruleCfg));
                        }
                    }
                    configuration.overhaul.fissionSFR.allBlocks.add(block);configuration.overhaul.fissionSFR.blocks.add(block);
                }
                for(multiblock.configuration.overhaul.fissionsfr.PlacementRule rule : overhaulSFRPostLoadMap.keySet()){
                    byte index = overhaulSFRPostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.AIR;
                    }else{
                        rule.block = configuration.overhaul.fissionSFR.allBlocks.get(index-1);
                    }
                }
                ConfigList fuels = fissionSFR.get("fuels");
                for(Iterator fit = fuels.iterator(); fit.hasNext();){
                    Config fuelCfg = (Config)fit.next();
                    multiblock.configuration.overhaul.fissionsfr.BlockRecipe fuel = new multiblock.configuration.overhaul.fissionsfr.BlockRecipe(fuelCfg.get("name"), "null");
                    fuel.fuelCellEfficiency = fuelCfg.get("efficiency");
                    fuel.fuelCellHeat = fuelCfg.get("heat");
                    fuel.fuelCellTime = fuelCfg.get("time");
                    fuel.fuelCellCriticality = fuelCfg.get("criticality");
                    fuel.fuelCellSelfPriming = fuelCfg.get("selfPriming", false);
                    for(multiblock.configuration.overhaul.fissionsfr.Block b : configuration.overhaul.fissionSFR.allBlocks){
                        if(b.fuelCell){
                            b.allRecipes.add(fuel);b.recipes.add(fuel);
                        }
                    }
                }
                ConfigList sources = fissionSFR.get("sources");
                for(Iterator sit = sources.iterator(); sit.hasNext();){
                    Config sourceCfg = (Config)sit.next();
                    multiblock.configuration.overhaul.fissionsfr.Block source = new multiblock.configuration.overhaul.fissionsfr.Block(sourceCfg.get("name"));
                    source.source = true;
                    source.sourceEfficiency = sourceCfg.get("efficiency");
                    configuration.overhaul.fissionSFR.allBlocks.add(source);configuration.overhaul.fissionSFR.blocks.add(source);
                }
                ConfigList irradiatorRecipes = fissionSFR.get("irradiatorRecipes");
                for(Iterator irit = irradiatorRecipes.iterator(); irit.hasNext();){
                    Config irradiatorRecipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.fissionsfr.BlockRecipe irrecipe = new multiblock.configuration.overhaul.fissionsfr.BlockRecipe(irradiatorRecipeCfg.get("name"), "null");
                    irrecipe.irradiatorEfficiency = irradiatorRecipeCfg.get("efficiency");
                    irrecipe.irradiatorHeat = irradiatorRecipeCfg.get("heat");
                    for(multiblock.configuration.overhaul.fissionsfr.Block b : configuration.overhaul.fissionSFR.allBlocks){
                        if(b.irradiator){
                            b.allRecipes.add(irrecipe);b.recipes.add(irrecipe);
                        }
                    }
                }
                ConfigList coolantRecipes = fissionSFR.get("coolantRecipes");
                for(Iterator irit = coolantRecipes.iterator(); irit.hasNext();){
                    Config coolantRecipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.fissionsfr.CoolantRecipe coolRecipe = new multiblock.configuration.overhaul.fissionsfr.CoolantRecipe(coolantRecipeCfg.get("input"), coolantRecipeCfg.get("output"), coolantRecipeCfg.get("heat"), coolantRecipeCfg.getFloat("outputRatio"));
                    configuration.overhaul.fissionSFR.allCoolantRecipes.add(coolRecipe);configuration.overhaul.fissionSFR.coolantRecipes.add(coolRecipe);
                }
                for(multiblock.configuration.overhaul.fissionsfr.Block b : configuration.overhaul.fissionSFR.allBlocks){
                    if(!b.allRecipes.isEmpty()){
                        b.port = new multiblock.configuration.overhaul.fissionsfr.Block("null");
                    }
                }
                if(configuration.addon){
                    multiblock.configuration.overhaul.fissionsfr.Block cell = new multiblock.configuration.overhaul.fissionsfr.Block("Fuel Cell");
                    cell.fuelCell = true;
                    configuration.overhaul.fissionSFR.allBlocks.add(cell);
                    cell.allRecipes.add(new multiblock.configuration.overhaul.fissionsfr.BlockRecipe("",""));
                    multiblock.configuration.overhaul.fissionsfr.Block irradiator = new multiblock.configuration.overhaul.fissionsfr.Block("Neutron Irradiator");
                    irradiator.irradiator = true;
                    irradiator.allRecipes.add(new multiblock.configuration.overhaul.fissionsfr.BlockRecipe("",""));
                    configuration.overhaul.fissionSFR.allBlocks.add(irradiator);
                }
            }
//</editor-folirradiator
            //<editor-fold defaultstate="collapsed" desc="Fission MSR Configuration">
            if(overhaul.hasProperty("fissionMSR")){
                configuration.overhaul.fissionMSR = new multiblock.configuration.overhaul.fissionmsr.FissionMSRConfiguration();
                Config fissionMSR = overhaul.get("fissionMSR");
                if(!partial&&!configuration.addon){
                    configuration.overhaul.fissionMSR.minSize = fissionMSR.get("minSize");
                    configuration.overhaul.fissionMSR.maxSize = fissionMSR.get("maxSize");
                    configuration.overhaul.fissionMSR.neutronReach = fissionMSR.get("neutronReach");
                    configuration.overhaul.fissionMSR.coolingEfficiencyLeniency = fissionMSR.get("coolingEfficiencyLeniency");
                    configuration.overhaul.fissionMSR.sparsityPenaltyMult = fissionMSR.get("sparsityPenaltyMult");
                    configuration.overhaul.fissionMSR.sparsityPenaltyThreshold = fissionMSR.get("sparsityPenaltyThreshold");
                }
                ConfigList blocks = fissionMSR.get("blocks");
                overhaulMSRPostLoadMap.clear();
                for(Iterator bit = blocks.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.fissionmsr.Block block = new multiblock.configuration.overhaul.fissionmsr.Block(blockCfg.get("name"));
                    int cooling = blockCfg.get("cooling", 0);
                    if(cooling!=0){
                        block.heater = true;
                        multiblock.configuration.overhaul.fissionmsr.BlockRecipe recipe = new multiblock.configuration.overhaul.fissionmsr.BlockRecipe(blockCfg.get("input", ""), blockCfg.get("output", ""));
                        recipe.heaterCooling = cooling;
                        recipe.inputRate = blockCfg.hasProperty("input")?1:0;
                        recipe.outputRate = blockCfg.hasProperty("output")?1:0;
                        block.allRecipes.add(recipe);block.recipes.add(recipe);
                    }
                    block.cluster = blockCfg.get("cluster", false);
                    block.createCluster = blockCfg.get("createCluster", false);
                    block.conductor = blockCfg.get("conductor", false);
                    block.fuelVessel = blockCfg.get("fuelVessel", false);
                    if(blockCfg.get("reflector", false)){
                        block.reflector = true;
                        block.reflectorHasBaseStats = true;
                        block.reflectorEfficiency = blockCfg.get("efficiency");
                        block.reflectorReflectivity = blockCfg.get("reflectivity");
                    }
                    block.irradiator = blockCfg.get("irradiator", false);
                    if(blockCfg.get("moderator", false)){
                        block.moderator = true;
                        block.moderatorHasBaseStats = true;
                        block.moderatorActive = blockCfg.get("activeModerator", false);
                        block.moderatorFlux = blockCfg.get("flux");
                        block.moderatorEfficiency = blockCfg.get("efficiency");
                    }
                    if(blockCfg.get("shield", false)){
                        block.shield = true;
                        block.shieldHasBaseStats = true;
                        block.shieldHeat = blockCfg.get("heatMult");
                        block.shieldEfficiency = blockCfg.get("efficiency");
                    }
                    block.blocksLOS = blockCfg.get("blocksLOS", false);
                    block.functional = blockCfg.get("functional");
                    if(blockCfg.hasProperty("texture"))block.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("closedTexture"))block.setShieldClosedTexture(loadNCPFTexture(blockCfg.get("closedTexture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            block.rules.add(readOverMSRRule(ruleCfg));
                        }
                    }
                    configuration.overhaul.fissionMSR.allBlocks.add(block);configuration.overhaul.fissionMSR.blocks.add(block);
                }
                for(multiblock.configuration.overhaul.fissionmsr.PlacementRule rule : overhaulMSRPostLoadMap.keySet()){
                    byte index = overhaulMSRPostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.AIR;
                    }else{
                        rule.block = configuration.overhaul.fissionMSR.allBlocks.get(index-1);
                    }
                }
                ConfigList fuels = fissionMSR.get("fuels");
                for(Iterator fit = fuels.iterator(); fit.hasNext();){
                    Config fuelCfg = (Config)fit.next();
                    multiblock.configuration.overhaul.fissionmsr.BlockRecipe fuel = new multiblock.configuration.overhaul.fissionmsr.BlockRecipe(fuelCfg.get("name"), "null");
                    fuel.inputRate = fuel.outputRate = 1;
                    fuel.fuelVesselEfficiency = fuelCfg.get("efficiency");
                    fuel.fuelVesselHeat = fuelCfg.get("heat");
                    fuel.fuelVesselTime = fuelCfg.get("time");
                    fuel.fuelVesselCriticality = fuelCfg.get("criticality");
                    fuel.fuelVesselSelfPriming = fuelCfg.get("selfPriming", false);
                    for(multiblock.configuration.overhaul.fissionmsr.Block b : configuration.overhaul.fissionMSR.allBlocks){
                        if(b.fuelVessel){
                            b.allRecipes.add(fuel);b.recipes.add(fuel);
                        }
                    }
                }
                ConfigList sources = fissionMSR.get("sources");
                for(Iterator sit = sources.iterator(); sit.hasNext();){
                    Config sourceCfg = (Config)sit.next();
                    multiblock.configuration.overhaul.fissionmsr.Block source = new multiblock.configuration.overhaul.fissionmsr.Block(sourceCfg.get("name"));
                    source.source = true;
                    source.sourceEfficiency = sourceCfg.get("efficiency");
                    configuration.overhaul.fissionMSR.allBlocks.add(source);configuration.overhaul.fissionMSR.blocks.add(source);
                }
                ConfigList irradiatorRecipes = fissionMSR.get("irradiatorRecipes");
                for(Iterator irit = irradiatorRecipes.iterator(); irit.hasNext();){
                    Config irradiatorRecipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.fissionmsr.BlockRecipe irrecipe = new multiblock.configuration.overhaul.fissionmsr.BlockRecipe(irradiatorRecipeCfg.get("name"), "null");
                    irrecipe.irradiatorEfficiency = irradiatorRecipeCfg.get("efficiency");
                    irrecipe.irradiatorHeat = irradiatorRecipeCfg.get("heat");
                    for(multiblock.configuration.overhaul.fissionmsr.Block b : configuration.overhaul.fissionMSR.allBlocks){
                        if(b.irradiator){
                            b.allRecipes.add(irrecipe);b.recipes.add(irrecipe);
                        }
                    }
                }
                for(multiblock.configuration.overhaul.fissionmsr.Block b : configuration.overhaul.fissionMSR.allBlocks){
                    if(!b.allRecipes.isEmpty()){
                        b.port = new multiblock.configuration.overhaul.fissionmsr.Block("null");
                    }
                }
                if(configuration.addon){
                    multiblock.configuration.overhaul.fissionmsr.Block vessel = new multiblock.configuration.overhaul.fissionmsr.Block("Fuel Vessel");
                    vessel.fuelVessel = true;
                    configuration.overhaul.fissionMSR.allBlocks.add(vessel);
                    vessel.allRecipes.add(new multiblock.configuration.overhaul.fissionmsr.BlockRecipe("",""));
                    multiblock.configuration.overhaul.fissionmsr.Block irradiator = new multiblock.configuration.overhaul.fissionmsr.Block("Neutron Irradiator");
                    irradiator.irradiator = true;
                    irradiator.allRecipes.add(new multiblock.configuration.overhaul.fissionmsr.BlockRecipe("",""));
                    configuration.overhaul.fissionMSR.allBlocks.add(irradiator);
                }
            }
//</editor-folirradiator
            //<editor-fold defaultstate="collapsed" desc="Turbine Configuration">
            if(overhaul.hasProperty("turbine")){
                configuration.overhaul.turbine = new multiblock.configuration.overhaul.turbine.TurbineConfiguration();
                Config turbine = overhaul.get("turbine");
                if(!partial&&!configuration.addon){
                    configuration.overhaul.turbine.minWidth = turbine.get("minWidth");
                    configuration.overhaul.turbine.minLength = turbine.get("minLength");
                    configuration.overhaul.turbine.maxSize = turbine.get("maxSize");
                    configuration.overhaul.turbine.fluidPerBlade = turbine.get("fluidPerBlade");
                    configuration.overhaul.turbine.throughputEfficiencyLeniencyMult = turbine.get("throughputEfficiencyLeniencyMult");
                    configuration.overhaul.turbine.throughputEfficiencyLeniencyThreshold = turbine.get("throughputEfficiencyLeniencyThreshold");
                    configuration.overhaul.turbine.throughputFactor = turbine.get("throughputFactor");
                    configuration.overhaul.turbine.powerBonus = turbine.get("powerBonus");
                }
                ConfigList coils = turbine.get("coils");
                overhaulTurbinePostLoadMap.clear();
                for(Iterator bit = coils.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.turbine.Block coil = new multiblock.configuration.overhaul.turbine.Block(blockCfg.get("name"));
                    coil.bearing = blockCfg.get("bearing", false);
                    coil.connector = blockCfg.get("connector", false);
                    float eff = blockCfg.get("efficiency");
                    if(eff>0){
                        coil.coil = true;
                        coil.coilEfficiency = blockCfg.get("efficiency");
                    }
                    if(blockCfg.hasProperty("texture"))coil.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            coil.rules.add(readOverTurbineRule(ruleCfg));
                        }
                    }
                    configuration.overhaul.turbine.allBlocks.add(coil);configuration.overhaul.turbine.blocks.add(coil);
                }
                ConfigList blades = turbine.get("blades");
                for(Iterator bit = blades.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.turbine.Block blade = new multiblock.configuration.overhaul.turbine.Block(blockCfg.get("name"));
                    blade.blade = true;
                    blade.bladeExpansion = blockCfg.get("expansion");
                    blade.bladeEfficiency = blockCfg.get("efficiency");
                    blade.bladeStator = blockCfg.get("stator");
                    if(blockCfg.hasProperty("texture"))blade.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    configuration.overhaul.turbine.allBlocks.add(blade);configuration.overhaul.turbine.blocks.add(blade);
                }
                ArrayList<multiblock.configuration.overhaul.turbine.Block> allCoils = new ArrayList<>();
                ArrayList<multiblock.configuration.overhaul.turbine.Block> allBlades = new ArrayList<>();
                for(multiblock.configuration.overhaul.turbine.Block b : configuration.overhaul.turbine.allBlocks){
                    if(b.blade)allBlades.add(b);
                    else allCoils.add(b);
                }
                for(multiblock.configuration.overhaul.turbine.PlacementRule rule : overhaulTurbinePostLoadMap.keySet()){
                    byte index = overhaulTurbinePostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.CASING;
                    }else{
                        rule.block = allCoils.get(index-1);
                    }
                }
                ConfigList recipes = turbine.get("recipes");
                for(Iterator irit = recipes.iterator(); irit.hasNext();){
                    Config recipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.turbine.Recipe recipe = new multiblock.configuration.overhaul.turbine.Recipe(recipeCfg.get("input"), recipeCfg.get("output"), recipeCfg.get("power"), recipeCfg.get("coefficient"));
                    configuration.overhaul.turbine.allRecipes.add(recipe);configuration.overhaul.turbine.recipes.add(recipe);
                }
            }
//</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Fusion Configuration">
            if(overhaul.hasProperty("fusion")){
                configuration.overhaul.fusion = new multiblock.configuration.overhaul.fusion.FusionConfiguration();
                Config fusion = overhaul.get("fusion");
                if(!partial&&!configuration.addon){
                    configuration.overhaul.fusion.minInnerRadius = fusion.get("minInnerRadius");
                    configuration.overhaul.fusion.maxInnerRadius = fusion.get("maxInnerRadius");
                    configuration.overhaul.fusion.minCoreSize = fusion.get("minCoreSize");
                    configuration.overhaul.fusion.maxCoreSize = fusion.get("maxCoreSize");
                    configuration.overhaul.fusion.minToroidWidth = fusion.get("minToroidWidth");
                    configuration.overhaul.fusion.maxToroidWidth = fusion.get("maxToroidWidth");
                    configuration.overhaul.fusion.minLiningThickness = fusion.get("minLiningThickness");
                    configuration.overhaul.fusion.maxLiningThickness = fusion.get("maxLiningThickness");
                    configuration.overhaul.fusion.coolingEfficiencyLeniency = fusion.get("coolingEfficiencyLeniency");
                    configuration.overhaul.fusion.sparsityPenaltyMult = fusion.get("sparsityPenaltyMult");
                    configuration.overhaul.fusion.sparsityPenaltyThreshold = fusion.get("sparsityPenaltyThreshold");
                }
                ConfigList blocks = fusion.get("blocks");
                overhaulFusionPostLoadMap.clear();
                for(Iterator bit = blocks.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.fusion.Block block = new multiblock.configuration.overhaul.fusion.Block(blockCfg.get("name"));
                    int cooling = blockCfg.get("cooling", 0);
                    if(cooling!=0){
                        block.heatsink = true;
                        block.heatsinkHasBaseStats = true;
                        block.heatsinkCooling = cooling;
                    }
                    block.cluster = blockCfg.get("cluster", false);
                    block.createCluster = blockCfg.get("createCluster", false);
                    block.conductor = blockCfg.get("conductor", false);
                    block.core = blockCfg.get("core", false);
                    block.connector = blockCfg.get("connector", false);
                    block.electromagnet = blockCfg.get("electromagnet", false);
                    block.heatingBlanket = blockCfg.get("heatingBlanket", false);
                    if(blockCfg.get("reflector", false)){
                        block.reflector = true;
                        block.reflectorHasBaseStats = true;
                        block.reflectorEfficiency = blockCfg.get("efficiency");
                    }
                    block.breedingBlanket = blockCfg.get("breedingBlanket", false);
                    block.breedingBlanketAugmented = blockCfg.get("augmentedBreedingBlanket", false);
                    if(blockCfg.get("shielding", false)){
                        block.shielding = true;
                        block.shieldingHasBaseStats = true;
                        block.shieldingShieldiness = blockCfg.get("shieldiness");
                    }
                    block.functional = blockCfg.get("functional");
                    if(blockCfg.hasProperty("texture"))block.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            block.rules.add(readOverFusionRule(ruleCfg));
                        }
                    }
                    configuration.overhaul.fusion.allBlocks.add(block);configuration.overhaul.fusion.blocks.add(block);
                }
                for(multiblock.configuration.overhaul.fusion.PlacementRule rule : overhaulFusionPostLoadMap.keySet()){
                    byte index = overhaulFusionPostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.overhaul.fusion.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.overhaul.fusion.PlacementRule.BlockType.AIR;
                    }else{
                        rule.block = configuration.overhaul.fusion.allBlocks.get(index-1);
                    }
                }
                ConfigList breedingBlanketRecipes = fusion.get("breedingBlanketRecipes");
                for(Iterator irit = breedingBlanketRecipes.iterator(); irit.hasNext();){
                    Config breedingBlanketRecipeCfg = (Config)irit.next();
                    for(multiblock.configuration.overhaul.fusion.Block b : configuration.overhaul.fusion.allBlocks){
                        if(b.breedingBlanket){
                            multiblock.configuration.overhaul.fusion.BlockRecipe breebrecipe = new multiblock.configuration.overhaul.fusion.BlockRecipe(breedingBlanketRecipeCfg.get("name"), "null");
                            breebrecipe.breedingBlanketEfficiency = breedingBlanketRecipeCfg.get("efficiency");
                            breebrecipe.breedingBlanketHeat = breedingBlanketRecipeCfg.get("heat", 0);
                            breebrecipe.breedingBlanketAugmented = b.breedingBlanketAugmented;
                            b.allRecipes.add(breebrecipe);b.recipes.add(breebrecipe);
                        }
                    }
                }
                ConfigList recipes = fusion.get("recipes");
                for(Iterator irit = recipes.iterator(); irit.hasNext();){
                    Config recipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.fusion.Recipe recipe = new multiblock.configuration.overhaul.fusion.Recipe(recipeCfg.get("name"), "null", recipeCfg.get("efficiency"), recipeCfg.get("heat"), recipeCfg.get("time"), recipeCfg.getFloat("fluxiness"));
                    configuration.overhaul.fusion.allRecipes.add(recipe);configuration.overhaul.fusion.recipes.add(recipe);
                }
                ConfigList coolantRecipes = fusion.get("coolantRecipes");
                for(Iterator coit = coolantRecipes.iterator(); coit.hasNext();){
                    Config coolantRecipeCfg = (Config)coit.next();
                    multiblock.configuration.overhaul.fusion.CoolantRecipe coolantRecipe = new multiblock.configuration.overhaul.fusion.CoolantRecipe(coolantRecipeCfg.get("input"), coolantRecipeCfg.get("output"), coolantRecipeCfg.get("heat"), coolantRecipeCfg.getFloat("outputRatio"));
                    configuration.overhaul.fusion.allCoolantRecipes.add(coolantRecipe);configuration.overhaul.fusion.coolantRecipes.add(coolantRecipe);
                }
            }
//</editor-fold>
        }
//</editor-fold>
        if(config.hasProperty("addons")){
            ConfigList addons = config.get("addons");
            for(int i = 0; i<addons.size(); i++){
                configuration.addons.add(loadAddon(configuration, addons.get(i)));
            }
        }
        return configuration;
    }
    private Configuration loadAddon(Configuration parent, Config config){
        boolean partial = config.get("partial");
        Configuration configuration;
        if(partial)configuration = new PartialConfiguration(config.get("name"), config.get("version"), config.get("underhaulVersion"));
        else configuration = new Configuration(config.get("name"), config.get("version"), config.get("underhaulVersion"));
        configuration.addon = config.get("addon");
        //<editor-fold defaultstate="collapsed" desc="Underhaul Configuration">
        if(config.hasProperty("underhaul")){
            configuration.underhaul = new UnderhaulConfiguration();
            Config underhaul = config.get("underhaul");
            if(underhaul.hasProperty("fissionSFR")){
                configuration.underhaul.fissionSFR = new multiblock.configuration.underhaul.fissionsfr.FissionSFRConfiguration();
                Config fissionSFR = underhaul.get("fissionSFR");
                ConfigList blocks = fissionSFR.get("blocks");
                underhaulPostLoadMap.clear();
                for(Iterator bit = blocks.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.underhaul.fissionsfr.Block block = new multiblock.configuration.underhaul.fissionsfr.Block(blockCfg.get("name"));
                    block.active = blockCfg.get("active");
                    block.cooling = blockCfg.get("cooling", 0);
                    block.fuelCell = blockCfg.get("fuelCell", false);
                    block.moderator = blockCfg.get("moderator", false);
                    if(blockCfg.hasProperty("texture"))block.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            block.rules.add(readUnderRule(ruleCfg));
                        }
                    }
                    parent.underhaul.fissionSFR.allBlocks.add(block);configuration.underhaul.fissionSFR.blocks.add(block);
                }
                for(multiblock.configuration.underhaul.fissionsfr.PlacementRule rule : underhaulPostLoadMap.keySet()){
                    byte index = underhaulPostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.underhaul.fissionsfr.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.underhaul.fissionsfr.PlacementRule.BlockType.AIR;
                    }else{
                        rule.block = parent.underhaul.fissionSFR.allBlocks.get(index-1);
                    }
                }
                ConfigList fuels = fissionSFR.get("fuels");
                for(Iterator fit = fuels.iterator(); fit.hasNext();){
                    Config fuelCfg = (Config)fit.next();
                    multiblock.configuration.underhaul.fissionsfr.Fuel fuel = new multiblock.configuration.underhaul.fissionsfr.Fuel(fuelCfg.get("name"), fuelCfg.get("power"), fuelCfg.get("heat"), fuelCfg.get("time"));
                    parent.underhaul.fissionSFR.allFuels.add(fuel);configuration.underhaul.fissionSFR.fuels.add(fuel);
                }
            }
        }
//</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="Overhaul Configuration">
        if(config.hasProperty("overhaul")){
            configuration.overhaul = new OverhaulConfiguration();
            Config overhaul = config.get("overhaul");
            //<editor-fold defaultstate="collapsed" desc="Fission SFR Configuration">
            if(overhaul.hasProperty("fissionSFR")){
                configuration.overhaul.fissionSFR = new multiblock.configuration.overhaul.fissionsfr.FissionSFRConfiguration();
                Config fissionSFR = overhaul.get("fissionSFR");
                ConfigList blocks = fissionSFR.get("blocks");
                overhaulSFRPostLoadMap.clear();
                for(Iterator bit = blocks.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.fissionsfr.Block block = new multiblock.configuration.overhaul.fissionsfr.Block(blockCfg.get("name"));
                    int cooling = blockCfg.get("cooling", 0);
                    if(cooling!=0){
                        block.heatsink = true;
                        block.heatsinkHasBaseStats = true;
                        block.heatsinkCooling = cooling;
                    }
                    block.cluster = blockCfg.get("cluster", false);
                    block.createCluster = blockCfg.get("createCluster", false);
                    block.conductor = blockCfg.get("conductor", false);
                    block.fuelCell = blockCfg.get("fuelCell", false);
                    if(blockCfg.get("reflector", false)){
                        block.reflector = true;
                        block.reflectorHasBaseStats = true;
                        block.reflectorEfficiency = blockCfg.get("efficiency");
                        block.reflectorReflectivity = blockCfg.get("reflectivity");
                    }
                    block.irradiator = blockCfg.get("irradiator", false);
                    if(blockCfg.get("moderator", false)){
                        block.moderator = true;
                        block.moderatorHasBaseStats = true;
                        block.moderatorActive = blockCfg.get("activeModerator", false);
                        block.moderatorFlux = blockCfg.get("flux");
                        block.moderatorEfficiency = blockCfg.get("efficiency");
                    }
                    if(blockCfg.get("shield", false)){
                        block.shield = true;
                        block.shieldHasBaseStats = true;
                        block.shieldHeat = blockCfg.get("heatMult");
                        block.shieldEfficiency = blockCfg.get("efficiency");
                    }
                    block.blocksLOS = blockCfg.get("blocksLOS", false);
                    block.functional = blockCfg.get("functional");
                    if(blockCfg.hasProperty("texture"))block.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("closedTexture"))block.setShieldClosedTexture(loadNCPFTexture(blockCfg.get("closedTexture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            block.rules.add(readOverSFRRule(ruleCfg));
                        }
                    }
                    parent.overhaul.fissionSFR.allBlocks.add(block);configuration.overhaul.fissionSFR.blocks.add(block);
                }
                for(multiblock.configuration.overhaul.fissionsfr.PlacementRule rule : overhaulSFRPostLoadMap.keySet()){
                    byte index = overhaulSFRPostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.overhaul.fissionsfr.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.overhaul.fissionsfr.PlacementRule.BlockType.AIR;
                    }else{
                        rule.block = parent.overhaul.fissionSFR.allBlocks.get(index-1);
                    }
                }
                for(multiblock.configuration.overhaul.fissionsfr.Block b : parent.overhaul.fissionSFR.allBlocks){
                    if(!b.allRecipes.isEmpty()){
                        multiblock.configuration.overhaul.fissionsfr.Block bl = new multiblock.configuration.overhaul.fissionsfr.Block(b.name);
                        bl.fuelCell = b.fuelCell;
                        bl.irradiator = b.irradiator;
                        configuration.overhaul.fissionSFR.allBlocks.add(bl);
                    }
                }
                ConfigList fuels = fissionSFR.get("fuels");
                for(Iterator fit = fuels.iterator(); fit.hasNext();){
                    Config fuelCfg = (Config)fit.next();
                    multiblock.configuration.overhaul.fissionsfr.BlockRecipe fuel = new multiblock.configuration.overhaul.fissionsfr.BlockRecipe(fuelCfg.get("name"), "null");
                    fuel.fuelCellEfficiency = fuelCfg.get("efficiency");
                    fuel.fuelCellHeat = fuelCfg.get("heat");
                    fuel.fuelCellTime = fuelCfg.get("time");
                    fuel.fuelCellCriticality = fuelCfg.get("criticality");
                    fuel.fuelCellSelfPriming = fuelCfg.get("selfPriming", false);
                    for(multiblock.configuration.overhaul.fissionsfr.Block b : parent.overhaul.fissionSFR.allBlocks){
                        if(b.fuelCell){
                            b.allRecipes.add(fuel);
                        }
                    }
                    for(multiblock.configuration.overhaul.fissionsfr.Block b : configuration.overhaul.fissionSFR.allBlocks){
                        if(b.fuelCell){
                            b.recipes.add(fuel);
                        }
                    }
                }
                ConfigList sources = fissionSFR.get("sources");
                for(Iterator sit = sources.iterator(); sit.hasNext();){
                    Config sourceCfg = (Config)sit.next();
                    multiblock.configuration.overhaul.fissionsfr.Block source = new multiblock.configuration.overhaul.fissionsfr.Block(sourceCfg.get("name"));
                    source.source = true;
                    source.sourceEfficiency = sourceCfg.get("efficiency");
                    parent.overhaul.fissionSFR.allBlocks.add(source);configuration.overhaul.fissionSFR.blocks.add(source);
                }
                ConfigList irradiatorRecipes = fissionSFR.get("irradiatorRecipes");
                for(Iterator irit = irradiatorRecipes.iterator(); irit.hasNext();){
                    Config irradiatorRecipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.fissionsfr.BlockRecipe irrecipe = new multiblock.configuration.overhaul.fissionsfr.BlockRecipe(irradiatorRecipeCfg.get("name"), "null");
                    irrecipe.irradiatorEfficiency = irradiatorRecipeCfg.get("efficiency");
                    irrecipe.irradiatorHeat = irradiatorRecipeCfg.get("heat");
                    for(multiblock.configuration.overhaul.fissionsfr.Block b : parent.overhaul.fissionSFR.allBlocks){
                        if(b.irradiator){
                            b.allRecipes.add(irrecipe);
                        }
                    }
                    for(multiblock.configuration.overhaul.fissionsfr.Block b : configuration.overhaul.fissionSFR.allBlocks){
                        if(b.irradiator){
                            b.recipes.add(irrecipe);
                        }
                    }
                }
                ConfigList coolantRecipes = fissionSFR.get("coolantRecipes");
                for(Iterator irit = coolantRecipes.iterator(); irit.hasNext();){
                    Config coolantRecipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.fissionsfr.CoolantRecipe coolRecipe = new multiblock.configuration.overhaul.fissionsfr.CoolantRecipe(coolantRecipeCfg.get("input"), coolantRecipeCfg.get("output"), coolantRecipeCfg.get("heat"), coolantRecipeCfg.getFloat("outputRatio"));
                    parent.overhaul.fissionSFR.allCoolantRecipes.add(coolRecipe);configuration.overhaul.fissionSFR.coolantRecipes.add(coolRecipe);
                }
                for(multiblock.configuration.overhaul.fissionsfr.Block b : parent.overhaul.fissionSFR.allBlocks){
                    if(!b.allRecipes.isEmpty()){
                        b.port = new multiblock.configuration.overhaul.fissionsfr.Block("null");
                    }
                }
            }
//</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Fission MSR Configuration">
            if(overhaul.hasProperty("fissionMSR")){
                configuration.overhaul.fissionMSR = new multiblock.configuration.overhaul.fissionmsr.FissionMSRConfiguration();
                Config fissionMSR = overhaul.get("fissionMSR");
                ConfigList blocks = fissionMSR.get("blocks");
                overhaulMSRPostLoadMap.clear();
                for(Iterator bit = blocks.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.fissionmsr.Block block = new multiblock.configuration.overhaul.fissionmsr.Block(blockCfg.get("name"));
                    int cooling = blockCfg.get("cooling", 0);
                    if(cooling!=0){
                        block.heater = true;
                        multiblock.configuration.overhaul.fissionmsr.BlockRecipe recipe = new multiblock.configuration.overhaul.fissionmsr.BlockRecipe(blockCfg.get("input", ""), blockCfg.get("output", ""));
                        recipe.heaterCooling = cooling;
                        recipe.inputRate = blockCfg.hasProperty("input")?1:0;
                        recipe.outputRate = blockCfg.hasProperty("output")?1:0;
                        block.allRecipes.add(recipe);block.recipes.add(recipe);
                    }
                    block.cluster = blockCfg.get("cluster", false);
                    block.createCluster = blockCfg.get("createCluster", false);
                    block.conductor = blockCfg.get("conductor", false);
                    block.fuelVessel = blockCfg.get("fuelVessel", false);
                    if(blockCfg.get("reflector", false)){
                        block.reflector = true;
                        block.reflectorHasBaseStats = true;
                        block.reflectorEfficiency = blockCfg.get("efficiency");
                        block.reflectorReflectivity = blockCfg.get("reflectivity");
                    }
                    block.irradiator = blockCfg.get("irradiator", false);
                    if(blockCfg.get("moderator", false)){
                        block.moderator = true;
                        block.moderatorHasBaseStats = true;
                        block.moderatorActive = blockCfg.get("activeModerator", false);
                        block.moderatorFlux = blockCfg.get("flux");
                        block.moderatorEfficiency = blockCfg.get("efficiency");
                    }
                    if(blockCfg.get("shield", false)){
                        block.shield = true;
                        block.shieldHasBaseStats = true;
                        block.shieldHeat = blockCfg.get("heatMult");
                        block.shieldEfficiency = blockCfg.get("efficiency");
                    }
                    block.blocksLOS = blockCfg.get("blocksLOS", false);
                    block.functional = blockCfg.get("functional");
                    if(blockCfg.hasProperty("texture"))block.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("closedTexture"))block.setShieldClosedTexture(loadNCPFTexture(blockCfg.get("closedTexture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            block.rules.add(readOverMSRRule(ruleCfg));
                        }
                    }
                    parent.overhaul.fissionMSR.allBlocks.add(block);configuration.overhaul.fissionMSR.blocks.add(block);
                }
                for(multiblock.configuration.overhaul.fissionmsr.PlacementRule rule : overhaulMSRPostLoadMap.keySet()){
                    byte index = overhaulMSRPostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.overhaul.fissionmsr.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.overhaul.fissionmsr.PlacementRule.BlockType.AIR;
                    }else{
                        rule.block = parent.overhaul.fissionMSR.allBlocks.get(index-1);
                    }
                }
                for(multiblock.configuration.overhaul.fissionmsr.Block b : parent.overhaul.fissionMSR.allBlocks){
                    if(!b.allRecipes.isEmpty()){
                        multiblock.configuration.overhaul.fissionmsr.Block bl = new multiblock.configuration.overhaul.fissionmsr.Block(b.name);
                        bl.fuelVessel = b.fuelVessel;
                        bl.irradiator = b.irradiator;
                        configuration.overhaul.fissionMSR.allBlocks.add(bl);
                    }
                }
                ConfigList fuels = fissionMSR.get("fuels");
                for(Iterator fit = fuels.iterator(); fit.hasNext();){
                    Config fuelCfg = (Config)fit.next();
                    multiblock.configuration.overhaul.fissionmsr.BlockRecipe fuel = new multiblock.configuration.overhaul.fissionmsr.BlockRecipe(fuelCfg.get("name"), "null");
                    fuel.inputRate = fuel.outputRate = 1;
                    fuel.fuelVesselEfficiency = fuelCfg.get("efficiency");
                    fuel.fuelVesselHeat = fuelCfg.get("heat");
                    fuel.fuelVesselTime = fuelCfg.get("time");
                    fuel.fuelVesselCriticality = fuelCfg.get("criticality");
                    fuel.fuelVesselSelfPriming = fuelCfg.get("selfPriming", false);
                    for(multiblock.configuration.overhaul.fissionmsr.Block b : parent.overhaul.fissionMSR.allBlocks){
                        if(b.fuelVessel){
                            b.allRecipes.add(fuel);
                        }
                    }
                    for(multiblock.configuration.overhaul.fissionmsr.Block b : configuration.overhaul.fissionMSR.allBlocks){
                        if(b.fuelVessel){
                            b.recipes.add(fuel);
                        }
                    }
                }
                ConfigList sources = fissionMSR.get("sources");
                for(Iterator sit = sources.iterator(); sit.hasNext();){
                    Config sourceCfg = (Config)sit.next();
                    multiblock.configuration.overhaul.fissionmsr.Block source = new multiblock.configuration.overhaul.fissionmsr.Block(sourceCfg.get("name"));
                    source.source = true;
                    source.sourceEfficiency = sourceCfg.get("efficiency");
                    parent.overhaul.fissionMSR.allBlocks.add(source);configuration.overhaul.fissionMSR.blocks.add(source);
                }
                ConfigList irradiatorRecipes = fissionMSR.get("irradiatorRecipes");
                for(Iterator irit = irradiatorRecipes.iterator(); irit.hasNext();){
                    Config irradiatorRecipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.fissionmsr.BlockRecipe irrecipe = new multiblock.configuration.overhaul.fissionmsr.BlockRecipe(irradiatorRecipeCfg.get("name"), "null");
                    irrecipe.irradiatorEfficiency = irradiatorRecipeCfg.get("efficiency");
                    irrecipe.irradiatorHeat = irradiatorRecipeCfg.get("heat");
                    for(multiblock.configuration.overhaul.fissionmsr.Block b : parent.overhaul.fissionMSR.allBlocks){
                        if(b.irradiator){
                            b.allRecipes.add(irrecipe);
                        }
                    }
                    for(multiblock.configuration.overhaul.fissionmsr.Block b : configuration.overhaul.fissionMSR.allBlocks){
                        if(b.irradiator){
                            b.recipes.add(irrecipe);
                        }
                    }
                }
                for(multiblock.configuration.overhaul.fissionmsr.Block b : parent.overhaul.fissionMSR.allBlocks){
                    if(!b.allRecipes.isEmpty()){
                        b.port = new multiblock.configuration.overhaul.fissionmsr.Block("null");
                    }
                }
            }
//</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Turbine Configuration">
            if(overhaul.hasProperty("turbine")){
                configuration.overhaul.turbine = new multiblock.configuration.overhaul.turbine.TurbineConfiguration();
                Config turbine = overhaul.get("turbine");
                ConfigList coils = turbine.get("coils");
                overhaulTurbinePostLoadMap.clear();
                for(Iterator bit = coils.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.turbine.Block coil = new multiblock.configuration.overhaul.turbine.Block(blockCfg.get("name"));
                    coil.bearing = blockCfg.get("bearing", false);
                    coil.connector = blockCfg.get("connector", false);
                    float eff = blockCfg.get("efficiency");
                    if(eff>0){
                        coil.coil = true;
                        coil.coilEfficiency = blockCfg.get("efficiency");
                    }
                    if(blockCfg.hasProperty("texture"))coil.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    if(blockCfg.hasProperty("rules")){
                        ConfigList rules = blockCfg.get("rules");
                        for(Iterator rit = rules.iterator(); rit.hasNext();){
                            Config ruleCfg = (Config)rit.next();
                            coil.rules.add(readOverTurbineRule(ruleCfg));
                        }
                    }
                    parent.overhaul.turbine.allBlocks.add(coil);configuration.overhaul.turbine.blocks.add(coil);
                }
                ConfigList blades = turbine.get("blades");
                for(Iterator bit = blades.iterator(); bit.hasNext();){
                    Config blockCfg = (Config)bit.next();
                    multiblock.configuration.overhaul.turbine.Block blade = new multiblock.configuration.overhaul.turbine.Block(blockCfg.get("name"));
                    blade.blade = true;
                    blade.bladeExpansion = blockCfg.get("expansion");
                    blade.bladeEfficiency = blockCfg.get("efficiency");
                    blade.bladeStator = blockCfg.get("stator");
                    if(blockCfg.hasProperty("texture"))blade.setTexture(loadNCPFTexture(blockCfg.get("texture")));
                    parent.overhaul.turbine.allBlocks.add(blade);configuration.overhaul.turbine.blocks.add(blade);
                }
                ArrayList<multiblock.configuration.overhaul.turbine.Block> allCoils = new ArrayList<>();
                ArrayList<multiblock.configuration.overhaul.turbine.Block> allBlades = new ArrayList<>();
                for(multiblock.configuration.overhaul.turbine.Block b : parent.overhaul.turbine.allBlocks){
                    if(b.blade)allBlades.add(b);
                    else allCoils.add(b);
                }
                for(multiblock.configuration.overhaul.turbine.PlacementRule rule : overhaulTurbinePostLoadMap.keySet()){
                    byte index = overhaulTurbinePostLoadMap.get(rule);
                    if(index==0){
                        if(rule.ruleType==multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.AXIAL)rule.ruleType=multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.AXIAL_GROUP;
                        if(rule.ruleType==multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.BETWEEN)rule.ruleType=multiblock.configuration.overhaul.turbine.PlacementRule.RuleType.BETWEEN_GROUP;
                        rule.blockType = multiblock.configuration.overhaul.turbine.PlacementRule.BlockType.CASING;
                    }else{
                        rule.block = allCoils.get(index-1);
                    }
                }
                ConfigList recipes = turbine.get("recipes");
                for(Iterator irit = recipes.iterator(); irit.hasNext();){
                    Config recipeCfg = (Config)irit.next();
                    multiblock.configuration.overhaul.turbine.Recipe recipe = new multiblock.configuration.overhaul.turbine.Recipe(recipeCfg.get("input"), recipeCfg.get("output"), recipeCfg.get("power"), recipeCfg.get("coefficient"));
                    parent.overhaul.turbine.allRecipes.add(recipe);configuration.overhaul.turbine.recipes.add(recipe);
                }
            }
//</editor-fold>
        }
//</editor-fold>
        if(config.hasProperty("addons")){
            ConfigList addons = config.get("addons");
            for(int i = 0; i<addons.size(); i++){
                configuration.addons.add(loadAddon(configuration, addons.get(i)));
            }
        }
        return configuration;
    }
    private Image loadNCPFTexture(ConfigNumberList texture){
        int size = (int) texture.get(0);
        Image image = new Image(size, size);
        int index = 1;
        for(int x = 0; x<image.getWidth(); x++){
            for(int y = 0; y<image.getHeight(); y++){
                image.setRGB(x, y, (int)texture.get(index));
                index++;
            }
        }
        return image;
    }
}