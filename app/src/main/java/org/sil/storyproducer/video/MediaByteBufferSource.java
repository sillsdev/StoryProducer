package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface MediaByteBufferSource {

    /**
     * Get
     * @return
     */
    MediaFormat getFormat();
    MediaHelper.MediaType getType();
    boolean isDone();
    void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info);
    ByteBuffer getBuffer(MediaCodec.BufferInfo info);
    void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException;
}
