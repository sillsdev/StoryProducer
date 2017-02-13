package org.sil.storyproducer.tools.file;

import java.io.File;

public class VideoFiles {
    public static File getDefaultLocation(String story) {
        return FileSystem.getProjectDirectory(story);
    }
}
