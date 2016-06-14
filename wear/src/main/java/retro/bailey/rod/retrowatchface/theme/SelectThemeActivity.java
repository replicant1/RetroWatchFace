package retro.bailey.rod.retrowatchface.theme;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
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
            Log.d(TAG, "onClick: itemId=" + viewHolder.getItemId() + ",tag=" + viewHolder.itemView.getTag()
                    + ",adapterPosition=" + viewHolder.getAdapterPosition());
//            updateConfigDataItem(viewHolder.)
            finish();
        }

        @Override
        public void onTopEmptyRegionClick() {
            Log.d(TAG, "onTopEmptyRegionClick");
        }
    }

    private static final String TAG = SelectThemeActivity.class.getSimpleName();

    private WearableListView themeListView;

    private ThemeListViewAdapter themeListViewAdapter;

    private Themes THEMES;

    private GoogleApiClient googleApiClient;

    private final ListViewClickListener listViewClickListener = new ListViewClickListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Into onCreate for Retro configuration activity");

        super.onCreate(savedInstanceState);

        initThemes();

        setContentView(R.layout.activity_retro_watch_face_configuration);

        themeListViewAdapter = new ThemeListViewAdapter(getApplicationContext(), getThemes());
        themeListView = (WearableListView) findViewById(R.id.wearable_list);
        themeListView.setAdapter(themeListViewAdapter);
        themeListView.setHasFixedSize(true);
        themeListView.setClickListener(listViewClickListener);
//        themeListView.addOnScrollListener();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {

                    @Override
                    public void onConnected(@Nullable Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspected: " + cause);
                    }
                }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {

                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.d(TAG, "onConnectionFailed: " + connectionResult);
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if ((googleApiClient != null) && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
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
