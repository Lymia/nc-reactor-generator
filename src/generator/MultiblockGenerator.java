package generator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import multiblock.Block;
import multiblock.Multiblock;
import multiblock.Range;
import planner.FormattedText;
import planner.exception.MissingConfigurationEntryException;
import planner.menu.component.MenuComponentMinimaList;
public abstract class MultiblockGenerator{
    public int iterations = 0;
    public final Object iterationSynchronizer = new Object();
    protected Random rand = new MersenneTwister();
    public static final ArrayList<MultiblockGenerator> generators = new ArrayList<>();
    static{
        generators.add(new OverhaulTurbineStandardGenerator(null));
        generators.add(new CoreBasedGenerator(null));
        generators.add(new StandardGenerator(null));
        generators.add(new NoveltySearchGenerator(null));
    }
    protected ArrayList<Priority> priorities;
    private Object threadronyzer = new Object();
    public final Multiblock multiblock;
    private ArrayList<UUID> threads = new ArrayList<>();
    private HashMap<UUID, Long> crashedThreads = new HashMap<>();
    public MultiblockGenerator(Multiblock multiblock){
        this.multiblock = multiblock;
        if(multiblock!=null)priorities = multiblock.getGenerationPriorities();
    }
    public abstract MultiblockGenerator newInstance(Multiblock multi);
    public abstract ArrayList<Multiblock>[] getMultiblockLists();
    public abstract boolean canGenerateFor(Multiblock multiblock);
    public abstract String getName();
    public static ArrayList<MultiblockGenerator> getGenerators(Multiblock m){
        ArrayList<MultiblockGenerator> valid = new ArrayList<>();
        for(MultiblockGenerator gen : generators){
            if(gen.canGenerateFor(m))valid.add(gen);
        }
        return valid;
    }
    public abstract void addSettings(MenuComponentMinimaList generatorSettings, Multiblock multi);
    public abstract void refreshSettingsFromGUI(ArrayList<Range<Block>> allowedBlocks);
    public abstract void refreshSettings(Settings settings);
    public int getActiveThreads(){
        synchronized(threadronyzer){
            return threads.size();
        }
    }
    /**
     * MULTITHREADED
     */
    public abstract void tick();
    public void startThread(){
        synchronized(threadronyzer){
            UUID uid = UUID.randomUUID();
            threads.add(uid);
            Thread thread = new Thread(() -> {
                try{
                    while(threads.contains(uid)){
                        tick();
                    }
                }catch(Exception ex){
                    threads.remove(uid);
                    crashedThreads.put(uid, System.nanoTime());
                    throw new RuntimeException(ex);
                }
            });
            thread.setName("Generation Thread "+uid.toString());
            thread.setDaemon(true);
            thread.start();
        }
    }
    public void stopThread(){
        synchronized(threadronyzer){
            threads.remove(0);
        }
    }
    public void stopAllThreads(){
        synchronized(threadronyzer){
            threads.clear();
        }
        System.out.println(iterations+" iterations");
    }
    public abstract void importMultiblock(Multiblock multiblock) throws MissingConfigurationEntryException;
    public Multiblock getMainMultiblock(){
        ArrayList<Multiblock>[] lists = getMultiblockLists();
        if(lists.length==0)return null;
        ArrayList<Multiblock> list = lists[0];
        if(list.isEmpty())return null;
        return list.get(0);
    }
    public FormattedText getMainMultiblockTooltip(){
        Multiblock main = getMainMultiblock();
        if(main==null)return new FormattedText("");
        main.metadata.put("Author", "S'plodo-bot");
        return main.getTooltip(true);
    }
    public String getMainMultiblockBotTooltip(){
        Multiblock main = getMainMultiblock();
        if(main==null)return "";
        main.metadata.put("Author", "S'plodo-bot");
        return main.getBotTooltip();
    }
    public boolean isRunning(){
        return !threads.isEmpty();
    }
    public void countIteration(){
        synchronized(iterationSynchronizer){
            iterations++;
        }
    }
    public int getCrashedThreads(long crashedThreadTime){
        int crashed = 0;
        for(UUID uid : crashedThreads.keySet()){
            if(System.nanoTime()-crashedThreads.get(uid)<=crashedThreadTime)crashed++;
        }
        return crashed;
    }
}