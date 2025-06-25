package cn.onekeyminer.onekeyminer.capability;

import cn.onekeyminer.onekeyminer.Onekeyminer;
import cn.onekeyminer.onekeyminer.config.ClientConfig;
import cn.onekeyminer.onekeyminer.network.NetworkHandler;
import cn.onekeyminer.onekeyminer.network.ChainModePacket;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = Onekeyminer.MODID)
public class ChainModeCapability {
    public static final Capability<IChainMode> CHAIN_MODE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Logger LOGGER = LogManager.getLogger();

    public static void init(BusGroup modEventBus) {
        // 注册玩家克隆事件，在玩家死亡后保留链式挖掘状态
        FMLCommonSetupEvent.getBus(modEventBus).addListener(ChainModeCapability::onPlayerClone);
        // 注册能力附加事件
        FMLCommonSetupEvent.getBus(modEventBus).addListener(Player.class, ChainModeCapability::attachCapabilities);
    }
    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IChainMode.class);
        LOGGER.debug("注册链式挖掘能力");
    }
    /**
     * 检查连锁模式是否激活
     */
    public static boolean isChainModeActive(Player player) {
        boolean result = player.getCapability(CHAIN_MODE_CAPABILITY).map(IChainMode::isActive).orElse(false);
        if (ClientConfig.DEBUG.get()) {
            LOGGER.debug("检查玩家 {} 的链式挖掘模式: {}, 能力是否存在: {}", 
                player.getName().getString(), 
                result, 
                player.getCapability(CHAIN_MODE_CAPABILITY).isPresent());
        }
        return result;
    }
@SubscribeEvent
public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
    if (!(event.getObject() instanceof Player)) return;

    if (!event.getObject().getCapability(CHAIN_MODE_CAPABILITY).isPresent()) {
        event.addCapability(ResourceLocation.fromNamespaceAndPath(Onekeyminer.MODID, "chain_mode"),
                            new ChainModeProvider());
        // 避免使用player.getName()，因为此时gameProfile可能为null
        LOGGER.debug("为玩家附加链式挖掘能力");
    }
}
    /**
     * 设置玩家的链式挖掘状态
     */
    public static void setChainMode(ServerPlayer player, boolean active) {
        player.getCapability(CHAIN_MODE_CAPABILITY).ifPresent(chainMode -> {
            if (ClientConfig.DEBUG.get()) {
                LOGGER.debug("设置玩家 {} 的链式挖掘模式为: {}", player.getName().getString(), active);
            }
            chainMode.setActive(active);

            NetworkHandler.sendToPlayer(new ChainModePacket(active), player);
        });
    }

    /**
     * 在玩家死亡后复制链式挖掘状态
     */
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        if (event.isWasDeath()) {
            original.getCapability(CHAIN_MODE_CAPABILITY).ifPresent(origCap -> {
                clone.getCapability(CHAIN_MODE_CAPABILITY).ifPresent(cloneCap -> {
                    cloneCap.setActive(origCap.isActive());
                });
            });
        }
    }

    /**
     * 附加能力到玩家
     */
    private static void attachCapabilities(AttachCapabilitiesEvent<Player> event) {
        if (ClientConfig.DEBUG.get()) {
            LOGGER.debug("在玩家 {} 身上附加链式挖掘能力", event.getObject().getName().getString());
        }
        event.addCapability(ResourceLocation.fromNamespaceAndPath(Onekeyminer.MODID, "chain_mode"), new ChainModeProvider());
        event.addListener(() -> {
            if (ClientConfig.DEBUG.get()) {
                LOGGER.debug("玩家 {} 的链式挖掘能力已失效", event.getObject().getName().getString());
            }
        });
    }

    /**
     * 在玩家登录时同步能力状态
     */
    @Mod.EventBusSubscriber(modid = Onekeyminer.MODID)
    public static class PlayerLoginHandler {
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(CHAIN_MODE_CAPABILITY).ifPresent(chainMode -> {
                    if (ClientConfig.DEBUG.get()) {
                        LOGGER.debug("在玩家 {} 登录时同步链式挖掘模式: {}", 
                            serverPlayer.getName().getString(), chainMode.isActive());
                    }
                    // 发送状态包到客户端（同步状态）
                    NetworkHandler.sendToPlayer(new ChainModePacket(chainMode.isActive()), serverPlayer);
                });
            }
        }
    }

    public interface IChainMode {
        boolean isActive();

        void setActive(boolean active);
    }

    public static class ChainMode implements IChainMode {
        private boolean active = false;

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void setActive(boolean active) {
            this.active = active;
        }
    }

    public static class ChainModeProvider implements ICapabilitySerializable<CompoundTag> {
        private final ChainMode instance = new ChainMode();
        private final LazyOptional<IChainMode> lazyOptional = LazyOptional.of(() -> instance);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == CHAIN_MODE_CAPABILITY ? lazyOptional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("active", instance.isActive());
            if (ClientConfig.DEBUG.get()) {
                LOGGER.debug("序列化玩家的链式挖掘模式: {}", instance.isActive());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            boolean active = tag.getBoolean("active").get();
            instance.setActive(active);
            if (ClientConfig.DEBUG.get()) {
                LOGGER.debug("反序列化玩家的链式挖掘模式: {}", active);
            }
        }

        public CompoundTag serializeNBT() {
            return serializeNBT(null);
        }

        public void deserializeNBT(CompoundTag tag) {
            deserializeNBT(null, tag);
        }
    }
}
