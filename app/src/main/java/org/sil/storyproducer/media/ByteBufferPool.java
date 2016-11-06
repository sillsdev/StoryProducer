package org.sil.storyproducer.media;

import org.sil.storyproducer.media.pipe.InvalidBufferException;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Provides a pool of ByteBuffers to preserve memory. This class is not thread-safe,
 * but it does provide thread-safe operations through the static "shared" functions.
 */

public class ByteBufferPool {
    private final int mCapacity;

    private ArrayList<ByteBuffer> buffers = new ArrayList<>(4);
    private ArrayList<Boolean> bufferAvailable = new ArrayList<>(4);

    private static ByteBufferPool singleton = new ByteBufferPool();

    public ByteBufferPool() {
        this(MediaHelper.MAX_INPUT_BUFFER_SIZE);
    }
    public ByteBufferPool(int capacity) {
        mCapacity = capacity;
    }

    public static synchronized ByteBuffer getShared() {
        return singleton.get();
    }

    public ByteBuffer get() {
        ByteBuffer buffer;

        for(int i = 0; i < bufferAvailable.size(); i++) {
            buffer = buffers.get(i);
            if(buffer == null) {
                buffer = ByteBuffer.allocate(mCapacity);
                buffers.set(i, buffer);
                bufferAvailable.set(i, false);
                return buffer;
            }

            if(bufferAvailable.get(i)) {
                bufferAvailable.set(i, false);
                return buffer;
            }
        }

        buffer = ByteBuffer.allocate(mCapacity);
        buffers.add(buffer);
        bufferAvailable.add(false);
        return buffer;
    }

    public static synchronized void releaseShared(ByteBuffer buffer) throws InvalidBufferException {
        singleton.release(buffer);
    }

    public void release(ByteBuffer buffer) throws InvalidBufferException {
        for(int i = 0; i < buffers.size(); i++) {
            if(buffers.get(i) == buffer) {
                buffer.clear();
                bufferAvailable.set(i, true);
                return;
            }
        }
        throw new InvalidBufferException("I don't own that buffer!");
    }
}
