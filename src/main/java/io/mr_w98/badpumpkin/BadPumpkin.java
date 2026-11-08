package io.mr_w98.badpumpkin;

import plugin.PluginMetadata;
import plugin.PumpkinPlugin;
import pumpkin.context.Context;
import pumpkin.runtime.Result;

import java.util.List;

public class BadPumpkin extends PumpkinPlugin {

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "BadPumpkin",
                "0.0.1",
                List.of("mr-w98"),
                "Play Bad Apple on PumpkinMC",
                List.of(),
                List.of()
        );
    }

    @Override
    public Result<Void, String> onLoad(Context context) {
        return Result.success(null);
    }

    @Override
    public Result<Void, String> onUnload(Context context) {
        return Result.success(null);
    }
}
