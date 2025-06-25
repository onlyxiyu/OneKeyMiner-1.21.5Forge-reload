package cn.onekeyminer.onekeyminer.chain;

import cn.onekeyminer.onekeyminer.Onekeyminer;
import cn.onekeyminer.onekeyminer.capability.ChainModeCapability;
import cn.onekeyminer.onekeyminer.config.CommonConfig;
import cn.onekeyminer.onekeyminer.config.ServerConfig;
import cn.onekeyminer.onekeyminer.network.ChainActionPacket;
import cn.onekeyminer.onekeyminer.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import net.minecraft.network.chat.Component;

import java.util.*;

import static net.minecraft.world.item.enchantment.Enchantments.FORTUNE;
import static net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH;

/**
 * 连锁挖掘处理器
 */
public class ChainHandler {
    // 防止重复挖掘的位置集合
    private static final Set<BlockPos> CURRENTLY_MINING = new HashSet<>();

    /**
     * 连锁挖掘入口点 - 外部调用接口
     */
    public static boolean tryChainMine(ServerPlayer player, BlockPos pos, BlockState state, ItemStack tool) {
        // 清理集合，避免上次操作残留
        CURRENTLY_MINING.clear();

        // 参数验证
        if ((player == null || pos == null || state == null || tool == null)) {
            Onekeyminer.LOGGER.debug("连锁挖掘参数无效");
            return false;
        }

        // 验证连锁挖掘条件
        if (!validateChainMiningConditions(player, pos, state, tool)) {
            return false;
        }

        // 所有条件验证通过，执行连锁挖掘
        String blockId = state.getBlock().getDescriptionId();
        String blockName = state.getBlock().getName().getString();

        // 执行连锁挖掘逻辑
        int blocksMined = performChainMining(player, pos, state, tool);

        // 发送反馈
        if (blocksMined > 0) {
            sendFeedback(player, blocksMined);
        }

        return blocksMined > 0;
    }

    /**
     * 验证是否满足连锁挖掘的条件
     */
    private static boolean validateChainMiningConditions(ServerPlayer player, BlockPos pos, BlockState state, ItemStack tool) {
        // 检查连锁模式是否激活
        if (!ChainModeCapability.isChainModeActive(player)) {
            return false;
        }

        // 检查方块是否为空气
        if (state.isAir()) {
            return false;
        }

        // 检查蹲下需求
        if (CommonConfig.REQUIRE_SNEAKING.get() && !player.isShiftKeyDown()) {
                Onekeyminer.LOGGER.debug("需要下蹲但玩家未下蹲");

            return false;
        }

        // 检查创造模式设置
        if (player.isCreative() && !CommonConfig.ENABLE_IN_CREATIVE.get()) {
            return false;
        }

        // 检查饥饿度
        if (!player.isCreative() &&
                player.getFoodData().getFoodLevel() < ServerConfig.HUNGER_THRESHOLD.get()) {

            return false;
        }

        // 检查方块是否可挖掘
        String blockId = state.getBlock().getDescriptionId();
        String blockName = state.getBlock().getName().getString();
        if (!CommonConfig.isBlockMineable(blockId)) {
            return false;
        }

        // 工具兼容性检查
        if (!CommonConfig.IGNORE_TOOL_COMPATIBILITY.get() &&
                !player.hasCorrectToolForDrops(state)) {

            return false;
        }

        // 工具耐久度保护检查
        if (!player.isCreative() && tool.isDamageableItem()) {
            Double durabilityProtection = ServerConfig.TOOL_DURABILITY_THRESHOLD.get();
            if (tool.getDamageValue() >= tool.getMaxDamage() - durabilityProtection) {
                return false;
            }
        }

        return true;
    }

    /**
     * 执行连锁挖掘的核心逻辑
     * @return 挖掘的方块数量
     */
    private static int performChainMining(ServerPlayer player, BlockPos startPos, BlockState startState, ItemStack tool) {
        Level level = player.level();
        if (!(level instanceof ServerLevel)) {
            return 0;
        }
        ServerLevel serverLevel = (ServerLevel) level;

        // 获取配置限制
        int maxBlocks = player.isCreative()
                ? CommonConfig.MAX_BLOCKS_IN_CHAIN_CREATIVE.get()
                : CommonConfig.MAX_BLOCKS_IN_CHAIN.get();
        int maxDepth = CommonConfig.MAX_CHAIN_DEPTH.get();
        boolean diagonal = CommonConfig.ENABLE_DIAGONAL_CHAINING.get();
        boolean matchFullState = CommonConfig.MATCH_BLOCK_STATE.get();
        boolean matchseedState = CommonConfig.matchseedBlockState.get();
        boolean teleportDrops = CommonConfig.TELEPORT_DROPS_TO_PLAYER.get();

        // 跟踪挖掘状态
        Queue<BlockPos> toMine = new LinkedList<>();
        Set<BlockPos> minedPositions = new HashSet<>();
        toMine.add(startPos);

        int blocksMined = 0;

        while (!toMine.isEmpty() && blocksMined < maxBlocks) {
            BlockPos currentPos = toMine.poll();

            // 跳过已处理的位置
            if (minedPositions.contains(currentPos)) {
                continue;
            }

            // 深度限制检查
            if (Math.abs(currentPos.getY() - startPos.getY()) > maxDepth) {
                continue;
            }

            // 检查方块状态匹配
            BlockState currentState = level.getBlockState(currentPos);
            if (!isBlockMatching(startState, currentState, matchFullState,matchseedState)) {
                continue;
            }

            // 标记为已处理
            minedPositions.add(currentPos);

            // 根据配置选择破坏方块的方法
            boolean blockDestroyed;
            if (teleportDrops) {
                blockDestroyed = breakBlockWithTeleport(serverLevel, currentPos, currentState, player, tool);
            } else {
                blockDestroyed = player.level().destroyBlock(currentPos, true, player);

                // 消耗工具耐久
                if (blockDestroyed && !player.isCreative() && tool.isDamageableItem()) {
                    tool.hurtAndBreak(1, player, player.getMainHandItem().getEquipmentSlot());

                    // 检查工具是否已损坏
                    if (tool.isEmpty()) {
                        break;
                    }
                }
            }

            // 如果成功破坏方块，添加计数并继续
            if (blockDestroyed) {
                blocksMined++;

                // 将相邻方块添加到待检查队列
                for (BlockPos neighbor : getNeighborPositions(currentPos, diagonal)) {
                    if (!minedPositions.contains(neighbor)) {
                        toMine.add(neighbor);
                    }
                }
            }
        }

        return blocksMined;
    }

    /**
     * 检查方块状态是否匹配
     */
    private static boolean isBlockMatching(BlockState reference, BlockState target, boolean matchFullState,boolean matchSeedState) {
        if (target.isAir()) {
            return false;
        }

        // 完全匹配方块状态

        if ((matchFullState && !(target.getBlock() instanceof CropBlock)||(matchSeedState && target.getBlock() instanceof CropBlock))) {
            return reference.equals(target);
        }

        // 仅匹配方块类型，忽略方块状态（如朝向、阶段等）
        if (reference.getBlock() == target.getBlock()) {
            // 特殊处理：确保某些特殊方块的特定属性匹配
            // 例如，不同类型的木头应该分开连锁
            if (hasSpecialMatchingRules(reference.getBlock())) {
                return matchSpecialBlockProperties(reference, target);
            }
            return true;
        }

        return false;
    }

    /**
     * 检查方块是否有特殊匹配规则
     */
    private static boolean hasSpecialMatchingRules(Block block) {
        // 这里可以添加需要特殊处理的方块类型
        String blockId = block.getDescriptionId();

        // 例如，特殊处理矿石、木头等
        return blockId.contains("ore") ||
                blockId.contains("log") ||
                blockId.contains("wood") ||
                blockId.contains("leaves");
    }

    /**
     * 匹配特殊方块的属性
     */
    private static boolean matchSpecialBlockProperties(BlockState reference, BlockState target) {
        // 对于矿石，应该允许不同朝向的相同矿石连锁
        if (reference.getBlock().getDescriptionId().contains("ore")) {
            return true;
        }

        // 对于木头，应该只连锁相同木材类型的木头
        if (reference.getBlock().getDescriptionId().contains("log") ||
                reference.getBlock().getDescriptionId().contains("wood")) {
            // 提取木材类型（如oak, spruce等）
            String refType = extractWoodType(reference.getBlock().getDescriptionId());
            String targetType = extractWoodType(target.getBlock().getDescriptionId());
            return refType.equals(targetType);
        }

        // 对于树叶，应该只连锁相同类型的树叶
        if (reference.getBlock().getDescriptionId().contains("leaves")) {
            String refType = extractWoodType(reference.getBlock().getDescriptionId());
            String targetType = extractWoodType(target.getBlock().getDescriptionId());
            return refType.equals(targetType);
        }

        // 默认情况下允许匹配
        return true;
    }

    /**
     * 从方块ID中提取木材类型
     */
    private static String extractWoodType(String blockId) {
        // 从类似 "block.minecraft.oak_log" 的ID中提取 "oak"
        for (String woodType : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "cherry"}) {
            if (blockId.contains(woodType)) {
                return woodType;
            }
        }
        return blockId; // 如果找不到匹配的类型，返回原始ID
    }

    /**
     * 破坏方块并将掉落物传送到玩家位置
     */
    private static boolean breakBlockWithTeleport(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, ItemStack tool) {
        if (level == null || player == null) return false;

        // 获取玩家位置作为掉落点
        BlockPos dropPos = player.blockPosition();

        try {
            // 先获取方块掉落物和经验值
            BlockEntity blockEntity = level.getBlockEntity(pos);

            // 创建战利品上下文
            LootParams.Builder builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, tool)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);

            // 获取方块的掉落物
            List<ItemStack> drops = state.getDrops(builder);
            boolean hasSilkTouch = tool.getEnchantments().keySet().stream()
                    .anyMatch(holder -> holder.is(SILK_TOUCH));
            // 获取方块的经验值（如果有）
            int xp = 0;
            if (state.getBlock() instanceof DropExperienceBlock expBlock) {
                if(hasSilkTouch) xp = expBlock.getExpDrop(state, level, level.random, pos, 0, 1);
                else xp = expBlock.getExpDrop(state, level, level.random, pos, 0, 0);
            }

            // 使用destroyBlock触发正常的方块破坏事件（会自动处理掉落物和经验，但我们会覆盖掉落逻辑）
            boolean broken = player.level().destroyBlock(pos, false, player);
            if (!broken) return false;

            // 在玩家位置生成掉落物（覆盖原有掉落逻辑）
            for (ItemStack itemStack : drops) {
                if (!itemStack.isEmpty()) {
                    Block.popResource(level, dropPos, itemStack);
                }
            }

            // 在玩家位置生成经验球
            if (xp > 0) {
                ExperienceOrb orb = new ExperienceOrb(level, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, xp);
                level.addFreshEntity(orb);
            }

            // 播放方块破坏音效
            level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

            // 消耗工具耐久度
            if (!player.isCreative() && tool.isDamageableItem()) {
                tool.hurtAndBreak(1, player, player.getMainHandItem().getEquipmentSlot());

                // 如果工具损坏，可能会变成空气
                if (tool.isEmpty()) {
                    return true; // 仍然成功破坏了方块
                }
            }

            return true;
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("传送掉落物时出错: {}", e.getMessage());
            return false;
        }
    }


    /**
     * 获取相邻方块位置
     */
    private static List<BlockPos> getNeighborPositions(BlockPos pos, boolean includeDiagonal) {
        List<BlockPos> neighbors = new ArrayList<>(14); // 预分配容量

        // 直接相邻的6个方向
        neighbors.add(pos.above());
        neighbors.add(pos.below());
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());

        // 如果允许对角线，添加12个对角位置
        if (includeDiagonal) {
            // 水平对角线
            neighbors.add(pos.north().east());
            neighbors.add(pos.north().west());
            neighbors.add(pos.south().east());
            neighbors.add(pos.south().west());

            // 垂直对角线
            neighbors.add(pos.above().north());
            neighbors.add(pos.above().south());
            neighbors.add(pos.above().east());
            neighbors.add(pos.above().west());
            neighbors.add(pos.below().north());
            neighbors.add(pos.below().south());
            neighbors.add(pos.below().east());
            neighbors.add(pos.below().west());
            // 上平面的四个角落
            neighbors.add(pos.above().north().east());
            neighbors.add(pos.above().north().west());
            neighbors.add(pos.above().south().east());
            neighbors.add(pos.above().south().west());

            // 下平面的四个角落
            neighbors.add(pos.below().north().east());
            neighbors.add(pos.below().north().west());
            neighbors.add(pos.below().south().east());
            neighbors.add(pos.below().south().west());
        }

        return neighbors;
    }

    /**
     * 向玩家发送连锁挖掘反馈
     */
    private static void sendFeedback(ServerPlayer player, int blocksMined) {
        // 始终发送客户端通知包，让客户端根据其配置决定如何显示
        NetworkHandler.sendToPlayer(new ChainActionPacket("mining", blocksMined), player);
    }
} 