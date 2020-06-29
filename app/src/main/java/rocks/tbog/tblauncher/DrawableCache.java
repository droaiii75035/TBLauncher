package rocks.tbog.tblauncher;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

public class DrawableCache {
    private boolean mEnabled = true;
    private HashMap<String, DrawableInfo> mCache = new HashMap<>();

    private static class DrawableInfo {

        final Drawable drawable;

        DrawableInfo(Drawable drawable) {
            this.drawable = drawable;
        }

    }

    public void cacheDrawable(@NonNull String name, Drawable drawable) {
        if (!mEnabled)
            return;
        DrawableInfo info = new DrawableInfo(drawable);
        mCache.put(name, info);
    }

    @Nullable
    public Drawable getCachedDrawable(@NonNull String name) {
        DrawableInfo info = mCache.get(name);
        if (info != null)
            return info.drawable;
        return null;
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled == enabled)
            return;
        mEnabled = enabled;
        clearCache();
    }

    public void clearCache() {
        mCache.clear();
    }
}
