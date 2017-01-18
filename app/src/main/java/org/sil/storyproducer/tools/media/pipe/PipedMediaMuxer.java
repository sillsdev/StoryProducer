package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>This media pipeline component multiplexes encoded audio and video streams into an output file.
 * This class primarily encapsulates a {@link MediaMuxer}.</p>
 * <p>Components commonly used in conjunction with this class are {@link PipedMediaCodec}
 * (particularly its subclasses {@link PipedMediaEncoder} and {@link PipedVideoSurfaceEncoder})
 * and {@link PipedMediaExtractor}.</p>
 */
public class PipedMediaMuxer implements Closeable, PipedMediaByteBufferDest {
    private static final String TAG = "PipedMediaMuxer";

    private final String mPath;
    private final int mFormat;

    private MediaMuxer mMuxer = null;

    private PipedMediaByteBufferSource mAudioSource = null;
    private int mAudioTrackIndex = -1;
    private MediaFormat mAudioOutputFormat;
    private int mAudioBitrate = -1;
    private StreamThread mAudioThread;

    private PipedMediaByteBufferSource mVideoSource = null;
    private int mVideoTrackIndex = -1;
    private MediaFormat mVideoOutputFormat;
    private int mVideoBitrate = -1;
    private StreamThread mVideoThread;

    /**
     * Create a muxer.
     * @param path the output media file.
     * @param format the format of the output media file
     *               (from {@link android.media.MediaMuxer.OutputFormat}).
     */
    public PipedMediaMuxer(String path, int format) {
        mPath = path;
        mFormat = format;
    }

    @Override
    public void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException {
        if(src.getMediaType() == MediaHelper.MediaType.AUDIO) {
            if(mAudioSource == null) {
                mAudioSource = src;
            }
            else {
                throw new SourceUnacceptableException("audio source already provided");
            }
        }
        else if(src.getMediaType() == MediaHelper.MediaType.VIDEO) {
            if(mVideoSource == null) {
                mVideoSource = src;
            }
            else {
                throw new SourceUnacceptableException("video source already provided");
            }
        }
    }

    private void start() throws IOException, SourceUnacceptableException {
        File output = new File(mPath);
        //Ensure file exists to avoid bugs on some devices.
        if(!output.exists()) {
            output.createNewFile();
        }
        mMuxer = new MediaMuxer(mPath, mFormat);

        if (mAudioSource != null) {
            if(MediaHelper.VERBOSE) Log.v(TAG, "setting up audio track.");
            mAudioSource.setup();

            mAudioOutputFormat = mAudioSource.getOutputFormat();
            //TODO: fudge bitrate since it isn't available
//            mAudioBitrate = mAudioOutputFormat.getInteger(MediaFormat.KEY_BIT_RATE);

            if(MediaHelper.VERBOSE) Log.v(TAG, "adding audio track.");
            mAudioTrackIndex = mMuxer.addTrack(mAudioOutputFormat);
        }
        if (mVideoSource != null) {
            if(MediaHelper.VERBOSE) Log.v(TAG, "setting up video track.");
            mVideoSource.setup();

            mVideoOutputFormat = mVideoSource.getOutputFormat();
            //TODO: fudge bitrate since it isn't available
//            mVideoBitrate = mVideoOutputFormat.getInteger(MediaFormat.KEY_BIT_RATE);

            if(MediaHelper.VERBOSE) Log.v(TAG, "adding video track.");
            mVideoTrackIndex = mMuxer.addTrack(mVideoOutputFormat);
        }
        if(MediaHelper.VERBOSE) Log.v(TAG, "starting");
        mMuxer.start();
    }

    /**
     * Set the muxer in motion.
     * @throws IOException
     * @throws SourceUnacceptableException
     */
    public void crunch() throws IOException, SourceUnacceptableException {
        start();

        if(mAudioSource != null) {
            mAudioThread = new StreamThread(mMuxer, mAudioSource, mAudioTrackIndex, mAudioBitrate);
            mAudioThread.start();
        }

        if(mVideoSource != null) {
            mVideoThread = new StreamThread(mMuxer, mVideoSource, mVideoTrackIndex, mVideoBitrate);
            mVideoThread.start();
        }

        if(mAudioThread != null) {
            try {
                mAudioThread.join();
            } catch (InterruptedException e) {
                Log.w(TAG, "Audio thread did not end!", e);
            }
        }

        if(mVideoThread != null) {
            try {
                mVideoThread.join();
            } catch (InterruptedException e) {
                Log.w(TAG, "Video thread did not end!", e);
            }
        }

        close();
    }

    private class StreamThread extends Thread {
        private final MediaMuxer mMuxer;
        private final PipedMediaByteBufferSource mSource;
        private final int mTrackIndex;

        private final int mBitrate;
        private long mProgress = 0;

        public StreamThread(MediaMuxer muxer, PipedMediaByteBufferSource src, int trackIndex, int bitrate) {
            mMuxer = muxer;
            mSource = src;
            mTrackIndex = trackIndex;
            mBitrate = bitrate;
        }

        @Override
        public void run() {
            ByteBuffer buffer;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (!mSource.isDone()) {
                buffer = mSource.getBuffer(info);
                if (MediaHelper.VERBOSE)
                    Log.v(TAG, "[track " + mTrackIndex + "] writing output buffer of size "
                            + info.size + " for time " + info.presentationTimeUs);

                //TODO: determine presentation time for end of this buffer if possible
                mProgress = info.presentationTimeUs;// + (info.size * 1000000L / 8 / mBitrate);

                synchronized (mMuxer) {
                    mMuxer.writeSampleData(mTrackIndex, buffer, info);
                }
                mSource.releaseBuffer(buffer);
            }
        }

        public long getProgress() {
            return mProgress;
        }
    }

    public long getAudioProgress() {
        if(mAudioThread != null) {
            return mAudioThread.getProgress();
        }
        return 0;
    }

    public long getVideoProgress() {
        if(mVideoThread != null) {
            return mVideoThread.getProgress();
        }
        return 0;
    }

    @Override
    public void close() {
        //Close sources.
        if(mAudioSource != null) {
            mAudioSource.close();
            mAudioSource = null;
        }
        if(mVideoSource != null) {
            mVideoSource.close();
            mVideoSource = null;
        }

        //Close self.
        if(mMuxer != null) {
            try {
                mMuxer.stop();
            }
            catch(IllegalStateException e) {
                Log.e(TAG, "Failed to stop MediaMuxer!", e);
            }
            finally {
                mMuxer.release();
            }
            mMuxer = null;
        }
    }
}
