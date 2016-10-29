package org.sil.storyproducer.video;

import android.graphics.Canvas;

public interface MediaSurfaceSource extends PipedMediaSource {
    long fillCanvas(Canvas canv);
}
