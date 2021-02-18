package multiblock.overhaul.fissionmsr;
import discord.Bot;
import generator.Priority;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import multiblock.Action;
import multiblock.Direction;
import multiblock.Multiblock;
import multiblock.PartCount;
import multiblock.Range;
import multiblock.action.MSRAllShieldsAction;
import multiblock.action.SetblockAction;
import multiblock.action.SetblocksAction;
import multiblock.configuration.Configuration;
import multiblock.configuration.overhaul.fissionmsr.Fuel;
import multiblock.configuration.overhaul.fissionmsr.IrradiatorRecipe;
import multiblock.configuration.overhaul.fissionmsr.Source;
import multiblock.overhaul.fissionsfr.OverhaulSFR;
import multiblock.ppe.ClearInvalid;
import multiblock.ppe.MSRFill;
import multiblock.ppe.PostProcessingEffect;
import multiblock.ppe.SmartFillOverhaulMSR;
import multiblock.symmetry.AxialSymmetry;
import multiblock.symmetry.Symmetry;
import planner.Core;
import planner.FormattedText;
import planner.Main;
import planner.Task;
import planner.editor.module.Module;
import planner.editor.suggestion.Suggestion;
import planner.editor.suggestion.Suggestor;
import planner.file.NCPFFile;
import planner.menu.component.MenuComponentMinimaList;
import planner.menu.component.generator.MenuComponentMSRToggleFuel;
import planner.menu.component.generator.MenuComponentMSRToggleIrradiatorRecipe;
import planner.menu.component.generator.MenuComponentMSRToggleSource;
import simplelibrary.Stack;
import simplelibrary.config2.Config;
import simplelibrary.config2.ConfigNumberList;
import simplelibrary.opengl.Renderer2D;
public class OverhaulMSR extends Multiblock<Block>{
    public ArrayList<Cluster> clusters = new ArrayList<>();
    private ArrayList<VesselGroup> vesselGroups = new ArrayList<>();
    public int totalFuelVessels;
    public int totalCooling;
    public int totalHeat;
    public int netHeat;
    public float totalEfficiency;
    public float totalHeatMult;
    public int totalIrradiation;
    public int functionalBlocks;
    public float sparsityMult;
    public HashMap<String, Float> totalOutput = new HashMap<>();
    public float totalTotalOutput;
    public float shutdownFactor;
    private int calculationStep = 0;//0 is initial calculation, 1 is shield check, 2 is shutdown factor check
    private ArrayList<VesselGroup> vesselGroupsWereActive = new ArrayList<>();//used for shield check
    private ArrayList<Block> vesselsWereActive = new ArrayList<>();//used for shield check
    public OverhaulMSR(){
        this(null);
    }
    public OverhaulMSR(Configuration configuration){
        this(configuration, 7, 5, 7);
    }
    public OverhaulMSR(Configuration configuration, int x, int y, int z){
        super(configuration, x, y, z);
    }
    @Override
    public String getDefinitionName(){
        return "Overhaul MSR";
    }
    @Override
    public OverhaulMSR newInstance(Configuration configuration){
        return new OverhaulMSR(configuration);
    }
    @Override
    public Multiblock<Block> newInstance(Configuration configuration, int x, int y, int z){
        return new OverhaulMSR(configuration, x, y, z);
    }
    @Override
    public void getAvailableBlocks(List<Block> blocks){
        if(getConfiguration()==null||getConfiguration().overhaul==null||getConfiguration().overhaul.fissionMSR==null)return;
        for(multiblock.configuration.overhaul.fissionmsr.Block block : getConfiguration().overhaul.fissionMSR.allBlocks){
            blocks.add(new Block(getConfiguration(), -1, -1, -1, block));
        }
    }
    @Override
    public int getMinX(){
        return getConfiguration().overhaul.fissionMSR.minSize;
    }
    @Override
    public int getMinY(){
        return getConfiguration().overhaul.fissionMSR.minSize;
    }
    @Override
    public int getMinZ(){
        return getConfiguration().overhaul.fissionMSR.minSize;
    }
    @Override
    public int getMaxX(){
        return getConfiguration().overhaul.fissionMSR.maxSize;
    }
    @Override
    public int getMaxY(){
        return getConfiguration().overhaul.fissionMSR.maxSize;
    }
    @Override
    public int getMaxZ(){
        return getConfiguration().overhaul.fissionMSR.maxSize;
    }
    @Override
    public synchronized void doCalculate(List<Block> blocks){
        Task buildGroups = new Task("Building Vessel Groups");
        Task propogateFlux = new Task("Propogating Neutron Flux");
        Task rePropogateFlux = new Task("Re-propogating Neutron Flux");
        Task postFluxCalc = new Task("Performing Post-Flux Calculations");
        Task calcHeaters = new Task("Calculating Heaters");
        Task buildClusters = new Task("Building Clusters");
        Task calcClusters = new Task("Calculating Clusters");
        Task calcStats = new Task("Calculating Stats");
        Task calcPartialShutdown = new Task("Calculating Partial Shutdown");
        Task calcShutdown = new Task("Calculating Shutdown Factor");
        switch(calculationStep){
            case 0:
                calculateTask.addSubtask(buildGroups);
                calculateTask.addSubtask(propogateFlux);
                calculateTask.addSubtask(rePropogateFlux);
                calculateTask.addSubtask(postFluxCalc);
                calculateTask.addSubtask(calcHeaters);
                calculateTask.addSubtask(buildClusters);
                calculateTask.addSubtask(calcClusters);
                calculateTask.addSubtask(calcStats);
                break;
            case 1:
                calcPartialShutdown.addSubtask(buildGroups);
                calcPartialShutdown.addSubtask(propogateFlux);
                calcPartialShutdown.addSubtask(rePropogateFlux);
                calcPartialShutdown.addSubtask(postFluxCalc);
                calcPartialShutdown.addSubtask(calcHeaters);
                calcPartialShutdown.addSubtask(buildClusters);
                calcPartialShutdown.addSubtask(calcClusters);
                calcPartialShutdown.addSubtask(calcStats);
                calculateTask.addSubtask(calcPartialShutdown);
                break;
            case 2:
                calcShutdown.addSubtask(buildGroups);
                calcShutdown.addSubtask(propogateFlux);
                calcShutdown.addSubtask(rePropogateFlux);
                calcShutdown.addSubtask(postFluxCalc);
                calcShutdown.addSubtask(calcHeaters);
                calcShutdown.addSubtask(buildClusters);
                calcShutdown.addSubtask(calcClusters);
                calcShutdown.addSubtask(calcStats);
                calculateTask.addSubtask(calcShutdown);
                break;
        }
        HashMap<Block, Boolean> shieldsWere = new HashMap<>();
        List<Block> allBlocks = getBlocks();
        if(calculationStep!=1){//temporarily open all shields
            for(Block block : allBlocks){
                shieldsWere.put(block, block.closed);
                block.closed = false;
            }
        }
        vesselGroups.clear();
        for(Block b : allBlocks){
            b.vesselGroup = null;
        }
        for(int i = 0; i<allBlocks.size(); i++){
            Block block = allBlocks.get(i);//detect groups
            VesselGroup group = getVesselGroup(block);
            if(group==null)continue;//that's not a vessel group!
            if(vesselGroups.contains(group))continue;//already know about that one!
            vesselGroups.add(group);
            buildGroups.progress = i/(double)allBlocks.size();
        }
        buildGroups.finish();
        for(int i = 0; i<vesselGroups.size(); i++){
            VesselGroup group = vesselGroups.get(i);
            group.propogateNeutronFlux(this, calculationStep==1&&vesselGroupsWereActive.contains(group));
            propogateFlux.progress = i/(double)vesselGroups.size();
        }
        propogateFlux.finish();
        int lastActive, nowActive;
        int n = 0;
        do{
            n++;
            rePropogateFlux.name = "Re-propogating Neutron Flux"+(n>1?" ("+n+")":"");
            lastActive = 0;
            for(VesselGroup group : vesselGroups){
                boolean wasActive = group.isActive();
                group.hadFlux = group.neutronFlux;
                group.clearData();
                if(wasActive)lastActive+=group.size();
                group.wasActive = wasActive;
            }
            for(int i = 0; i<blocks.size(); i++){
                Block block = blocks.get(i);//why not vessel groups...?
                block.rePropogateNeutronFlux(this, calculationStep==1&&vesselsWereActive.contains(block));
                rePropogateFlux.progress = i/(double)blocks.size();
            }
            nowActive = 0;
            for(VesselGroup group : vesselGroups){
                if(group.isActive())nowActive+=group.size();
                if(!group.wasActive){
                    group.neutronFlux = group.hadFlux;
                }
            }
        }while(nowActive!=lastActive);
        rePropogateFlux.finish();
        for(int i = 0; i<blocks.size(); i++){
            Block block = blocks.get(i);
            if(block.isFuelVessel())block.postFluxCalc(this);
            postFluxCalc.progress = i/(double)blocks.size();
        }
        postFluxCalc.finish();
        boolean somethingChanged;
        n = 0;
        do{
            somethingChanged = false;
            n++;
            calcHeaters.name = "Calculating Heaters"+(n>1?" ("+n+")":"");
            for(int i = 0; i<blocks.size(); i++){
                if(blocks.get(i).calculateHeater(this))somethingChanged = true;
                calcHeaters.progress = i/(double)blocks.size();
            }
        }while(somethingChanged);
        calcHeaters.finish();
        for(VesselGroup group : vesselGroups){
            group.positionalEfficiency*=group.getBunchingFactor();
            for(Block block : group.blocks){
                float criticalityModifier = (float) (1/(1+Math.exp(2*(group.neutronFlux-2*block.vesselGroup.criticality))));
                block.efficiency = block.fuel.efficiency*group.positionalEfficiency*(block.source==null?1:block.source.efficiency)*criticalityModifier;
            }
        }
        for(int i = 0; i<allBlocks.size(); i++){
            Cluster cluster = getCluster(allBlocks.get(i));//detect clusters
            if(cluster==null)continue;//that's not a cluster!
            synchronized(clusters){
                if(clusters.contains(cluster))continue;//already know about that one!
                clusters.add(cluster);
            }
            buildClusters.progress = i/(double)allBlocks.size();
        }
        buildClusters.finish();
        synchronized(clusters){
            for(int i = 0; i<clusters.size(); i++){
                Cluster cluster = clusters.get(i);
                int fuelVessels = 0;
                ArrayList<VesselGroup> alreadyProcessedGroups = new ArrayList<>();
                for(int j = 0; j<cluster.blocks.size(); j++){
                    Block b = cluster.blocks.get(j);
                    if(b.isFuelVesselActive()){
                        if(alreadyProcessedGroups.contains(b.vesselGroup))continue;
                        alreadyProcessedGroups.add(b.vesselGroup);
                        fuelVessels+=b.vesselGroup.size();
                        cluster.efficiency+=b.efficiency;
                        cluster.totalHeat+=b.vesselGroup.moderatorLines*b.fuel.heat*b.vesselGroup.getBunchingFactor();
                        cluster.heatMult+=b.vesselGroup.getHeatMult();
                    }
                    if(b.isHeaterActive()){
                        cluster.totalCooling+=b.template.cooling;
                    }
                    if(b.isShieldActive()){
                        cluster.totalHeat+=b.template.heatMult*b.flux;
                    }
                    if(b.isIrradiatorActive()){
                        cluster.irradiation+=b.flux;
                        if(b.irradiatorRecipe!=null)cluster.totalHeat+=b.irradiatorRecipe.heat*b.flux;
                    }
                    calcClusters.progress = (i+j/(double)cluster.blocks.size())/(double)clusters.size();
                }
                cluster.efficiency/=fuelVessels;
                cluster.heatMult/=fuelVessels;
                if(Double.isNaN(cluster.efficiency))cluster.efficiency = 0;
                if(Double.isNaN(cluster.heatMult))cluster.heatMult = 0;
                cluster.netHeat = cluster.totalHeat-cluster.totalCooling;
                if(cluster.totalCooling==0)cluster.coolingPenaltyMult = 1;
                else cluster.coolingPenaltyMult = Math.min(1, (cluster.totalHeat+getConfiguration().overhaul.fissionMSR.coolingEfficiencyLeniency)/(float)cluster.totalCooling);
                cluster.efficiency*=cluster.coolingPenaltyMult;
                totalFuelVessels+=fuelVessels;
                totalCooling+=cluster.totalCooling;
                totalHeat+=cluster.totalHeat;
                netHeat+=cluster.netHeat;
                totalEfficiency+=cluster.efficiency*fuelVessels;
                totalHeatMult+=cluster.heatMult*fuelVessels;
                totalIrradiation+=cluster.irradiation;
                calcClusters.progress = (i+1)/(double)clusters.size();
            }
        }
        calcClusters.finish();
        totalEfficiency/=totalFuelVessels;
        totalHeatMult/=totalFuelVessels;
        if(Double.isNaN(totalEfficiency))totalEfficiency = 0;
        if(Double.isNaN(totalHeatMult))totalHeatMult = 0;
        functionalBlocks = 0;
        for(Block block : allBlocks){
            if(block.isFunctional())functionalBlocks++;
        }
        int volume = getX()*getY()*getZ();
        sparsityMult = (float) (functionalBlocks/(float)volume>=getConfiguration().overhaul.fissionMSR.sparsityPenaltyThreshold?1:getConfiguration().overhaul.fissionMSR.sparsityPenaltyMult+(1-getConfiguration().overhaul.fissionMSR.sparsityPenaltyMult)*Math.sin(Math.PI*functionalBlocks/(2*volume*getConfiguration().overhaul.fissionMSR.sparsityPenaltyThreshold)));
        totalEfficiency*=sparsityMult;
        synchronized(clusters){
            for(Cluster c : clusters){
                for(Block b : c.blocks){
                    if(b.template.cooling!=0){
                        float out = c.efficiency*sparsityMult*b.template.outputRate;
                        totalOutput.put(b.template.output, (totalOutput.containsKey(b.template.output)?totalOutput.get(b.template.output):0)+out);
                        totalTotalOutput+=out;
                    }
                }
            }
        }
        calcStats.finish();
        for(Block b : shieldsWere.keySet()){
            b.closed = shieldsWere.get(b);
        }
        if(calculationStep!=1){
            calculatePartialShutdown();
        }
        calcPartialShutdown.finish();
        if(calculationStep==0){
            shutdownFactor = calculateShutdownFactor();
        }
        calcShutdown.finish();
    }
    private void calculatePartialShutdown(){
        int last = calculationStep;
        calculationStep = 1;
        vesselsWereActive.clear();
        vesselGroupsWereActive.clear();
        for(Block b : getBlocks())if(b!=null&&b.isFuelVesselActive())vesselsWereActive.add(b);
        for(VesselGroup group : vesselGroups)if(group.isActive())vesselGroupsWereActive.add(group);
        recalculate();
        calculationStep = last;
    }
    private float calculateShutdownFactor(){
        Stack<Action> copy = future.copy();
        calculationStep = 2;
        action(new MSRAllShieldsAction(true), true);
        float offOut = totalTotalOutput;
        undo();
        calculationStep = 0;
        future = copy;
        return 1-(offOut/totalTotalOutput);
    }
    @Override
    protected Block newCasing(int x, int y, int z){
        return new Block(getConfiguration(), x, y, z, null);
    }
    @Override
    public synchronized FormattedText getTooltip(){
        return tooltip(true);
    }
    @Override
    public String getExtraBotTooltip(){
        return tooltip(false).text;
    }
    public FormattedText tooltip(boolean showDetails){
        if(this.showDetails!=null)showDetails = this.showDetails;
        String outs = "";
        ArrayList<String> outputList = new ArrayList<>(totalOutput.keySet());
        Collections.sort(outputList);
        for(String s : outputList){
            if(showDetails)outs+="\n "+Math.round(totalOutput.get(s))+" mb/t of "+s;
        }
        synchronized(clusters){
            int validClusters = 0;
            for(Cluster c : clusters){
                if(c.isValid())validClusters++;
            }
            FormattedText text = new FormattedText("Total output: "+Math.round(totalTotalOutput)+" mb/t"+outs+"\n"
                    + "Total Heat: "+totalHeat+"H/t\n"
                    + "Total Cooling: "+totalCooling+"H/t\n"
                    + "Net Heat: "+netHeat+"H/t\n"
                    + "Overall Efficiency: "+percent(totalEfficiency, 0)+"\n"
                    + "Overall Heat Multiplier: "+percent(totalHeatMult, 0)+"\n"
                    + "Sparsity Penalty Multiplier: "+Math.round(sparsityMult*10000)/10000d+"\n"
                    + "Clusters: "+(validClusters==clusters.size()?clusters.size():(validClusters+"/"+clusters.size()))+"\n"
                    + "Total Irradiation: "+totalIrradiation+"\n"
                    + "Shutdown Factor: "+percent(shutdownFactor, 2));
            text.addText(getModuleTooltip()+"\n");
            for(Fuel f : getConfiguration().overhaul.fissionMSR.allFuels){
                int i = getFuelCount(f);
                if(i>0)text.addText("\n"+f.name+": "+i);
            }
            if(showDetails){
                HashMap<String, Integer> counts = new HashMap<>();
                HashMap<String, Color> colors = new HashMap<>();
                ArrayList<String> order = new ArrayList<>();
                for(Cluster c : clusters){
                    String str = c.getTooltip();
                    if(counts.containsKey(str)){
                        counts.put(str, counts.get(str)+1);
                    }else{
                        counts.put(str, 1);
                        order.add(str);
                    }
                    if(!c.isCreated()){
                        colors.put(str, Core.theme.getRGBA(Color.white));
                    }else if(!c.isConnectedToWall){
                        colors.put(str, Core.theme.getRGBA(Color.pink));
                    }else if(c.netHeat>0)colors.put(str, Core.theme.getRed());
                    else if(c.coolingPenaltyMult!=1)colors.put(str, Core.theme.getBlue());
                }
                for(String str : order){
                    int count = counts.get(str);
                    String s;
                    if(count==1)s="\n\n"+str;
                    else{
                        s="\n\n"+count+" similar clusters:\n\n"+str;
                    }
                    text.addText(s, colors.get(str));
                }
            }
            return text;
        }
    }
    @Override
    public int getMultiblockID(){
        return 2;
    }
    @Override
    protected void save(NCPFFile ncpf, Configuration configuration, Config config){
        ConfigNumberList size = new ConfigNumberList();
        size.add(getX());
        size.add(getY());
        size.add(getZ());
        config.set("size", size);
        boolean compact = isCompact(configuration);//find perfect compression ratio
        config.set("compact", compact);
        ConfigNumberList blox = new ConfigNumberList();
        if(compact){
            for(int x = 0; x<getX(); x++){
                for(int y = 0; y<getY(); y++){
                    for(int z = 0; z<getZ(); z++){
                        Block block = getBlock(x, y, z);
                        if(block==null)blox.add(0);
                        else blox.add(configuration.overhaul.fissionMSR.allBlocks.indexOf(block.template)+1);
                    }
                }
            }
        }else{
            for(Block block : getBlocks()){
                blox.add(block.x);
                blox.add(block.y);
                blox.add(block.z);
                blox.add(configuration.overhaul.fissionMSR.allBlocks.indexOf(block.template)+1);
            }
        }
        ConfigNumberList fuels = new ConfigNumberList();
        ConfigNumberList sources = new ConfigNumberList();
        ConfigNumberList irradiatorRecipes = new ConfigNumberList();
        for(Block block : getBlocks()){
            if(block.template.fuelVessel)fuels.add(configuration.overhaul.fissionMSR.allFuels.indexOf(block.fuel));
            if(block.template.fuelVessel)sources.add(configuration.overhaul.fissionMSR.allSources.indexOf(block.source)+1);
            if(block.template.irradiator)irradiatorRecipes.add(configuration.overhaul.fissionMSR.allIrradiatorRecipes.indexOf(block.irradiatorRecipe)+1);
        }
        config.set("blocks", blox);
        config.set("fuels", fuels);
        config.set("sources", sources);
        config.set("irradiatorRecipes", irradiatorRecipes);
    }
    private boolean isCompact(Configuration configuration){
        int blockCount = getBlocks().size();
        int volume = getX()*getY()*getZ();
        int bitsPerDim = logBase(2, Math.max(getX(), Math.max(getY(), getZ())));
        int bitsPerType = logBase(2, configuration.overhaul.fissionMSR.allBlocks.size());
        int compactBits = bitsPerType*volume;
        int spaciousBits = 4*Math.max(bitsPerDim, bitsPerType)*blockCount;
        return compactBits<spaciousBits;
    }
    private static int logBase(int base, int n){
        return (int)(Math.log(n)/Math.log(base));
    }
    @Override
    public void convertTo(Configuration to){
        if(to.overhaul==null||to.overhaul.fissionMSR==null)return;
        for(Block block : getBlocks()){
            block.convertTo(to);
        }
        configuration = to;
    }
    @Override
    public boolean validate(){
        boolean changed = false;
        BLOCKS:for(Block block : getBlocks()){
            if(block.source!=null){
                for(Direction d : directions){
                    int i = 0;
                    while(true){
                        i++;
                        Block b = getBlock(block.x+d.x*i, block.y+d.y*i, block.z+d.z*i);
                        if(b==null)continue;//air
                        if(b.isCasing())continue BLOCKS;
                        if(b.template.blocksLOS){
                            break;
                        }
                    }
                }
                block.source = null;
                changed = true;
            }
        }
        return changed;
    }
    public Cluster getCluster(Block block){
        if(block==null)return null;
        if(!block.canCluster())return null;
        synchronized(clusters){
            for(Cluster cluster : clusters){
                if(cluster.contains(block))return cluster;
            }
        }
        return new Cluster(block);
    }
    public VesselGroup getVesselGroup(Block block){
        if(block==null)return null;
        if(!block.isFuelVessel())return null;
        for(VesselGroup vesselGroup : vesselGroups){
            if(vesselGroup.contains(block))return vesselGroup;
        }
        return new VesselGroup(block);
    }
    public int getFuelCount(Fuel f){
        int count = 0;
        for(Block block : getBlocks()){
            if(block.fuel==f)count++;
        }
        return count;
    }
    public HashMap<Fuel, Integer> getFuelCounts(){
        HashMap<Fuel, Integer> counts = new HashMap<>();
        for(Fuel f : getConfiguration().overhaul.fissionMSR.allFuels){
            int count = getFuelCount(f);
            if(count!=0)counts.put(f, count);
        }
        return counts;
    }
    public OverhaulSFR convertToSFR(){
        OverhaulSFR sfr = new OverhaulSFR(configuration, getX(), getY(), getZ(), getConfiguration().overhaul.fissionSFR.allCoolantRecipes.get(0));
        for(int x = 0; x<getX(); x++){
            for(int y = 0; y<getY(); y++){
                for(int z = 0; z<getZ(); z++){
                    Block b = getBlock(x, y, z);
                    sfr.setBlockExact(x, y, z, b==null?null:b.convertToSFR());
                }
            }
        }
        sfr.metadata.putAll(metadata);
        return sfr;
    }
    @Override
    public void addGeneratorSettings(MenuComponentMinimaList multiblockSettings){
        if(fuelToggles==null)fuelToggles = new HashMap<>();
        if(sourceToggles==null)sourceToggles = new HashMap<>();
        if(irradiatorRecipeToggles==null)irradiatorRecipeToggles = new HashMap<>();
        fuelToggles.clear();
        for(Fuel f : getConfiguration().overhaul.fissionMSR.allFuels){
            MenuComponentMSRToggleFuel toggle = new MenuComponentMSRToggleFuel(f);
            fuelToggles.put(f, toggle);
            multiblockSettings.add(toggle);
        }
        sourceToggles.clear();
        for(Source s : getConfiguration().overhaul.fissionMSR.allSources){
            MenuComponentMSRToggleSource toggle = new MenuComponentMSRToggleSource(s);
            sourceToggles.put(s, toggle);
            multiblockSettings.add(toggle);
        }
        irradiatorRecipeToggles.clear();
        for(IrradiatorRecipe r : getConfiguration().overhaul.fissionMSR.allIrradiatorRecipes){
            MenuComponentMSRToggleIrradiatorRecipe toggle = new MenuComponentMSRToggleIrradiatorRecipe(r);
            irradiatorRecipeToggles.put(r, toggle);
            multiblockSettings.add(toggle);
        }
    }
    private HashMap<Fuel, MenuComponentMSRToggleFuel> fuelToggles;
    public ArrayList<Range<Fuel>> validFuels = new ArrayList<>();
    public void setValidFuels(ArrayList<Range<Fuel>> fuels){
        validFuels = fuels;
    }
    public ArrayList<Range<Fuel>> getValidFuels(){
        if(fuelToggles==null){
            return validFuels;
        }
        ArrayList<Range<Fuel>> validFuels = new ArrayList<>();
        for(Fuel f :fuelToggles.keySet()){
            if(fuelToggles.get(f).enabled)validFuels.add(new Range<>(f,fuelToggles.get(f).min,fuelToggles.get(f).max));
        }
        return validFuels;
    }
    private HashMap<Source, MenuComponentMSRToggleSource> sourceToggles;
    public ArrayList<Range<Source>> validSources = new ArrayList<>();
    public void setValidSources(ArrayList<Range<Source>> sources){
        validSources = sources;
    }
    public ArrayList<Range<Source>> getValidSources(){
        if(sourceToggles==null){
            return validSources;
        }
        ArrayList<Range<Source>> validSources = new ArrayList<>();
        for(Source s :sourceToggles.keySet()){
            if(sourceToggles.get(s).enabled)validSources.add(new Range<>(s,sourceToggles.get(s).min,sourceToggles.get(s).max));
        }
        return validSources;
    }
    private HashMap<IrradiatorRecipe, MenuComponentMSRToggleIrradiatorRecipe> irradiatorRecipeToggles;
    public ArrayList<Range<IrradiatorRecipe>> validIrradiatorRecipes = new ArrayList<>();
    public void setValidIrradiatorRecipes(ArrayList<Range<IrradiatorRecipe>> irradiatorRecipes){
        validIrradiatorRecipes = irradiatorRecipes;
    }
    public ArrayList<Range<IrradiatorRecipe>> getValidIrradiatorRecipes(){
        if(irradiatorRecipeToggles==null){
            return validIrradiatorRecipes;
        }
        ArrayList<Range<IrradiatorRecipe>> validIrradiatorRecipes = new ArrayList<>();
        for(IrradiatorRecipe r :irradiatorRecipeToggles.keySet()){
            if(irradiatorRecipeToggles.get(r).enabled)validIrradiatorRecipes.add(new Range<>(r,irradiatorRecipeToggles.get(r).min,irradiatorRecipeToggles.get(r).max));
        }
        return validIrradiatorRecipes;
    }
    private boolean isValid(){
        return totalTotalOutput>0;
    }
    private int getBadVessels(){
        int badVessels = 0;
        for(Block b : getBlocks()){
            if(b.isFuelVessel()&&!b.isFuelVesselActive())badVessels++;
        }
        return badVessels;
    }
    private int getVessels(){
        int vessels = 0;
        for(Block b : getBlocks()){
            if(b.isFuelVessel())vessels++;
        }
        return vessels;
    }
    @Override
    public void getGenerationPriorities(ArrayList<Priority> priorities){
        priorities.add(new Priority<OverhaulMSR>("Valid (>0 output)", true, true){
            @Override
            protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                if(main.isValid()&&!other.isValid())return 1;
                if(!main.isValid()&&other.isValid())return -1;
                return 0;
            }
        });
        priorities.add(new Priority<OverhaulMSR>("Minimize Bad Vessels", true, true){
            @Override
            protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                return other.getBadVessels()-main.getBadVessels();
            }
        });
        priorities.add(new Priority<OverhaulMSR>("Shutdownable", true, true){
            @Override
            protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                return main.shutdownFactor-other.shutdownFactor;
            }
        });
        priorities.add(new Priority<OverhaulMSR>("Stability", false, true){
            @Override
            protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                return Math.max(0, other.netHeat)-Math.max(0, main.netHeat);
            }
        });
        priorities.add(new Priority<OverhaulMSR>("Efficiency", true, true){
            @Override
            protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                return (int) Math.round(main.totalEfficiency*100-other.totalEfficiency*100);
            }
        });
        priorities.add(new Priority<OverhaulMSR>("Output", true, true){
            @Override
            protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                return main.totalTotalOutput-other.totalTotalOutput;
            }
        });
        priorities.add(new Priority<OverhaulMSR>("Irradiation", true, true){
            @Override
            protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                return main.totalIrradiation-other.totalIrradiation;
            }
        });
        priorities.add(new Priority<OverhaulMSR>("Vessel Count", true, true){
            @Override
            protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                return main.getVessels()-other.getVessels();
            }
        });
        for(Module m : Core.modules){
            if(m.isActive())m.getGenerationPriorities(this, priorities);
        }
    }
    @Override
    public void getGenerationPriorityPresets(ArrayList<Priority> priorities, ArrayList<Priority.Preset> presets){
        presets.add(new Priority.Preset("Efficiency", priorities.get(0), priorities.get(1), priorities.get(2), priorities.get(3), priorities.get(4), priorities.get(5)).addAlternative("Efficient"));
        presets.add(new Priority.Preset("Output", priorities.get(0), priorities.get(1), priorities.get(2), priorities.get(3), priorities.get(5), priorities.get(4)));
        presets.add(new Priority.Preset("Irradiation", priorities.get(0), priorities.get(1), priorities.get(2), priorities.get(3), priorities.get(6), priorities.get(4), priorities.get(5)).addAlternative("Irradiate").addAlternative("Irradiator"));
    }
    @Override
    public void getSymmetries(ArrayList<Symmetry> symmetries){
        symmetries.add(AxialSymmetry.X);
        symmetries.add(AxialSymmetry.Y);
        symmetries.add(AxialSymmetry.Z);
    }
    @Override
    public void getPostProcessingEffects(ArrayList<PostProcessingEffect> postProcessingEffects){
        postProcessingEffects.add(new ClearInvalid());
        postProcessingEffects.add(new SmartFillOverhaulMSR());
        for(multiblock.configuration.overhaul.fissionmsr.Block b : getConfiguration().overhaul.fissionMSR.allBlocks){
            if(b.conductor||(b.cluster&&!b.functional))postProcessingEffects.add(new MSRFill(b));
        }
    }
    @Override
    protected void getFluidOutputs(HashMap<String, Double> outputs){
        for(String key : totalOutput.keySet()){
            outputs.put(key, (double)totalOutput.get(key));
        }
    }
    public class Cluster{
        public ArrayList<Block> blocks = new ArrayList<>();
        public boolean isConnectedToWall = false;
        public float efficiency;
        public int totalHeat, totalCooling, netHeat;
        public float heatMult, coolingPenaltyMult;
        public int irradiation;
        public Cluster(Block block){
            blocks.addAll(toList(getBlocks(block, false)));
            isConnectedToWall = wallCheck(blocks);
            if(!isConnectedToWall){
                isConnectedToWall = wallCheck(toList(getBlocks(block, true)));
            }
            for(Block b : blocks){
                b.cluster = this;
            }
        }
        private Cluster(){}
        private boolean isValid(){
            return isConnectedToWall&&isCreated();
        }
        public boolean isCreated(){
            for(Block block : blocks){
                if(block.template.createCluster)return true;
            }
            return false;
        }
        public boolean contains(Block block){
            return blocks.contains(block);
        }
        private boolean wallCheck(ArrayList<Block> blocks){
            for(Block block : blocks){
                if(block.x==0||block.y==0||block.z==0)return true;
                if(block.x==getX()-1||block.y==getY()-1||block.z==getZ()-1)return true;
            }
            return false;
        }
        public String getTooltip(){
            if(!isCreated())return "Invalid cluster!";
            if(!isValid())return "Cluster is not connected to the casing!";
            return "Efficiency: "+percent(efficiency, 0)+"\n"
                + "Total Heating: "+totalHeat+"H/t\n"
                + "Total Cooling: "+totalCooling+"H/t\n"
                + "Net Heating: "+netHeat+"H/t\n"
                + "Heat Multiplier: "+percent(heatMult, 0)+"\n"
                + "Cooling penalty mult: "+Math.round(coolingPenaltyMult*10000)/10000d;
        }
        private Cluster copy(OverhaulMSR newMSR){
            Cluster copy = new Cluster();
            for(Block b : blocks){
                copy.blocks.add(newMSR.getBlock(b.x, b.y, b.z));
            }
            copy.isConnectedToWall = isConnectedToWall;
            copy.efficiency = efficiency;
            copy.totalHeat = totalHeat;
            copy.totalCooling = totalCooling;
            copy.netHeat = netHeat;
            copy.heatMult = heatMult;
            copy.coolingPenaltyMult = coolingPenaltyMult;
            copy.irradiation = irradiation;
            return copy;
        }
        /**
         * Block search algorithm from my Tree Feller for Bukkit.
         */
        private HashMap<Integer, ArrayList<Block>> getBlocks(Block start, boolean useConductors){
            //layer zero
            HashMap<Integer, ArrayList<Block>>results = new HashMap<>();
            ArrayList<Block> zero = new ArrayList<>();
            if(start.canCluster()||(useConductors&&start.isConductor())){
                zero.add(start);
            }
            results.put(0, zero);
            //all the other layers
            int maxDistance = getX()*getY()*getZ();//the algorithm requires a max search distance. Rather than changing that, I'll just be lazy and give it a big enough number
            for(int i = 0; i<maxDistance; i++){
                ArrayList<Block> layer = new ArrayList<>();
                ArrayList<Block> lastLayer = new ArrayList<>(results.get(i));
                if(i==0&&lastLayer.isEmpty()){
                    lastLayer.add(start);
                }
                for(Block block : lastLayer){
                    FOR:for(int j = 0; j<6; j++){
                        int dx=0,dy=0,dz=0;
                        switch(j){//This is a primitive version of the Direction class used in other places here, but I'll just leave it as it is
                            case 0:
                                dx = -1;
                                break;
                            case 1:
                                dx = 1;
                                break;
                            case 2:
                                dy = -1;
                                break;
                            case 3:
                                dy = 1;
                                break;
                            case 4:
                                dz = -1;
                                break;
                            case 5:
                                dz = 1;
                                break;
                            default:
                                throw new IllegalArgumentException("How did this happen?");
                        }
                        Block newBlock = getBlock(block.x+dx,block.y+dy,block.z+dz);
                        if(newBlock==null)continue;
                        if(!(newBlock.canCluster()||(useConductors&&newBlock.isConductor()))){//that's not part of this bunch
                            continue;
                        }
                        for(Block oldbl : lastLayer){//if(lastLayer.contains(newBlock))continue;//if the new block is on the same layer, ignore
                            if(oldbl==newBlock){
                                continue FOR;
                            }
                        }
                        if(i>0){
                            for(Block oldbl : results.get(i-1)){//if(i>0&&results.get(i-1).contains(newBlock))continue;//if the new block is on the previous layer, ignore
                                if(oldbl==newBlock){
                                    continue FOR;
                                }
                            }
                        }
                        for(Block oldbl : layer){//if(layer.contains(newBlock))continue;//if the new block is on the next layer, but already processed, ignore
                            if(oldbl==newBlock){
                                continue FOR;
                            }
                        }
                        layer.add(newBlock);
                    }
                }
                if(layer.isEmpty())break;
                results.put(i+1, layer);
            }
            return results;
        }
        public boolean contains(int x, int y, int z){
            for(Block b : blocks){
                if(b.x==x&&b.y==y&&b.z==z)return true;
            }
            return false;
        }
    }
    public class VesselGroup{
        public ArrayList<Block> blocks = new ArrayList<>();
        public int criticality = 0;
        public int neutronFlux = 0;
        public int moderatorLines;
        public float positionalEfficiency;
        public int hadFlux;
        public boolean wasActive;
        public int openFaces = -1; 
        public VesselGroup(Block block){
            blocks.addAll(toList(getBlocks(block)));
            int fuelCriticality = 0;
            for(Block b : blocks){
                fuelCriticality = b.fuel.criticality;
                b.vesselGroup = this;
            }
            criticality = fuelCriticality*getSurfaceFactor();
        }
        private VesselGroup(){}
        public boolean contains(Block block){
            return blocks.contains(block);
        }
        /**
         * Block search algorithm from my Tree Feller for Bukkit.
         */
        private HashMap<Integer, ArrayList<Block>> getBlocks(Block start){
            //layer zero
            HashMap<Integer, ArrayList<Block>>results = new HashMap<>();
            ArrayList<Block> zero = new ArrayList<>();
            if(start.isFuelVessel()){
                zero.add(start);
            }
            results.put(0, zero);
            //all the other layers
            int maxDistance = getX()*getY()*getZ();//the algorithm requires a max search distance. Rather than changing that, I'll just be lazy and give it a big enough number
            for(int i = 0; i<maxDistance; i++){
                ArrayList<Block> layer = new ArrayList<>();
                ArrayList<Block> lastLayer = new ArrayList<>(results.get(i));
                if(i==0&&lastLayer.isEmpty()){
                    lastLayer.add(start);
                }
                for(Block block : lastLayer){
                    FOR:for(int j = 0; j<6; j++){
                        int dx=0,dy=0,dz=0;
                        switch(j){//This is a primitive version of the Direction class used in other places here, but I'll just leave it as it is
                            case 0:
                                dx = -1;
                                break;
                            case 1:
                                dx = 1;
                                break;
                            case 2:
                                dy = -1;
                                break;
                            case 3:
                                dy = 1;
                                break;
                            case 4:
                                dz = -1;
                                break;
                            case 5:
                                dz = 1;
                                break;
                            default:
                                throw new IllegalArgumentException("How did this happen?");
                        }
                        Block newBlock = getBlock(block.x+dx,block.y+dy,block.z+dz);
                        if(newBlock==null)continue;
                        if(!newBlock.isFuelVessel()||newBlock.fuel!=start.fuel){//that's not part of this bunch
                            continue;
                        }
                        for(Block oldbl : lastLayer){//if(lastLayer.contains(newBlock))continue;//if the new block is on the same layer, ignore
                            if(oldbl==newBlock){
                                continue FOR;
                            }
                        }
                        if(i>0){
                            for(Block oldbl : results.get(i-1)){//if(i>0&&results.get(i-1).contains(newBlock))continue;//if the new block is on the previous layer, ignore
                                if(oldbl==newBlock){
                                    continue FOR;
                                }
                            }
                        }
                        for(Block oldbl : layer){//if(layer.contains(newBlock))continue;//if the new block is on the next layer, but already processed, ignore
                            if(oldbl==newBlock){
                                continue FOR;
                            }
                        }
                        layer.add(newBlock);
                    }
                }
                if(layer.isEmpty())break;
                results.put(i+1, layer);
            }
            return results;
        }
        public int size(){
            return blocks.size();
        }
        public int getOpenFaces(){
            if(openFaces==-1){
                int open = 0;
                for(Block b1 : blocks){
                    DIRECTION:for(Direction d : directions){
                        int x = b1.x+d.x;
                        int y = b1.y+d.y;
                        int z = b1.z+d.z;
                        for(Block b2 : blocks){
                            if(b2.x==x&&b2.y==y&&b2.z==z)continue DIRECTION;
                        }
                        open++;
                    }
                }
                openFaces = open;
            }
            return openFaces;
        }
        public int getBunchingFactor(){
            return 6*size()/getOpenFaces();
        }
        public int getSurfaceFactor(){
            return getOpenFaces()/6;
        }
        private boolean isActive(){
            return neutronFlux>=criticality;
        }
        private void clearData(){
            for(Block b : blocks)b.clearData();
            openFaces = -1;
            wasActive = false;
            neutronFlux = 0;
            positionalEfficiency = 0;
            moderatorLines = 0;
        }
        public float getHeatMult(){
            return moderatorLines*getBunchingFactor();
        }
        public int getRequiredSources(){
            return getSurfaceFactor();
        }
        public int getSources(){
            int sources = 0;
            for(Block b : blocks){
                if(b.isPrimed())sources++;
            }
            return sources;
        }
        public boolean isPrimed(){
            return getSources()>=getRequiredSources();
        }
        public void propogateNeutronFlux(OverhaulMSR msr, boolean force){
            for(Block b : blocks){
                b.propogateNeutronFlux(msr, force);
            }
        }
        public void rePropogateNeutronFlux(OverhaulMSR msr, boolean force){
            for(Block b : blocks){
                b.rePropogateNeutronFlux(msr, force);
            }
        }
    }
    @Override
    public synchronized void clearData(List<Block> blocks){
        super.clearData(blocks);
        synchronized(clusters){
            clusters.clear();
        }
        totalOutput.clear();
        shutdownFactor = totalTotalOutput = totalEfficiency = totalHeatMult = sparsityMult = totalFuelVessels = totalCooling = totalHeat = netHeat = totalIrradiation = functionalBlocks = 0;
    }
    /**
     * Converts the tiered search returned by getBlocks into a list of blocks.<br>
     * Also from my tree feller
     */
    private static ArrayList<Block> toList(HashMap<Integer, ArrayList<Block>> blocks){
        ArrayList<Block> list = new ArrayList<>();
        for(int i : blocks.keySet()){
            list.addAll(blocks.get(i));
        }
        return list;
    }
    @Override
    public boolean exists(){
        return getConfiguration().overhaul!=null&&getConfiguration().overhaul.fissionMSR!=null;
    }
    @Override
    public OverhaulMSR blankCopy(){
        return new OverhaulMSR(configuration, getX(), getY(), getZ());
    }
    @Override
    public synchronized OverhaulMSR doCopy(){
        OverhaulMSR copy = blankCopy();
        for(int x = 0; x<getX(); x++){
            for(int y = 0; y<getY(); y++){
                for(int z = 0; z<getZ(); z++){
                    Block get = getBlock(x, y, z);
                    if(get!=null)copy.setBlockExact(x, y, z, get.copy());
                }
            }
        }
        synchronized(clusters){
            for(Cluster cluster : clusters){
                copy.clusters.add(cluster.copy(copy));
            }
        }
        copy.totalFuelVessels = totalFuelVessels;
        copy.totalCooling = totalCooling;
        copy.totalHeat = totalHeat;
        copy.netHeat = netHeat;
        copy.totalEfficiency = totalEfficiency;
        copy.totalHeatMult = totalHeatMult;
        copy.totalIrradiation = totalIrradiation;
        copy.functionalBlocks = functionalBlocks;
        copy.sparsityMult = sparsityMult;
        copy.totalOutput.putAll(totalOutput);
        copy.totalTotalOutput = totalTotalOutput;
        copy.shutdownFactor = shutdownFactor;
        return copy;
    }
    @Override
    protected int doCount(Object o){
        int count = 0;
        if(o instanceof Fuel){
            Fuel f = (Fuel)o;
            for(int x = 0; x<getX(); x++){
                for(int y = 0; y<getY(); y++){
                    for(int z = 0; z<getZ(); z++){
                        Block b = getBlock(x, y, z);
                        if(b==null)continue;
                        if(b.fuel==f)count++;
                    }
                }
            }
            return count;
        }
        if(o instanceof Source){
            Source s = (Source)o;
            for(int x = 0; x<getX(); x++){
                for(int y = 0; y<getY(); y++){
                    for(int z = 0; z<getZ(); z++){
                        Block b = getBlock(x, y, z);
                        if(b==null)continue;
                        if(b.source==s)count++;
                    }
                }
            }
            return count;
        }
        if(o instanceof IrradiatorRecipe){
            IrradiatorRecipe r = (IrradiatorRecipe)o;
            for(int x = 0; x<getX(); x++){
                for(int y = 0; y<getY(); y++){
                    for(int z = 0; z<getZ(); z++){
                        Block b = getBlock(x, y, z);
                        if(b==null)continue;
                        if(b.irradiatorRecipe==r)count++;
                    }
                }
            }
            return count;
        }
        throw new IllegalArgumentException("Cannot count "+o.getClass().getName()+" in "+getDefinitionName()+"!");
    }
    @Override
    public String getGeneralName(){
        return "Reactor";
    }
    @Override
    public boolean isCompatible(Multiblock<Block> other){
        return true;
    }
    @Override
    protected void getExtraParts(ArrayList<PartCount> parts){
        int sources = 0;
        for(Source s : getConfiguration().overhaul.fissionMSR.allSources){
            int num = count(s);
            sources+=num;
            if(num>0){
                Core.BufferRenderer renderer = (buff) -> {
                    float fac = (float) Math.pow(s.efficiency, 10);
                    float r = Math.min(1, -2*fac+2);
                    float g = Math.min(1, fac*2);
                    float b = 0;
                    Core.applyColor(Core.theme.getRGBA(r, g, b, 1));
                    Renderer2D.drawRect(0, 0, buff.width, buff.height, Core.sourceCircle);
                    Core.applyWhite();
                };
                parts.add(new PartCount(Main.isBot?Bot.makeImage(64, 64, renderer):Core.makeImage(64, 64, renderer), s.name+" Neutron Source", num));
            }
        }
        parts.add(new PartCount(null, "Casing", (getX()+2)*(getZ()+2)*2+(getX()+2)*getY()*2+getY()*getZ()*2-1-sources));
    }
    @Override
    public String getDescriptionTooltip(){
        return "Overhaul MSRs are Molten Salt Fission reactors in NuclearCraft: Overhauled";
    }
    @Override
    public void getSuggestors(ArrayList<Suggestor> suggestors){
        suggestors.add(new Suggestor<OverhaulMSR>("Fuel Vessel Suggestor", -1, -1){
            ArrayList<Priority> priorities = new ArrayList<>();
            {
                priorities.add(new Priority<OverhaulMSR>("Efficiency", true, true){
                    @Override
                    protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                        return main.totalEfficiency-other.totalEfficiency;
                    }
                });
                priorities.add(new Priority<OverhaulMSR>("Output", true, true){
                    @Override
                    protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                        return main.totalTotalOutput-other.totalTotalOutput;
                    }
                });
            }
            @Override
            public String getDescription(){
                return "Suggests adding Fuel vessels with moderators to increase efficiency and output";
            }
            @Override
            public void generateSuggestions(OverhaulMSR multiblock, Suggestor.SuggestionAcceptor suggestor){
                ArrayList<Block> vessels = new ArrayList<>();
                multiblock.getAvailableBlocks(vessels);
                for(Iterator<Block> it = vessels.iterator(); it.hasNext();){
                    Block b = it.next();
                    if(!b.isFuelVessel())it.remove();
                }
                ArrayList<Block> moderators = new ArrayList<>();
                multiblock.getAvailableBlocks(moderators);
                for(Iterator<Block> it = moderators.iterator(); it.hasNext();){
                    Block b = it.next();
                    if(!b.isModerator())it.remove();
                }
                HashSet<Fuel> fuels = new HashSet<>();
                int vesselCount = 0;
                for(int y = 0; y<multiblock.getY(); y++){
                    for(int z = 0; z<multiblock.getZ(); z++){
                        for(int x = 0; x<multiblock.getX(); x++){
                            Block b = multiblock.getBlock(x, y, z);
                            if(b!=null&&b.isFuelVessel())vesselCount++;
                            if(b!=null&&b.fuel!=null)fuels.add(b.fuel);
                        }
                    }
                }
                suggestor.setCount((multiblock.getX()*multiblock.getY()*multiblock.getZ()-vesselCount)*vessels.size()*moderators.size());
                for(Block vessel : vessels){
                    for(Block moderator : moderators){
                        for(int y = 0; y<multiblock.getY(); y++){
                            for(int z = 0; z<multiblock.getZ(); z++){
                                for(int x = 0; x<multiblock.getX(); x++){
                                    Block was = multiblock.getBlock(x, y, z);
                                    if(was!=null&&was.isFuelVessel())continue;
                                    for(Fuel fuel : fuels){
                                        ArrayList<Action> actions = new ArrayList<>();
                                        Block ce = (Block)vessel.newInstance(x, y, z);
                                        ce.fuel = fuel;
                                        actions.add(new SetblockAction(x, y, z, ce));
                                        SetblocksAction multi = new SetblocksAction(moderator);
                                        DIRECTION:for(Direction d : directions){
                                            ArrayList<int[]> toSet = new ArrayList<>();
                                            boolean yep = false;
                                            for(int i = 1; i<=configuration.overhaul.fissionMSR.neutronReach+1; i++){
                                                int X = x+d.x*i;
                                                int Y = y+d.y*i;
                                                int Z = z+d.z*i;
                                                Block b = multiblock.getBlock(X, Y, Z);
                                                if(b!=null){
                                                    if(b.isCasing())break;//end of the line
                                                    if(b.isModerator())continue;//already a moderator
                                                    if(b.isFuelVessel()){
                                                        yep = true;
                                                        break;
                                                    }
                                                }
                                                if(i<=configuration.overhaul.fissionMSR.neutronReach){
                                                    toSet.add(new int[]{X,Y,Z});
                                                }
                                            }
                                            if(yep){
                                                for(int[] b : toSet)multi.add(b[0], b[1], b[2]);
                                            }
                                        }
                                        if(!multi.isEmpty())actions.add(multi);
                                        if(suggestor.acceptingSuggestions())suggestor.suggest(new Suggestion("Add "+vessel.getName()+(multi.isEmpty()?"":" with "+moderator.getName()), actions, priorities));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        suggestors.add(new Suggestor<OverhaulMSR>("Moderator Line Upgrader", -1, -1){
            ArrayList<Priority> priorities = new ArrayList<>();
            {
                priorities.add(new Priority<OverhaulMSR>("Efficiency", true, true){
                    @Override
                    protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                        return (int) Math.round(main.totalEfficiency*100-other.totalEfficiency*100);
                    }
                });
                priorities.add(new Priority<OverhaulMSR>("Irradiation", true, true){
                    @Override
                    protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                        return main.totalIrradiation-other.totalIrradiation;
                    }
                });
            }
            @Override
            public String getDescription(){
                return "Suggests changing moderator lines to increase efficiency or irradiation";
            }
            @Override
            public void generateSuggestions(OverhaulMSR multiblock, Suggestor.SuggestionAcceptor suggestor){
                ArrayList<Block> moderators = new ArrayList<>();
                multiblock.getAvailableBlocks(moderators);
                for(Iterator<Block> it = moderators.iterator(); it.hasNext();){
                    Block block = it.next();
                    if(!block.isModerator()||block.template.flux<=0)it.remove();
                }
                int count = 0;
                for(Block block : multiblock.getBlocks()){
                    if(block.isFuelVessel())count++;
                }
                suggestor.setCount(count*6*moderators.size());
                for(Block block : multiblock.getBlocks()){
                    if(!block.isFuelVessel())continue;
                    DIRECTION:for(Direction d : directions){
                        ArrayList<Block> line = new ArrayList<>();
                        int x = block.x;
                        int y = block.y;
                        int z = block.z;
                        for(int i = 0; i<getConfiguration().overhaul.fissionMSR.neutronReach+1; i++){
                            x+=d.x;
                            y+=d.y;
                            z+=d.z;
                            Block b = multiblock.getBlock(x, y, z);
                            if(b==null){
                                suggestor.task.max--;
                                continue DIRECTION;
                            }
                            if(!b.isModerator()){
                                if(b.isFuelVessel()||b.isIrradiator()||b.isReflector())break;
                                suggestor.task.max--;
                                continue DIRECTION;
                            }
                            line.add(b);
                        }
                        if(line.size()>getConfiguration().overhaul.fissionMSR.neutronReach){
                            suggestor.task.max--;
                            continue;
                        }//too long
                        for(Block mod : moderators){
                            ArrayList<Action> actions = new ArrayList<>();
                            for(Block b : line){
                                actions.add(new SetblockAction(b.x, b.y, b.z, mod.newInstance(b.x, b.y, b.z)));
                            }
                            suggestor.suggest(new Suggestion("Replace Moderator Line with "+mod.getName().replace(" Moderator", ""), actions, priorities));
                        }
                    }
                }
            }
        });
        suggestors.add(new Suggestor<OverhaulMSR>("Single Moderator Upgrader", -1, -1){
            ArrayList<Priority> priorities = new ArrayList<>();
            {
                priorities.add(new Priority<OverhaulMSR>("Efficiency", true, true){
                    @Override
                    protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                        return (int) Math.round(main.totalEfficiency*100-other.totalEfficiency*100);
                    }
                });
                priorities.add(new Priority<OverhaulMSR>("Irradiation", true, true){
                    @Override
                    protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                        return main.totalIrradiation-other.totalIrradiation;
                    }
                });
            }
            @Override
            public String getDescription(){
                return "Suggests changing single moderators to increase efficiency or irradiation";
            }
            @Override
            public void generateSuggestions(OverhaulMSR multiblock, Suggestor.SuggestionAcceptor suggestor){
                ArrayList<Block> blocks = new ArrayList<>();
                multiblock.getAvailableBlocks(blocks);
                for(Iterator<Block> it = blocks.iterator(); it.hasNext();){
                    Block b = it.next();
                    if(!b.isModerator()||b.template.flux<=0)it.remove();
                }
                int count = 0;
                for(Block b : multiblock.getBlocks()){
                    if(b.isModerator())count++;
                }
                suggestor.setCount(count*blocks.size());
                for(Block block : multiblock.getBlocks()){
                    if(!block.isModerator())continue;
                    for(Block b : blocks){
                        suggestor.suggest(new Suggestion("Upgrade Moderator from "+block.getName().replace(" Moderator", "")+" to "+b.getName().replace(" Moderator", ""), new SetblockAction(block.x, block.y, block.z, b.newInstance(block.x, block.y, block.z)), priorities));
                    }
                }
            }
        });
        suggestors.add(new Suggestor<OverhaulMSR>("Heatsink Suggestor", -1, -1){
            ArrayList<Priority> priorities = new ArrayList<>();
            {
                priorities.add(new Priority<OverhaulMSR>("Temperature", true, true){
                    @Override
                    protected double doCompare(OverhaulMSR main, OverhaulMSR other){
                        return other.netHeat-main.netHeat;
                    }
                });
            }
            @Override
            public String getDescription(){
                return "Suggests adding or replacing heat sinks to cool the reactor";
            }
            @Override
            public void generateSuggestions(OverhaulMSR multiblock, Suggestor.SuggestionAcceptor suggestor){
                ArrayList<Block> blocks = new ArrayList<>();
                multiblock.getAvailableBlocks(blocks);
                for(Iterator<Block> it = blocks.iterator(); it.hasNext();){
                    Block b = it.next();
                    if(!b.isHeater())it.remove();
                }
                int count = 0;
                for(int x = 0; x<multiblock.getX(); x++){
                    for(int y = 0; y<multiblock.getY(); y++){
                        for(int z = 0; z<multiblock.getZ(); z++){
                            Block block = multiblock.getBlock(x, y, z);
                            if(block==null||block.canBeQuickReplaced()){
                                count++;
                            }
                        }
                    }
                }
                suggestor.setCount(count*blocks.size());
                for(int x = 0; x<multiblock.getX(); x++){
                    for(int y = 0; y<multiblock.getY(); y++){
                        for(int z = 0; z<multiblock.getZ(); z++){
                            for(Block newBlock : blocks){
                                Block block = multiblock.getBlock(x, y, z);
                                if(block==null||block.canBeQuickReplaced()){
                                    if(newBlock.template.cooling>(block==null?0:block.template.cooling)&&multiblock.isValid(newBlock, x, y, z))suggestor.suggest(new Suggestion(block==null?"Add "+newBlock.getName():"Replace "+block.getName()+" with "+newBlock.getName(), new SetblockAction(x, y, z, newBlock.newInstance(x, y, z)), priorities));
                                    else suggestor.task.max--;
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}