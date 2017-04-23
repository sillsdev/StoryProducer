package org.sil.storyproducer.tools.media.wavaudio;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is used to concatenate two Wav files together.
 * <br/>
 * Assumes the header of the Wav file resembles Microsoft's RIFF specification.<br/>
 * A specification can be found <a href=http://soundfile.sapp.org/doc/WaveFormat/>here</a>.
 */
public class WavFileConcatenator {
    private static final String TAG = "WavFileConcatentor";

    private static final int HEADER_SIZE_BYTES = 44;
    private static byte[] firstAudioFileByte;
    private static byte[] secondAudioFileByte;
    private static byte[] totalFile;
    private static File firstFile;

    /**
     * The function that will concatenate two audio files together and place the new concatenated
     * audio file in the location of the firstAudioFile location.
     * @param firstAudioFile The first audio file where the concatenation will be written to.
     * @param secondAudioFile The second audio file to be read. This will be appended to the
     *                        firstAudioFile
     * @throws FileNotFoundException
     */
    public static void ConcatenateAudioFiles(File firstAudioFile, File secondAudioFile) throws FileNotFoundException {
        if (firstAudioFile == null || secondAudioFile == null) {
            throw new FileNotFoundException("Could not find file! Class: WavFileConcatenator");
        }
        firstFile = firstAudioFile;

        readInFiles(firstAudioFile, secondAudioFile);
        concatenateAudioFiles((int) firstAudioFile.length(), ((int) secondAudioFile.length()));
        writeTotalFile();
    }

    /**
     * Reads in the audio files and places the audio files into byte array.
     * @param firstAudioFile
     * @param secondAudioFile
     */
    private static void readInFiles(File firstAudioFile, File secondAudioFile) {
        firstAudioFileByte = new byte[(int) firstAudioFile.length()];
        secondAudioFileByte = new byte[(int) secondAudioFile.length()];
        try {
            FileInputStream fil = new FileInputStream(firstAudioFile);
            fil.read(firstAudioFileByte);
            fil.close();
            fil = new FileInputStream(secondAudioFile);
            fil.read(secondAudioFileByte);
            fil.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not find WAV file");
        } catch (IOException e) {
            Log.e(TAG, "Could not open WAV file");
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
     * @param firstAudioFileLength
     * @param secondAudioFileLength
     */
    private static void concatenateAudioFiles(int firstAudioFileLength, int secondAudioFileLength) {
        totalFile = new byte[firstAudioFileLength + (secondAudioFileLength - HEADER_SIZE_BYTES)];
        byte[] bytes;

        //read in the total file size in as big endian.
        int firstFileSizeBigEndian = (   ((firstAudioFileByte[7] << 24) & 0xFF000000)
                                        |((firstAudioFileByte[6] << 16) & 0x00FF0000)
                                        |((firstAudioFileByte[5] << 8) & 0x0000FF00)
                                        | firstAudioFileByte[4] & 0xFF);
        //Change the raw audio file header to be first audio file size plus second audio file size minus the header size of the second file
        firstFileSizeBigEndian += (secondAudioFileLength - HEADER_SIZE_BYTES);
        bytes = swapEndian(firstFileSizeBigEndian);
        firstAudioFileByte[4] = bytes[0];
        firstAudioFileByte[5] = bytes[1];
        firstAudioFileByte[6] = bytes[2];
        firstAudioFileByte[7] = bytes[3];

        //read in the audio section header size in as big endian.
        int firstFileAudioSectionSizeBigEndian = (  ((firstAudioFileByte[43] << 24) & 0xFF000000)
                                                    |((firstAudioFileByte[42] << 16) & 0x00FF0000)
                                                    |((firstAudioFileByte[41] << 8) & 0x0000FF00)
                                                    | firstAudioFileByte[40] & 0xFF);
        //Change the raw audio header size for the first file to accommodate the second raw audio file
        firstFileAudioSectionSizeBigEndian += (secondAudioFileLength - HEADER_SIZE_BYTES);
        bytes = swapEndian(firstFileAudioSectionSizeBigEndian);
        firstAudioFileByte[40] = bytes[0];
        firstAudioFileByte[41] = bytes[1];
        firstAudioFileByte[42] = bytes[2];
        firstAudioFileByte[43] = bytes[3];

        int totalFileIndex = 0;
        for (byte s : firstAudioFileByte) {
            totalFile[totalFileIndex++] = s;
        }
        for (int i = HEADER_SIZE_BYTES; i < secondAudioFileLength - HEADER_SIZE_BYTES; i++) {
            totalFile[totalFileIndex++] = secondAudioFileByte[i];
        }
    }

    /**
     * Write the totalFile byte array to file.
     */
    private static void writeTotalFile() {
        try {
            FileOutputStream fos = new FileOutputStream(firstFile);
            fos.write(totalFile);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not open Wav file for writing!");
        } catch (IOException e) {
            Log.e(TAG, "Could not write to WAV file");
        }
    }

    /**
     * This function is used to swap the endianness of an integer. <br/>
     * Always start from the zero index and move up to the third index if you want to have the
     * swapped data to appear in correct order.
     *
     * @param i The integer that will have the endianness swapped.
     * @return The same integer i in different endianness. Start with the zero index and increment.
     */
    private static byte[] swapEndian(int i) {
        byte[] byteArray = (ByteBuffer.allocate(4).putInt(i).array());
        //now swap positions in array to swap endianness
        byte tempByte = byteArray[0];
        byteArray[0] = byteArray[3];
        byteArray[3] = tempByte;
        tempByte = byteArray[1];
        byteArray[1] = byteArray[2];
        byteArray[2] = tempByte;

        return byteArray;
    }
}
