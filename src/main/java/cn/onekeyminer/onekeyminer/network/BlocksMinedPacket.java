package cn.onekeyminer.onekeyminer.network;

import cn.onekeyminer.onekeyminer.capability.ChainModeCapability;
import cn.onekeyminer.onekeyminer.client.ClientUtils;
import cn.onekeyminer.onekeyminer.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * 挖掘方块数量消息数据包
 */
public class BlocksMinedPacket {
    private final int blockCount;
    
    public BlocksMinedPacket(int blockCount) {
        this.blockCount = blockCount;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(blockCount);
    }
    
    public static BlocksMinedPacket decode(FriendlyByteBuf buf) {
        return new BlocksMinedPacket(buf.readInt());
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(BlocksMinedPacket packet, CustomPayloadEvent.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (minecraft.player == null) return;
            if (ClientConfig.SHOW_BLOCK_COUNT.get()) {
                ClientUtils.showBlockCountMessage(packet.blockCount);
            }
        });
    }
} 