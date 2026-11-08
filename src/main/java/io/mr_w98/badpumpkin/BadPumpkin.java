package io.mr_w98.badpumpkin;

import plugin.PluginMetadata;
import plugin.PumpkinPlugin;
import pumpkin.command.Command;
import pumpkin.command.CommandError;
import pumpkin.command.CommandNode;
import pumpkin.command.CommandSender;
import pumpkin.command.ConsumedArgs;
import pumpkin.common.Position;
import pumpkin.context.Context;
import pumpkin.logging.Level;
import pumpkin.logging.Logging;
import pumpkin.permission.Permission;
import pumpkin.permission.PermissionDefault;
import pumpkin.permission.PermissionLevel;
import pumpkin.player.JavaMinecraftVersion;
import pumpkin.player.JavaPlayer;
import pumpkin.player.Player;
import pumpkin.runtime.Option;
import pumpkin.runtime.Result;
import pumpkin.scheduler.Scheduler;
import pumpkin.server.Server;
import pumpkin.text.TextComponent;
import pumpkin.uuid.Uuid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BadPumpkin extends PumpkinPlugin {

    private static final String PERMISSION = "BadPumpkin:command.badapple";
    private static final String VIDEOS_FOLDER = "videos";
    private static final String VIDEO_EXTENSION = ".bin";
    private static final JavaMinecraftVersion SUPPORTED_VERSION = JavaMinecraftVersion.V_26_3;

    private static final long COMMAND_PLAY = 1;
    private static final long COMMAND_STOP = 2;
    private static final long TASK_TICK = 1;

    private static final int ENTITY_ID = 0x7EADA991;
    private static final Uuid ENTITY_UUID = new Uuid(0x0BADA991E0000000L, 0x8000000000000001L);
    private static final double SCREEN_DISTANCE = 16;

    private File videosFolder;
    private Playback playback;
    private long tickTaskId = -1;

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "BadPumpkin",
                "0.0.1",
                List.of("mr-w98"),
                "Play Bad Apple on PumpkinMC",
                List.of(),
                List.of("fs.write.data")
        );
    }

    @Override
    public Result<Void, String> onLoad(Context context) {
        try {
            videosFolder = new File(context.getDataFolder(), VIDEOS_FOLDER);
            videosFolder.mkdirs();

            Result<Void, String> registered = context.registerPermission(new Permission(PERMISSION, "Allows playing Bad Apple", new PermissionDefault.Op(PermissionLevel.TWO), List.of()));
            if (registered.isFailure()) return registered;

            CommandNode play = CommandNode.literal("play");
            play.executeWithHandlerId(COMMAND_PLAY);
            CommandNode stop = CommandNode.literal("stop");
            stop.executeWithHandlerId(COMMAND_STOP);

            Command command = Command.create(List.of("badapple"), "Play Bad Apple");
            command.then(play);
            command.then(stop);
            context.registerCommand(command, PERMISSION);
            return Result.success(null);
        } finally {
            context.close();
        }
    }

    @Override
    public Result<Void, String> onUnload(Context context) {
        try {
            stopPlayback();
            return Result.success(null);
        } finally {
            context.close();
        }
    }

    @Override
    public Result<Integer, CommandError> handleCommand(long commandId, CommandSender sender, Server server, ConsumedArgs args) {
        try {
            if (commandId == COMMAND_PLAY) return play(sender, server);
            if (commandId == COMMAND_STOP) return stop();
            return Result.failure(new CommandError.InvalidRequirement());
        } finally {
            sender.close();
            server.close();
            args.close();
        }
    }

    @Override
    public void handleTask(long handlerId, Server server) {
        try {
            if (handlerId != TASK_TICK || playback == null) return;

            if (!playback.tick()) {
                stopPlayback();
                Logging.log(Level.INFO, "Bad Apple playback finished");
            }
        } catch (IOException | RuntimeException failure) {
            stopPlayback();
            Logging.log(Level.ERROR, "Bad Apple playback stopped: " + failure);
        } finally {
            server.close();
        }
    }

    private Result<Integer, CommandError> play(CommandSender sender, Server server) {
        Option<Player> senderPlayer = sender.asPlayer();
        if (!senderPlayer.isSome()) return failure("Run this command as a player.");

        File video = firstVideo();
        if (video == null) return failure("No " + VIDEO_EXTENSION + " file in " + videosFolder.getPath());

        Player anchor = senderPlayer.value();
        FrameReader reader;
        try {
            reader = new FrameReader(video.getPath());
        } catch (IOException failure) {
            return failure("Cannot open " + video.getPath());
        }

        stopPlayback();

        TextDisplayScreen screen = new TextDisplayScreen(ENTITY_ID, ENTITY_UUID, screenPosition(anchor), collectViewers(server));
        playback = new Playback(reader, screen, new ConsoleScreen());
        tickTaskId = Scheduler.scheduleRepeatingTask(TASK_TICK, 1, 1);
        return Result.success(1);
    }

    private Result<Integer, CommandError> stop() {
        if (playback == null) return failure("Nothing is playing.");

        stopPlayback();
        return Result.success(1);
    }

    private void stopPlayback() {
        if (tickTaskId >= 0) {
            Scheduler.cancelTask(tickTaskId);
            tickTaskId = -1;
        }

        if (playback != null) {
            playback.close();
            playback = null;
        }
    }

    private File firstVideo() {
        File[] files = videosFolder.listFiles();
        if (files == null) return null;

        File first = null;
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(VIDEO_EXTENSION)) continue;
            if (first == null || file.getName().compareTo(first.getName()) < 0) first = file;
        }
        return first;
    }

    private static List<JavaPlayer> collectViewers(Server server) {
        List<JavaPlayer> viewers = new ArrayList<>();
        for (Player player : server.getAllPlayers()) {
            Option<JavaPlayer> java = player.asJava();
            if (java.isSome() && java.value().getVersion() == SUPPORTED_VERSION) viewers.add(java.value());
        }
        return viewers;
    }

    private static Position screenPosition(Player anchor) {
        Position feet = anchor.getPosition();
        double yaw = Math.toRadians(anchor.getYaw());
        return new Position(feet.f0() - Math.sin(yaw) * SCREEN_DISTANCE, feet.f1(), feet.f2() + Math.cos(yaw) * SCREEN_DISTANCE);
    }

    private static Result<Integer, CommandError> failure(String message) {
        return Result.failure(new CommandError.CommandFailed(TextComponent.text(message)));
    }
}
