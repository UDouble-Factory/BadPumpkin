package io.mr_w98.badpumpkin;

import java.io.IOException;

final class Playback {

    private final FrameReader reader;
    private final TextDisplayScreen screen;
    private final ConsoleScreen console;
    private final long startMillis;

    Playback(FrameReader reader, TextDisplayScreen screen, ConsoleScreen console) {
        this.reader = reader;
        this.screen = screen;
        this.console = console;
        this.startMillis = System.currentTimeMillis();

        screen.show();
        console.show();
    }

    boolean tick() throws IOException {
        int target = (int) ((System.currentTimeMillis() - startMillis) * FrameReader.FPS / 1000);
        if (target <= reader.index()) return true;
        if (!reader.advanceTo(target)) return false;

        byte[] frame = reader.frame();
        if (screen.hasViewers()) screen.render(frame);
        console.render(frame);
        return true;
    }

    void close() {
        screen.hide();
        console.hide();

        try {
            reader.close();
        } catch (IOException ignored) {
        }
    }
}
