package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Thin wrapper for a {@link ByteBuffer} and {@link MediaCodec.BufferInfo} pair
 */
public class MediaBuffer {
    public final ByteBuffer buffer;
    public final MediaCodec.BufferInfo info;

    public MediaBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        this.buffer = buffer;
        this.info = info;
    }
}
