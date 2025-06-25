package cn.onekeyminer.onekeyminer.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue TOOL_PROTECTION_ENABLED;
    public static final ForgeConfigSpec.DoubleValue TOOL_DURABILITY_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue HUNGER_PROTECTION_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HUNGER_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue EXPERIENCE_MULTIPLIER_ENABLED;
    public static final ForgeConfigSpec.DoubleValue EXPERIENCE_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("服务器配置||Server Config").push("server");

        TOOL_PROTECTION_ENABLED = builder
                .comment("启用工具保护模式||Enable tool protection mode")
                .define("toolProtectionEnabled", true);

        TOOL_DURABILITY_THRESHOLD = builder
                .comment("工具耐久阈值||Tool durability threshold")
                .defineInRange("toolDurabilityThreshold", 0.1, 0.01, 1000.0);

        HUNGER_PROTECTION_ENABLED = builder
                .comment("启用饥饿度保护||Enable hunger protection")
                .define("hungerProtectionEnabled", true);

        HUNGER_THRESHOLD = builder
                .comment("饥饿度阈值||Hunger threshold")
                .defineInRange("hungerThreshold", 3.0, 0.0, 20.0);

        EXPERIENCE_MULTIPLIER_ENABLED = builder
                .comment("启用经验修正||Enable experience multiplier")
                .define("experienceMultiplierEnabled", true);

        EXPERIENCE_MULTIPLIER = builder
                .comment("经验倍率调整||Experience multiplier adjustment")
                .defineInRange("experienceMultiplier", 0.8, 0.1, 2.0);

        builder.pop();
        SPEC = builder.build();
    }
}
