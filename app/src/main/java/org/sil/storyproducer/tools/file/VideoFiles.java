package org.sil.storyproducer.tools.file;

import java.io.File;

/**
 * VideoFiles represents an abstraction of the video outputs of projects.
 */
public class VideoFiles {
    public static File getDefaultLocation(String story) {
        return FileSystem.getMoviesDirectory(story);
    }

    public static File getTempLocation(String story) {
        return FileSystem.getHiddenTempDirectory(story);
    }
}
