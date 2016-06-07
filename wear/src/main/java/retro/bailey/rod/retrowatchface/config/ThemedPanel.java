package retro.bailey.rod.retrowatchface.config;

/**
 * Created by rodbailey on 7/06/2016.
 */
public class ThemedPanel {
    public String backgroundColor;
    public String textColor;
    public String textFont;

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString() + ":");
        
        buf.append("backgroundColor=" + backgroundColor);
        buf.append(",textColor=" + textColor);
        buf.append(",textFont=" + textFont);

        return buf.toString();
    }
}
