package cn.onekeyminer.onekeyminer.network;

import cn.onekeyminer.onekeyminer.Onekeyminer;
import cn.onekeyminer.onekeyminer.client.ClientPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * 连锁操作数据包 - 用于向客户端发送连锁操作信息
 */
public class ChainActionPacket {
    private final String actionType; // 操作类型: "mining", "interaction", "planting"
    private final int count;         // 操作的方块数量
    
    public ChainActionPacket(String actionType, int count) {
        this.actionType = actionType;
        this.count = count;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(actionType);
        buf.writeInt(count);
    }
    
    public static ChainActionPacket decode(FriendlyByteBuf buf) {
        String actionType = buf.readUtf();
        int count = buf.readInt();
        return new ChainActionPacket(actionType, count);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(ChainActionPacket packet, CustomPayloadEvent.Context context) {
        if (context == null) {
            Onekeyminer.LOGGER.error("ChainActionPacket.handleOnClient: context is null");
            return;
        }
        context.enqueueWork(() -> {
            try {
                ClientPacketHandler.handleChainActionPacket(packet);
            } catch (Exception e) {
                Onekeyminer.LOGGER.error("处理连锁消息数据包时出错: {}", e.getMessage(), e);
            }
        });
    }

    public String getActionType() {
        return actionType;
    }
    public int getCount() {
        return count;
    }
} 
