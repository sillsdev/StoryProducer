package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

public class PipedAudioDecoderMaverick implements PipedMediaByteBufferSource {
    private static final String TAG = "PipedAudioMaverick";

    private String mPath;
    private int mSampleRate = 48000;
    private int mChannelCount = 2;
    private float mVolumeModifier = 1f;

    private PipedMediaByteBufferSource mSource;

    private PipedMediaExtractor mExtractor;
    private PipedMediaDecoder mDecoder;
    private PipedAudioResampler mResampler;

    public PipedAudioDecoderMaverick(String path) {
        mPath = path;
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

    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }

    public void setChannelCount(int channelCount) {
        mChannelCount = channelCount;
    }

    public void setVolumeModifier(float volumeModifier) {
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
        mExtractor = new PipedMediaExtractor(mPath, MediaHelper.MediaType.AUDIO);

        mDecoder = new PipedMediaDecoder();
        mDecoder.addSource(mExtractor);

        mResampler = new PipedAudioResampler(mSampleRate, mChannelCount);
        mResampler.setVolumeModifier(mVolumeModifier);

        mResampler.addSource(mDecoder);
        mResampler.setup();

        mSource = mResampler;

        //TODO: try to only use a resampler if necessary
//        mDecoder.setup();
//
//        mSource = mDecoder;
//
//        MediaFormat sourceFormat = mDecoder.getOutputFormat();
//        int sourceSampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//        if(mSampleRate == 0) {
//            mSampleRate = sourceSampleRate;
//        }
//        int sourceChannelCount = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//        if(mChannelCount == 0) {
//            mChannelCount = sourceChannelCount;
//        }
//
//        boolean shouldCorrectSampleRate = sourceSampleRate != mSampleRate;
//        boolean shouldCorrectChannelCount = sourceChannelCount != mChannelCount;
//        boolean shouldModifiyVolume = Math.abs(mVolumeModifier - 1) > .001f;
//
//        if(shouldCorrectSampleRate || shouldCorrectChannelCount || shouldModifiyVolume) {
//            mResampler = new PipedAudioResampler(mSampleRate, mChannelCount);
//            mResampler.setVolumeModifier(mVolumeModifier);
//
//            mResampler.addSource(mDecoder);
//            mResampler.setup();
//
//            mSource = mResampler;
//        }
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
