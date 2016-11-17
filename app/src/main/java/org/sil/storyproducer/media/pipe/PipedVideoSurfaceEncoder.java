package org.sil.storyproducer.media.pipe;

import android.graphics.Canvas;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import org.sil.storyproducer.media.MediaHelper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

public class PipedVideoSurfaceEncoder extends PipedMediaCodec {
    private Surface mSurface;

    private MediaFormat mConfigureFormat;
    private PipedVideoSurfaceSource mSource;

    private Queue<Long> mPresentationTimeQueue = new LinkedList<>();

    private long mCurrentPresentationTime;

    public PipedVideoSurfaceEncoder() {
        //empty default constructor
    }

    @Override
    protected String getComponentName() {
        return "surface encoder";
    }

    @Override
    protected void spinInput() {
        if(mSource.isDone()) {
            Log.d("SurfaceEncoder", "surface encoder: depleted source retrieval");
            return;
        }

        if(mSource == null) {
            throw new RuntimeException("No source provided!");
        }

        while(!mSource.isDone()) {
            Canvas canv = mSurface.lockCanvas(null);
            mCurrentPresentationTime = mSource.fillCanvas(canv);
            mPresentationTimeQueue.add(mCurrentPresentationTime);
            mSurface.unlockCanvasAndPost(canv);
        }

        mCodec.signalEndOfInputStream();
    }

    @Override
    protected void correctTime(MediaCodec.BufferInfo info) {
        try {
            info.presentationTimeUs = mPresentationTimeQueue.remove();
        }
        catch (NoSuchElementException e) {
            throw new RuntimeException("Tried to correct time for extra frame", e);
        }
    }

    public void addSource(PipedVideoSurfaceSource src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("I already got a source");
        }
        mSource = src;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return MediaHelper.MediaType.VIDEO;
    }

    @Override
    public void setup() throws IOException {
        mConfigureFormat = mSource.getOutputFormat();

        mConfigureFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE);
        mConfigureFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        mCodec = MediaCodec.createEncoderByType(mConfigureFormat.getString(MediaFormat.KEY_MIME));
        mCodec.configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mSurface = mCodec.createInputSurface();

        start();
    }
}
