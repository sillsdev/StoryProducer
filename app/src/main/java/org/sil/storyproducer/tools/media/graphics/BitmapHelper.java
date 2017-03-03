package org.sil.storyproducer.tools.media.graphics;

import android.graphics.BitmapFactory;
import android.graphics.Rect;

/**
 * This class provides helper methods for working with Bitmaps.
 */
public class BitmapHelper {
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
    }
}
