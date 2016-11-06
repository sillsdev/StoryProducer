package org.sil.storyproducer.media;

import android.media.MediaFormat;

/**
 * Provides static methods for miscellaneous low-level media tasks.
 */
final public class MediaHelper {
    /**
     * lots of logging?
     */
    public static final boolean VERBOSE = false;

    /**
     * the maximum size of input buffers; currently used to prevent buffer overflow.
     */
    public static final int MAX_INPUT_BUFFER_SIZE = 128 * 1024;
    public static final long TIMEOUT_USEC = 1000;

    /**
     * Extract the {@link MediaType} from the format.
     * @param format
     * @return
     */
    public static MediaType getTypeFromFormat(MediaFormat format) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        return getTypeFromMime(mime);
    }

    /**
     * Extract the {@link MediaType} from the mime string.
     * @param mime
     * @return
     */
    public static MediaType getTypeFromMime(String mime) {
        if (mime.startsWith("video")) {
            return MediaType.VIDEO;
        } else if (mime.startsWith("audio")) {
            return MediaType.AUDIO;
        }
        throw new RuntimeException("Unclassified mime type: " + mime);
    }

    /**
     * Copy the value of the given (integer) attribute from one format to another.
     * @param srcFormat
     * @param destFormat
     * @param key
     */
    public static void copyFormatIntKey(MediaFormat srcFormat, MediaFormat destFormat, String key) {
        if(srcFormat.containsKey(key)) {
            destFormat.setInteger(key, srcFormat.getInteger(key));
        }
    }

    /**
     * Copy the value of the given (string) attribute from one format to another.
     * @param srcFormat
     * @param destFormat
     * @param key
     */
    public static void copyMediaFormatStringKey(MediaFormat srcFormat, MediaFormat destFormat, String key) {
        if(srcFormat.containsKey(key)) {
            destFormat.setString(key, srcFormat.getString(key));
        }
    }

    /**
     * Create a new format with only the mime attribute set.
     * @param mime
     * @return
     */
    public static MediaFormat createFormat(String mime) {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, mime);
        return format;
    }

    public enum MediaType {
        AUDIO,
        VIDEO,
        ;
    }
}
