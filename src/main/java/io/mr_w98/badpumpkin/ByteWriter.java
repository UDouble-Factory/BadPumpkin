package io.mr_w98.badpumpkin;

import java.util.Arrays;

final class ByteWriter {

    private byte[] buffer = new byte[1024];
    private int size;

    void writeByte(int value) {
        ensure(1);
        buffer[size++] = (byte) value;
    }

    void writeShort(int value) {
        writeByte(value >>> 8);
        writeByte(value);
    }

    void writeInt(int value) {
        writeByte(value >>> 24);
        writeByte(value >>> 16);
        writeByte(value >>> 8);
        writeByte(value);
    }

    void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    void writeVarInt(int value) {
        while ((value & ~0x7F) != 0) {
            writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        writeByte(value);
    }

    void writeModifiedUtf8(String value) {
        int lengthPosition = size;
        writeShort(0);

        int start = size;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != 0 && c < 0x80) {
                writeByte(c);
            } else if (c < 0x800) {
                writeByte(0xC0 | (c >> 6));
                writeByte(0x80 | (c & 0x3F));
            } else {
                writeByte(0xE0 | (c >> 12));
                writeByte(0x80 | ((c >> 6) & 0x3F));
                writeByte(0x80 | (c & 0x3F));
            }
        }

        int length = size - start;
        if (length > 0xFFFF) throw new IllegalArgumentException("NBT string too long");
        buffer[lengthPosition] = (byte) (length >>> 8);
        buffer[lengthPosition + 1] = (byte) length;
    }

    int reserveInt() {
        int position = size;
        writeInt(0);
        return position;
    }

    void patchInt(int position, int value) {
        buffer[position] = (byte) (value >>> 24);
        buffer[position + 1] = (byte) (value >>> 16);
        buffer[position + 2] = (byte) (value >>> 8);
        buffer[position + 3] = (byte) value;
    }

    void reset() {
        size = 0;
    }

    byte[] toByteArray() {
        return Arrays.copyOf(buffer, size);
    }

    private void ensure(int extra) {
        if (size + extra > buffer.length) buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, size + extra));
    }
}
