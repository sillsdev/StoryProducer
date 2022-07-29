package org.tyndalebt.spadv.tools.media.graphics;

import android.graphics.Rect;

/**
 * A simple helper for manipulating rectangles.
 */
public class RectHelper {
    /**
     * Coerce source rectangle to fit within (inclusive) the bounds of the clip rectangle by
     * cropping it if necessary.
     * Note: This method naively assumes that the two rectangles intersect.
     * @param src
     * @param clip
     * @return
     */
    public static Rect clip(Rect src, Rect clip) {
        src.left = Math.max(src.left, clip.left);
        src.top = Math.max(src.top, clip.top);
        src.right = Math.min(src.right, clip.right);
        src.bottom = Math.min(src.bottom, clip.bottom);
        return src;
    }

    public static Rect scale(Rect src, float scale) {
        src.left *= scale;
        src.top *= scale;
        src.right *= scale;
        src.bottom *= scale;
        return src;
    }

    public static Rect translate(Rect src, int x, int y) {
        src.left += x;
        src.top += y;
        src.right += x;
        src.bottom += y;
        return src;
    }
}
