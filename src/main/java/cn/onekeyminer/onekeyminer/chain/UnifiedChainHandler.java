package cn.onekeyminer.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * 统一连锁处理器 - 整合连锁挖掘、交互和种植功能
 */
@Mod.EventBusSubscriber(modid = Onekeyminer.MODID)
public class UnifiedChainHandler {

    /**
     * 处理玩家右键点击方块事件
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 忽略非玩家实体
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.getEntity();

        try {
            // 检查连锁模式是否激活
            if (!ChainModeCapability.isChainModeActive(player)) {
                return;
            }
            
            // 检查是否需要潜行
            if (CommonConfig.REQUIRE_SNEAKING.get() && !player.isShiftKeyDown()) {
                return;
            }
            
            // 检查创造模式设置
            if (player.isCreative() && !CommonConfig.ENABLE_IN_CREATIVE.get()) {
                return;
            }
            if(!isToolItem(player.getItemInHand(event.getHand()))) {
                return;
            }
            // 获取玩家手中的物品
            ItemStack heldItem = player.getItemInHand(event.getHand());
            BlockState targetState = event.getLevel().getBlockState(event.getPos());

            // 判断物品类型并调用相应处理方法
            if (InteractionHandler.isValidInteractionTool(heldItem)) {
                // 工具类 - 使用连锁交互
                if(ClientConfig.DEBUG.get()) {
                    Onekeyminer.LOGGER.debug("触发工具连锁交互: {}", heldItem.getItem().getClass().getSimpleName());
                }
                // 使用新的交互方法
                InteractionHandler.tryChainInteraction(
                    player, 
                    (ServerLevel)event.getLevel(),
                    event.getPos(), 
                    event.getFace(), 
                    // 从BlockHitResult获取点击位置的Vec3
                    event.getHitVec().getLocation(), 
                    event.getHand()
                );
            } else if (isPlantableItem(heldItem)) {
                // 种植物类 - 使用连锁种植
                PlantingHandler.handleChainPlanting(player, event);
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("处理玩家交互事件时发生错误: {}", e.getMessage());
            Onekeyminer.LOGGER.debug("详细错误栈", e);
        }
    }

    /**
     * 处理玩家右键点击实体事件
     * 
     * 当玩家使用剪刀右键点击羊时，会检查以下配置：
     * - 连锁模式是否激活 (通过ChainModeCapability)
     * - 是否需要潜行 (requireSneaking)
     * - 创造模式下是否启用 (enableInCreative)
     * - 使用maxBlocksInChain或maxBlocksInChainCreative作为最大羊的数量
     * - 使用maxChainDepth*0.5作为基础搜索范围
     * - 启用对角连锁时 (enableDiagonalChaining) 搜索范围增加50%
     * - 如果启用teleportDropsToPlayer，羊毛会掉落在玩家位置
     */
    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        // 忽略非玩家实体
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.getEntity();

        try {
            // 检查连锁模式是否激活
            if (!ChainModeCapability.isChainModeActive(player)) {
                return;
            }
            
            // 检查是否需要潜行
            if (CommonConfig.REQUIRE_SNEAKING.get() && !player.isShiftKeyDown()) {
                return;
            }
            
            // 检查创造模式设置
            if (player.isCreative() && !CommonConfig.ENABLE_IN_CREATIVE.get()) {
                return;
            }

            // 获取玩家手中的物品
            ItemStack heldItem = player.getItemInHand(event.getHand());
            
            // 目标实体
            Entity targetEntity = event.getTarget();
            
            // 处理剪羊毛
            if (heldItem.getItem() instanceof ShearsItem && targetEntity instanceof Sheep) {
                Sheep sheep = (Sheep) targetEntity;
                // 检查羊是否可剪毛
                if (sheep.isShearable(heldItem, player.level(), sheep.blockPosition())) {
                    if(ClientConfig.DEBUG.get()) {
                        Onekeyminer.LOGGER.debug("检测到剪羊毛操作");
                    }
                    // 在下一个游戏刻执行，确保原始操作已完成

                        // 执行连锁剪羊毛
                        InteractionHandler.tryChainShearing(player, sheep, event.getHand());
                }
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("处理实体交互事件时发生错误: {}", e.getMessage());
            Onekeyminer.LOGGER.debug("详细错误栈", e);
        }
    }

    /**
     * 处理方块破坏事件
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
       if (!(event.getPlayer() instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.getPlayer();

        try {
            // 检查连锁模式是否激活
            if (!ChainModeCapability.isChainModeActive(player)) {
                return;
            }

            // 获取玩家手中的工具
            ItemStack tool = player.getMainHandItem();
            BlockState state = event.getState();

            // 调用连锁挖掘处理器
            ChainHandler.tryChainMine(player, event.getPos(), state, tool);
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("处理方块破坏事件时发生错误: {}", e.getMessage());
            Onekeyminer.LOGGER.debug("详细错误栈", e);
        }
    }

    /**
     * 判断是否是工具类物品
     */
    public static boolean isToolItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();
        return item instanceof ShearsItem ||    // 剪刀
               item instanceof HoeItem ||       // 锄头
               item instanceof AxeItem ||       // 斧头（特别列出以确保兼容性）
               item instanceof ShovelItem
                || isPlantableItem(stack);      // 铲子（特别列出以确保兼容性）
    }

    /**
     * 判断是否是可种植物品
     */
    public static boolean isPlantableItem(ItemStack stack) {
        return PlantingHandler.isPlantableItem(stack);
    }
} 