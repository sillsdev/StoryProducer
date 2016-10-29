package org.sil.storyproducer.video;

import android.graphics.Canvas;

public interface MediaSurfaceSource {
    boolean isDone();
    long fillCanvas(Canvas canv);
}
