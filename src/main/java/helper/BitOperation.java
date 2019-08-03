package helper;

public class BitOperation {
    /**
     * Shifts and joins the two bytes by the specified offset.
     * @param a The byte to shift and join with
     * @param b The byte to join a
     * @param offset The offset to join the two (usually the size of int b).
     * @return
     */
    public static byte joinRight(byte a, byte b, byte offset)
    {
        return (byte) ((a >> offset) | b);
    }


    /**
     * Shifts and joins the two integers by the specified offset.
     * NOTE* There is overflow since integers may be larger than a byte.
     * @param a The int to shift and join with
     * @param b The int to join with
     * @param offset The offset to join the two (usually the size of int b).
     * @return
     */
    public static byte joinRight(int a, int b, int offset)
    {
        return joinRight((byte) a, (byte) b, (byte) offset);
    }


    public static byte[] split(int a, int numBytes)
    {
        if (numBytes < 0)
            return null;

        byte[] bytes = new byte[numBytes];
        for (int i = 0; i < numBytes; ++i)
        {
            bytes[i] = (byte) (a >>> (i * 8));
        }
        return bytes;
    }

    public static byte[] copyBytesIntoArray(byte[] byteArr, int offset, int num)
    {
        byte[] intBytes = split(num, 4);
        for (int i = offset; i < offset + 4; ++i)
        {
            byteArr[i] = intBytes[i - offset];
        }
        return byteArr;
    }
}
