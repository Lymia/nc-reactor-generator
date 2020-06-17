package planner.menu.configuration.underhaul.fissionsfr;
import org.lwjgl.opengl.Display;
import planner.Core;
import planner.configuration.underhaul.fissionsfr.Fuel;
import planner.menu.component.MenuComponentMinimaList;
import planner.menu.component.MenuComponentMinimalistButton;
import simplelibrary.opengl.gui.GUI;
import simplelibrary.opengl.gui.Menu;
import simplelibrary.opengl.gui.components.MenuComponent;
import simplelibrary.opengl.gui.components.MenuComponentButton;
public class MenuFuelsConfiguration extends Menu{
    private final MenuComponentMinimaList list = add(new MenuComponentMinimaList(0, 0, 0, 0, 50));
    private final MenuComponentMinimalistButton add = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Add Fuel", true, true));
    private final MenuComponentMinimalistButton back = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Back", true, true));
    public MenuFuelsConfiguration(GUI gui, Menu parent){
        super(gui, parent);
        add.addActionListener((e) -> {
            Fuel b = new Fuel("New Fuel", 0, 0, 0);
            Core.configuration.underhaul.fissionSFR.fuels.add(b);
            gui.open(new MenuFuelConfiguration(gui, this, b));
        });
        back.addActionListener((e) -> {
            gui.open(parent);
        });
    }
    @Override
    public void onGUIOpened(){
        list.components.clear();
        for(Fuel b : Core.configuration.underhaul.fissionSFR.fuels){
            list.add(new MenuComponentFuelConfiguration(b));
        }
    }
    @Override
    public void render(int millisSinceLastTick){
        list.width = Display.getWidth();
        list.height = Display.getHeight()-back.height-add.height;
        for(MenuComponent component : list.components){
            component.width = list.width-(list.hasVertScrollbar()?list.vertScrollbarWidth:0);
        }
        add.width = back.width = Display.getWidth();
        add.height = back.height = Display.getHeight()/16;
        back.y = Display.getHeight()-back.height;
        add.y = back.y-add.height;
        super.render(millisSinceLastTick);
    }
    @Override
    public void buttonClicked(MenuComponentButton button){
        for(MenuComponent c : list.components){
            if(c instanceof MenuComponentFuelConfiguration){
                if(button==((MenuComponentFuelConfiguration) c).delete){
                    Core.configuration.underhaul.fissionSFR.fuels.remove(((MenuComponentFuelConfiguration) c).fuel);
                    onGUIOpened();
                    return;
                }
                if(button==((MenuComponentFuelConfiguration) c).edit){
                    gui.open(new MenuFuelConfiguration(gui, this, ((MenuComponentFuelConfiguration) c).fuel));
                    return;
                }
            }
        }
        super.buttonClicked(button);
    }
}