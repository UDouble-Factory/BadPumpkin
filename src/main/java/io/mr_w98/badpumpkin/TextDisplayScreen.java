package io.mr_w98.badpumpkin;

import pumpkin.common.Position;
import pumpkin.javapackets.CRemoveEntities;
import pumpkin.javapackets.CSetEntityMetadata;
import pumpkin.javapackets.CSpawnEntity;
import pumpkin.javapackets.ClientboundPacket;
import pumpkin.player.JavaPlayer;
import pumpkin.runtime.Tuple3;
import pumpkin.uuid.Uuid;

import java.util.Iterator;
import java.util.List;

final class TextDisplayScreen {

    static final int COLUMNS = 128;
    static final int ROWS = 96;

    private static final int CELL_WIDTH = FrameReader.WIDTH / COLUMNS;
    private static final int CELL_HEIGHT = FrameReader.HEIGHT / ROWS;
    private static final int WHITE_THRESHOLD = CELL_WIDTH * CELL_HEIGHT / 2;

    private static final int ENTITY_TYPE_TEXT_DISPLAY = 135;

    private static final int INDEX_SCALE = 12;
    private static final int INDEX_BILLBOARD = 15;
    private static final int INDEX_BRIGHTNESS = 16;
    private static final int INDEX_TEXT = 23;
    private static final int INDEX_LINE_WIDTH = 24;
    private static final int INDEX_BACKGROUND = 25;

    private static final int TYPE_BYTE = 0;
    private static final int TYPE_INT = 1;
    private static final int TYPE_COMPONENT = 5;
    private static final int TYPE_VECTOR3 = 39;

    private static final int BILLBOARD_VERTICAL = 1;
    private static final int FULL_BRIGHT = (15 << 4) | (15 << 20);
    private static final int LINE_WIDTH = 4096;
    private static final int BACKGROUND_BLACK = 0xFF000000;

    private static final float FONT_PIXEL = 1f / 40f;
    private static final float GLYPH_ADVANCE = 5f;
    private static final float LINE_HEIGHT = 9f;
    private static final float SCREEN_HEIGHT = 12f;
    private static final float SCREEN_WIDTH = SCREEN_HEIGHT * FrameReader.WIDTH / FrameReader.HEIGHT;
    private static final float SCALE_X = SCREEN_WIDTH / (COLUMNS * GLYPH_ADVANCE * FONT_PIXEL);
    private static final float SCALE_Y = SCREEN_HEIGHT / (ROWS * LINE_HEIGHT * FONT_PIXEL);

    private static final int TAG_END = 0;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;

    private static final String GLYPHS = "█".repeat(COLUMNS);

    private final int entityId;
    private final Uuid uuid;
    private final Position position;
    private final List<JavaPlayer> viewers;
    private final ByteWriter writer = new ByteWriter();
    private final boolean[] cells = new boolean[COLUMNS];

    TextDisplayScreen(int entityId, Uuid uuid, Position position, List<JavaPlayer> viewers) {
        this.entityId = entityId;
        this.uuid = uuid;
        this.position = position;
        this.viewers = viewers;
    }

    void show() {
        CSpawnEntity spawn = new CSpawnEntity(entityId, uuid, ENTITY_TYPE_TEXT_DISPLAY, new Tuple3<>(position.f0(), position.f1(), position.f2()), "[0.0,0.0,0.0]", 0, 0, 0, 0);
        send(new ClientboundPacket.CSpawnEntity(spawn));

        writer.reset();
        writeVector3Entry(INDEX_SCALE, SCALE_X, SCALE_Y, 1f);
        writeByteEntry(INDEX_BILLBOARD, BILLBOARD_VERTICAL);
        writeIntEntry(INDEX_BRIGHTNESS, FULL_BRIGHT);
        writeIntEntry(INDEX_LINE_WIDTH, LINE_WIDTH);
        writeIntEntry(INDEX_BACKGROUND, BACKGROUND_BLACK);
        writer.writeByte(0xFF);
        send(new ClientboundPacket.CSetEntityMetadata(new CSetEntityMetadata(entityId, writer.toByteArray())));
    }

    void render(byte[] frame) {
        writer.reset();
        writer.writeByte(INDEX_TEXT);
        writer.writeVarInt(TYPE_COMPONENT);
        writeFrameComponent(frame);
        writer.writeByte(0xFF);
        send(new ClientboundPacket.CSetEntityMetadata(new CSetEntityMetadata(entityId, writer.toByteArray())));
    }

    void hide() {
        send(new ClientboundPacket.CRemoveEntities(new CRemoveEntities(List.of(entityId))));
    }

    boolean hasViewers() {
        return !viewers.isEmpty();
    }

    private void writeFrameComponent(byte[] frame) {
        writer.writeByte(TAG_COMPOUND);
        writeNbtString("text", "");
        writeNbtString("color", "black");

        writer.writeByte(TAG_LIST);
        writer.writeModifiedUtf8("extra");
        writer.writeByte(TAG_COMPOUND);
        int countPosition = writer.reserveInt();

        int runs = 0;
        for (int row = 0; row < ROWS; row++) {
            int y = row * CELL_HEIGHT;
            for (int column = 0; column < COLUMNS; column++) {
                int x = column * CELL_WIDTH;
                cells[column] = FrameReader.countWhite(frame, x, y, x + CELL_WIDTH, y + CELL_HEIGHT) > WHITE_THRESHOLD;
            }

            int start = 0;
            while (start < COLUMNS) {
                boolean white = cells[start];
                int end = start + 1;
                while (end < COLUMNS && cells[end] == white) end++;

                String text = GLYPHS.substring(0, end - start);
                if (end == COLUMNS && row < ROWS - 1) text += "\n";

                writeNbtString("text", text);
                if (white) writeNbtString("color", "white");
                writer.writeByte(TAG_END);

                runs++;
                start = end;
            }
        }

        writer.patchInt(countPosition, runs);
        writer.writeByte(TAG_END);
    }

    private void writeNbtString(String name, String value) {
        writer.writeByte(TAG_STRING);
        writer.writeModifiedUtf8(name);
        writer.writeModifiedUtf8(value);
    }

    private void writeByteEntry(int index, int value) {
        writer.writeByte(index);
        writer.writeVarInt(TYPE_BYTE);
        writer.writeByte(value);
    }

    private void writeIntEntry(int index, int value) {
        writer.writeByte(index);
        writer.writeVarInt(TYPE_INT);
        writer.writeVarInt(value);
    }

    private void writeVector3Entry(int index, float x, float y, float z) {
        writer.writeByte(index);
        writer.writeVarInt(TYPE_VECTOR3);
        writer.writeFloat(x);
        writer.writeFloat(y);
        writer.writeFloat(z);
    }

    private void send(ClientboundPacket packet) {
        Iterator<JavaPlayer> iterator = viewers.iterator();
        while (iterator.hasNext()) {
            JavaPlayer viewer = iterator.next();
            try {
                viewer.sendPacket(packet);
            } catch (RuntimeException failure) {
                iterator.remove();
            }
        }
    }
}
