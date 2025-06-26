package cn.onekeyminer.onekeyminer.client;

import cn.onekeyminer.onekeyminer.Onekeyminer;
import cn.onekeyminer.onekeyminer.config.ClientConfig;
import cn.onekeyminer.onekeyminer.network.ChainModePacket;
import cn.onekeyminer.onekeyminer.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 按键绑定管理类
 * 处理连锁模式的按键绑定、状态切换和定时发送网络包
 */
@Mod.EventBusSubscriber(modid = Onekeyminer.MODID, value = Dist.CLIENT)
public class KeyBindings {
    // 连锁模式按键绑定
    public static final KeyMapping CHAIN_KEY = new KeyMapping(
            "key.onekeyminer.chain",           // 翻译键（对应 lang 文件中的键）
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.onekeyminer"       // 按键��别（显示在控制设置中）
    );

      // 连锁模式状态
    private static boolean chainModeActive = false;
    // 数据包发送计时器（按住模式）
    private static int packetCounter = 0;
    // 连锁模式自动关闭计时器（非按住模式）
    private static int frozenTimerCounter = 0;
    // 上次检查时间
    private static long lastCheckTime = 0;
    // 计时器
    private static Timer timer = null;

    /**
     * 注册按键绑定事件
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHAIN_KEY);

    }

    /**
     * 注册客户端处理器，由主类调用
     * 使用Java Timer替代Tick事件
     */
    public static void registerClientTick() {
        if(ClientConfig.REQUIRE_KEY_HOLD.get())  Onekeyminer.LOGGER.info("初始化客户端定时器");

        // 取消旧定时器（如果存在）
        if (timer != null) {
            timer.cancel();
        }

        // 创建新的定时器，每100毫秒执行一次检查
        timer = new Timer("ChainModeTimer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handleTimerTick();
            }
        }, 0, 100); // 每100毫秒执行一次

        lastCheckTime = System.currentTimeMillis();
    }

    /**
     * 处理按键输入事件
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (CHAIN_KEY.consumeClick()) {
            Boolean requireKeyHold = ClientConfig.REQUIRE_KEY_HOLD.get();
            if (requireKeyHold == null) {
                if(ClientConfig.REQUIRE_KEY_HOLD.get()) Onekeyminer.LOGGER.error("配置项 requireKeyHold 未正确初始化，使用默认值 false");
                requireKeyHold = false;
            }
            if (requireKeyHold) {
                // 按住键才能触发连锁（每次按下发送一次激活消息）
                chainModeActive = true;
                NetworkHandler.sendToServer(new ChainModePacket(true));
                packetCounter = 0; // 重置计时器
                if(ClientConfig.REQUIRE_KEY_HOLD.get()) Onekeyminer.LOGGER.debug("Key pressed, activating chain mode (hold mode)");
            } else {
                // 点击切换连锁模式
                chainModeActive = !chainModeActive;
                NetworkHandler.sendToServer(new ChainModePacket(chainModeActive));
                if(ClientConfig.REQUIRE_KEY_HOLD.get()) Onekeyminer.LOGGER.debug("Key pressed, toggling chain mode: {}", chainModeActive);

                // 重置冻结计时器
                if (chainModeActive) {
                    frozenTimerCounter = ClientConfig.FROZEN_TIMER.get() * 1000;
                }

                // 显示状态消息
                ClientUtils.showStatusMessage(chainModeActive ?
                        "message.onekeyminer.mode_enabled" :
                        "message.onekeyminer.mode_disabled");
            }

            // 添加调试日志，检查客户端状态
            if(ClientConfig.REQUIRE_KEY_HOLD.get()) Onekeyminer.LOGGER.debug("Client chain mode state after key press: {}", chainModeActive);
        }
    }

    /**
     * 处理定时器触发事件
     * 用于周期性发送状态包和处理倒计时
     * 在按住模式下每1000毫秒发送一次状态包
     * 在非按住模式下处理自动关闭倒计时
     */
    public static void handleTimerTick() {
        // 获取Minecraft实例
        Minecraft minecraft = Minecraft.getInstance();
        // 安全检查 - 确保在游戏主线程中执行UI相关操作
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;

        // 计算自上次检查以来经过的时间
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastCheckTime;
        lastCheckTime = currentTime;

        // 防止意外的大跳变（例如游戏暂停后恢复）
        if (elapsed <= 0 || elapsed > 5000) return;

        if (ClientConfig.REQUIRE_KEY_HOLD.get()) {
            // 按住模式：每1000毫秒发送一次状态
            if (CHAIN_KEY.isDown()) {
                packetCounter += elapsed;
                if (packetCounter >= 500) {
                    // 在游戏主线程中执行
                    minecraft.execute(() -> {
                        NetworkHandler.sendToServer(new ChainModePacket(true));
                    });
                    packetCounter = 0;
                }
            } else if (chainModeActive) {
                // 如果键被释放且模式仍为激活，发送停用消息
                chainModeActive = false;
                // 在游戏主线程中执行
                minecraft.execute(() -> {
                    NetworkHandler.sendToServer(new ChainModePacket(false));
                });
            }
        } else if (chainModeActive) {
            // 非按住模式：处理冻结计时器
            if (frozenTimerCounter > 0) {
                frozenTimerCounter -= elapsed;
                // 仅在调试模式下每秒记录一次倒计时

                if (frozenTimerCounter <= 0) {
                    // 计时结束，关闭连锁模式
                    chainModeActive = false;
                    // 在游戏主线程中执行
                    minecraft.execute(() -> {
                        NetworkHandler.sendToServer(new ChainModePacket(false));
                        ClientUtils.showStatusMessage("message.onekeyminer.mode_disabled");
                    });
                }
            }
        }
    }

    /**
     * 获取当前连锁模式状态
     * @return 连锁模式是否激活
     */
//    public static boolean isChainModeActive() {
//        return chainModeActive;
//    }
}

