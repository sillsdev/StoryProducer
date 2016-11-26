package org.sil.storyproducer.media.pipe;

import android.media.MediaCodec;
import android.util.Log;

import org.sil.storyproducer.media.ByteBufferPool;
import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public abstract class PipedAudioShortManipulator implements PipedMediaByteBufferSource {
    private static final String TAG = "PipedAudioShorter";

    private ByteBufferPool mBufferPool = new ByteBufferPool();

    //Only fill output buffer this much. This prevents buffer overflow.
    private static final float PERCENT_BUFFER_FILL = .75f;

    protected int mSampleRate;
    protected int mChannelCount;
    private long mSeekTime = 0;
    private int mAbsoluteSampleIndex = 0;

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.MediaType.AUDIO;
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        spinOutput(buffer, info);
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) {
        ByteBuffer buffer = mBufferPool.get();
        spinOutput(buffer, info);
        return buffer;
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException {
        mBufferPool.release(buffer);
    }

    private void spinOutput(ByteBuffer outBuffer, MediaCodec.BufferInfo info) {
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
        outShortBuffer.clear();

        while(!isDone()) {
            //interleave channels
            //N.B. Always put all samples (of different channels) of the same time in the same buffer.
            for(int i = 0; i < mChannelCount; i++) {
                short sample = getSampleForTime(mSeekTime, i);
                outShortBuffer.put(sample);

                //increment by short size (2 bytes)
                info.size += 2;
            }

            //Keep track of the current presentation time in the output audio stream.
            mAbsoluteSampleIndex++;
            mSeekTime = getTimeFromIndex(mSampleRate, mAbsoluteSampleIndex);

            //Don't overflow the buffer!
            if(info.size > PERCENT_BUFFER_FILL * outBuffer.capacity()) {
                break;
            }
        }

        //just to be sure
        outBuffer.position(info.offset);
        outBuffer.limit(info.offset + info.size);

        if(isDone()) {
            info.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        }

        if (MediaHelper.VERBOSE) {
            Log.d(TAG, "short manip: returned output buffer: size " + info.size + " for time " + info.presentationTimeUs);
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
    protected long getTimeFromIndex(long sampleRate, int index) {
        return index * 1000000L / sampleRate;
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
    public void close() throws IOException {
        //TODO
    }
}
