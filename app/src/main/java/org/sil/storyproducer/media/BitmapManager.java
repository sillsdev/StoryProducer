package org.sil.storyproducer.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

public class BitmapManager {
    private static LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(3) {
        @Override
        protected Bitmap create(String key) {
            return BitmapFactory.decodeFile(key);
        }
    };

    public static Bitmap get(String path) {
        return mCache.get(path);
    }
}
