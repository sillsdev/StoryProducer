package org.sil.storyproducer.tools.media.pipe;

import android.graphics.Canvas;

/**
 * Describes a component of the media pipeline which draws frames to a provided canvas when called.
 */
public interface PipedVideoSurfaceSource extends PipedMediaSource {
    /**
     * Request that this component draw a frame to the canvas.
     * @param canv the canvas to be drawn upon.
     * @return the presentation time (in microseconds) of the drawn frame.
     */
    long fillCanvas(Canvas canv);
}
