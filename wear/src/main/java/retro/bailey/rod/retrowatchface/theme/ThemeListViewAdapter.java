package retro.bailey.rod.retrowatchface.theme;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import retro.bailey.rod.retrowatchface.R;
import retro.bailey.rod.retrowatchface.config.Themes;

/**
 * Data source for the list view in the configuration activity. Each item in the list
 * is a particular "theme" - a set of colours and fonts with which the retro watch face
 * is rendered. This adapter vends a "view" for each "theme" in the list. Recycling logic is
 * included.
 */
public class ThemeListViewAdapter extends WearableListView.Adapter {

    private Context context;

    private final LayoutInflater inflater;

    private Themes themes;

    public ThemeListViewAdapter(Context context, Themes themes) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.themes = themes;
    }

    /** Provides a referenc eto the type of views you're using */
    public static class ThemeListItemViewHolder extends WearableListView.ViewHolder {

        private ImageView iconImageView;
        private TextView textView;

        public ThemeListItemViewHolder(View itemView) {
            super(itemView);
            iconImageView = (ImageView) itemView.findViewById(R.id.theme_icon);
            textView = (TextView) itemView.findViewById(R.id.theme_name);
        }

        public ImageView getIconImageView() {
            return iconImageView;
        }

        public TextView getTextView() {
            return textView;
        }
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate custom layout for list item
        return new ThemeListItemViewHolder(inflater.inflate(R.layout.list_adapter_item_theme, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        ThemeListItemViewHolder itemViewHolder = (ThemeListItemViewHolder) holder;

        TextView textView = itemViewHolder.getTextView();
        textView.setText(themes.themes.get(position).name);

        ((ThemeListItemViewHolder) holder).itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return themes.themes.size();
    }
}
