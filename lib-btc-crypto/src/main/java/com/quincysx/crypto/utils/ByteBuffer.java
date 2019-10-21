package com.quincysx.crypto.utils;

/**
 * @author QuincySx
 * @date 2018/8/30 下午3:48
 */
public class ByteBuffer {
    private byte[] buffer = new byte[]{};

    public void concat(byte[] b) {
        byte[] c = new byte[buffer.length + b.length];
        System.arraycopy(buffer, 0, c, 0, buffer.length);
        System.arraycopy(b, 0, c, buffer.length, b.length);
        buffer = c;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }
}
