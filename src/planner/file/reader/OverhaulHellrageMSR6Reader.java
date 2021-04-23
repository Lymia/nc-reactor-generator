package planner.file.reader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import multiblock.configuration.overhaul.fissionmsr.BlockRecipe;
import multiblock.overhaul.fissionmsr.OverhaulMSR;
import planner.Core;
import planner.file.FormatReader;
import planner.file.JSON;
import planner.file.NCPFFile;
public class OverhaulHellrageMSR6Reader implements FormatReader{
    @Override
    public boolean formatMatches(InputStream in){
        JSON.JSONObject hellrage = JSON.parse(in);
        JSON.JSONObject saveVersion = hellrage.getJSONObject("SaveVersion");
        int major = saveVersion.getInt("Major");
        int minor = saveVersion.getInt("Minor");
        int build = saveVersion.getInt("Build");
        JSON.JSONObject data = hellrage.getJSONObject("Data");
        JSON.JSONObject fuelVessels = data.getJSONObject("FuelCells");
        for(String name : fuelVessels.keySet()){
            if(!name.startsWith("[F4]"))return false;//that's not an MSR!
        }
        return major==2&&minor==1&&build>=1;//&&build<=7;
    }
    @Override
    public synchronized NCPFFile read(InputStream in){
        JSON.JSONObject hellrage = JSON.parse(in);
        JSON.JSONObject data = hellrage.getJSONObject("Data");
        JSON.JSONObject dims = data.getJSONObject("InteriorDimensions");
        OverhaulMSR msr = new OverhaulMSR(null, dims.getInt("X"), dims.getInt("Y"), dims.getInt("Z"));
        JSON.JSONObject heatSinks = data.getJSONObject("HeatSinks");
        for(String name : heatSinks.keySet()){
            multiblock.configuration.overhaul.fissionmsr.Block block = null;
            for(multiblock.configuration.overhaul.fissionmsr.Block blok : Core.configuration.overhaul.fissionMSR.allBlocks){
                for(String nam : blok.getLegacyNames())if(nam.toLowerCase(Locale.ENGLISH).replace(" ", "").replace("coolant", "").replace("heater", "").replace("liquid", "").equalsIgnoreCase(name.toLowerCase(Locale.ENGLISH).replace("water", "standard").replace(" ", "")))block = blok;
            }
            if(block==null)throw new IllegalArgumentException("Unknown block: "+name);
            JSON.JSONArray array = heatSinks.getJSONArray(name);
            for(Object blok : array){
                JSON.JSONObject blockLoc = (JSON.JSONObject) blok;
                int x = blockLoc.getInt("X");
                int y = blockLoc.getInt("Y");
                int z = blockLoc.getInt("Z");
                msr.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(Core.configuration, x, y, z, block));
                if(block.heater&&!block.allRecipes.isEmpty())msr.getBlock(x,y,z).recipe = block.allRecipes.get(0);
            }
        }
        JSON.JSONObject moderators = data.getJSONObject("Moderators");
        for(String name : moderators.keySet()){
            multiblock.configuration.overhaul.fissionmsr.Block block = null;
            for(multiblock.configuration.overhaul.fissionmsr.Block blok : Core.configuration.overhaul.fissionMSR.allBlocks){
                for(String nam : blok.getLegacyNames())if(nam.toLowerCase(Locale.ENGLISH).replace(" ", "").replace("moderator", "").equalsIgnoreCase(name.replace(" ", "")))block = blok;
            }
            if(block==null)throw new IllegalArgumentException("Unknown block: "+name);
            JSON.JSONArray array = moderators.getJSONArray(name);
            for(Object blok : array){
                JSON.JSONObject blockLoc = (JSON.JSONObject) blok;
                int x = blockLoc.getInt("X");
                int y = blockLoc.getInt("Y");
                int z = blockLoc.getInt("Z");
                msr.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(Core.configuration, x, y, z, block));
            }
        }
        multiblock.configuration.overhaul.fissionmsr.Block conductor = null;
        for(multiblock.configuration.overhaul.fissionmsr.Block blok : Core.configuration.overhaul.fissionMSR.allBlocks){
            for(String nam : blok.getLegacyNames())if(nam.equalsIgnoreCase("conductor"))conductor = blok;
        }
        if(conductor==null)throw new IllegalArgumentException("Unknown block: Conductor");
        JSON.JSONArray conductors = data.getJSONArray("Conductors");
        if(conductors!=null){
            for(Object blok : conductors){
                    JSON.JSONObject blockLoc = (JSON.JSONObject) blok;
                    int x = blockLoc.getInt("X");
                    int y = blockLoc.getInt("Y");
                    int z = blockLoc.getInt("Z");
                msr.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(Core.configuration, x, y, z, conductor));
            }
        }
        JSON.JSONObject reflectors = data.getJSONObject("Reflectors");
        for(String name : reflectors.keySet()){
            multiblock.configuration.overhaul.fissionmsr.Block block = null;
            for(multiblock.configuration.overhaul.fissionmsr.Block blok : Core.configuration.overhaul.fissionMSR.allBlocks){
                for(String nam : blok.getLegacyNames())if(nam.toLowerCase(Locale.ENGLISH).replace(" ", "").replace("reflector", "").equalsIgnoreCase(name.replace(" ", "")))block = blok;
            }
            if(block==null)throw new IllegalArgumentException("Unknown block: "+name);
            JSON.JSONArray array = reflectors.getJSONArray(name);
            for(Object blok : array){
                JSON.JSONObject blockLoc = (JSON.JSONObject) blok;
                int x = blockLoc.getInt("X");
                int y = blockLoc.getInt("Y");
                int z = blockLoc.getInt("Z");
                msr.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(Core.configuration, x, y, z, block));
            }
        }
        JSON.JSONObject neutronShields = data.getJSONObject("NeutronShields");
        for(String name : neutronShields.keySet()){
            multiblock.configuration.overhaul.fissionmsr.Block block = null;
            for(multiblock.configuration.overhaul.fissionmsr.Block blok : Core.configuration.overhaul.fissionMSR.allBlocks){
                for(String nam : blok.getLegacyNames())if(nam.toLowerCase(Locale.ENGLISH).replace(" ", "").replace("neutronshield", "").replace("shield", "").equalsIgnoreCase(name.replace(" ", "")))block = blok;
            }
            if(block==null)throw new IllegalArgumentException("Unknown block: "+name);
            JSON.JSONArray array = neutronShields.getJSONArray(name);
            for(Object blok : array){
                JSON.JSONObject blockLoc = (JSON.JSONObject) blok;
                int x = blockLoc.getInt("X");
                int y = blockLoc.getInt("Y");
                int z = blockLoc.getInt("Z");
                msr.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(Core.configuration, x, y, z, block));
            }
        }
        multiblock.configuration.overhaul.fissionmsr.Block irradiator = null;
        for(multiblock.configuration.overhaul.fissionmsr.Block blok : Core.configuration.overhaul.fissionMSR.allBlocks){
            if(blok.irradiator)irradiator = blok;
        }
        if(irradiator==null)throw new IllegalArgumentException("Unknown block: Irradiator");
        JSON.JSONObject irradiators = data.getJSONObject("Irradiators");
        for(String name : irradiators.keySet()){
            BlockRecipe irrecipe = null;
            try{
                JSON.JSONObject recipe = JSON.parse(name);
                for(BlockRecipe irr : irradiator.allRecipes){
                    if(irr.irradiatorHeat==recipe.getFloat("HeatPerFlux")&&irr.irradiatorEfficiency==recipe.getFloat("EfficiencyMultiplier"))irrecipe = irr;
                }
            }catch(IOException ex){
                throw new IllegalArgumentException("Invalid irradiator recipe: "+name);
            }
            JSON.JSONArray array = irradiators.getJSONArray(name);
            for(Object blok : array){
                JSON.JSONObject blockLoc = (JSON.JSONObject) blok;
                int x = blockLoc.getInt("X");
                int y = blockLoc.getInt("Y");
                int z = blockLoc.getInt("Z");
                msr.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(Core.configuration, x, y, z, irradiator));
                msr.getBlock(x, y, z).recipe = irrecipe;
            }
        }
        multiblock.configuration.overhaul.fissionmsr.Block vessel = null;
        for(multiblock.configuration.overhaul.fissionmsr.Block blok : Core.configuration.overhaul.fissionMSR.allBlocks){
            if(blok.fuelVessel)vessel = blok;
        }
        if(vessel==null)throw new IllegalArgumentException("Unknown block: Fuel Vessel");
        JSON.JSONObject fuelVessels = data.getJSONObject("FuelCells");
        HashMap<multiblock.overhaul.fissionmsr.Block, multiblock.configuration.overhaul.fissionmsr.Block> sources = new HashMap<>();
        for(String name : fuelVessels.keySet()){
            String[] fuelSettings = name.split(";");
            String fuelName = fuelSettings[0];
            boolean hasSource = Boolean.parseBoolean(fuelSettings[1]);
            BlockRecipe fuel = null;
            for(BlockRecipe feul : vessel.allRecipes){
                for(String nam : feul.getLegacyNames())if(nam.toLowerCase(Locale.ENGLISH).replace(" ", "").equalsIgnoreCase(fuelName.substring(4).replace(" ", "")))fuel = feul;
            }
            if(fuelName.startsWith("[F4]"))fuelName = fuelName.substring(4)+" Fluoride";
            for(BlockRecipe feul : vessel.allRecipes){
                for(String nam : feul.getLegacyNames())if(nam.toLowerCase(Locale.ENGLISH).replace(" ", "").equalsIgnoreCase(fuelName.replace(" ", "")))fuel = feul;
            }
            if(fuel==null)throw new IllegalArgumentException("Unknown fuel: "+name);
            multiblock.configuration.overhaul.fissionmsr.Block src = null;
            if(hasSource){
                String sourceName = fuelSettings[2];
                if(sourceName.equals("Self"))hasSource = false;
                else{
                    for(multiblock.configuration.overhaul.fissionmsr.Block scr : Core.configuration.overhaul.fissionMSR.allBlocks){
                        if(!scr.source)continue;
                        for(String nam : scr.getLegacyNames())if(nam.equalsIgnoreCase(sourceName))src = scr;
                    }
                    if(src==null)throw new IllegalArgumentException("Unknown source: "+name);
                }
            }
            JSON.JSONArray array = fuelVessels.getJSONArray(name);
            for(Object blok : array){
                JSON.JSONObject blockLoc = (JSON.JSONObject) blok;
                int x = blockLoc.getInt("X");
                int y = blockLoc.getInt("Y");
                int z = blockLoc.getInt("Z");
                msr.setBlockExact(x, y, z, new multiblock.overhaul.fissionmsr.Block(Core.configuration, x, y, z, vessel));
                msr.getBlock(x, y, z).recipe = fuel;
                if(hasSource)sources.put(msr.getBlock(x, y, z), src);
            }
        }
        for(multiblock.overhaul.fissionmsr.Block key : sources.keySet()){
            key.addNeutronSource(msr, sources.get(key));
        }
        NCPFFile file = new NCPFFile();
        msr.buildDefaultCasingOnConvert();
        file.multiblocks.add(msr);
        return file;
    }
}