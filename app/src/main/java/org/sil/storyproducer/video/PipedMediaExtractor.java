package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PipedMediaExtractor implements Closeable, MediaByteBufferSource {
    private static final boolean VERBOSE = true;
    private static final String TAG = "PipedMediaExtractor";

    private MediaExtractor mExtractor;

    private MediaFormat mFormat;
    private MediaHelper.MediaType mType;

    private boolean mIsDone = false;
    private boolean mEOSSent = false;

    private ArrayList<ByteBuffer> buffers = new ArrayList<>(4);
    private ArrayList<Boolean> bufferAvailable = new ArrayList<>(4);

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
    public MediaFormat getFormat() {
        return mFormat;
    }

    @Override
    public MediaHelper.MediaType getType() {
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
        //TODO: determine capacity
        final int capacity = MediaHelper.MAX_INPUT_BUFFER_SIZE;
        ByteBuffer buffer;

        for(int i = 0; i < bufferAvailable.size(); i++) {
            buffer = buffers.get(i);
            if(buffer == null) {
                buffer = ByteBuffer.allocate(capacity);
                buffers.set(i, buffer);
                bufferAvailable.set(i, false);
                return buffer;
            }

            if(bufferAvailable.get(i)) {
                bufferAvailable.set(i, false);
                return buffer;
            }
        }

        buffer = ByteBuffer.allocate(capacity);
        buffers.add(buffer);
        bufferAvailable.add(false);
        return buffer;
    }

    @Override
    public void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException {
        for(int i = 0; i < buffers.size(); i++) {
            if(buffers.get(i) == buffer) {
                buffer.clear();
                bufferAvailable.set(i, true);
                return;
            }
        }
        throw new InvalidBufferException("I don't own that buffer!");
    }

    private void spinOutput(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        if(mIsDone && mEOSSent) {
            throw new RuntimeException("spinOutput called after depleted");
        }

        while (!mIsDone || !mEOSSent) {
            buffer.clear();

//            if(mIsDone && !mEOSSent) {
//                if (VERBOSE) Log.d(TAG, "extractor: EOS");
//                info.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                mEOSSent = true;
//                return;
//            }

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
            if (VERBOSE) {
                Log.d(TAG, "extractor: returned buffer of size " + info.size + " for time " + info.presentationTimeUs);
            }

            if (info.size >= 0) {
                buffer.position(info.offset);
                buffer.limit(info.offset + info.size);
                mExtractor.advance();
            }
            else {
                if (VERBOSE) Log.d(TAG, "extractor: EOS");
                info.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                mIsDone = true;
                mEOSSent = true;
//                return;
            }
//            mIsDone = !mExtractor.advance();
//            if (mIsDone) {
//                if (VERBOSE) Log.d(TAG, "extractor: EOS");
////                //TODO: is EOS queued currently?
//            }

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
