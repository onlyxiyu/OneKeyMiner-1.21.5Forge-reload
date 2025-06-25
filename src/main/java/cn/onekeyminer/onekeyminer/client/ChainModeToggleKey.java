package cn.onekeyminer.onekeyminer.client;

import cn.onekeyminer.onekeyminer.network.ChainModePacket;
import cn.onekeyminer.onekeyminer.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import cn.onekeyminer.onekeyminer.Onekeyminer;

@Mod.EventBusSubscriber
public class ChainModeToggleKey {
    public static KeyMapping chainModeKey;
    private static boolean currentState = false;
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (chainModeKey != null && chainModeKey.consumeClick()) {
            // 切换连锁模式
            currentState = !currentState;
            
            // 发送到服务器
            NetworkHandler.sendToServer(new ChainModePacket(currentState));
            
            // 添加调试日志
            Onekeyminer.LOGGER.debug("Toggled chain mode to: {}", currentState);
        }
    }
} 
