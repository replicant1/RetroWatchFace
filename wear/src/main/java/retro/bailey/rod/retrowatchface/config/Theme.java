package retro.bailey.rod.retrowatchface.config;

/**
 * Created by rodbailey on 7/06/2016.
 */
public class Theme {
    public String name;
    public String backgroundColor;
    public ThemedPanel day;
    public ThemedPanel time;
    public ThemedPanel date;

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString() + ":");

        buf.append("name=" + name);
        buf.append(",backgroundColor=" + backgroundColor);
        buf.append(",day=" + day);
        buf.append(",time=" + time);
        buf.append(",date=" + date);

        return buf.toString();
    }
}
