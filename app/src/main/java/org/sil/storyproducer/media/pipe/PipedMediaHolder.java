package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This media pipeline component essentially contributes nothing to the pipeline.
 * The purpose of such a class is to allow potential optimizations in pipelines in a polymorphic sense.
 */
public class PipedMediaHolder implements PipedMediaByteBufferSource, PipedMediaByteBufferDest {
    private PipedMediaByteBufferSource mSource;

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        mSource = src;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return mSource.getMediaType();
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        mSource.fillBuffer(buffer, info);
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) {
        return mSource.getBuffer(info);
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException {
        mSource.releaseBuffer(buffer);
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        mSource.setup();
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mSource.getOutputFormat();
    }

    @Override
    public boolean isDone() {
        return mSource.isDone();
    }

    @Override
    public void close() throws IOException {
        mSource.close();
    }
}
