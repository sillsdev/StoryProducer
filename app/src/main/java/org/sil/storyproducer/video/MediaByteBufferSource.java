package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface MediaByteBufferSource extends PipedMediaSource {
    MediaHelper.MediaType getType();
    void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info);
    ByteBuffer getBuffer(MediaCodec.BufferInfo info);
    void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException;
}
