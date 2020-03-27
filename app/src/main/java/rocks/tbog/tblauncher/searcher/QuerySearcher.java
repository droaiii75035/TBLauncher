package rocks.tbog.tblauncher.searcher;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ValuedHistoryRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.MapCompat;

/**
 * AsyncTask retrieving data from the providers and updating the view
 *
 * @author dorvaryn
 */
public class QuerySearcher extends Searcher {
    private static int MAX_RESULT_COUNT = -1;
    private final String trimmedQuery;
    private HashMap<String, Integer> knownIds;
    /**
     * Store user preferences
     */
    private final SharedPreferences prefs;

    public QuerySearcher(ISearchActivity activity, String query) {
        super(activity, query);
        this.trimmedQuery = query.trim();
        prefs = PreferenceManager.getDefaultSharedPreferences(activity.getContext());

    }

    @Override
    protected int getMaxResultCount() {
        if (MAX_RESULT_COUNT == -1) {
            // Convert `"number-of-display-elements"` to double first before truncating to int to avoid
            // `java.lang.NumberFormatException` crashes for values larger than `Integer.MAX_VALUE`
            try {
                MAX_RESULT_COUNT = Double.valueOf(prefs.getString("number-of-display-elements", String.valueOf(DEFAULT_MAX_RESULTS))).intValue();
            } catch (NumberFormatException e) {
                // If, for any reason, setting is empty, return default value.
                MAX_RESULT_COUNT = DEFAULT_MAX_RESULTS;
            }
        }

        return MAX_RESULT_COUNT;
    }

    @Override
    public boolean addResult(EntryItem... pojos) {
        // Give a boost if item was previously selected for this query
        for (EntryItem pojo : pojos) {
            int historyRecord = MapCompat.getOrDefault(knownIds, pojo.id, 0);
            if (historyRecord != 0) {
                pojo.setRelevance( pojo.getRelevance() + 25 * historyRecord);
            }
        }

        // call super implementation to update the adapter
        return super.addResult(pojos);
    }

    /**
     * Called on the background thread
     */
    @Override
    protected Void doInBackground(Void... voids) {
        ISearchActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;

        // Have we ever made the same query and selected something ?
        List<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(activity.getContext(), trimmedQuery);
        knownIds = new HashMap<>();
        for (ValuedHistoryRecord id : lastIdsForQuery) {
            knownIds.put(id.record, id.value);
        }

        // Request results via "addResult"
        TBApplication.getApplication(activity.getContext()).getDataHandler().requestResults(trimmedQuery, this);
        return null;
    }

    public static void clearMaxResultCountCache() {
        MAX_RESULT_COUNT = -1;
    }
}
