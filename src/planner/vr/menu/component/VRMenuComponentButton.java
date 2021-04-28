package planner.vr.menu.component;
import planner.core.Color;
import java.util.ArrayList;
import java.util.function.Supplier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.openvr.TrackedDevicePose;
import org.lwjgl.openvr.VR;
import planner.Core;
import planner.vr.VRCore;
import planner.vr.VRMenuComponent;
import simplelibrary.font.FontManager;
import simplelibrary.opengl.ImageStash;
import simplelibrary.opengl.Renderer2D;
public class VRMenuComponentButton extends VRMenuComponent{
    private String text;
    public boolean enabled;
    private boolean darker;
    private float textInset = .01f;//1cm
    private Supplier<Color> textColor = () -> {
        return Core.theme.getTextColor();
    };
    public boolean isPressed;
    private final ArrayList<Runnable> listeners = new ArrayList<>();
    /**
     * How far in front of the button the text should hover
     */
    private double textOffset = .001f;//1mm
    public VRMenuComponentButton(double x, double y, double z, double width, double height, double depth, double rx, double ry, double rz, String text, boolean enabled, boolean darker){
        super(x, y, z, width, height, depth, rx, ry, rz);
        this.text = text;
        this.enabled = enabled;
        this.darker = darker;
    }
    public VRMenuComponentButton setTextColor(Supplier<Color> color){
        textColor = color;
        return this;
    }
    @Override
    public void renderComponent(TrackedDevicePose.Buffer tdpb){
        Color col;
        if(darker){
             col = Core.theme.getDarkButtonColor();
            if(enabled){
                if(isPressed)col = Core.theme.getDarkerDarkButtonColor();
                else if(!isDeviceOver.isEmpty())col = Core.theme.getBrighterDarkButtonColor();
            }else{
                col = Core.theme.getDarkerDarkButtonColor();
            }
        }else{
            col = Core.theme.getButtonColor();
            if(enabled){
                if(isPressed)col = Core.theme.getDarkerButtonColor();
                else if(!isDeviceOver.isEmpty())col = Core.theme.getBrighterButtonColor();
            }else{
                col = Core.theme.getDarkerButtonColor();
            }
        }
        Core.applyColor(col);
        ImageStash.instance.bindTexture(0);
        VRCore.drawCube(0, 0, 0, width, height, depth, 0);
        Core.applyColor(textColor.get());
        drawText();
    }
    public void drawText(){
        double textLength = FontManager.getLengthForStringWithHeight(text, height);
        double scale = Math.min(1, (width-textInset*2)/textLength);
        double textHeight = ((height-textInset*2)*scale)-.005;
        GL11.glPushMatrix();
        GL11.glTranslated(0, height/2, depth+textOffset);
        GL11.glScaled(1, -1, 1);
        Renderer2D.drawCenteredText(0, -textHeight/2, width, textHeight/2, text);
        GL11.glPopMatrix();
    }
    @Override
    public void keyEvent(int device, int button, boolean pressed){
        super.keyEvent(device, button, pressed);
        if(button==VR.EVRButtonId_k_EButton_SteamVR_Trigger){
            isPressed = pressed;
            if(pressed){
                for(Runnable listener : listeners){
                    listener.run();
                }
            }
        }
    }
    public void addActionListener(Runnable a){
        listeners.add(a);
    }
    public void removeActionListener(Runnable a){
        listeners.remove(a);
    }
}