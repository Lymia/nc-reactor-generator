package planner.menu.configuration.overhaul;
import multiblock.configuration.Configuration;
import multiblock.configuration.overhaul.fissionmsr.FissionMSRConfiguration;
import multiblock.configuration.overhaul.fissionsfr.FissionSFRConfiguration;
import multiblock.configuration.overhaul.fusion.FusionConfiguration;
import multiblock.configuration.overhaul.turbine.TurbineConfiguration;
import planner.Core;
import planner.menu.component.MenuComponentMinimalistButton;
import planner.menu.configuration.overhaul.fissionmsr.MenuFissionMSRConfiguration;
import planner.menu.configuration.overhaul.fissionsfr.MenuFissionSFRConfiguration;
import planner.menu.configuration.overhaul.fusion.MenuFusionConfiguration;
import planner.menu.configuration.overhaul.turbine.MenuTurbineConfiguration;
import simplelibrary.opengl.gui.GUI;
import simplelibrary.opengl.gui.Menu;
public class MenuOverhaulConfiguration extends Menu{
    private final MenuComponentMinimalistButton fissionSFR;
    private final MenuComponentMinimalistButton fissionMSR;
    private final MenuComponentMinimalistButton turbine;
    private final MenuComponentMinimalistButton fusion;
    private final MenuComponentMinimalistButton deleteSFR = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Delete (Shift)", false, true).setTooltip("Delete the Overhaul SFR configuration"));
    private final MenuComponentMinimalistButton deleteMSR = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Delete (Shift)", false, true).setTooltip("Delete the Overhaul MSR configuration"));
    private final MenuComponentMinimalistButton deleteTurbine = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Delete (Shift)", false, true).setTooltip("Delete the Overhaul Turbine configuration"));
    private final MenuComponentMinimalistButton deleteFusion = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Delete (Shift)", false, true).setTooltip("Delete the Overhaul Fusion Reactor configuration"));
    private final MenuComponentMinimalistButton back = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Back", true, true));
    private final Configuration configuration;
    public MenuOverhaulConfiguration(GUI gui, Menu parent, Configuration configuration){
        super(gui, parent);
        fissionSFR = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Solid-Fueled Fission Configuration", configuration.overhaul.fissionSFR!=null, true).setTooltip("Modify the Overhaul SFR configuration"));
        fissionMSR = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Molten Salt Fission Configuration", configuration.overhaul.fissionMSR!=null, true).setTooltip("Modify the Overhaul MSR configuration"));
        turbine = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Turbine Configuration", configuration.overhaul.turbine!=null, true).setTooltip("Modify the Overhaul Turbine configuration"));
        fusion = add(new MenuComponentMinimalistButton(0, 0, 0, 0, "Fusion Reactor Configuration", configuration.overhaul.fusion!=null, true).setTooltip("Modify the Overhaul Fusion Reactor configuration"));
        fissionSFR.addActionListener((e) -> {
            gui.open(new MenuFissionSFRConfiguration(gui, this, configuration));
        });
        fissionMSR.addActionListener((e) -> {
            gui.open(new MenuFissionMSRConfiguration(gui, this, configuration));
        });
        turbine.addActionListener((e) -> {
            gui.open(new MenuTurbineConfiguration(gui, this, configuration));
        });
        fusion.addActionListener((e) -> {
            gui.open(new MenuFusionConfiguration(gui, this, configuration));
        });
        deleteSFR.addActionListener((e) -> {
            if(configuration.overhaul.fissionSFR==null){
                configuration.overhaul.fissionSFR = new FissionSFRConfiguration();
            }else{
                configuration.overhaul.fissionSFR = null;
            }
            onGUIOpened();
        });
        deleteMSR.addActionListener((e) -> {
            if(configuration.overhaul.fissionMSR==null){
                configuration.overhaul.fissionMSR = new FissionMSRConfiguration();
            }else{
                configuration.overhaul.fissionMSR = null;
            }
            onGUIOpened();
        });
        deleteTurbine.addActionListener((e) -> {
            if(configuration.overhaul.turbine==null){
                configuration.overhaul.turbine = new TurbineConfiguration();
            }else{
                configuration.overhaul.turbine = null;
            }
            onGUIOpened();
        });
        deleteFusion.addActionListener((e) -> {
            if(configuration.overhaul.fusion==null){
                configuration.overhaul.fusion = new FusionConfiguration();
            }else{
                configuration.overhaul.fusion = null;
            }
            onGUIOpened();
        });
        back.addActionListener((e) -> {
            gui.open(parent);
        });
        this.configuration = configuration;
    }
    @Override
    public void onGUIOpened(){
        fissionSFR.enabled = configuration.overhaul.fissionSFR!=null&&Core.configuration.overhaul.fissionSFR!=null;
        fissionMSR.enabled = configuration.overhaul.fissionMSR!=null&&Core.configuration.overhaul.fissionMSR!=null;
        turbine.enabled = configuration.overhaul.turbine!=null&&Core.configuration.overhaul.turbine!=null;
        fusion.enabled = configuration.overhaul.fusion!=null&&Core.configuration.overhaul.fusion!=null;
    }
    @Override
    public void render(int millisSinceLastTick){
        if(configuration.overhaul.fissionSFR==null){
            deleteSFR.enabled = (configuration.addon&&Core.configuration.overhaul.fissionSFR==null)?false:(Core.isShiftPressed());
        }else{
            deleteSFR.enabled = Core.isShiftPressed();
        }
        if(configuration.overhaul.fissionMSR==null){
            deleteMSR.enabled = (configuration.addon&&Core.configuration.overhaul.fissionMSR==null)?false:(Core.isShiftPressed());
        }else{
            deleteMSR.enabled = Core.isShiftPressed();
        }
        if(configuration.overhaul.turbine==null){
            deleteTurbine.enabled = (configuration.addon&&Core.configuration.overhaul.turbine==null)?false:(Core.isShiftPressed());
        }else{
            deleteTurbine.enabled = Core.isShiftPressed();
        }
        if(configuration.overhaul.fusion==null){
            deleteFusion.enabled = (configuration.addon&&Core.configuration.overhaul.fusion==null)?false:(Core.isShiftPressed());
        }else{
            deleteFusion.enabled = Core.isShiftPressed();
        }
        deleteSFR.label = (configuration.overhaul.fissionSFR==null?"Create":"Delete")+" (Shift)";
        deleteMSR.label = (configuration.overhaul.fissionMSR==null?"Create":"Delete")+" (Shift)";
        deleteTurbine.label = (configuration.overhaul.turbine==null?"Create":"Delete")+" (Shift)";
        deleteFusion.label = (configuration.overhaul.fusion==null?"Create":"Delete")+" (Shift)";
        back.width = gui.helper.displayWidth();
        fissionSFR.width = fissionMSR.width = turbine.width = fusion.width = gui.helper.displayWidth()*3/4;
        deleteSFR.width = deleteMSR.width = deleteTurbine.width = deleteFusion.width = gui.helper.displayWidth()/4;
        deleteSFR.x = deleteMSR.x = deleteTurbine.x = deleteFusion.x = fissionSFR.width;
        deleteSFR.height = deleteMSR.height = deleteTurbine.height = deleteFusion.height = fissionSFR.height = fissionMSR.height = turbine.height = fusion.height = back.height = gui.helper.displayHeight()/16;
        deleteMSR.y = fissionMSR.y = fissionSFR.height;
        deleteTurbine.y = turbine.y = fissionMSR.y+fissionMSR.height;
        deleteFusion.y = fusion.y = turbine.y+turbine.height;
        back.y = gui.helper.displayHeight()-back.height;
        super.render(millisSinceLastTick);
    }
}