package cn.onekeyminer.onekeyminer.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_KEY_HOLD;
    public static final ForgeConfigSpec.BooleanValue SHOW_BLOCK_COUNT;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.ConfigValue<String> MESSAGE_STYLE;
    public static final ForgeConfigSpec.IntValue FROZEN_TIMER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("一键连锁 - 客户端配置||OneKeyMiner Client Config").push("client");

        REQUIRE_KEY_HOLD = builder
                .comment("是否需要按住键来激活连锁模式||Whether to hold the key to activate the chain mode")
                .translation("config.onekeyminer.requireKeyHold")
                .define("requireKeyHold", false);

        DEBUG = builder
                .comment("是否启用调试模式||Whether to enable debug mode")
                .translation("config.onekeyminer.Debug")
                .define("Debug", false);

        SHOW_BLOCK_COUNT = builder
                .comment("是否显示连锁方块数量||Whether to show the number of chain blocks")
                .translation("config.onekeyminer.showBlockCount")
                .define("showBlockCount", true);

        MESSAGE_STYLE = builder
                .comment("消息显示样式 (actionbar 或 chat)||Message display style (actionbar or chat)")
                .translation("config.onekeyminer.messageStyle")
                .define("messageStyle", "actionbar");

        FROZEN_TIMER = builder
                .comment("按住模式关闭时，连锁状态重置时间（秒）||Chain state reset time when hold mode is off (seconds)")
                .defineInRange("frozenTimer", 60, 1, 32767);

        builder.pop();
        SPEC = builder.build();
    }
}
