package helper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteConversion {


    /**
     * Converts an array of bytes to an array of shorts (assuming
     * @param bArray The array of bytes to convert
     * @param isLittleEndian Flag to set the byte order (little endian or big endian).
     * @return
     */
    public static short[] byteArrayToShortArray(byte[] bArray, boolean isLittleEndian)
    {
        int bLen = bArray.length;
        int j = 0;
        short[] retShorts = new short[bLen / 2];

        ByteBuffer bBuffer = ByteBuffer.allocate(2);
        bBuffer.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        //Loop through two bytes at a time and put them in a byte buffer
        for (int i = 0; i < bLen - 1 && j < bLen / 2; i +=2) {
            bBuffer.put(bArray[i]);
            bBuffer.put(bArray[i + 1]);
            retShorts[j++] = bBuffer.getShort(0);
            bBuffer.clear();
        }

        return retShorts;
    }

    /**
     * Converts a short array to a byte array.
     * @param sArray
     * @return
     */

    public static byte[] shortArrayToByteArray(short[] sArray, boolean isLittleEndian)
    {
        int sLen = sArray.length;
        byte[] retBytes = new byte[sLen * 2];
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        int j = 0;
        for (int i = 0; i < sLen; ++i)
        {
            byteBuffer.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
            byte[] shortBytes = byteBuffer.putShort(sArray[i]).array();
            retBytes[j] = shortBytes[0];
            retBytes[j+1] = shortBytes[1];
            j+=2;
            byteBuffer.clear();
        }

        return retBytes;
    }

    public static boolean testByteArrayConversion(byte[] b)
    {
        short[] shortArr = byteArrayToShortArray(b, true);

        byte[] shortArrToB = shortArrayToByteArray(shortArr, true);

        //Test the length of the short array to byte.
        if (shortArrToB.length != b.length)
            return false;

        else {

            for (int i = 0; i < shortArrToB.length; ++i)
                if (shortArrToB[i] != b[i]) {
                   System.out.println(String.format("ShortArrB[%d] = %d, b[%d] = %d", i, shortArrToB[i], i, b[i]));
                   return false;
                }

            return true;
        }
    }
}
