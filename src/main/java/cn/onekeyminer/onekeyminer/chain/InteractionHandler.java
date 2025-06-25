package cn.onekeyminer.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.context.UseOnContext;

import cn.onekeyminer.onekeyminer.Onekeyminer;
import cn.onekeyminer.onekeyminer.capability.ChainModeCapability;
import cn.onekeyminer.onekeyminer.config.ClientConfig;
import cn.onekeyminer.onekeyminer.config.CommonConfig;
import cn.onekeyminer.onekeyminer.config.ServerConfig;
import cn.onekeyminer.onekeyminer.network.ChainActionPacket;
import cn.onekeyminer.onekeyminer.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.network.chat.Component;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;


import java.util.*;

/**
 * 处理连锁交互（例如右键方块）
 */
public class InteractionHandler {
    // 防止重复交互的方块位置集合
    private static final Set<BlockPos> PROCESSED_POSITIONS = new HashSet<>();
    // 防止重复交互的实体ID集合
    private static final Set<Integer> PROCESSED_ENTITY_IDS = new HashSet<>();

    /**
     * 检查物品是否是有效的交互工具
     */
    public static boolean isValidInteractionTool(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();
        return item instanceof HoeItem ||       // 锄头
                item instanceof AxeItem ||       // 斧子
                item instanceof ShovelItem ||    // 铲子
                item instanceof ShearsItem ||    // 剪刀
                item instanceof BrushItem;       // 刷子
    }

    /**
     * 处理玩家交互事件
     * 用于兼容旧的调用方式
     * @deprecated 使用 {@link #tryChainInteraction} 代替
     */
    @Deprecated
    public static void handleChainInteraction(ServerPlayer player, PlayerInteractEvent.RightClickBlock event) {
        // 调用新的实现方法
        tryChainInteraction(
                player,
                (ServerLevel)event.getLevel(),
                event.getPos(),
                event.getFace(),
                // 从BlockHitResult获取点击位置的Vec3
                event.getHitVec().getLocation(),
                event.getHand()
        );
    }

    /**
     * 尝试连锁交互
     * @param player 玩家
     * @param level 世界
     * @param pos 初始方块位置
     * @param side 交互面
     * @param hitVec 点击坐标
     * @param hand 使用的手
     * @return 是否成功处理了连锁交互
     */
    public static boolean tryChainInteraction(ServerPlayer player, ServerLevel level, BlockPos pos,
                                              Direction side, Vec3 hitVec, InteractionHand hand) {
        // 清空已处理集合
        PROCESSED_POSITIONS.clear();

        // 获取玩家手中物品
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty() || !isValidInteractionTool(heldItem)) {
            return false;
        }

        // 获取初始方块状态
        BlockState initialState = level.getBlockState(pos);
        if (initialState.isAir()) {
            return false;
        }
        if(ClientConfig.DEBUG.get()) Onekeyminer.LOGGER.debug("尝试连锁交互: {}", initialState.getBlock().getDescriptionId());

        // 创建初始方块点击结果
        BlockHitResult initialHit = new BlockHitResult(hitVec, side, pos, false);

        // 获取配置的最大交互数量
        int maxBlocks = player.isCreative()
                ? CommonConfig.MAX_BLOCKS_IN_CHAIN_CREATIVE.get()
                : CommonConfig.MAX_BLOCKS_IN_CHAIN.get();

        // 是否启用对角线检查
        boolean includeDiagonal = CommonConfig.ENABLE_DIAGONAL_CHAINING.get();

        // 使用洪水填充算法执行连锁交互
        int processedCount = floodFillInteraction(player, level, initialState, initialHit, hand,
                maxBlocks, CommonConfig.MAX_MINING_DEPTH.get(), includeDiagonal);

        // 发送交互结果
        if (processedCount > 0) {
            // 向客户端发送反馈
            NetworkHandler.sendToPlayer(new ChainActionPacket("interaction", processedCount), player);
            return true;
        }

        return false;
    }

    /**
     * 使用洪水填充算法执行连锁交互
     */
    private static int floodFillInteraction(ServerPlayer player, ServerLevel level, BlockState targetState,
                                            BlockHitResult initialHit, InteractionHand hand,
                                            int maxBlocks, int maxDepth, boolean includeDiagonal) {
        // 使用队列进行广度优先搜索
        Queue<BlockSearchNode> queue = new LinkedList<>();

        // 添加初始节点
        queue.add(new BlockSearchNode(initialHit.getBlockPos(), 0));

        // 已处理的方块数量
        int processedCount = 0;

        // 获取玩家手中物品
        ItemStack heldItem = player.getItemInHand(hand);

        while (!queue.isEmpty() && processedCount < maxBlocks) {
            // 获取下一个待处理的节点
            BlockSearchNode node = queue.poll();
            BlockPos pos = node.pos;

            // 如果深度超过最大值，跳过
            if (node.depth > maxDepth) {
                continue;
            }

            // 如果方块已处理，跳过
            if (PROCESSED_POSITIONS.contains(pos)) {
                continue;
            }

            // 检查方块状态是否匹配目标
            BlockState state = level.getBlockState(pos);
            if (!isMatchingBlock(state, targetState)) {
                continue;
            }

            // 检查工具是否已损坏
            if (heldItem.isEmpty() || heldItem.getDamageValue() >= heldItem.getMaxDamage() - 1) {
                break;
            }

            // 标记为已处理
            PROCESSED_POSITIONS.add(pos);

            // 创建点击结果并执行交互
            BlockHitResult hit = new BlockHitResult(initialHit.getLocation(), initialHit.getDirection(), pos, false);
            // 修正方法调用 - 先使用物品在方块上，而不是直接调用方块的useItemOn方法
            UseOnContext context = new UseOnContext(player, hand, hit);
            InteractionResult result = heldItem.useOn(context);

            if (result.consumesAction()) {
                processedCount++;

                if(ClientConfig.DEBUG.get()) Onekeyminer.LOGGER.debug("连锁交互成功: {}", pos);
            }

            // 将相邻方块添加到队列
            for (BlockPos neighbor : getNeighborPositions(pos, includeDiagonal)) {
                queue.offer(new BlockSearchNode(neighbor, node.depth + 1));
            }
        }

        Onekeyminer.LOGGER.debug("连锁交互完成，共处理 {} 个方块", processedCount);
        return processedCount;
    }

    /**
     * 检查方块状态是否匹配目标
     */
    private static boolean isMatchingBlock(BlockState state, BlockState target) {
        if (state.isAir() || target.isAir()) {
            return false;
        }

        if ((CommonConfig.MATCH_BLOCK_STATE.get()&& !(target.getBlock() instanceof CropBlock))||(CommonConfig.matchseedBlockState.get() && target.getBlock() instanceof CropBlock)) {
            // 完全匹配方块状态
            return state.equals(target);
        } else {
            // 只匹配方块类型
            return state.getBlock() == target.getBlock();
        }
    }

    /**
     * 获取相邻方块位置
     */
    private static List<BlockPos> getNeighborPositions(BlockPos pos, boolean includeDiagonal) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 直接相邻的6个方向
        neighbors.add(pos.above());
        neighbors.add(pos.below());
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());

        // 如果允许对角线，添加12个对角线位置
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

            // 完全对角线（八个角）
            neighbors.add(pos.above().north().east());
            neighbors.add(pos.above().north().west());
            neighbors.add(pos.above().south().east());
            neighbors.add(pos.above().south().west());
            neighbors.add(pos.below().north().east());
            neighbors.add(pos.below().north().west());
            neighbors.add(pos.below().south().east());
            neighbors.add(pos.below().south().west());
        }

        return neighbors;
    }

    /**
     * 方块搜索节点，用于跟踪深度
     */
    private static class BlockSearchNode {
        final BlockPos pos;
        final int depth;

        BlockSearchNode(BlockPos pos, int depth) {
            this.pos = pos;
            this.depth = depth;
        }
    }

    /**
     * 处理剪羊毛的连锁操作 - 不依赖InteractionResult
     * @param player 玩家
     * @param targetSheep 被交互的目标羊
     * @param hand 使用的手
     * @return 是否成功处理了连锁剪羊毛
     */
    public static boolean tryChainShearing(ServerPlayer player, Sheep targetSheep, InteractionHand hand) {
        // 清空已处理集合
        PROCESSED_ENTITY_IDS.clear();

        // 获取玩家手中物品
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ShearsItem)) {
            return false;
        }

        if(ClientConfig.DEBUG.get()) {
            Onekeyminer.LOGGER.debug("尝试连锁剪羊毛: {}", targetSheep.getId());
        }

        // 获取配置的最大交互数量
        int maxEntities = player.isCreative()
                ? CommonConfig.MAX_BLOCKS_IN_CHAIN_CREATIVE.get()
                : CommonConfig.MAX_BLOCKS_IN_CHAIN.get();

        // 获取搜索范围 - 使用maxChainDepth作为基础半径，对角连锁时增加范围
        double searchRadius = CommonConfig.MAX_CHAIN_DEPTH.get() * 0.5;
        if (CommonConfig.ENABLE_DIAGONAL_CHAINING.get()) {
            // 对角连锁时增加50%范围
            searchRadius *= 1.5;
        }

        // 将初始羊添加到已处理集合
        PROCESSED_ENTITY_IDS.add(targetSheep.getId());

        // 检查是否启用传送掉落物
        boolean teleportDrops = CommonConfig.TELEPORT_DROPS_TO_PLAYER.get();
        BlockPos playerPos = player.blockPosition();
        ServerLevel level = (ServerLevel) player.level();

        // 一次性获取所有区域内的羊
        AABB searchBox = new AABB(
                targetSheep.getX() - searchRadius, targetSheep.getY() - searchRadius, targetSheep.getZ() - searchRadius,
                targetSheep.getX() + searchRadius, targetSheep.getY() + searchRadius, targetSheep.getZ() + searchRadius);

        List<Sheep> allSheepInArea = level.getEntitiesOfClass(
                Sheep.class,
                searchBox,
                sheep -> sheep.isAlive() && sheep.readyForShearing() && !PROCESSED_ENTITY_IDS.contains(sheep.getId())
        );

        if(ClientConfig.DEBUG.get()) {
            Onekeyminer.LOGGER.debug("找到 {} 只可剪羊", allSheepInArea.size() + 1); // +1 包括目标羊
        }

        // 计数器
        int shearedCount = 1; // 包括初始的羊

        // 批量处理所有羊
        for (Sheep sheep : allSheepInArea) {
            // 如果已经达到最大数量，停止处理
            if (shearedCount >= maxEntities) {
                break;
            }

            try {
                // 检查羊是否可以被剪毛和是否有毛可剪
                if (sheep.isShearable(heldItem, player.level(), sheep.blockPosition()) && sheep.readyForShearing()) {
                    // 保存剪毛前的状态
                    boolean wasShearable = sheep.readyForShearing();
                    int woolColorBefore = sheep.getColor().getId();

                    // 使用版本无关的方法检查剪毛成功与否
                    boolean shearingSuccessful = false;

                    if (teleportDrops) {
                        // 保存羊原来的位置
                        double originalX = sheep.getX();
                        double originalY = sheep.getY();
                        double originalZ = sheep.getZ();

                        try {
                            // 临时将羊移动到玩家位置附近
                            sheep.setPos(playerPos.getX(), playerPos.getY(), playerPos.getZ());

                            // 使用原版方法进行剪毛，这在1.21.4中工作正常
                            if (sheep.readyForShearing()) {
                                // 直接调用原版方法
                                List<ItemStack> items = sheep.onSheared(player, heldItem, level, sheep.blockPosition(),1);

                                // 检查是否有掉落物表示剪毛成功
                                shearingSuccessful = !items.isEmpty();

                                // 模拟伤害工具
                                if (shearingSuccessful && !player.getAbilities().instabuild) {
                                    heldItem.hurtAndBreak(1, player, player.getMainHandItem().getEquipmentSlot());
                                }

                                // 触发掉落物生成
                                items.forEach(itemStack -> {
                                    ItemEntity itemEntity = new ItemEntity(level, sheep.getX(), sheep.getY(1.0D), sheep.getZ(), itemStack);
                                    level.addFreshEntity(itemEntity);
                                });

                                if(ClientConfig.DEBUG.get()) {
                                    Onekeyminer.LOGGER.debug("剪羊状态: 使用onSheared方法={}, 掉落物数量={}", true, items.size());
                                }
                            }

                            // 恢复羊的原始位置
                            sheep.setPos(originalX, originalY, originalZ);
                        } catch (Exception e) {
                            Onekeyminer.LOGGER.error("传送羊毛掉落物时出错: {}", e.getMessage());
                            sheep.setPos(originalX, originalY, originalZ);
                        }
                    } else {
                        // 对于不需要传送掉落物的情况，也直接使用onSheared方法
                        if (sheep.readyForShearing()) {
                            // 直接调用原版方法
                            List<ItemStack> items = sheep.onSheared(player, heldItem, level, sheep.blockPosition(),1);

                            // 检查是否有掉落物表示剪毛成功
                            shearingSuccessful = !items.isEmpty();

                            // 模拟伤害工具
                            if (shearingSuccessful && !player.getAbilities().instabuild) {
                                heldItem.hurtAndBreak(1, player, player.getMainHandItem().getEquipmentSlot());
                            }

                            // 触发掉落物生成
                            items.forEach(itemStack -> {
                                ItemEntity itemEntity = new ItemEntity(level, sheep.getX(), sheep.getY(1.0D), sheep.getZ(), itemStack);
                                level.addFreshEntity(itemEntity);
                            });

                            if(ClientConfig.DEBUG.get()) {
                                Onekeyminer.LOGGER.debug("剪羊状态: 使用onSheared方法={}, 掉落物数量={}", true, items.size());
                            }
                        }
                    }

                    // 如果剪毛成功
                    if (shearingSuccessful) {
                        // 标记为已处理并增加计数
                        PROCESSED_ENTITY_IDS.add(sheep.getId());
                        shearedCount++;

                        if(ClientConfig.DEBUG.get()) {
                            Onekeyminer.LOGGER.debug("成功剪了羊 {}", sheep.getId());
                        }

                        // 如果工具损坏了，停止处理
                        if (heldItem.isEmpty()) {
                            break;
                        }
                    } else if(ClientConfig.DEBUG.get()) {
                        Onekeyminer.LOGGER.debug("剪羊 {} 失败，尽管它应该是可剪的", sheep.getId());
                    }
                }
            } catch (Exception e) {
                // 捕获并记录处理单个羊时的错误，但继续处理其他羊
                Onekeyminer.LOGGER.error("处理羊 {} 时出错: {}", sheep.getId(), e.getMessage());
            }
        }

        if(ClientConfig.DEBUG.get()) {
            Onekeyminer.LOGGER.debug("成功剪了 {} 只羊", shearedCount);
        }

        // 根据客户端配置发送反馈消息
        if (shearedCount > 1 && ClientConfig.SHOW_BLOCK_COUNT.get()) {
            // 获取消息风格
            String messageStyle = ClientConfig.MESSAGE_STYLE.get();

            // 发送反馈
            if (messageStyle.equals("chat") || messageStyle.equals("both")) {
                player.sendSystemMessage(Component.translatable("message.onekeyminer.chain_shearing", shearedCount));
            }

            if (messageStyle.equals("actionbar") || messageStyle.equals("both")) {
                NetworkHandler.sendToPlayer(new ChainActionPacket("shearing", shearedCount), player);
            }

            return true;
        }

        return shearedCount > 1;
    }
} 