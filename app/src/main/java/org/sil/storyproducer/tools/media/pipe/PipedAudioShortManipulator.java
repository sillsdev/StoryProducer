package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.util.Log;

import org.sil.storyproducer.tools.media.ByteBufferQueue;
import org.sil.storyproducer.tools.media.MediaHelper;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * <p>This abstract media pipeline component provides a base for audio components
 * which care about touching every output short.</p>
 */
public abstract class PipedAudioShortManipulator implements PipedMediaByteBufferSource {
    private static final String TAG = "PipedAudioShorter";

    private Thread mThread;
    protected volatile boolean mIsDone = false;
    protected volatile State mComponentState = State.UNINITIALIZED;

    private static final int BUFFER_COUNT = 4;
    private final ByteBufferQueue mBufferQueue = new ByteBufferQueue(BUFFER_COUNT);

    private static final int MAX_BUFFER_CAPACITY = MediaHelper.MAX_INPUT_BUFFER_SIZE;
    private final short[] mShortBuffer = new short[MAX_BUFFER_CAPACITY / 2];

    protected int mSampleRate;
    protected int mChannelCount;
    protected long mSeekTime = 0;

    private int mAbsoluteSampleIndex = 0;

    @Override
    public boolean isDone() {
        return mIsDone;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.MediaType.AUDIO;
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        ByteBuffer myBuffer = mBufferQueue.getFilledBuffer(info);
        buffer.put(myBuffer);
        mBufferQueue.releaseUsedBuffer(myBuffer);
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) {
        return mBufferQueue.getFilledBuffer(info);
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException {
        mBufferQueue.releaseUsedBuffer(buffer);
    }

    protected void start() {
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                spinInput();
            }
        });
        mComponentState = State.RUNNING;
        mThread.start();
    }

    private void spinInput() {
        while(mComponentState != State.CLOSED && !mIsDone) {
            long durationNs = 0;
            if (MediaHelper.VERBOSE) {
                durationNs = -System.nanoTime();
            }
            ByteBuffer outBuffer = mBufferQueue.getEmptyBuffer();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            //reset output buffer
            outBuffer.clear();

            //reset output buffer info
            info.size = 0;
            info.offset = 0;
            //N.B. mSeekTime is currently the time of the first sample in this buffer.
            info.presentationTimeUs = mSeekTime;
            info.flags = 0;

            //prepare a ShortBuffer view of the output buffer
            ShortBuffer outShortBuffer = MediaHelper.getShortBuffer(outBuffer);

            int length = outShortBuffer.remaining();
            int cap = outBuffer.capacity();
            int pos = 0;//outShortBuffer.arrayOffset();

            outShortBuffer.get(mShortBuffer, pos, length);
            short[] outBufferA = mShortBuffer;//outShortBuffer.array();
            outShortBuffer.clear();

            while (!mIsDone) {
                //interleave channels
                //N.B. Always put all samples (of different channels) of the same time in the same buffer.
                for (int i = 0; i < mChannelCount; i++) {
//                short sample = getSampleForTime(mSeekTime, i);
                    outBufferA[pos++] = getSampleForTime(mSeekTime, i);
//                outShortBuffer.put(sample);

                    //increment by short size (2 bytes)
                    info.size += 2;
                }

                //Keep track of the current presentation time in the output audio stream.
                mAbsoluteSampleIndex++;
                mSeekTime = getTimeFromIndex(mSampleRate, mAbsoluteSampleIndex);
                onTimeUpdate();

                //Don't overflow the buffer!
                int shortsForNextSample = mChannelCount;
                if (pos + shortsForNextSample > length) {
                    break;
                }
            }

            outShortBuffer.put(outBufferA, 0, length);

            //just to be sure
            outBuffer.position(info.offset);
            outBuffer.limit(info.offset + info.size);

            if (mIsDone) {
                info.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }

            mBufferQueue.sendFilledBuffer(outBuffer, info);

            if (MediaHelper.VERBOSE) {
                durationNs += System.nanoTime();
                float sec = durationNs / 1000000000L;
                Log.d(TAG, "spinInput: return output buffer after " + MediaHelper.getDecimal(sec) + " seconds: size " + info.size + " for time " + info.presentationTimeUs);
            }
        }
    }

    /**
     * <p>Get the sample time from the sample index given a sample rate.</p>
     *
     * <p>Note: This method provides more accurate timestamps than simply keeping track
     * of the current timestamp and incrementing it by the time per sample.</p>
     * @param sampleRate
     * @param index
     * @return
     */
    protected final long getTimeFromIndex(long sampleRate, int index) {
        return index * 1000000L / sampleRate;
    }

    protected void onTimeUpdate() {

    }

    /**
     * <p>Get a sample for a given time and channel from the source media pipeline component using linear interpolation.</p>
     *
     * <p>Note: Sequential calls to this function must provide strictly increasing times.</p>
     * @param time
     * @param channel
     * @return
     */
    protected abstract short getSampleForTime(long time, int channel);

    @Override
    public void close() {
        //Do nothing.
        mComponentState = State.CLOSED;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
