package retro.bailey.rod.retrowatchface.config;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by rodbailey on 7/06/2016.
 */
public class Themes {
    public List<Theme> themes = new LinkedList<Theme>();

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append(":themes=");

        if (themes == null) {
            buf.append("null");
        }
        else {
            buf.append("[");
            for (Theme theme : themes ) {
                buf.append(theme.toString() + ",");
            }
            buf.append("]");
        }

        return buf.toString();
    }
}
