package cn.onekeyminer.onekeyminer.event;

import cn.onekeyminer.onekeyminer.Onekeyminer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端事件处理器
 * 处理与服务器相关的事件，如玩家登录
 */
@Mod.EventBusSubscriber(modid = Onekeyminer.MODID)
public class ServerEventHandler {

    // Discord群聊链接
    private static final String DISCORD_LINK = "https://discord.gg/BNJuU33p";
    
    /**
     * 处理玩家登录事件
     * 发送欢迎消息和Discord群组信息
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 获取服务器实例
            MinecraftServer server = player.getServer();
            if (server != null) {
                // 创建一个任务，在下一个服务器tick执行
                server.executeBlocking(new Runnable() {
                    @Override
                    public void run() {
                        // 检查玩家是否仍然在线
                        if (player.isRemoved() || !player.isAlive()) {
                            return;
                        }
                        
                        // 使用命令源向玩家发送消息
                        CommandSourceStack source = server.createCommandSourceStack();
                        
                        // 发送欢迎消息
                        server.getCommands().performPrefixedCommand(
                                source,
                                "tellraw " + player.getScoreboardName() + " {\"translate\":\"message.onekeyminer.welcome\",\"color\":\"aqua\"}"
                        );
                        
                        // 发送Discord邀请
                        server.getCommands().performPrefixedCommand(
                                source,
                                "tellraw " + player.getScoreboardName() + " [{\"translate\":\"message.onekeyminer.discord.invite\",\"color\":\"green\"},{\"text\":\" \",\"color\":\"white\"},{\"text\":\"" + DISCORD_LINK + "\",\"color\":\"blue\",\"underlined\":true,\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + DISCORD_LINK + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"translate\":\"message.onekeyminer.discord.tooltip\"}}}]"
                        );
                    }
                });
            }
        }
    }
} 