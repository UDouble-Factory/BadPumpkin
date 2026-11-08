package io.mr_w98.badpumpkin;

import java.io.PrintStream;

final class ConsoleScreen {

    private static final int COLUMNS = 100;
    private static final int ROWS = 38;
    private static final String RAMP = " .:-=+*#%@";
    private static final char ESCAPE = 27;

    private final PrintStream out = System.out;
    private final StringBuilder builder = new StringBuilder((COLUMNS + 1) * ROWS + 8);

    void show() {
        out.print(ESCAPE + "[2J");
        out.flush();
    }

    void render(byte[] frame) {
        builder.setLength(0);
        builder.append(ESCAPE).append("[H");

        for (int row = 0; row < ROWS; row++) {
            int y0 = row * FrameReader.HEIGHT / ROWS;
            int y1 = (row + 1) * FrameReader.HEIGHT / ROWS;
            for (int column = 0; column < COLUMNS; column++) {
                int x0 = column * FrameReader.WIDTH / COLUMNS;
                int x1 = (column + 1) * FrameReader.WIDTH / COLUMNS;
                int white = FrameReader.countWhite(frame, x0, y0, x1, y1);
                int area = (x1 - x0) * (y1 - y0);
                builder.append(RAMP.charAt(white * (RAMP.length() - 1) / area));
            }
            builder.append('\n');
        }

        out.print(builder);
        out.flush();
    }

    void hide() {
        out.println();
        out.flush();
    }
}
