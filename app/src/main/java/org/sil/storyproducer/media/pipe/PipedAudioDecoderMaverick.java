package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>This media pipeline component is a thin wrapper for the commonly used triumvirate of
 * {@link PipedMediaExtractor}, {@link PipedMediaDecoder}, and {@link PipedAudioResampler}.</p>
 */
public class PipedAudioDecoderMaverick implements PipedMediaByteBufferSource {
    private static final String TAG = "PipedAudioMaverick";

    private final String mPath;
    private final int mSampleRate;
    private final int mChannelCount;
    private final float mVolumeModifier;

    private PipedMediaByteBufferSource mSource;

    public PipedAudioDecoderMaverick(String path) {
        this(path, 0, 0);
    }

    public PipedAudioDecoderMaverick(String path, int sampleRate, int channelCount) {
        this(path, sampleRate, channelCount, 1);
    }

    public PipedAudioDecoderMaverick(String path, int sampleRate, int channelCount, float volumeModifier) {
        mPath = path;
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        mVolumeModifier = volumeModifier;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.MediaType.AUDIO;
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
        PipedMediaExtractor extractor = new PipedMediaExtractor(mPath, MediaHelper.MediaType.AUDIO);

        PipedMediaDecoder decoder = new PipedMediaDecoder();
        decoder.addSource(extractor);

        mSource = decoder;

        //Only use a resampler if the sample rate is specified.
        if(mSampleRate > 0) {
            PipedAudioResampler resampler = new PipedAudioResampler(mSampleRate, mChannelCount);
            resampler.setVolumeModifier(mVolumeModifier);
            resampler.addSource(decoder);
            mSource = resampler;
        }

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
    public void close() {
        if(mSource != null) {
            mSource.close();
        }
    }
}
