package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * <p>This media pipeline component loops a single audio file for a specified amount of time.</p>
 */
public class PipedAudioLooper extends PipedAudioShortManipulator {
    private static final String TAG = "PipedAudioMixer";

    private final String mPath;
    private final long mDurationUs;

    private float mVolumeModifier = 1f;

    private PipedMediaByteBufferSource mSource;

    public PipedAudioLooper(String src, long durationUs) {
        mPath = src;
        mDurationUs = durationUs;
    }

    public PipedAudioLooper(String src, long durationUs, int sampleRate, int channelCount) {
        this(src, durationUs, sampleRate, channelCount, 1);
    }

    public PipedAudioLooper(String src, long durationUs, int sampleRate, int channelCount, float volumeModifier) {
        mPath = src;
        mDurationUs = durationUs;
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        mVolumeModifier = volumeModifier;
    }

    private MediaFormat mOutputFormat;

    private ByteBuffer mSourceBuffer;
    private ShortBuffer mSourceShortBuffer;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        mSource = new PipedAudioDecoderMaverick(mPath, mSampleRate, mChannelCount, mVolumeModifier);
        mSource.setup();
        fetchSourceBuffer();

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    @Override
    public boolean isDone() {
        return mSeekTime >= mDurationUs;
    }

    @Override
    protected short getSampleForTime(long time, int channel) {
        while(mSourceShortBuffer != null && mSourceShortBuffer.remaining() <= 0) {
            releaseSourceBuffer();
            fetchSourceBuffer();
        }
        if(mSourceShortBuffer == null) {
            mSource.close();
            mSource = new PipedAudioDecoderMaverick(mPath, mSampleRate, mChannelCount, mVolumeModifier);
            try {
                mSource.setup();
                fetchSourceBuffer();
            } catch (IOException e) {
                //TODO: handle exception properly
                e.printStackTrace();
            } catch (SourceUnacceptableException e) {
                //TODO: handle exception properly
                e.printStackTrace();
            }
        }
        if(mSourceShortBuffer != null) {
            return mSourceShortBuffer.get();
        }

        //TODO: can this happen?
        return 0;
    }

    private void fetchSourceBuffer() {
        if(mSource.isDone()) {
            return;
        }
        ByteBuffer buffer = mSource.getBuffer(mInfo);
        mSourceBuffer = buffer;
        mSourceShortBuffer = MediaHelper.getShortBuffer(buffer);
    }

    private void releaseSourceBuffer() {
        mSource.releaseBuffer(mSourceBuffer);
        mSourceBuffer = null;
        mSourceShortBuffer = null;
    }

    @Override
    public void close() {
        super.close();
        if(mSource != null) {
            mSource.close();
        }
    }
}
