package org.sil.storyproducer.video;

import android.media.MediaFormat;

import java.io.IOException;

public interface PipedMediaSource {
    void setup() throws IOException;
    MediaFormat getFormat();
    boolean isDone();
}
