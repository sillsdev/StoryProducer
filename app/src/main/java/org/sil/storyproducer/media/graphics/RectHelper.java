package org.sil.storyproducer.media.graphics;

import android.graphics.Rect;

/**
 * A simple helper for manipulating rectangles.
 */
public class RectHelper {
    public static void scale(Rect src, float scale) {
        src.left *= scale;
        src.top *= scale;
        src.right *= scale;
        src.bottom *= scale;
    }

    public static void translate(Rect src, int x, int y) {
        src.left += x;
        src.top += y;
        src.right += x;
        src.bottom += y;
    }
}
