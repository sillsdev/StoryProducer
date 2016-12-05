package org.sil.storyproducer.tools;

import android.graphics.Bitmap;

public class BitmapScaler
{
    // Scale and maintain aspect ratio given a desired width
    // BitmapScaler.scaleToFitWidth(bitmap, 100);
    public static Bitmap scaleToFitWidth(Bitmap b, int width)
    {
        float factor = width / (float) b.getWidth();
        return Bitmap.createScaledBitmap(b, width, (int) (b.getHeight() * factor), true);
    }


    // Scale and maintain aspect ratio given a desired height
    // BitmapScaler.scaleToFitHeight(bitmap, 100);
    public static Bitmap scaleToFitHeight(Bitmap bitmap, int height)
    {
        float factor = height / (float) bitmap.getHeight();
        return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * factor), height, true);
    }

}
