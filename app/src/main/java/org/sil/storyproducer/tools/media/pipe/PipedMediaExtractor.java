package org.sil.storyproducer.tools.media.pipe;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import org.sil.storyproducer.tools.media.ByteBufferPool;
import org.sil.storyproducer.tools.media.MediaHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>This media pipeline component extracts a media file from a file and outputs an encoded media
 * stream. This class primarily encapsulates a {@link MediaExtractor}.</p>
 */
public class PipedMediaExtractor implements PipedMediaByteBufferSource {
    private static final String TAG = "PipedMediaExtractor";

    private MediaExtractor mExtractor;

    private MediaFormat mFormat;
    private MediaHelper.MediaType mType;

    private boolean mIsDone = false;

    private ByteBufferPool mBufferPool = new ByteBufferPool();

    /**
     * Create extractor from specified file.
     * @param path path of the media file.
     * @param type (audio/video) track to select from file.
     * @throws IOException
     */
    public PipedMediaExtractor(String path, MediaHelper.MediaType type) throws IOException {
        mType = type;

        mExtractor = new MediaExtractor();
        mExtractor.setDataSource(path);

        for(int i = 0; i < mExtractor.getTrackCount(); i++) {
            mFormat = mExtractor.getTrackFormat(i);
            if(MediaHelper.getTypeFromFormat(mFormat) == type) {
                mExtractor.selectTrack(i);
                return;
            }
        }
        throw new IOException("File does not contain track of type " + type.name());
    }

    @Override
    public void setup() throws IOException, SourceUnacceptableException {
        //Do nothing.
    }

    @Override
    public MediaFormat getOutputFormat() {
        return mFormat;
    }

    @Override
    public MediaHelper.MediaType getMediaType() {
        return mType;
    }

    @Override
    public boolean isDone() {
        return mIsDone;
    }

    @Override
    public void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        spinOutput(buffer, info);
    }

    @Override
    public ByteBuffer getBuffer(MediaCodec.BufferInfo info) {
        ByteBuffer buffer = getBuffer();
        spinOutput(buffer, info);
        return buffer;
    }

    private ByteBuffer getBuffer() {
        return mBufferPool.get();
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException {
        mBufferPool.release(buffer);
    }

    private void spinOutput(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        if(mIsDone) {
            throw new RuntimeException("spinOutput called after depleted");
        }

        while (!mIsDone) {
            buffer.clear();

            info.offset = 0;
            info.size = mExtractor.readSampleData(buffer, 0);
            info.presentationTimeUs = mExtractor.getSampleTime();
            int actualFlags = mExtractor.getSampleFlags();
            info.flags = 0;
            //TODO: Do we need this SDK check?
            if(Build.VERSION.SDK_INT >= 21 && (actualFlags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                info.flags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
            }
            else if(Build.VERSION.SDK_INT < 21 && (actualFlags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
                info.flags |= MediaCodec.BUFFER_FLAG_SYNC_FRAME;
            }
            if((actualFlags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                info.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }
            //TODO: Why aren't these listed in documentation but in annotations?
//            if((actualFlags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
//                info.flags |= MediaCodec.BUFFER_FLAG_SYNC_FRAME;
//            }
//            if((actualFlags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                info.flags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
//            }
            if (MediaHelper.VERBOSE) {
                Log.d(TAG, "extractor: returned buffer of size " + info.size + " for time " + info.presentationTimeUs);
            }

            if (info.size >= 0) {
                buffer.position(info.offset);
                buffer.limit(info.offset + info.size);
                mExtractor.advance();
            }
            else {
                if (MediaHelper.VERBOSE) Log.d(TAG, "extractor: EOS");
                info.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                mIsDone = true;
            }

            return;
        }
    }

    @Override
    public void close() {
        if(mExtractor != null) {
            mExtractor.release();
        }
    }
}
