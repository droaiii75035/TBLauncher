package rocks.tbog.tblauncher.searcher;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.HashSet;
import java.util.PriorityQueue;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;

public class TagSearcher extends Searcher {
    final EntryWithTags.TagDetails tagDetails;
    final HashSet<String> foundIdSet = new HashSet<>();

    public TagSearcher(ISearchActivity activity, @NonNull String query) {
        super(activity, query);
        tagDetails = new EntryWithTags.TagDetails(query);
    }

    @Override
    PriorityQueue<EntryItem> getPojoProcessor(ISearchActivity activity) {
        return new PriorityQueue<>(INITIAL_CAPACITY, EntryItem.NAME_COMPARATOR);
    }

    @WorkerThread
    @Override
    public boolean addResult(EntryItem... pojos) {
        if (isCancelled())
            return false;

        ISearchActivity activity = activityWeakReference.get();
        if (activity == null)
            return false;

        for (EntryItem entryItem : pojos) {
            if (entryItem instanceof EntryWithTags) {
                if (((EntryWithTags) entryItem).getTags().contains(tagDetails)) {
                    if (!foundIdSet.contains(entryItem.id)) {
                        foundIdSet.add(entryItem.id);
                        this.processedPojos.add(entryItem);
                    }
                }
            }
        }
        return true;
    }

    @WorkerThread
    @Override
    protected Void doInBackground(Void... voids) {
        ISearchActivity searchActivity = activityWeakReference.get();
        Context context = searchActivity != null ? searchActivity.getContext() : null;
        if (context == null)
            return null;

        // Request results via "addResult"
        TBApplication.getApplication(context).getDataHandler().requestAllRecords(this);
        return null;
    }
}
