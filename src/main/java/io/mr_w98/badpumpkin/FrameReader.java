package io.mr_w98.badpumpkin;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

final class FrameReader implements AutoCloseable {

    static final int WIDTH = 512;
    static final int HEIGHT = 384;
    static final int FPS = 20;
    static final int FRAME_BYTES = WIDTH * HEIGHT / 8;

    private final InputStream input;
    private final byte[] frame = new byte[FRAME_BYTES];
    private int index = -1;

    FrameReader(String path) throws IOException {
        input = new BufferedInputStream(new FileInputStream(path), FRAME_BYTES * 4);
    }

    boolean advanceTo(int target) throws IOException {
        while (index < target) {
            if (!readFrame()) return false;
            index++;
        }
        return true;
    }

    int index() {
        return index;
    }

    byte[] frame() {
        return frame;
    }

    static int countWhite(byte[] frame, int x0, int y0, int x1, int y1) {
        int count = 0;
        for (int y = y0; y < y1; y++) {
            int row = y * WIDTH;
            for (int x = x0; x < x1; x++) {
                int bit = row + x;
                count += (frame[bit >> 3] >> (7 - (bit & 7))) & 1;
            }
        }
        return count;
    }

    private boolean readFrame() throws IOException {
        int offset = 0;
        while (offset < FRAME_BYTES) {
            int read = input.read(frame, offset, FRAME_BYTES - offset);
            if (read < 0) return false;
            offset += read;
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}
