package multiblock.action;
import java.util.ArrayList;
import multiblock.Action;
import multiblock.Block;
import multiblock.Multiblock;
import planner.menu.MenuEdit;
public class PasteAction extends Action<Multiblock>{
    private final ArrayList<Block> was = new ArrayList<>();
    private final ArrayList<int[]> wasAir = new ArrayList<>();
    private final int x;
    private final int y;
    private final int z;
    private final ArrayList<MenuEdit.ClipboardEntry> blocks;
    public PasteAction(ArrayList<MenuEdit.ClipboardEntry> blocks, int x, int y, int z){
        this.blocks = blocks;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    @Override
    public void doApply(Multiblock multiblock, boolean allowUndo){
        was.clear();
        for(MenuEdit.ClipboardEntry entry : blocks){
            int X = entry.x+x;
            int Y = entry.y+y;
            int Z = entry.z+z;
            if(X<0)continue;
            if(Y<0)continue;
            if(Z<0)continue;
            if(X>multiblock.getX()-1)continue;
            if(Y>multiblock.getY()-1)continue;
            if(Z>multiblock.getZ()-1)continue;
            if(allowUndo){
                Block bl = multiblock.getBlock(X, Y, Z);
                if(bl!=null)was.add(bl);
                else wasAir.add(new int[]{X,Y,Z});
            }
            multiblock.setBlock(X, Y, Z, entry.block);
        }
    }
    @Override
    public void doUndo(Multiblock multiblock){
        for(Block b : was){
            multiblock.setBlockExact(b.x, b.y, b.z, b);
        }
        for(int[] loc : wasAir){
            multiblock.setBlockExact(loc[0], loc[1], loc[2], null);
        }
    }
    @Override
    protected void getAffectedBlocks(Multiblock multiblock, ArrayList<Block> blocks){
        for(MenuEdit.ClipboardEntry entry : this.blocks){
            Block block = multiblock.getBlock(entry.x+x, entry.y+y, entry.z+z);
            if(block==null)continue;
            if(!block.isCasing()&&!blocks.contains(block)){
                blocks.add(block);
            }
        }
    }
}