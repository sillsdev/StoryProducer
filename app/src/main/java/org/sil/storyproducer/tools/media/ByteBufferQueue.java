package org.sil.storyproducer.tools.media;

import android.media.MediaCodec;
import android.util.Log;

import org.sil.storyproducer.tools.media.pipe.InvalidBufferException;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ByteBufferQueue {
    private static final String TAG = "ByteBufferQueue";

    private final Object mLock = new Object();

    private final int mCapacity;

    private static final int BUFFER_CAPACITY = 16 * 1024;
    private final ByteBufferPool mBufferPool = new ByteBufferPool(BUFFER_CAPACITY);

    private int mBuffersOut = 0;

    private final BlockingQueue<MediaBuffer> mFilledBuffers;

    public ByteBufferQueue(int capacity) {
        mCapacity = capacity;
        mFilledBuffers = new ArrayBlockingQueue<>(capacity);
    }

    public ByteBuffer getEmptyBuffer() {
        while(true) {
            synchronized (mLock) {
                boolean bufferIsAvailable = mBuffersOut < mCapacity;
                if (bufferIsAvailable) {
                    mBuffersOut++;
                    return mBufferPool.get();
                }
            }

            try {
                Log.d(TAG, "empty buffer unavailable");
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendFilledBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        //TODO: offer? put? timeout?
        try {
            mFilledBuffers.put(new MediaBuffer(buffer, info));
        } catch (InterruptedException e) {
            //TODO
            e.printStackTrace();
        }
    }

    public ByteBuffer getFilledBuffer(MediaCodec.BufferInfo info) {
        //TODO: poll? handle timeout?
        MediaBuffer mb = null;
        try {
            while(mb == null) {
                mb = mFilledBuffers.poll(1, TimeUnit.SECONDS);
                if(MediaHelper.VERBOSE && mb == null) {
                    Log.d(TAG, "couldn't get filled buffer");
                }
            }
        } catch (InterruptedException e) {
            //TODO
            e.printStackTrace();
        }
        MediaHelper.copyBufferInfo(mb.info, info);
        return mb.buffer;
    }

    public void releaseUsedBuffer(ByteBuffer buffer) throws InvalidBufferException {
        synchronized (mLock) {
            mBufferPool.release(buffer);
            mBuffersOut--;
        }
    }
}
