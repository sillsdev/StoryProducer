package org.tyndalebt.spadv.tools.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.text.DecimalFormat

/**
 * Provides static methods for miscellaneous low-level media tasks.
 */
object MediaHelper {
    /** lots of logging?  */
    val VERBOSE = false
    val DEBUG = false

    //Note: Perhaps this max size should be increased in the future.
    /** the maximum size of input buffers; currently used to prevent buffer overflow  */
    val MAX_INPUT_BUFFER_SIZE = 128 * 1024
    val TIMEOUT_USEC: Long = 10000

    val MIMETYPE_RAW_AUDIO = "audio/raw"


    private val form2Dec = DecimalFormat("#0.00")

    /**
     * Get the duration of an audio file in microseconds.
     * @param context
     * @param uri
     * @return microsecond duration of the audio file
     */
    fun getAudioDuration(context: Context, uri: Uri): Long {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            return (Integer.parseInt(durationStr) * 1000).toLong()
        } catch (e: Exception) {
            //I don't know what happened, but lets not stop everything.
        }

        return 0
    }

    /**
     * Get a 2-decimal number for printing.
     * @param number
     * @return formatted number
     */
    fun getDecimal(number: Double): String {
        return form2Dec.format(number)
    }

    /**
     *
     * Get the sample/frame presentation time from the sample/frame index given a sample/frame rate.
     *
     *
     * Note: This method provides more accurate timestamps than simply keeping track
     * of the current timestamp and incrementing it by the time per sample/frame.
     * @param rate sample or frame rate
     * @param index particular sample or frame number
     * @return sample or frame presentation time in microseconds
     */
    fun getTimeFromIndex(rate: Long, index: Int): Long {
        return (index * 1000000.0 / rate).toLong()
    }

    /**
     * Extract the [MediaType] from the format.
     * @param format
     * @return
     */
    fun getTypeFromFormat(format: MediaFormat): MediaType {
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        return getTypeFromMime(mime)
    }

    /**
     * Extract the [MediaType] from the mime string.
     * @param mime
     * @return
     */
    fun getTypeFromMime(mime: String): MediaType {
        if (mime.startsWith("video")) {
            return MediaType.VIDEO
        } else if (mime.startsWith("audio")) {
            return MediaType.AUDIO
        }
        throw RuntimeException("Unclassified mime type: $mime")
    }

    /**
     * Get a ShortBuffer view of a ByteBuffer.
     * @param buffer
     * @return
     */
    fun getShortBuffer(buffer: ByteBuffer): ShortBuffer {
        return buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
    }

    /**
     * Copy buffer metadata from one object to another.
     * @param src metadata to read
     * @param dest metadata to overwrite
     */
    fun copyBufferInfo(src: MediaCodec.BufferInfo, dest: MediaCodec.BufferInfo) {
        dest.set(src.offset, src.size, src.presentationTimeUs, src.flags)
    }

    /**
     * Copy the value of the given (integer) attribute from one format to another.
     * @param srcFormat
     * @param destFormat
     * @param key
     */
    fun copyFormatIntKey(srcFormat: MediaFormat, destFormat: MediaFormat, key: String) {
        if (srcFormat.containsKey(key)) {
            destFormat.setInteger(key, srcFormat.getInteger(key))
        }
    }

    /**
     * Create a new format with only the mime attribute set.
     * @param mime
     * @return
     */
    fun createFormat(mime: String): MediaFormat {
        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, mime)
        return format
    }

    enum class MediaType {
        AUDIO,
        VIDEO
    }
}
