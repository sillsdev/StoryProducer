package org.sil.storyproducer.tools.media.graphics;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class KenBurnsEffectHelper {
    enum Direction {
        DOWN,
        UP,
        LEFT,
        RIGHT,
        ;
    }

    enum Position {

    }

    enum Zoom {
        IN,
        OUT,
        ;
    }

    public static KenBurnsEffect getRandom(String src, double widthToHeight) {
        //TODO: actually randomize
        return getScroll(src, widthToHeight, null);
    }

    /**
     *
     * @param src
     * @param widthToHeight width divided by height to indicate resolution
     * @param dir
     * @return
     */
    public static KenBurnsEffect getScroll(String src, double widthToHeight, Direction dir) {
        Rect dimensions = BitmapHelper.getDimensions(src);
        int width = dimensions.width();
        int height = dimensions.height();

        //TODO: support different directions of scroll

        int scrollAmount = (int) (width * 0.25);

        int kbfxWidth = width - scrollAmount;
        int kbfxHeight = (int) (kbfxWidth / widthToHeight);

        if(kbfxHeight > height) {
            kbfxHeight = height;
            kbfxWidth = (int) (kbfxHeight * widthToHeight);

            scrollAmount = width - kbfxWidth;
        }

        int kbfxVertPadding = (height - kbfxHeight) / 2;

        Rect start = new Rect(0, kbfxVertPadding, width - scrollAmount, height - kbfxVertPadding);
        Rect end = new Rect(scrollAmount, kbfxVertPadding, width, height - kbfxVertPadding);

        return new KenBurnsEffect(start, end);
    }

    public static KenBurnsEffect getZoom(String src, double widthToHeight, Zoom zoom) {
        Rect dimensions = BitmapHelper.getDimensions(src);
        int width = dimensions.width();
        int height = dimensions.height();

        int adjustedWidth = width;
        int adjustedHeight = (int) (width / widthToHeight);
        int widthOffset = 0;
        int heightOffset = (height - adjustedHeight) / 2;

        if(adjustedHeight > height) {
            adjustedWidth = (int) (height * widthToHeight);
            adjustedHeight = height;
            widthOffset = (width - adjustedWidth) / 2;
            heightOffset = 0;
        }

        //TODO: don't assume rational picture dimensions
        int kbfxWidth = width / 2;
        int kbfxHeight = (int) (kbfxWidth / widthToHeight);

        Rect zoomedOut = new Rect(widthOffset, heightOffset, adjustedWidth - widthOffset, adjustedHeight - heightOffset);

        //TODO: support positional zooming
        //Top right
        Rect zoomedIn = new Rect(adjustedWidth - widthOffset - kbfxWidth, heightOffset, adjustedWidth - widthOffset, heightOffset + kbfxHeight);

        if(zoom == Zoom.IN) {
            return new KenBurnsEffect(zoomedOut, zoomedIn);
        }
        else {
            return new KenBurnsEffect(zoomedIn, zoomedOut);
        }
    }
}
