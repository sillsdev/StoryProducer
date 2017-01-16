package org.sil.storyproducer.tools.media;

import android.media.MediaCodec;
import android.util.Log;

import org.sil.storyproducer.tools.media.pipe.InvalidBufferException;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ByteBufferQueue is a producer-consumer data structure specialized for ByteBuffers.
 * The idea is to allow one thread to fill empty buffers and another to use filled buffers.
 */
public class ByteBufferQueue {
    private static final String TAG = "ByteBufferQueue";

    private final Object mLock = new Object();

    private final int mCapacity;

    //TODO: check/re-evaluate this value
    private static final int BUFFER_CAPACITY = 16 * 1024;
    private final ByteBufferPool mBufferPool = new ByteBufferPool(BUFFER_CAPACITY);

    private int mBuffersOut = 0;

    private final BlockingQueue<MediaBuffer> mFilledBuffers;

    public ByteBufferQueue(int capacity) {
        mCapacity = capacity;
        mFilledBuffers = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * Check if there is anything meaningful in queue (i.e. if the consumer queue is empty).
     * @return whether the queue contains any filled buffers
     */
    public boolean isEmpty() {
        return mFilledBuffers.isEmpty();
    }

    /**
     * (Producer operation) Pull an empty buffer from the queue, blocking until one becomes available.
     * @return empty buffer
     */
    public ByteBuffer getEmptyBuffer() {
        int loops = 0;
        while(true) {
            synchronized (mLock) {
                boolean bufferIsAvailable = mBuffersOut < mCapacity;
                if (bufferIsAvailable) {
                    mBuffersOut++;
                    return mBufferPool.get();
                }
            }

            //If unable to get a buffer, wait a little bit and try again.
            try {
                if(MediaHelper.VERBOSE && loops++ > 1000) {
                    Log.d(TAG, "empty buffer unavailable");
                    loops = 0;
                }
                Thread.sleep(1);
            } catch (InterruptedException e) {
                //TODO: handle interrupt
                e.printStackTrace();
            }
        }
    }

    /**
     * (Producer operation) Pass a filled buffer from the producer to the consumer.
     * @param buffer filled buffer
     * @param info filled buffer metadata
     */
    public void sendFilledBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        try {
            mFilledBuffers.put(new MediaBuffer(buffer, info));
        } catch (InterruptedException e) {
            //TODO: handle interrupt
            e.printStackTrace();
        }
    }

    /**
     * (Consumer operation) Pull a filled buffer from the queue, blocking until one becomes available.
     * @param info filled buffer metadata (filled by function)
     * @return filled buffer
     */
    public ByteBuffer getFilledBuffer(MediaCodec.BufferInfo info) {
        MediaBuffer mb = null;
        try {
            while(mb == null) {
                mb = mFilledBuffers.poll(1, TimeUnit.SECONDS);
                if(MediaHelper.VERBOSE && mb == null) {
                    Log.d(TAG, "filled buffer unavailable");
                }
            }
        } catch (InterruptedException e) {
            //TODO: handle interrupt
            e.printStackTrace();
        }
        MediaHelper.copyBufferInfo(mb.info, info);
        return mb.buffer;
    }

    /**
     * (Consumer operation) Pass a used buffer back to the queue for reuse.
     * @param buffer used buffer
     * @throws InvalidBufferException if buffer does not belong to queue
     */
    public void releaseUsedBuffer(ByteBuffer buffer) throws InvalidBufferException {
        synchronized (mLock) {
            mBufferPool.release(buffer);
            mBuffersOut--;
        }
    }
}
