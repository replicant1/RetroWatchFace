package retro.bailey.rod.retrowatchface.theme;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.wearable.view.WearableListView;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import retro.bailey.rod.retrowatchface.R;

/**
 * Activity for setting the current "theme" of the RetroWatchService.
 */
public class SelectThemeActivity extends Activity {

    /**
     * Listens for taps on the list of watch configurations
     */
    public class ListViewClickListener implements WearableListView.ClickListener {

        @Override
        public void onClick(WearableListView.ViewHolder viewHolder) {
            Log.d(TAG, "onClick: itemId=" + viewHolder.getItemId() + ",tag=" + viewHolder.itemView.getTag());
        }

        @Override
        public void onTopEmptyRegionClick() {
            Log.d(TAG, "onTopEmptyRegionClick");
        }
    }

    private static final String TAG = SelectThemeActivity.class.getSimpleName();

    private WearableListView themeListView;

    private ThemeListViewAdapter themeListViewAdapter;

    private static List<Theme> THEMES = new LinkedList<Theme>();

    static {
        THEMES.add(new Theme("Theme 1"));
        THEMES.add(new Theme("Theme 2"));
        THEMES.add(new Theme("Theme 3"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Into onCreate for Retro configuration activity");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_retro_watch_face_configuration);

        themeListViewAdapter = new ThemeListViewAdapter(getApplicationContext(), getThemes());
        themeListView = (WearableListView) findViewById(R.id.wearable_list);
        themeListView.setAdapter(themeListViewAdapter);
    }

    private List<Theme> getThemes() {
        return THEMES;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ",resultCode=" + resultCode + ",data=" + data);
    }
}
