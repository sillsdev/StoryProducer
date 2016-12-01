package org.sil.storyproducer.media.story;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.support.v4.util.LruCache;

/**
 * This class provides a simple LRU cache for Bitmaps and contains helper methods.
 */
class StoryBitmapManager {
    private static LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(3) {
        @Override
        protected Bitmap create(String key) {
            return BitmapFactory.decodeFile(key);
        }
    };

    public static Bitmap get(String path) {
        return mCache.get(path);
    }

    /**
     * Get the dimensions of a bitmap/image.
     * @param path path to image file.
     * @return dimensions of image.
     */
    public static Rect getDimensions(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        return new Rect(0, 0, imageWidth, imageHeight);
//        String imageType = options.outMimeType;
    }
}
