package org.sil.storyproducer.video;

import android.media.MediaFormat;

final public class MediaHelper {
    public static final boolean VERBOSE = true;

    public static final int MAX_INPUT_BUFFER_SIZE = 128 * 1024;
    public static long TIMEOUT_USEC = 1000;

    public static MediaType getTypeFromFormat(MediaFormat format) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if(mime.startsWith("video")) {
            return MediaType.VIDEO;
        }
        else if(mime.startsWith("audio")) {
            return MediaType.AUDIO;
        }
        throw new RuntimeException("Unclassified mime type: " + mime);
    }

    public enum MediaType {
        AUDIO,
        VIDEO,
        ;
    }
}
