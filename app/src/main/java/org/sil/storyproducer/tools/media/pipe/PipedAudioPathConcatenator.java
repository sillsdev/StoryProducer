package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>This media pipeline component concatenates raw audio streams with specified transition time
 * in between streams. Note that this transition time is halved for the beginning and end of the stream.</p>
 * <p>This component also optionally ensures that each audio stream matches an expected duration.</p>
 */
public class PipedAudioPathConcatenator implements PipedMediaByteBufferSource {
    private static final String TAG = "PipedAudioPathConcatenator";

    private PipedAudioConcatenator mBase;

    private final int mSampleRate;
    private final int mChannelCount;

    /**
     * Create concatenator with specified transition time, using the first audio source's format.
     * @param transitionUs length of audio transitions (dead space between audio sources) in microseconds.
     */
    public PipedAudioPathConcatenator(long transitionUs) {
        this(transitionUs, 0, 0);
    }

    /**
     * Create concatenator with specified transition time, resampling the audio stream.
     * @param transitionUs length of audio transitions (dead space between audio sources) in microseconds.
     * @param sampleRate desired sample rate.
     * @param channelCount desired channel count.
     */
    public PipedAudioPathConcatenator(long transitionUs, int sampleRate, int channelCount) {
        mBase = new PipedAudioConcatenator(transitionUs);
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mBase.getOutputFormat();
    }

    @Override
    public boolean isDone() {
        return mBase.isDone();
    }

    /**
     * <p>Add a source without an expected duration. The audio stream will be used in its entirety.</p>
     *
     * @param sourcePath source audio path.
     */
    public void addSource(String sourcePath) throws SourceUnacceptableException {
        addSource(sourcePath, 0);
    }

    /**
     * <p>Add a source with an expected duration. The expected duration is guaranteed.</p>
     *
     * <p>In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.</p>
     *
     * <p>This function differs from {@link #addLoopingSource(String, long)} by padding the source audio
     * with silence until the duration has elapsed. If duration is shorter than the source audio length, both
     * functions will behave the same.</p>
     *
     * @param sourcePath source audio path.
     * @param duration expected duration of the source audio stream.
     */
    public void addSource(String sourcePath, long duration) {
        PipedMediaByteBufferSource source = null;
        if(sourcePath != null) {
            //If sample rate and channel count were specified, apply them.
            if (mSampleRate > 0) {
                source = new PipedAudioDecoderMaverick(sourcePath, mSampleRate, mChannelCount);
            } else {
                source = new PipedAudioDecoderMaverick(sourcePath);
            }
        }

        mBase.addSource(source, duration);
    }

    /**
     * <p>Add a source with an expected duration. The expected duration is guaranteed.</p>
     *
     * <p>In other words, if a duration is specified for all sources, the output audio stream is
     * guaranteed to be within a couple of samples of the sum of all specified durations and n delays.</p>
     *
     * <p>This function differs from {@link #addSource(String, long)} by looping the source audio
     * until the duration has elapsed. If duration is shorter than the source audio length, both
     * functions will behave the same.</p>
     *
     * @param sourcePath source audio path.
     * @param duration expected duration of the source audio stream.
     */
    public void addLoopingSource(String sourcePath, long duration) {
        PipedMediaByteBufferSource source = null;
        if(sourcePath != null) {
            //If sample rate and channel count were specified, apply them.
            if (mSampleRate > 0) {
                source = new PipedAudioLooper(sourcePath, duration, mSampleRate, mChannelCount);
            } else {
                source = new PipedAudioLooper(sourcePath, duration);
            }
        }

        mBase.addSource(source, duration);
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return mBase.getMediaType();
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        mBase.setup();
    }

    @Override
    public void close() {
        mBase.close();
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) throws SourceClosedException {
        mBase.fillBuffer(buffer, info);
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) throws SourceClosedException {
        return mBase.getBuffer(info);
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException, SourceClosedException {
        mBase.releaseBuffer(buffer);
    }
}
