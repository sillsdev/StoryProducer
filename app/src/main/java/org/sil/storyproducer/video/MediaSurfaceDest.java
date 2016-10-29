package org.sil.storyproducer.video;

public interface MediaSurfaceDest {
    void addSource(MediaSurfaceSource src) throws SourceUnacceptableException;
}
