package org.sil.storyproducer.media;

import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Provides static methods for miscellaneous low-level media tasks.
 */
final public class MediaHelper {
    /**
     * lots of logging?
     */
    public static final boolean VERBOSE = true;

    /**
     * the maximum size of input buffers; currently used to prevent buffer overflow.
     */
    public static final int MAX_INPUT_BUFFER_SIZE = 128 * 1024;
    public static final long TIMEOUT_USEC = 1000;

    public static final String MIMETYPE_RAW_AUDIO = "audio/raw";

    /**
     * Get the duration of an audio file in microseconds.
     * @param path
     * @return microsecond duration of the audio file
     */
    public static long getAudioDuration(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(durationStr) * 1000;
    }

    /**
     * <p>Get the sample/frame time from the sample/frame index given a sample/frame rate.</p>
     *
     * <p>Note: This method provides more accurate timestamps than simply keeping track
     * of the current timestamp and incrementing it by the time per sample/frame.</p>
     * @param rate
     * @param index
     * @return
     */
    public static long getTimeFromIndex(long rate, int index) {
        return index * 1000000L / rate;
    }

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
     * Get a ShortBuffer view of a ByteBuffer.
     * @param buffer
     * @return
     */
    public static ShortBuffer getShortBuffer(ByteBuffer buffer) {
        return buffer.order(ByteOrder.nativeOrder()).asShortBuffer();
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
