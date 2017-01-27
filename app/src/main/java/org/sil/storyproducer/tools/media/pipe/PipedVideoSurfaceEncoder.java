package org.sil.storyproducer.tools.media.pipe;

import android.graphics.Canvas;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * <p>This media pipeline component provides a simple encoder encapsulating a {@link MediaCodec}.
 * Unlike {@link PipedMediaEncoder}, it uses an input {@link Surface} instead of ByteBuffers.
 * This component takes raw canvas frames of a video and outputs an encoded video stream.</p>
 * <p>Sources for this component must implement {@link Source}.</p>
 */
public class PipedVideoSurfaceEncoder extends PipedMediaCodec {
    private static final String TAG = "PipedVideoSurfaceEnc";
    @Override
    protected String getComponentName() {
        return TAG;
    }

    private Surface mSurface;

    private MediaFormat mConfigureFormat;
    private Source mSource;

    private final Queue<Long> mPresentationTimeQueue = new LinkedList<>();

    private long mCurrentPresentationTime;

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.MediaType.VIDEO;
    }

    /**
     * Specify a canvas provider for this component in the pipeline.
     * @param src the preceding component (a canvas drawer) of the pipeline.
     * @throws SourceUnacceptableException
     */
    public void addSource(Source src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("I already got a source");
        }
        mSource = src;
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        mSource.setup();
        mConfigureFormat = mSource.getOutputFormat();

        mConfigureFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE);
        mConfigureFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        mCodec = MediaCodec.createEncoderByType(mConfigureFormat.getString(MediaFormat.KEY_MIME));
        mCodec.configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mSurface = mCodec.createInputSurface();

        mComponentState = State.SETUP;

        start();
    }

    @Override
    protected void spinInput() {
        if(mSource == null) {
            throw new RuntimeException("No source provided!");
        }

        while(mComponentState != State.CLOSED && !mSource.isDone()) {
            Canvas canv;
            //Note: This method of getting a canvas to draw to may be invalid
            //per documentation of MediaCodec.getInputSurface().
            if(Build.VERSION.SDK_INT >= 23) {
                canv = mSurface.lockHardwareCanvas();
            }
            else {
                canv = mSurface.lockCanvas(null);
            }
            mCurrentPresentationTime = mSource.fillCanvas(canv);
            synchronized (mPresentationTimeQueue) {
                mPresentationTimeQueue.add(mCurrentPresentationTime);
            }
            mSurface.unlockCanvasAndPost(canv);
        }

        if(mComponentState != State.CLOSED) {
            mCodec.signalEndOfInputStream();
        }

        mSource.close();
    }

    @Override
    protected void correctTime(MediaCodec.BufferInfo info) {
        try {
            synchronized (mPresentationTimeQueue) {
                info.presentationTimeUs = mPresentationTimeQueue.remove();
            }
        }
        catch (NoSuchElementException e) {
            throw new RuntimeException("Tried to correct time for extra frame", e);
        }
    }

    /**
     * Describes a component of the media pipeline which draws frames to a provided canvas when called.
     */
    public interface Source extends PipedMediaSource {
        /**
         * Request that this component draw a frame to the canvas.
         * @param canv the canvas to be drawn upon.
         * @return the presentation time (in microseconds) of the drawn frame.
         */
        long fillCanvas(Canvas canv);
    }
}
