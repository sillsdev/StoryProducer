package org.sil.storyproducer.tools.media.wavaudio;

import java.nio.ByteBuffer;

/**
 * This class contains helper methods for manipulating .wav files.
 */

class WavHelper {
    /**
     * This function is used to swap the endianness of an integer. <br/>
     * Always start from the zero index and move up to the third index if you want to have the
     * swapped data to appear in correct order.
     *
     * @param i The integer that will have the endianness swapped.
     * @return The same integer i in different endianness. Start with the zero index and increment.
     */
    static byte[] swapEndian(int i) {
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
