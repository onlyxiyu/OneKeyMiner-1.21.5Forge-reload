package cn.onekeyminer.onekeyminer.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class CommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_CHAIN_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NON_CHAINABLE_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SEED_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DIAGONAL_CHAINING;
    public static final ForgeConfigSpec.IntValue MAX_MINING_DEPTH;
    public static final ForgeConfigSpec.BooleanValue TELEPORT_DROPS_TO_PLAYER;
    public static final ForgeConfigSpec.BooleanValue IGNORE_TOOL_COMPATIBILITY;
    public static final ForgeConfigSpec.BooleanValue MATCH_BLOCK_STATE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IN_CREATIVE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_SNEAKING;
    public static final ForgeConfigSpec.IntValue MAX_BLOCKS_IN_CHAIN;
    public static final ForgeConfigSpec.IntValue MAX_BLOCKS_IN_CHAIN_CREATIVE;
    public static final ForgeConfigSpec.IntValue MAX_CHAIN_DEPTH;
    public static final ForgeConfigSpec.BooleanValue matchseedBlockState;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("通用配置||Common Config").push("general");

        MAX_CHAIN_BLOCKS = builder
                .comment("连锁操作的最大方块数||Maximum number of blocks for chain operations")
                .defineInRange("maxChainBlocks", 64, 1, 4096);

        NON_CHAINABLE_BLOCKS = builder
                .comment("不允许连锁挖掘的方块黑名单||Blocks that should not be chain mined")
                .defineList("nonChainableBlocks",
                        () -> {
                            List<String> defaults = new ArrayList<>();
                            defaults.add("minecraft:stone");
                            defaults.add("minecraft:bedrock");
                            return defaults;
                        },
                        obj -> obj instanceof String);

        SEED_BLACKLIST = builder
                .comment("不允许连锁种植的种子黑名单||Seeds that should not be chain planted")
                .defineList("seedBlacklist", ArrayList::new, obj -> obj instanceof String);

        ENABLE_DIAGONAL_CHAINING = builder
                .comment("是否允许对角线连锁||Whether to allow diagonal chaining")
                .define("enableDiagonalChaining", false);

        MAX_MINING_DEPTH = builder
                .comment("最大挖掘深度||Maximum mining depth")
                .defineInRange("maxMiningDepth", 16, 1, 64);

        TELEPORT_DROPS_TO_PLAYER = builder
                .comment("是否将连锁挖掘的掉落物传送到玩家身下||Whether to teleport the drops from chain mining to the player")
                .define("teleportDropsToPlayer", false);

        IGNORE_TOOL_COMPATIBILITY = builder
                .comment("是否忽略工具兼容性检查||Whether to ignore tool compatibility checks")
                .define("ignoreToolCompatibility", true);

        MATCH_BLOCK_STATE = builder
                .comment("是否匹配完整的方块状态||Whether to match the complete block state")
                .define("matchBlockState", false);

        matchseedBlockState =builder
                .comment("是否匹配完整的种子方块状态而不仅仅是种子方块类型 || Whether to match the complete state of the seed block rather than just the type of the seed block.")
                .define("matchseedBlockState", false);

        ENABLE_IN_CREATIVE = builder
                .comment("创造模式是否启用连锁挖掘||Whether to enable chain mining in creative mode")
                .define("enableInCreative", true);

        REQUIRE_SNEAKING = builder
                .comment("是否需要下蹲才能触发连锁挖掘||Whether to require sneaking to trigger chain mining")
                .define("requireSneaking", false);

        MAX_BLOCKS_IN_CHAIN = builder
                .comment("存活模式下连锁挖掘的最大方块数||Maximum number of blocks for chain mining in survival mode")
                .defineInRange("maxBlocksInChain", 64, 1, 4096);

        MAX_BLOCKS_IN_CHAIN_CREATIVE = builder
                .comment("创造模式下连锁挖掘的最大方块数||Maximum number of blocks for chain mining in creative mode")
                .defineInRange("maxBlocksInChainCreative", 256, 1, 16384);

        MAX_CHAIN_DEPTH = builder
                .comment("最大连锁搜索深度||Maximum chain search depth")
                .defineInRange("maxChainDepth", 16, 1, 64);

        builder.pop();
        SPEC = builder.build();
    }
    public static boolean isBlockMineable(String blockId) {
        // 将本地化键转换为资源ID格式
        // 例如: "block.minecraft.stone" -> "minecraft:stone"
        String resourceId = convertToResourceId(blockId);

        // 检查转换后的资源ID是否在黑名单中
        for (String blacklistedBlock : NON_CHAINABLE_BLOCKS.get()) {
            if (resourceId.equals(blacklistedBlock)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将本地化键转换为资源ID格式
     * @param descriptionId 描述ID (例如 "block.minecraft.stone")
     * @return 资源ID (例如 "minecraft:stone")
     */
    private static String convertToResourceId(String descriptionId) {
        if (descriptionId == null) return "";

        // 处理本地化键格式 "block.minecraft.stone"
        if (descriptionId.startsWith("block.")) {
            String[] parts = descriptionId.split("\\.");
            if (parts.length >= 3) {
                return parts[1] + ":" + parts[2];
            }
        }

        // 处理已经是资源ID格式的情况 "minecraft:stone"
        if (descriptionId.contains(":")) {
            return descriptionId;
        }

        // 处理没有命名空间的情况，添加默认"minecraft:"前缀
        if (!descriptionId.contains(".") && !descriptionId.contains(":")) {
            return "minecraft:" + descriptionId;
        }

        // 如果无法解析，返回原始ID
        return descriptionId;
    }

    /**
     * 检查种子是否在黑名单中
     */
    public static boolean isSeedBlacklisted(String seedId) {
        return SEED_BLACKLIST.get().contains(seedId);

}}
