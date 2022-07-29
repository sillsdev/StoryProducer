package org.tyndalebt.spadv.tools.media.pipe;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * <p>Describes a component of the media pipeline which will fill or return a {@link ByteBuffer} with data</p>.
 * <p>Note: This interface is only intended to be used by other media pipeline components.</p>
 */
public interface PipedMediaByteBufferSource extends PipedMediaSource {
    /**
     * Request that this component fill the buffer with data.
     * @param buffer the buffer (owned by the caller) to be filled.
     * @param info the info about the buffer to be filled.
     * @throws SourceClosedException if the source is closed prematurely.
     */
    void fillBuffer(ByteBuffer buffer, MediaCodec.BufferInfo info) throws SourceClosedException;

    /**
     * <p>Request that this component provide a buffer with data.</p>
     * <p>Note: This function should always be paired with a subsequent call to {@link #releaseBuffer(ByteBuffer)}.</p>
     * @param info the info about the buffer returned.
     * @return the buffer (owned by this component) filled with data.
     * @throws SourceClosedException if the source is closed prematurely.
     */
    ByteBuffer getBuffer(MediaCodec.BufferInfo info) throws SourceClosedException;

    /**
     * Return a buffer retrieved from {@link #getBuffer(MediaCodec.BufferInfo)} to this component.
     * @param buffer the relinquished buffer (owned by this component).
     *               The buffer should not be accessed or modified after this call.
     * @throws InvalidBufferException if the buffer did not belong to this component.
     * @throws SourceClosedException if the source is closed prematurely.
     */
    void releaseBuffer(ByteBuffer buffer) throws InvalidBufferException, SourceClosedException;
}
