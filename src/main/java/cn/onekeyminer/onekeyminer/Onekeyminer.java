package cn.onekeyminer.onekeyminer;


import cn.onekeyminer.onekeyminer.capability.ChainModeCapability;
import cn.onekeyminer.onekeyminer.client.KeyBindings;
import cn.onekeyminer.onekeyminer.config.ClientConfig;
import cn.onekeyminer.onekeyminer.config.CommonConfig;
import cn.onekeyminer.onekeyminer.config.ServerConfig;
import cn.onekeyminer.onekeyminer.event.ServerEventHandler;
import cn.onekeyminer.onekeyminer.network.NetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;

import static cn.onekeyminer.onekeyminer.client.KeyBindings.CHAIN_KEY;

@Mod(Onekeyminer.MODID)
public class Onekeyminer {
    public static final String MODID = "onekeyminer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);


    public Onekeyminer(FMLJavaModLoadingContext context) {
        LOGGER.info("初始化一键连锁模组");
        var modBusGroup = context.getModBusGroup();

        // 注册配置
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        // 注册设置事件
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(Onekeyminer::commonSetup);

        // 注册客户端事件（只在客户端加载）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLClientSetupEvent.getBus(modBusGroup).addListener(Onekeyminer::clientSetup);
        }

        LOGGER.info("注册连锁挖掘、连锁互动和连锁种植功能");
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("注册网络处理器");
        event.enqueueWork(NetworkHandler::register);
    }
    
    private static void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("设置客户端功能");
        event.enqueueWork(() -> {
            KeyBindings.registerClientTick();
        });
    }
}
