package retro.bailey.rod.retrowatchface.theme;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.wearable.view.WearableListView;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import retro.bailey.rod.retrowatchface.R;
import retro.bailey.rod.retrowatchface.config.*;

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

    private  Themes THEMES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Into onCreate for Retro configuration activity");

        super.onCreate(savedInstanceState);

        initThemes();

        setContentView(R.layout.activity_retro_watch_face_configuration);

        themeListViewAdapter = new ThemeListViewAdapter(getApplicationContext(), getThemes());
        themeListView = (WearableListView) findViewById(R.id.wearable_list);
        themeListView.setAdapter(themeListViewAdapter);
    }

    private Themes getThemes() {
        return THEMES;
    }

    private void initThemes() {
        BufferedReader reader = null;
        StringBuffer buffer = new StringBuffer();

        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("themes.json")));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException iox) {
            Log.w(TAG, iox);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
            }
        }

        String jsonString = buffer.toString();

        // By the time we get here, 'buffer' contains the JSON string from assets/themes.json
        Log.i(TAG, "Read themes.json: " + jsonString);

        // Parse the JSON string into an object graph
        Gson gson = new Gson();
       THEMES = gson.fromJson(jsonString, Themes.class);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ",resultCode=" + resultCode + ",data=" + data);
    }
}
