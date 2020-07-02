package planner.menu.component;
import java.awt.Color;
import planner.Core;
import planner.tool.EditorTool;
import simplelibrary.opengl.gui.components.MenuComponent;
public class MenuComponentEditorTool extends MenuComponent{
    public final EditorTool tool;
    public MenuComponentEditorTool(EditorTool tool){
        super(0, 0, 0, 0);
        color = Core.theme.getEditorListBorderColor();
        foregroundColor = Core.theme.getTextColor();
        selectedColor = foregroundColor.darker();
        this.tool = tool;
    }
    @Override
    public void render(){
        Color col = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        if(isMouseOver)col = col.brighter();
        Core.applyColor(col);
        drawRect(x, y, x+width, y+height, 0);
        Core.applyColor(foregroundColor);
        drawText();
        Core.applyColor(color);
        double border = height/8;
        if(isSelected){
            Core.applyColor(color.darker(), .85f);
            drawRect(x, y, x+border, y+border, 0);
            drawRect(x+width-border, y, x+width, y+border, 0);
            drawRect(x, y+height-border, x+border, y+height, 0);
            drawRect(x+width-border, y+height-border, x+width, y+height, 0);
            Core.applyColor(selectedColor, .85f);
            drawRect(x+border, y, x+width-border, y+border, 0);
            drawRect(x+border, y+height-border, x+width-border, y+height, 0);
            drawRect(x, y+border, x+border, y+height-border, 0);
            drawRect(x+width-border, y+border, x+width, y+height-border, 0);
        }
        if(isMouseOver){
            Core.applyColor(color, .6375f);
            drawRect(x, y, x+border, y+border, 0);
            drawRect(x+width-border, y, x+width, y+border, 0);
            drawRect(x, y+height-border, x+border, y+height, 0);
            drawRect(x+width-border, y+height-border, x+width, y+height, 0);
            Core.applyColor(foregroundColor, .6375f);
            drawRect(x+border, y, x+width-border, y+border, 0);
            drawRect(x+border, y+height-border, x+width-border, y+height, 0);
            drawRect(x, y+border, x+border, y+height-border, 0);
            drawRect(x+width-border, y+border, x+width, y+height-border, 0);
        }
    }
    public void drawText(){
        tool.render(x, y, width, height);
    }
}