package org.sil.storyproducer.video;

import android.graphics.Canvas;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class PipedMediaEncoderSurface extends PipedMediaCodec implements MediaSurfaceDest {
    private Surface mSurface;

    private MediaSurfaceSource mSource = null;

    private Queue<Long> mPresentationTimeQueue = new LinkedList<>();

    private long mCurrentPresentationTime;

    public PipedMediaEncoderSurface(MediaFormat format) throws IOException {
        super(format);
        mCodec = MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE);
        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mSurface = mCodec.createInputSurface();

        start();
    }

    @Override
    protected String getComponentName() {
        return "surface encoder";
    }

    private int mCounter = 0;

    @Override
    protected void spinInput() {
        if(mSource.isDone()) {
            Log.d("SurfaceEncoder", "surface encoder: depleted source retrieval");
            return;
        }

        if(mSource == null) {
            throw new RuntimeException("No source provided!");
        }
        Canvas canv = mSurface.lockCanvas(null);
        mCurrentPresentationTime = mSource.fillCanvas(canv);
        mPresentationTimeQueue.add(mCurrentPresentationTime);
        mSurface.unlockCanvasAndPost(canv);

        if(mSource.isDone()) {
            mCodec.signalEndOfInputStream();
        }
    }

    @Override
    protected void correctTime(MediaCodec.BufferInfo info) {
        info.presentationTimeUs = mPresentationTimeQueue.remove();
    }

    @Override
    public void addSource(MediaSurfaceSource src) throws SourceUnacceptableException {
        if(mSource != null) {
            throw new SourceUnacceptableException("I already got a source");
        }
        mSource = src;
    }
}
