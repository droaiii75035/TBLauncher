package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.FavRecord;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.result.ResultAdapter;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public abstract class EntryItem {

    /**
     * the layout will be used in a ListView
     */
    public static final int FLAG_DRAW_LIST = 1; // 1 << 0

    /**
     * the layout will be used in a GridView
     */
    public static final int FLAG_DRAW_GRID = 2; // 1 << 1

    /**
     * the layout will be used in a horizontal LinearLayout
     */
    public static final int FLAG_DRAW_QUICK_LIST = 4; // 1 << 2

    /**
     * layout should display an icon
     */
    public static final int FLAG_DRAW_ICON = 8; // 1 << 3

    /**
     * layout should display a text/name
     */
    public static final int FLAG_DRAW_NAME = 16; // 1 << 4

    /**
     * layout should display tags
     */
    public static final int FLAG_DRAW_TAGS = 32; // 1 << 5


    // Globally unique ID.
    // Usually starts with provider scheme, e.g. "app://" or "contact://" to
    // ensure unique constraint
    @NonNull
    public final String id;
    // normalized name, for faster search
    public StringNormalizer.Result normalizedName = null;
    // Name for this Entry, e.g. app name
    @NonNull
    private
    String name = "";

    // How relevant is this record? The higher, the most probable it will be displayed
    protected FuzzyScore.MatchInfo relevance = null;
    // Pointer to the normalizedName that the above relevance was calculated, used for highlighting
    protected StringNormalizer.Result relevanceSource = null;

    public EntryItem(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Set the user-displayable name of this container
     * <p/>
     * When this method a searchable version of the name will be generated for the name and stored
     * as `nameNormalized`. Additionally a mapping from the positions in the searchable name
     * to the positions in the displayable name will be stored (as `namePositionMap`).
     *
     * @param name User-friendly name of this container
     */
    public void setName(String name) {
        if (name != null) {
            // Set the actual user-friendly name
            this.name = name;
            this.normalizedName = StringNormalizer.normalizeWithResult(this.name, false);
        } else {
            this.name = "null";
            this.normalizedName = null;
        }
    }

    public void setName(String name, boolean generateNormalization) {
        if (generateNormalization) {
            setName(name);
        } else {
            this.name = name;
            this.normalizedName = null;
        }
    }

    public int getRelevance() {
        return relevance == null ? 0 : relevance.score;
    }

    public void setRelevance(StringNormalizer.Result normalizedName, FuzzyScore.MatchInfo matchInfo) {
        this.relevanceSource = normalizedName;
        this.relevance = new FuzzyScore.MatchInfo(matchInfo);
    }

    public void boostRelevance(int boost) {
        if (relevance != null)
            relevance.score += boost;
    }

    public void resetRelevance() {
        this.relevanceSource = null;
        this.relevance = null;
    }

    /**
     * ID to use in the history
     * (may be different from the one used in the adapter for display)
     */
    public String getHistoryId() {
        return this.id;
    }

    @LayoutRes
    public abstract int getResultLayout(int drawFlags);

    public abstract void displayResult(@NonNull View view, int drawFlags);

    public static class RelevanceComparator implements java.util.Comparator<EntryItem> {
        @Override
        public int compare(EntryItem lhs, EntryItem rhs) {
            if (lhs.getRelevance() == rhs.getRelevance()) {
                if (lhs.relevanceSource != null && rhs.relevanceSource != null)
                    return lhs.relevanceSource.compareTo(rhs.relevanceSource);
                else
                    return lhs.name.compareTo(rhs.name);
            }
            return lhs.getRelevance() - rhs.getRelevance();
        }

    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Default popup menu implementation, can be overridden by children class to display a more specific menu
     *
     * @return an inflated, listener-free PopupMenu
     */
    ListPopup buildPopupMenu(Context context, LinearAdapter adapter, final ResultAdapter parent, View parentView) {
        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_hist_fav));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_remove));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_favorites_add));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_favorites_remove));
        return inflatePopupMenu(adapter, context);
    }

    ListPopup inflatePopupMenu(@NonNull LinearAdapter adapter, @NonNull Context context) {
        ListPopup menu = new ListPopup(context);
        menu.setAdapter(adapter);

        // If app already pinned, do not display the "add to favorite" option
        // otherwise don't show the "remove favorite button"
        boolean foundInFavorites = false;
        ArrayList<FavRecord> favRecords = TBApplication.dataHandler(context).getFavorites();
        for (FavRecord fav : favRecords)
        {
            if (id.equals(fav.record))
            {
                foundInFavorites = true;
                break;
            }
        }
        if (foundInFavorites) {
            for (int i = 0; i < adapter.getCount(); i += 1) {
                LinearAdapter.MenuItem item = adapter.getItem(i);
                if (item instanceof LinearAdapter.Item) {
                    if (((LinearAdapter.Item) item).stringId == R.string.menu_favorites_add)
                        adapter.remove(item);
                }
            }
        } else {
            for (int i = 0; i < adapter.getCount(); i += 1) {
                LinearAdapter.MenuItem item = adapter.getItem(i);
                if (item instanceof LinearAdapter.Item) {
                    if (((LinearAdapter.Item) item).stringId == R.string.menu_favorites_remove)
                        adapter.remove(item);
                }
            }
        }

        if (BuildConfig.DEBUG) {
            adapter.add(new LinearAdapter.ItemTitle("Debug info"));
            adapter.add(new LinearAdapter.ItemString("Relevance: " + getRelevance()));
        }

        return menu;
    }

    /**
     * How to display the popup menu
     *
     * @return a PopupMenu object
     */
    @NonNull
    public ListPopup getPopupMenu(final Context context, final ResultAdapter resultAdapter, final View parentView) {
        LinearAdapter menuAdapter = new LinearAdapter();
        ListPopup menu = buildPopupMenu(context, menuAdapter, resultAdapter, parentView);

        menu.setOnItemClickListener((adapter, view, position) -> {
            LinearAdapter.MenuItem item = ((LinearAdapter) adapter).getItem(position);
            @StringRes int stringId = 0;
            if (item instanceof LinearAdapter.Item) {
                stringId = ((LinearAdapter.Item) adapter.getItem(position)).stringId;
            }
            popupMenuClickHandler(view.getContext(), item, stringId);
        });

        return menu;
    }

    /**
     * Handler for popup menu action.
     * Default implementation only handle remove from history action.
     *
     * @return Works in the same way as onOptionsItemSelected, return true if the action has been handled, false otherwise
     */
    @CallSuper
    boolean popupMenuClickHandler(@NonNull Context context, @NonNull LinearAdapter.MenuItem item, @StringRes int stringId) {
        switch (stringId) {
            case R.string.menu_remove:
                ResultHelper.removeFromResultsAndHistory(this, context);
                return true;
            case R.string.menu_favorites_add:
                ResultHelper.launchAddToFavorites(context, this);
                break;
            case R.string.menu_favorites_remove:
                ResultHelper.launchRemoveFromFavorites(context, this);
                break;
        }

//        FullscreenActivity mainActivity = (FullscreenActivity) context;
//        // Update favorite bar
//        mainActivity.onFavoriteChange();
//        mainActivity.launchOccurred();
//        // Update Search to reflect favorite add, if the "exclude favorites" option is active
//        if (mainActivity.prefs.getBoolean("exclude-favorites", false) && mainActivity.isViewingSearchResults()) {
//            mainActivity.updateSearchRecords(true);
//        }

        return false;
    }

    public void doLaunch(@NonNull View view) {
        throw new RuntimeException("No launch action defined for " + getClass().getSimpleName());
    }
}
