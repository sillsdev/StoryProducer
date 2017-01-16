package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * <p>This media pipeline component loops a single audio file for a specified amount of time.</p>
 */
public class PipedAudioLooper extends PipedAudioShortManipulator {
    private static final String TAG = "PipedAudioLooper";

    private final String mPath;
    private final long mDurationUs;

    private final float mVolumeModifier;

    private PipedMediaByteBufferSource mSource;

    private MediaFormat mOutputFormat;

    private final short[] mSourceBufferA = new short[MediaHelper.MAX_INPUT_BUFFER_SIZE / 2];

    private int mPos;
    private int mSize;
    private boolean mHasBuffer = false;

    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

    /**
     * Create looper from an audio file with specified duration, using the file's format.
     * @param path path of the audio file.
     * @param durationUs desired duration in microseconds.
     */
    public PipedAudioLooper(String path, long durationUs) {
        this(path, durationUs, 0, 0);
    }

    /**
     * Create looper from an audio file with specified duration, resampling the audio stream.
     * @param path path of the audio file.
     * @param durationUs desired duration in microseconds.
     * @param sampleRate desired sample rate.
     * @param channelCount desired channel count.
     */
    public PipedAudioLooper(String path, long durationUs, int sampleRate, int channelCount) {
        this(path, durationUs, sampleRate, channelCount, 1);
    }

    /**
     * Create looper from an audio file with specified duration, resampling the audio stream.
     * @param path path of the audio file.
     * @param durationUs desired duration in microseconds.
     * @param sampleRate desired sample rate.
     * @param channelCount desired channel count.
     * @param volumeModifier volume scaling factor.
     */
    public PipedAudioLooper(String path, long durationUs, int sampleRate, int channelCount, float volumeModifier) {
        mPath = path;
        mDurationUs = durationUs;
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        mVolumeModifier = volumeModifier;
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        mSource = new PipedAudioDecoderMaverick(mPath, mSampleRate, mChannelCount, mVolumeModifier);
        mSource.setup();
        fetchSourceBuffer();

        mOutputFormat = MediaHelper.createFormat(MediaHelper.MIMETYPE_RAW_AUDIO);
        mOutputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        mOutputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);

        start();
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mOutputFormat;
    }

    @Override
    protected short getSampleForChannel(int channel) {
        if(mHasBuffer) {
            try {
                return mSourceBufferA[mPos++];
            }
            catch(ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        //only necessary for exception case
        return 0;
    }

    @Override
    protected boolean loadSamplesForTime(long time) {
        //Component is done if duration is exceeded.
        if(time >= mDurationUs) {
            mSource.close();
            mSource = null;
            return false;
        }

        while(mHasBuffer && mPos >= mSize) {
            releaseSourceBuffer();
            fetchSourceBuffer();
        }
        if(!mHasBuffer) {
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

        return true;
    }

    private void fetchSourceBuffer() {
        if(mSource.isDone()) {
            return;
        }
        ByteBuffer buffer = mSource.getBuffer(mInfo);
        ShortBuffer sBuffer = MediaHelper.getShortBuffer(buffer);
        mPos = 0;
        mSize = sBuffer.remaining();
        sBuffer.get(mSourceBufferA, mPos, mSize);
        mSource.releaseBuffer(buffer);

        mHasBuffer = true;
    }

    private void releaseSourceBuffer() {
        mHasBuffer = false;
    }

    @Override
    public void close() {
        super.close();
        if(mSource != null) {
            mSource.close();
        }
    }
}
