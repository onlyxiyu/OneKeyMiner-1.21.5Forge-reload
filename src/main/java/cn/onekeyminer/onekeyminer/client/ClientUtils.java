package cn.onekeyminer.onekeyminer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;


public class ClientUtils {
    public static void showStatusMessage(String translationKey) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player != null) {
            Component message = Component.translatable(translationKey);
            player.displayClientMessage(message, true); // true表示覆盖操作栏消息
        }
    }
    
    public static void showBlockCountMessage(int blockCount) {
        if (blockCount <= 1) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player != null) {
            Component message = Component.translatable("message.onekeyminer.chain_count", blockCount);
            player.displayClientMessage(message, true);
        }
    }
} 