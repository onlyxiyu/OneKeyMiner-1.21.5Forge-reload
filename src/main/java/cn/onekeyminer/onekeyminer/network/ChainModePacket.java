package cn.onekeyminer.onekeyminer.network;

import cn.onekeyminer.onekeyminer.capability.ChainModeCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * 连锁模式切换消息数据包
 */
public class ChainModePacket {
    final boolean active;

    public ChainModePacket(boolean active) {
        this.active = active;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
    }

    public static ChainModePacket decode(FriendlyByteBuf buf) {
        return new ChainModePacket(buf.readBoolean());
    }

    public static void handleOnServer(ChainModePacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            if (context.getSender() != null && context.getSender() instanceof ServerPlayer serverPlayer) {
                // 设置玩家的连锁模式状态
                ChainModeCapability.setChainMode(serverPlayer, packet.active);
            }
        });
    }

    public static void handleOnClient(ChainModePacket packet, CustomPayloadEvent.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (minecraft.player == null) return;

            Component message = Component.translatable(
                    packet.active ? "message.onekeyminer.chain_mode_on" : "message.onekeyminer.chain_mode_off");
            minecraft.player.displayClientMessage(message, true);
        });
    }
} 