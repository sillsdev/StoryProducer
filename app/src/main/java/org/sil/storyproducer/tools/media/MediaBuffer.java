package org.sil.storyproducer.tools.media;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

public class MediaBuffer {
    public ByteBuffer buffer;
    public MediaCodec.BufferInfo info;

    public MediaBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        this.buffer = buffer;
        this.info = info;
    }
}
