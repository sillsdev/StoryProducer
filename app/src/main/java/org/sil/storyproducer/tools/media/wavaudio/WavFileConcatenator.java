package org.sil.storyproducer.tools.media.wavaudio;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class is used to concatenate two Wav files together.
 * <br/>
 * Assumes the header of the Wav file resembles Microsoft's RIFF specification.<br/>
 * A specification can be found <a href=http://soundfile.sapp.org/doc/WaveFormat/>here</a>.
 */
public class WavFileConcatenator {
    private static final String TAG = "WavFileConcatentor";

    private static final int HEADER_SIZE_BYTES = 44;

    private static final int FILE_SIZE_INDEX = 4;
    private static final int AUD_SIZE_INDEX = 40;
    private static byte[] destAudioFileByte;
    private static byte[] srcAudioFileByte;
    private static byte[] totalFile;
    private static File firstFile;

    /**
     * The function that will concatenate two audio files together and place the new concatenated
     * audio file in the location of the firstAudioFile location.
     * @param destAudioFile
     * @param srcAudioFile
     *
     * @throws FileNotFoundException
     */
    public static void ConcatenateAudioFiles(File destAudioFile, File srcAudioFile) throws FileNotFoundException {
        if (destAudioFile == null || srcAudioFile == null) {
            throw new FileNotFoundException("Could not find file!");
        }
        firstFile = destAudioFile;

        readInFiles(destAudioFile, srcAudioFile);
        concatenateAudioFiles((int) destAudioFile.length(), ((int) srcAudioFile.length()));
        writeFinalFile();
    }

    /**
     * Reads in the audio files and places the audio files into byte array.
     * @param destAudioFile
     * @param srcAudioFile
     */
    private static void readInFiles(File destAudioFile, File srcAudioFile) {
        destAudioFileByte = new byte[(int) destAudioFile.length()];
        srcAudioFileByte = new byte[(int) srcAudioFile.length()];
        try {
            FileInputStream fil = new FileInputStream(destAudioFile);
            fil.read(destAudioFileByte);
            fil.close();
            fil = new FileInputStream(srcAudioFile);
            fil.read(srcAudioFileByte);
            fil.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not find WAV file", e);
        } catch (IOException e) {
            Log.e(TAG, "Could not open WAV file", e);
        }
    }

    /**
     * Read the total file size and audio data section file size from the first WAV file
     * and add the second file data section size to the total file size and audio data
     * section for the first WAV file.
     * <br/>
     * After the headers have been changed in the firstAudioFile, append the audio data from
     * secondAudioFile to the first WAV file. Write both first full file and second WAV file audio section
     * into the totalFile array.
     * @param destAudioFileLength
     * @param srcAudioFileLength
     */
    private static void concatenateAudioFiles(int destAudioFileLength, int srcAudioFileLength) {
        totalFile = new byte[destAudioFileLength + (srcAudioFileLength - HEADER_SIZE_BYTES)];
        byte[] bytes;

        //read in the total file size in as big endian.
        int firstFileSizeBigEndian = (   ((destAudioFileByte[7] << 24) & 0xFF000000)
                                        |((destAudioFileByte[6] << 16) & 0x00FF0000)
                                        |((destAudioFileByte[5] << 8) & 0x0000FF00)
                                        | destAudioFileByte[4] & 0xFF);
        //Change the raw audio file header to be first audio file size plus second audio file size minus the header size of the second file
        firstFileSizeBigEndian += (srcAudioFileLength - HEADER_SIZE_BYTES);
        bytes = WavHelper.swapEndian(firstFileSizeBigEndian);
        destAudioFileByte[FILE_SIZE_INDEX] = bytes[0];
        destAudioFileByte[FILE_SIZE_INDEX + 1] = bytes[1];
        destAudioFileByte[FILE_SIZE_INDEX + 2] = bytes[2];
        destAudioFileByte[FILE_SIZE_INDEX + 3] = bytes[3];

        //read in the audio section header size in as big endian.
        int firstFileAudioSectionSizeBigEndian = (  ((destAudioFileByte[43] << 24) & 0xFF000000)
                                                    |((destAudioFileByte[42] << 16) & 0x00FF0000)
                                                    |((destAudioFileByte[41] << 8) & 0x0000FF00)
                                                    | destAudioFileByte[40] & 0xFF);
        //Change the raw audio header size for the first file to accommodate the second raw audio file
        firstFileAudioSectionSizeBigEndian += (srcAudioFileLength - HEADER_SIZE_BYTES);
        bytes = WavHelper.swapEndian(firstFileAudioSectionSizeBigEndian);
        destAudioFileByte[AUD_SIZE_INDEX] = bytes[0];
        destAudioFileByte[AUD_SIZE_INDEX + 1] = bytes[1];
        destAudioFileByte[AUD_SIZE_INDEX + 2] = bytes[2];
        destAudioFileByte[AUD_SIZE_INDEX + 3] = bytes[3];

        int totalFileIndex = 0;
        for (byte s : destAudioFileByte) {
            totalFile[totalFileIndex++] = s;
        }
        for (int i = HEADER_SIZE_BYTES; i < srcAudioFileLength - HEADER_SIZE_BYTES; i++) {
            totalFile[totalFileIndex++] = srcAudioFileByte[i];
        }
    }

    /**
     * Write the totalFile byte array to file.
     */
    private static void writeFinalFile() {
        try {
            FileOutputStream fos = new FileOutputStream(firstFile);
            fos.write(totalFile);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not open Wav file for writing!", e);
        } catch (IOException e) {
            Log.e(TAG, "Could not write to WAV file", e);
        }
    }

}
