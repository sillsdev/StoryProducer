package org.sil.storyproducer.tools.media.wavaudio;

import android.media.AudioFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Class created to append a header to an audio file.
 */
public class WavHeader {
    private static final int HEADER_SIZE_BYTES = 44;
    private static byte[] audioData;
    private static byte[] fileHeader;
    private static byte[] fmtHeader;
    private static byte[] audioDataHeader;
    private static AudioAttributes audioAttributes;

    public static byte[] createWavFile(short[] audData, AudioAttributes audioAttrib) {
        //convert using ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(audData.length * 2);
        for (short s : audData) {
            byteBuffer.putShort(s);
        }

        return createWavFile(byteBuffer.array(), audioAttrib);
    }

    public static byte[] createWavFile(byte[] audioDat, AudioAttributes audioAttrib) {
        audioData = audioDat;
        audioAttributes = audioAttrib;
        makeHeaders();
        byte[] totalFile = new byte[HEADER_SIZE_BYTES + audioData.length];
        byte[][] headerFiles = new byte[][]{fileHeader, fmtHeader, audioDataHeader, convertToLittleAudData(audioData)};
        int i = 0;
        for (byte[] bArr : headerFiles) {
            for (byte bite : bArr) {
                totalFile[i++] = bite;
            }
        }

        return totalFile;
    }

    /**
     * Specifying the various attributes of the raw data and how the raw data was captured.
     */
    public static class AudioAttributes {
        private int outputFormat;
        private int channelConfig;
        private int sampleRate;

        public AudioAttributes(int outputFormat, int channelConfig, int sampleRate) {
            this.outputFormat = outputFormat;
            this.channelConfig = channelConfig;
            this.sampleRate = sampleRate;
        }
    }

    /**
     * Method for creating the respective headers of the WAV file.
     */
    private static void makeHeaders() {
        createFileHeader();
        createFmtHeader();
        createAudDataHeader();
    }

    /**
     * Responsible for creating the actual WAV file header.
     */
    private static void createFileHeader() {
        fileHeader = new byte[12];
        fileHeader[0] = 'R';
        fileHeader[1] = 'I';
        fileHeader[2] = 'F';
        fileHeader[3] = 'F';

        //file size
        byte[] fileSize = swapEndian((HEADER_SIZE_BYTES - 8) + audioData.length);
        fileHeader[4] = fileSize[0];
        fileHeader[5] = fileSize[1];
        fileHeader[6] = fileSize[2];
        fileHeader[7] = fileSize[3];

        fileHeader[8] = 'W';
        fileHeader[9] = 'A';
        fileHeader[10] = 'V';
        fileHeader[11] = 'E';
    }

    /**
     * Responsible for creating the format header portion of the WAV file.
     */
    private static void createFmtHeader() {
        fmtHeader = new byte[24];
        fmtHeader[0] = 'f';
        fmtHeader[1] = 'm';
        fmtHeader[2] = 't';
        fmtHeader[3] = ' ';

        byte[] fmtSize = swapEndian(16);
        fmtHeader[4] = fmtSize[0];
        fmtHeader[5] = fmtSize[1];
        fmtHeader[6] = fmtSize[2];
        fmtHeader[7] = fmtSize[3];

        //assume PCM for file format
        fmtHeader[8] = 0x01;
        fmtHeader[9] = 0x00;

        //Mono or Stereo channel in
        fmtHeader[10] = (audioAttributes.channelConfig == AudioFormat.CHANNEL_IN_MONO) ? (byte) 0x01 : (byte) 0x02;
        fmtHeader[11] = 0x00;

        //Sample rate
        byte[] sampleRt = swapEndian(audioAttributes.sampleRate);
        fmtHeader[12] = sampleRt[0];
        fmtHeader[13] = sampleRt[1];
        fmtHeader[14] = sampleRt[2];
        fmtHeader[15] = sampleRt[3];

        //byte rate
        int numChannels = (audioAttributes.channelConfig == AudioFormat.CHANNEL_IN_MONO) ? 1 : 2;
        int bytesPerSample = (audioAttributes.outputFormat == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8) / 8;
        int byteRat = audioAttributes.sampleRate * numChannels * bytesPerSample;

        byte[] byteRate = swapEndian(byteRat);
        fmtHeader[16] = byteRate[0];
        fmtHeader[17] = byteRate[1];
        fmtHeader[18] = byteRate[2];
        fmtHeader[19] = byteRate[3];

        //block align
        byte[] blockAlign = swapEndian(numChannels * bytesPerSample);
        fmtHeader[20] = blockAlign[0];
        fmtHeader[21] = blockAlign[1];

        //bits per sample
        byte[] bitsPSample = swapEndian(bytesPerSample * 8);
        fmtHeader[22] = bitsPSample[0];
        fmtHeader[23] = bitsPSample[1];
    }

    /**
     * Responsible for creating the raw audio data header.
     */
    private static void createAudDataHeader() {
        audioDataHeader = new byte[8];
        audioDataHeader[0] = 'd';
        audioDataHeader[1] = 'a';
        audioDataHeader[2] = 't';
        audioDataHeader[3] = 'a';

        //size of the raw audio data
        byte[] dataSize = swapEndian(audioData.length);
        audioDataHeader[4] = dataSize[0];
        audioDataHeader[5] = dataSize[1];
        audioDataHeader[6] = dataSize[2];
        audioDataHeader[7] = dataSize[3];
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

    /**
     * This function is primarily used to swap the endianness of the raw audio data. <br/>
     * For some reason the AudioRecord {@link android.media.AudioRecord} class saves the raw audio
     * in Big-Endianness. But, WAVE files need to have the data section saved in little endianness.
     *
     * @param byteArray The array that will have the endianness changed.
     * @return The little endian version of the byteArray.
     */
    private static byte[] convertToLittleAudData(byte[] byteArray) {
        if (audioAttributes.outputFormat == AudioFormat.ENCODING_PCM_16BIT) {
            short[] shorts = new short[byteArray.length / 2];
            ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            return bytes.array();
        } else if (audioAttributes.outputFormat == AudioFormat.ENCODING_PCM_8BIT) {
            //TODO implement your own conversion. Be observant of bit sample size.
            return null;
        }
        return null;
    }

}
