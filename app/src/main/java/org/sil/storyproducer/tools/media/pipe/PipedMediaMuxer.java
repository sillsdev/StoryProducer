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

    private volatile boolean mAbnormallyEnded = false;

    private static final Object audioLock = new Object();
    private static final Object videoLock = new Object();
    private static final Object muxerLock = new Object();

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

    /**
     * Get approximate current progress of the audio track (i.e. the latest timestamp in microseconds).
     * @return approximate microseconds of completed audio
     */
    public long getAudioProgress() {
        return getAudioProgress(true);
    }
    private long getAudioProgress(boolean allowDeflect) {
        if(mAudioThread != null) {
            return mAudioThread.getProgress();
        }
        else if(allowDeflect) {
            //If there is no audio channel, use the video progress as audio progress.
            return getVideoProgress(false);
        }
        else {
            return 0;
        }
    }

    /**
     * Get approximate current progress of the video track (i.e. the latest timestamp in microseconds).
     * @return approximate microseconds of completed video
     */
    public long getVideoProgress() {
        return getVideoProgress(true);
    }
    private long getVideoProgress(boolean allowDeflect) {
        if(mVideoThread != null) {
            return mVideoThread.getProgress();
        }
        else if(allowDeflect) {
            //If there is no video channel, use the audio progress as video progress.
            return getAudioProgress(false);
        }
        else {
            return 0;
        }
    }

    /**
     * Set the muxer in motion.
     * @return whether the muxer finished its job.
     * @throws IOException
     * @throws SourceUnacceptableException
     */
    public boolean crunch() throws IOException, SourceUnacceptableException {
        start();

        synchronized (audioLock) {
            if (mAudioSource != null) {
                mAudioThread = new StreamThread(mMuxer, mAudioSource, mAudioTrackIndex, mAudioBitrate);
                mAudioThread.start();
            }
        }

        synchronized (videoLock) {
            if (mVideoSource != null) {
                mVideoThread = new StreamThread(mMuxer, mVideoSource, mVideoTrackIndex, mVideoBitrate);
                mVideoThread.start();
            }
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

        return !mAbnormallyEnded;
    }

    private void start() throws IOException, SourceUnacceptableException {
        File output = new File(mPath);
        //Ensure file exists to avoid bugs on some devices.
        if(!output.exists()) {
            output.createNewFile();
        }
        synchronized (muxerLock) {
            mMuxer = new MediaMuxer(mPath, mFormat);

            if (mAudioSource != null) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "setting up audio track.");
                mAudioSource.setup();

                mAudioOutputFormat = mAudioSource.getOutputFormat();
                //TODO: fudge bitrate since it isn't available
//            mAudioBitrate = mAudioOutputFormat.getInteger(MediaFormat.KEY_BIT_RATE);

                if (MediaHelper.VERBOSE) Log.v(TAG, "adding audio track.");
                mAudioTrackIndex = mMuxer.addTrack(mAudioOutputFormat);
            }
            if (mVideoSource != null) {
                if (MediaHelper.VERBOSE) Log.v(TAG, "setting up video track.");
                mVideoSource.setup();

                mVideoOutputFormat = mVideoSource.getOutputFormat();
                //TODO: fudge bitrate since it isn't available
//            mVideoBitrate = mVideoOutputFormat.getInteger(MediaFormat.KEY_BIT_RATE);

                if (MediaHelper.VERBOSE) Log.v(TAG, "adding video track.");
                mVideoTrackIndex = mMuxer.addTrack(mVideoOutputFormat);
            }
            if (MediaHelper.VERBOSE) Log.v(TAG, "starting");
            mMuxer.start();
        }
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
            try {
                while (!mSource.isDone()) {
                    buffer = mSource.getBuffer(info);
                    if (MediaHelper.VERBOSE)
                        Log.v(TAG, "[track " + mTrackIndex + "] writing output buffer of size "
                                + info.size + " for time " + info.presentationTimeUs);

                    //Update progress if progress increased. (There may be edge cases where
                    //presentation time is 0, and that is an undesirable progress indicator.)
                    //In other words, never allow regression, only progression.
                    if(info.presentationTimeUs > mProgress) {
                        //TODO: determine presentation time for end of this buffer if possible
                        mProgress = info.presentationTimeUs;// + (info.size * 1000000L / 8 / mBitrate);
                    }

                    synchronized (mMuxer) {
                        mMuxer.writeSampleData(mTrackIndex, buffer, info);
                    }
                    mSource.releaseBuffer(buffer);
                }
            }
            catch(SourceClosedException e) {
                Log.w(TAG, "Source closed forcibly", e);
                mAbnormallyEnded = true;
            }
        }

        public long getProgress() {
            return mProgress;
        }
    }

    @Override
    public void close() {
        synchronized(muxerLock) {
            //Close sources.
            synchronized (audioLock) {
                if (mAudioSource != null) {
                    mAudioSource.close();
                    mAudioSource = null;
                }
            }
            synchronized (videoLock) {
                if (mVideoSource != null) {
                    mVideoSource.close();
                    mVideoSource = null;
                }
            }

            //Close self.
            if (mMuxer != null) {
                try {
                    mMuxer.stop();
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Failed to stop MediaMuxer!", e);
                } finally {
                    try {
                        mMuxer.release();
                    }
                    catch(IllegalStateException e) {
                        //It isn't documented that MediaMuxer.release throws an IllegalStateException
                        //sometimes, but it has been seen experimentally.
                        Log.w(TAG, "Failed to release MediaMuxer", e);
                    }
                }
                mMuxer = null;
            }
        }
    }
}
