package org.palliums.libracore.utils;

/**
 * @author QuincySx
 * @date 2019/5/1 下午5:17
 */
public class ByteUtility {

    public static int next11Bits(byte[] bytes, int offset) {
        final int skip = offset / 8;
        final int lowerBitsToRemove = (3 * 8 - 11) - (offset % 8);
        return (((int) bytes[skip] & 0xff) << 16 |
                ((int) bytes[skip + 1] & 0xff) << 8 |
                (lowerBitsToRemove < 8
                        ? ((int) bytes[skip + 2] & 0xff)
                        : 0)) >> lowerBitsToRemove & (1 << 11) - 1;
    }

    public static void writeNext11(byte[] bytes, int value, int offset) {
        int skip = offset / 8;
        int bitSkip = offset % 8;
        {//byte 0
            byte firstValue = bytes[skip];
            byte toWrite = (byte) (value >> (3 + bitSkip));
            bytes[skip] = (byte) (firstValue | toWrite);
        }

        {//byte 1
            byte valueInByte = bytes[skip + 1];
            final int i = 5 - bitSkip;
            byte toWrite = (byte) (i > 0 ? (value << i) : (value >> -i));
            bytes[skip + 1] = (byte) (valueInByte | toWrite);
        }

        if (bitSkip >= 6) {//byte 2
            byte valueInByte = bytes[skip + 2];
            byte toWrite = (byte) (value << 13 - bitSkip);
            bytes[skip + 2] = (byte) (valueInByte | toWrite);
        }
    }

    public static byte[] combine(final byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        final byte[] bytes = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, bytes, offset, array.length);
            offset += array.length;
        }
        return bytes;
    }
}
