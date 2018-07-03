package org.sil.storyproducer.tools.media.wavaudio

import android.content.Context
import android.widget.Toast
import org.sil.storyproducer.tools.file.getStoryChildInputStream
import org.sil.storyproducer.tools.file.getStoryChildOutputStream
import org.sil.storyproducer.tools.file.storyRelPathExists


/**
 * This class is used to concatenate two Wav files together.
 * <br></br>
 * Assumes the header of the Wav file resembles Microsoft's RIFF specification.<br></br>
 * A specification can be found [here](http://soundfile.sapp.org/doc/WaveFormat/).
 */
fun ConcatenateAudioFiles(context: Context, destAudioRelPath: String, srcAudioRelPath: String) {

    val HEADER_SIZE_BYTES = 44

    val FILE_SIZE_INDEX = 4
    val AUD_SIZE_INDEX = 40

    if (!storyRelPathExists(context, destAudioRelPath) ||
            !storyRelPathExists(context, destAudioRelPath)){
        Toast.makeText(context, "Cannot concatenate files!", Toast.LENGTH_SHORT).show()
        return
    }
    //read the files in
    val destAudioFileByte = getStoryChildInputStream(context,destAudioRelPath)!!.readBytes()
    val srcAudioFileByte = getStoryChildInputStream(context,srcAudioRelPath)!!.readBytes()

    //concatentate the files
    val totalFile = ByteArray(destAudioFileByte.size + (srcAudioFileByte.size - HEADER_SIZE_BYTES))
    var bytes: ByteArray

    //read in the total file size in as big endian.
    var firstFileSizeBigEndian: Int = (destAudioFileByte[7]*1 shl 24 and -0x1000000
            or (destAudioFileByte[6]*1 shl 16 and 0x00FF0000)
            or (destAudioFileByte[5]*1 shl 8 and 0x0000FF00)
            or (destAudioFileByte[4]*1 and 0xFF))
    //Change the raw audio file header to be first audio file size plus second audio file size minus the header size of the second file
    firstFileSizeBigEndian += srcAudioFileByte.size - HEADER_SIZE_BYTES
    bytes = WavHelper.swapEndian(firstFileSizeBigEndian)
    destAudioFileByte[FILE_SIZE_INDEX] = bytes[0]
    destAudioFileByte[FILE_SIZE_INDEX + 1] = bytes[1]
    destAudioFileByte[FILE_SIZE_INDEX + 2] = bytes[2]
    destAudioFileByte[FILE_SIZE_INDEX + 3] = bytes[3]

    //read in the audio section header size in as big endian.
    var firstFileAudioSectionSizeBigEndian = (destAudioFileByte[43]*1  shl 24 and -0x1000000
            or (destAudioFileByte[42]*1  shl 16 and 0x00FF0000)
            or (destAudioFileByte[41]*1  shl 8 and 0x0000FF00)
            or (destAudioFileByte[40]*1  and 0xFF))
    //Change the raw audio header size for the first file to accommodate the second raw audio file
    firstFileAudioSectionSizeBigEndian += srcAudioFileByte.size - HEADER_SIZE_BYTES
    bytes = WavHelper.swapEndian(firstFileAudioSectionSizeBigEndian)
    destAudioFileByte[AUD_SIZE_INDEX] = bytes[0]
    destAudioFileByte[AUD_SIZE_INDEX + 1] = bytes[1]
    destAudioFileByte[AUD_SIZE_INDEX + 2] = bytes[2]
    destAudioFileByte[AUD_SIZE_INDEX + 3] = bytes[3]

    var totalFileIndex = 0
    for (s in destAudioFileByte) {
        totalFile[totalFileIndex++] = s
    }
    for (i in HEADER_SIZE_BYTES until srcAudioFileByte.size - HEADER_SIZE_BYTES) {
        totalFile[totalFileIndex++] = srcAudioFileByte!![i]
    }
    //write file to disk.
    val oStream = getStoryChildOutputStream(context,destAudioRelPath)
    oStream!!.write(totalFile)
    oStream.close()
}
