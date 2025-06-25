package cn.onekeyminer.onekeyminer.network;

import cn.onekeyminer.onekeyminer.Onekeyminer;

import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.function.BiConsumer;

/**
 * 网络处理类 - 用于处理客户端和服务器之间的通信
 */
public class NetworkHandler {

    public static final String VERSION = "1.0";
    public static final String CHANNEL_NAME = "onekeyminer_network";
    public static final SimpleChannel CHANNEL = ChannelBuilder
    .named(ResourceLocation.fromNamespaceAndPath("okmnf", CHANNEL_NAME))
           // .networkProtocolVersion(VERSION)
            .optional()
            .simpleChannel();

    public static void register() {

        CHANNEL.messageBuilder(ChainModePacket.class)
                .encoder(ChainModePacket::encode)
                .decoder(ChainModePacket::decode)
                .consumerNetworkThread((msg, ctx) -> {
                    ChainModePacket.handleOnServer(msg, ctx);
                })
                .add();

        CHANNEL.messageBuilder(BlocksMinedPacket.class)
                .encoder(BlocksMinedPacket::encode)
                .decoder(BlocksMinedPacket::decode)
                .consumerNetworkThread((msg, ctx) -> {
                    BlocksMinedPacket.handleOnClient(msg, ctx);
                })
                .add();

        CHANNEL.messageBuilder(ChainActionPacket.class)
                .encoder(ChainActionPacket::encode)
                .decoder(ChainActionPacket::decode)
                .consumerNetworkThread((msg, ctx) -> {
                    ChainActionPacket.handleOnClient(msg, ctx);
                })
                .add();

        CHANNEL.messageBuilder(ConfigSyncPacket.class)
                .encoder(ConfigSyncPacket::encode)
                .decoder(ConfigSyncPacket::decode)
                .consumerNetworkThread((msg, ctx) -> {
                    ConfigSyncPacket.handleOnClient(msg, ctx);
                })
                .add();
    }

    // 发送方法
    public static void sendToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(msg, player.connection.getConnection());
    }
    public static void sendToServer(Object msg) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            CHANNEL.send(msg, connection.getConnection());
        }
    }
}
