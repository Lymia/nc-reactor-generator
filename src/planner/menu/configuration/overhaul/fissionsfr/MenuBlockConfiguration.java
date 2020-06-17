package planner.menu.configuration.overhaul.fissionsfr;
import java.awt.Color;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import planner.configuration.overhaul.fissionsfr.Block;
import planner.menu.component.MenuComponentMinimalistButton;
import planner.menu.component.MenuComponentMinimalistOptionButton;
import planner.menu.component.MenuComponentMinimalistTextBox;
import simplelibrary.opengl.gui.GUI;
import simplelibrary.opengl.gui.Menu;
public class MenuBlockConfiguration extends Menu{
    private static final Color textColor = new Color(.1f, .1f, .2f, 1f);
    private final MenuComponentMinimalistTextBox name = add(new MenuComponentMinimalistTextBox(0, 0, 0, 0, "Name", true));
    private final MenuComponentMinimalistTextBox cooling = add(new MenuComponentMinimalistTextBox(0, 0, 0, 0, "", true));
    private final MenuComponentMinimalistOptionButton cluster = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Can Cluster", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton createCluster = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Creates Cluster", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton conductor = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Conductor", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton fuelCell = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Fuel Cell", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton reflector = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Reflector", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton irradiator = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Irradiator", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton moderator = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Moderator", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton activeModerator = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Active Moderator", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton shield = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Neutron Shield", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistTextBox flux = add(new MenuComponentMinimalistTextBox(0, 0, 0, 0, "", true));
    private final MenuComponentMinimalistTextBox efficiency = add(new MenuComponentMinimalistTextBox(0, 0, 0, 0, "", true));
    private final MenuComponentMinimalistTextBox reflectivity = add(new MenuComponentMinimalistTextBox(0, 0, 0, 0, "", true));
    private final MenuComponentMinimalistTextBox heatMult = add(new MenuComponentMinimalistTextBox(0, 0, 0, 0, "", true));
    private final MenuComponentMinimalistOptionButton blocksLOS = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Block Line of Sight", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistOptionButton functional = add(new MenuComponentMinimalistOptionButton(0, 0, 0, 0, "Functional", true, true, 0, "FALSE", "TRUE"));
    private final MenuComponentMinimalistButton rules = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Placement Rules", true, true));
    private final MenuComponentMinimalistButton back = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Back", true, true));
    private final Block block;
    public MenuBlockConfiguration(GUI gui, Menu parent, Block block){
        super(gui, parent);
        rules.addActionListener((e) -> {
            gui.open(new MenuPlacementRulesConfiguration(gui, this, block));
        });
        back.addActionListener((e) -> {
            block.name = name.text;
            block.cooling = Integer.parseInt(cooling.text);
            block.cluster = cluster.getIndex()==1;
            block.createCluster = createCluster.getIndex()==1;
            block.conductor = conductor.getIndex()==1;
            block.fuelCell = fuelCell.getIndex()==1;
            block.reflector = reflector.getIndex()==1;
            block.irradiator = irradiator.getIndex()==1;
            block.moderator = moderator.getIndex()==1;
            block.activeModerator = activeModerator.getIndex()==1;
            block.shield = shield.getIndex()==1;
            block.flux = Integer.parseInt(flux.text);
            block.efficiency = Float.parseFloat(efficiency.text);
            block.reflectivity = Float.parseFloat(reflectivity.text);
            block.heatMult = Integer.parseInt(heatMult.text);
            block.blocksLOS = blocksLOS.getIndex()==1;
            block.functional = functional.getIndex()==1;
            gui.open(parent);
        });
        this.block = block;
    }
    @Override
    public void onGUIOpened(){
        name.text = block.name;
        cooling.text = block.cooling+"";
        cluster.setIndex(block.cluster?1:0);
        createCluster.setIndex(block.createCluster?1:0);
        conductor.setIndex(block.conductor?1:0);
        fuelCell.setIndex(block.fuelCell?1:0);
        reflector.setIndex(block.reflector?1:0);
        irradiator.setIndex(block.irradiator?1:0);
        moderator.setIndex(block.moderator?1:0);
        activeModerator.setIndex(block.activeModerator?1:0);
        shield.setIndex(block.shield?1:0);
        flux.text = block.flux+"";
        efficiency.text = block.efficiency+"";
        reflectivity.text = block.reflectivity+"";
        heatMult.text = block.heatMult+"";
        blocksLOS.setIndex(block.blocksLOS?1:0);
        functional.setIndex(block.functional?1:0);
    }
    @Override
    public void render(int millisSinceLastTick){
        cooling.width = flux.width = efficiency.width = reflectivity.width = heatMult.width = Display.getWidth()*.75;
        cooling.x = flux.x = efficiency.x = reflectivity.x = heatMult.x = Display.getWidth()-cooling.width;
        functional.width = blocksLOS.width = shield.width = activeModerator.width = moderator.width = irradiator.width = reflector.width = fuelCell.width = conductor.width = createCluster.width = cluster.width = name.width = rules.width = back.width = Display.getWidth();
        functional.height = blocksLOS.height = heatMult.height = reflectivity.height = efficiency.height = flux.height = shield.height = activeModerator.height = moderator.height = irradiator.height = reflector.height = fuelCell.height = conductor.height = createCluster.height = cluster.height = cooling.height = name.height = rules.height = back.height = Display.getHeight()/20;
        cooling.y = name.height;
        cluster.y = cooling.y+cooling.height;
        createCluster.y = cluster.y+cluster.height;
        conductor.y = createCluster.y+createCluster.height;
        fuelCell.y = conductor.y+conductor.height;
        reflector.y = fuelCell.y+fuelCell.height;
        irradiator.y = reflector.y+reflector.height;
        moderator.y = irradiator.y+irradiator.height;
        activeModerator.y = moderator.y+moderator.height;
        shield.y = activeModerator.y+activeModerator.height;
        flux.y = shield.y+shield.height;
        efficiency.y = flux.y+flux.height;
        reflectivity.y = efficiency.y+efficiency.height;
        heatMult.y = reflectivity.y+reflectivity.height;
        blocksLOS.y = heatMult.y+heatMult.height;
        functional.y = blocksLOS.y+blocksLOS.height;
        rules.y = functional.y+functional.height;
        back.y = Display.getHeight()-back.height;
        GL11.glColor4f(textColor.getRed()/255f, textColor.getGreen()/255f, textColor.getBlue()/255f, textColor.getAlpha()/255f);
        drawText(0, Display.getHeight()/20, Display.getWidth()*.25, Display.getHeight()/20*2, "Cooling");
        drawText(0, Display.getHeight()/20*11, Display.getWidth()*.25, Display.getHeight()/20*12, "Neutron Flux");
        drawText(0, Display.getHeight()/20*12, Display.getWidth()*.25, Display.getHeight()/20*13, "Efficiency");
        drawText(0, Display.getHeight()/20*13, Display.getWidth()*.25, Display.getHeight()/20*14, "Reflectivity");
        drawText(0, Display.getHeight()/20*14, Display.getWidth()*.25, Display.getHeight()/20*15, "Heat Multiplier");
        GL11.glColor4f(1, 1, 1, 1);
        super.render(millisSinceLastTick);
    }
}