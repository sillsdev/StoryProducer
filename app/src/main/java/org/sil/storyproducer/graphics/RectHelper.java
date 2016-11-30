package org.sil.storyproducer.graphics;

import android.graphics.Rect;

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
