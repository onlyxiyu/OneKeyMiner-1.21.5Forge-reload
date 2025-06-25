package cn.onekeyminer.onekeyminer.network;

import cn.onekeyminer.onekeyminer.config.ClientConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import cn.onekeyminer.onekeyminer.Onekeyminer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.NetworkContext;

/**
 * 配置同步数据包 - 将服务器配置更改应用到客户端
 */
public class ConfigSyncPacket {
    private final String configKey;
    private final String configValue;
    
    public ConfigSyncPacket(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(configKey);
        buf.writeUtf(configValue);
    }
    
    public static ConfigSyncPacket decode(FriendlyByteBuf buf) {
        String key = buf.readUtf();
        String value = buf.readUtf();
        return new ConfigSyncPacket(key, value);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(ConfigSyncPacket packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            String key = packet.configKey;
            String value = packet.configValue;

            // 根据配置键应用相应的配置值
            switch (key) {
                case "keyhold":
                    ClientConfig.REQUIRE_KEY_HOLD.set(Boolean.parseBoolean(value));
                    break;
                case "showcount":
                    ClientConfig.SHOW_BLOCK_COUNT.set(Boolean.parseBoolean(value));
                    break;
                case "messagestyle":
                    ClientConfig.MESSAGE_STYLE.set(value);
                    break;
                default:
                    Onekeyminer.LOGGER.warn("收到未知配置键: {}", key);
            }

            Onekeyminer.LOGGER.debug("客户端配置已更新: {} = {}", key, value);
        });
    }
} 