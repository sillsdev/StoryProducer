package org.sil.storyproducer.video;

public interface MediaByteBufferDest {
    void addSource(MediaByteBufferSource src) throws SourceUnacceptableException;
}
