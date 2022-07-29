package org.tyndalebt.spadv.tools.media.pipe;

/**
 * Describes a component of the media pipeline which receives data from a {@link PipedMediaByteBufferSource}.
 */
public interface PipedMediaByteBufferDest {
    /**
     * Specify a predecessor of this component in the pipeline.
     * @param src the preceding component of the pipeline.
     * @throws SourceUnacceptableException if the source provided does not match the destination's expectations.
     */
    void addSource(PipedMediaByteBufferSource src) throws SourceUnacceptableException;
}
