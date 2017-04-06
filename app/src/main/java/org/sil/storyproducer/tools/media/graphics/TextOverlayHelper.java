package org.sil.storyproducer.tools.media.graphics;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextOverlayHelper {
    public static void overlayJPEG(File input, File output, TextOverlay overlay) throws IOException {
        Bitmap source = BitmapFactory.decodeFile(input.getAbsolutePath());
        Bitmap dest = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(dest);

        overlay.draw(canvas);

        //Write file.
        FileOutputStream out = new FileOutputStream(output);
        dest.compress(Bitmap.CompressFormat.JPEG, 95, out);
        out.flush();
        out.close();
    }
}
