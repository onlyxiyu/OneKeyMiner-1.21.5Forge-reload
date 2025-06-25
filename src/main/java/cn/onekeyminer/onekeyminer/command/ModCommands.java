package cn.onekeyminer.onekeyminer.command;

import cn.onekeyminer.onekeyminer.Onekeyminer;
import cn.onekeyminer.onekeyminer.capability.ChainModeCapability;
import cn.onekeyminer.onekeyminer.config.ClientConfig;
import cn.onekeyminer.onekeyminer.config.CommonConfig;
import cn.onekeyminer.onekeyminer.config.ConfigUtils;
import cn.onekeyminer.onekeyminer.config.ServerConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

@Mod.EventBusSubscriber(modid = Onekeyminer.MODID)
public class ModCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(
            Commands.literal("excavation")
                .then(Commands.literal("status")
                    .executes(ModCommands::showStatus))
                .then(Commands.literal("help")
                    .executes(ModCommands::showHelp))
                .then(Commands.literal("common")
                    .then(registerCommonConfigCommands()))
                .then(Commands.literal("client")
                    .then(registerClientConfigCommands()))
                .then(Commands.literal("server")
                    .then(registerServerConfigCommands()))
                .executes(ModCommands::showHelp)
        );
    }
    
    // 显示帮助信息
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("command.onekeyminer.help.title"), false);
        source.sendSuccess(() -> Component.translatable("command.onekeyminer.help.status"), false);
        source.sendSuccess(() -> Component.translatable("command.onekeyminer.help.server"), false); // 新增
        source.sendSuccess(() -> Component.translatable("command.onekeyminer.help.client"), false); // 新增
        return 1;
    }
    
    // 查看状态命令
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            
            // 显示当前玩家连锁模式状态
            boolean chainActive = !ChainModeCapability.isChainModeActive(player);
            context.getSource().sendSuccess(() -> 
                Component.translatable("command.onekeyminer.status", 
                    Component.translatable(chainActive ? 
                        "command.onekeyminer.status.on" : 
                        "command.onekeyminer.status.off")), 
                false);
            
            // 显示配置信息
            context.getSource().sendSuccess(() -> 
                Component.translatable("command.onekeyminer.status.maxblocks", 
                    CommonConfig.MAX_BLOCKS_IN_CHAIN),
                false);
            
            context.getSource().sendSuccess(() -> 
                Component.translatable("command.onekeyminer.status.maxdepth", 
                    CommonConfig.MAX_CHAIN_DEPTH),
                false);
                
            boolean diagonal = CommonConfig.ENABLE_DIAGONAL_CHAINING.get();
            context.getSource().sendSuccess(() -> 
                Component.translatable("command.onekeyminer.status.diagonal",
                    Component.translatable(diagonal ? 
                        "command.onekeyminer.enabled" : 
                        "command.onekeyminer.disabled")), 
                false);
                
            // 添加蹲下需求状态
            boolean REQUIRE_SNEAKING = CommonConfig.REQUIRE_SNEAKING.get();
            context.getSource().sendSuccess(() -> 
                Component.translatable("command.onekeyminer.status.sneaking",
                    Component.translatable(REQUIRE_SNEAKING ? 
                        "command.onekeyminer.required" : 
                        "command.onekeyminer.not_required")), 
                false);
                
            return 1;
        }
        return 0;
    }
    
    /**
     * 注册通用配置命令
     */
    private static LiteralArgumentBuilder<CommandSourceStack> registerCommonConfigCommands() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("options");
        
        // 最大连锁方块数
        builder.then(Commands.literal("maxChainBlocks")
            .executes(ctx -> showCommonConfigValue(ctx, "maxChainBlocks", CommonConfig.MAX_CHAIN_BLOCKS))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 4096))
                .executes(ctx -> setCommonConfigIntValue(ctx, "maxChainBlocks", 
                    CommonConfig.MAX_CHAIN_BLOCKS,
                    IntegerArgumentType.getInteger(ctx, "value")))));
        
        // 最大连锁深度
        builder.then(Commands.literal("maxChainDepth")
            .executes(ctx -> showCommonConfigValue(ctx, "maxChainDepth", CommonConfig.MAX_CHAIN_DEPTH))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> setCommonConfigIntValue(ctx, "maxChainDepth", 
                    CommonConfig.MAX_CHAIN_DEPTH, 
                    IntegerArgumentType.getInteger(ctx, "value")))));
        
        // 存活模式最大方块数
        builder.then(Commands.literal("maxBlocksInChain")
            .executes(ctx -> showCommonConfigValue(ctx, "maxBlocksInChain", CommonConfig.MAX_BLOCKS_IN_CHAIN))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 4096))
                .executes(ctx -> setCommonConfigIntValue(ctx, "maxBlocksInChain", 
                    CommonConfig.MAX_BLOCKS_IN_CHAIN, 
                    IntegerArgumentType.getInteger(ctx, "value")))));
        
        // 创造模式最大方块数
        builder.then(Commands.literal("maxBlocksInChainCreative")
            .executes(ctx -> showCommonConfigValue(ctx, "maxBlocksInChainCreative", CommonConfig.MAX_BLOCKS_IN_CHAIN_CREATIVE))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 16384))
                .executes(ctx -> setCommonConfigIntValue(ctx, "maxBlocksInChainCreative", 
                    CommonConfig.MAX_BLOCKS_IN_CHAIN_CREATIVE,
                    IntegerArgumentType.getInteger(ctx, "value")))));
        
        // 黑名单管理
        builder.then(Commands.literal("nonChainableBlocks")
            .then(Commands.literal("list")
                .executes(ModCommands::listNonChainableBlocks))
            .then(Commands.literal("add")
                .then(Commands.argument("blockId", StringArgumentType.string())
                    .executes(ctx -> addToNonChainableBlocks(ctx, StringArgumentType.getString(ctx, "blockId")))))
            .then(Commands.literal("remove")
                .then(Commands.argument("blockId", StringArgumentType.string())
                    .executes(ctx -> removeFromNonChainableBlocks(ctx, StringArgumentType.getString(ctx, "blockId"))))));
        
        // 种子黑名单管理
        builder.then(Commands.literal("seedBlacklist")
            .then(Commands.literal("list")
                .executes(ModCommands::listSeedBlacklist))
            .then(Commands.literal("add")
                .then(Commands.argument("seedId", StringArgumentType.string())
                    .executes(ctx -> addToSeedBlacklist(ctx, StringArgumentType.getString(ctx, "seedId")))))
            .then(Commands.literal("remove")
                .then(Commands.argument("seedId", StringArgumentType.string())
                    .executes(ctx -> removeFromSeedBlacklist(ctx, StringArgumentType.getString(ctx, "seedId"))))));
        
        // 布尔值配置项
        addBooleanConfigOption(builder, "enableDiagonalChaining", CommonConfig.ENABLE_DIAGONAL_CHAINING);
        addBooleanConfigOption(builder, "teleportDropsToPlayer", CommonConfig.TELEPORT_DROPS_TO_PLAYER);
        addBooleanConfigOption(builder, "ignoreToolCompatibility", CommonConfig.IGNORE_TOOL_COMPATIBILITY);
        addBooleanConfigOption(builder, "matchBlockState", CommonConfig.MATCH_BLOCK_STATE);
        addBooleanConfigOption(builder, "enableInCreative", CommonConfig.ENABLE_IN_CREATIVE);
        addBooleanConfigOption(builder, "requireSneaking", CommonConfig.REQUIRE_SNEAKING);
        addBooleanConfigOption(builder,"matchseedstate",CommonConfig.matchseedBlockState);
        
        return builder;
    }
    
    /**
     * 为布尔值配置项添加命令
     */
    private static void addBooleanConfigOption(LiteralArgumentBuilder<CommandSourceStack> builder,
                                              String optionName,
                                               net.minecraftforge.common.ForgeConfigSpec.BooleanValue configValue) {
        builder.then(Commands.literal(optionName)
            .executes(ctx -> showCommonConfigValue(ctx, optionName, configValue.get()))
            .then(Commands.argument("value", BoolArgumentType.bool())
                .executes(ctx -> setCommonConfigBoolValue(ctx, optionName, 
                    configValue, 
                    BoolArgumentType.getBool(ctx, "value")))));
    }
    
    /**
     * 显示通用配置值
     */
    private static int showCommonConfigValue(CommandContext<CommandSourceStack> context, 
                                           String name, 
                                           Object value) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.sendSystemMessage(Component.translatable("command.onekeyminer.config.value", name, value));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 设置整数配置值
     */
    private static int setCommonConfigIntValue(CommandContext<CommandSourceStack> context, 
                                             String name,
                                               net.minecraftforge.common.ForgeConfigSpec.IntValue configValue,
                                             int newValue) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        configValue.set(newValue);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        player.sendSystemMessage(Component.translatable("command.onekeyminer.config.set.int", name, newValue));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 设置布尔值配置值
     */
    private static int setCommonConfigBoolValue(CommandContext<CommandSourceStack> context, 
                                              String name,
                                                net.minecraftforge.common.ForgeConfigSpec.BooleanValue configValue,
                                              boolean newValue) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        configValue.set(newValue);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        String status = newValue ? 
            Component.translatable("command.onekeyminer.enabled").getString() : 
            Component.translatable("command.onekeyminer.disabled").getString();
        player.sendSystemMessage(Component.translatable("command.onekeyminer.config.set.bool", name, status));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 列出不可连锁挖掘的方块
     */
    private static int listNonChainableBlocks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<? extends String> blocks = CommonConfig.NON_CHAINABLE_BLOCKS.get();
        
        player.sendSystemMessage(Component.translatable("command.onekeyminer.blacklist.blocks.title"));
        if (blocks.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.onekeyminer.list.empty"));
        } else {
            for (String block : blocks) {
                player.sendSystemMessage(Component.translatable("command.onekeyminer.list.item", block));
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 添加方块到不可连锁挖掘列表
     */
    private static int addToNonChainableBlocks(CommandContext<CommandSourceStack> context, String blockId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> blocks = new ArrayList<>(CommonConfig.NON_CHAINABLE_BLOCKS.get());
        
        // 如果没有冒号，添加默认命名空间
        if (!blockId.contains(":")) {
            blockId = "minecraft:" + blockId;
        }
        
        if (blocks.contains(blockId)) {
            player.sendSystemMessage(Component.translatable("command.onekeyminer.blacklist.block.exists", blockId));
            return 0;
        }
        
        blocks.add(blockId);
        CommonConfig.NON_CHAINABLE_BLOCKS.set(blocks);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        
        player.sendSystemMessage(Component.translatable("command.onekeyminer.blacklist.block.added", blockId));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 从不可连锁挖掘列表中移除方块
     */
    private static int removeFromNonChainableBlocks(CommandContext<CommandSourceStack> context, String blockId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> blocks = new ArrayList<>(CommonConfig.NON_CHAINABLE_BLOCKS.get());
        
        // 如果没有冒号，添加默认命名空间
        if (!blockId.contains(":")) {
            blockId = "minecraft:" + blockId;
        }
        
        if (!blocks.contains(blockId)) {
            player.sendSystemMessage(Component.translatable("command.onekeyminer.blacklist.block.not_exists", blockId));
            return 0;
        }
        
        blocks.remove(blockId);
        CommonConfig.NON_CHAINABLE_BLOCKS.set(blocks);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        
        player.sendSystemMessage(Component.translatable("command.onekeyminer.blacklist.block.removed", blockId));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 列出不可连锁种植的种子
     */
    private static int listSeedBlacklist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<? extends String> seeds = CommonConfig.SEED_BLACKLIST.get();
        
        player.sendSystemMessage(Component.literal("§e不可连锁种植的种子列表："));
        if (seeds.isEmpty()) {
            player.sendSystemMessage(Component.literal("  §7(列表为空)"));
        } else {
            for (String seed : seeds) {
                player.sendSystemMessage(Component.literal("  §7- §r" + seed));
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 添加种子到不可连锁种植列表
     */
    private static int addToSeedBlacklist(CommandContext<CommandSourceStack> context, String seedId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> seeds = new ArrayList<>(CommonConfig.SEED_BLACKLIST.get());
        
        // 如果没有冒号，添加默认命名空间
        if (!seedId.contains(":")) {
            seedId = "minecraft:" + seedId;
        }
        
        if (seeds.contains(seedId)) {
            player.sendSystemMessage(Component.literal("§c种子 " + seedId + " 已在黑名单中"));
            return 0;
        }
        
        seeds.add(seedId);
        CommonConfig.SEED_BLACKLIST.set(seeds);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        
        player.sendSystemMessage(Component.literal("§a已将 " + seedId + " 添加到不可连锁种植列表"));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 从不可连锁种植列表中移除种子
     */
    private static int removeFromSeedBlacklist(CommandContext<CommandSourceStack> context, String seedId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> seeds = new ArrayList<>(CommonConfig.SEED_BLACKLIST.get());
        
        // 如果没有冒号，添加默认命名空间
        if (!seedId.contains(":")) {
            seedId = "minecraft:" + seedId;
        }
        
        if (!seeds.contains(seedId)) {
            player.sendSystemMessage(Component.literal("§c种子 " + seedId + " 不在黑名单中"));
            return 0;
        }
        
        seeds.remove(seedId);
        CommonConfig.SEED_BLACKLIST.set(seeds);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        
        player.sendSystemMessage(Component.literal("§a已从不可连锁种植列表中移除 " + seedId));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 注册客户端配置命令
     */
    private static LiteralArgumentBuilder<CommandSourceStack> registerClientConfigCommands() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("options");
        
        // 是否显示方块数量
        addBooleanConfigOption(builder, "showBlockCount", ClientConfig.SHOW_BLOCK_COUNT);
        
        // 消息样式
        builder.then(Commands.literal("messageStyle")
            .executes(ctx -> showConfigValue(ctx, "messageStyle", ClientConfig.MESSAGE_STYLE.get()))
            .then(Commands.argument("style", StringArgumentType.word())
                .suggests((context, suggestionBuilder) -> {
                    for (String style : Arrays.asList("chat", "actionbar", "both", "none")) {
                        if (style.startsWith(suggestionBuilder.getRemaining().toLowerCase())) {
                            suggestionBuilder.suggest(style);
                        }
                    }
                    return suggestionBuilder.buildFuture();
                })
                .executes(ctx -> {
                    String style = StringArgumentType.getString(ctx, "style");
                    if (Arrays.asList("chat", "actionbar", "both", "none").contains(style)) {
                        ClientConfig.MESSAGE_STYLE.set(style);
                        ConfigUtils.saveConfig(ClientConfig.SPEC);
                        ctx.getSource().getPlayerOrException().sendSystemMessage(
                            Component.translatable("command.onekeyminer.config.messagestyle.set", style));
                        return Command.SINGLE_SUCCESS;
                    } else {
                        ctx.getSource().getPlayerOrException().sendSystemMessage(
                            Component.translatable("command.onekeyminer.invalid_option"));
                        return 0;
                    }
                })));
        
        // 按键模式
        builder.then(Commands.literal("keyMode")
            .executes(ctx -> showConfigValue(ctx, "keyMode", ClientConfig.REQUIRE_KEY_HOLD.get()))
            .then(Commands.argument("mode", StringArgumentType.word())
                .suggests((context, suggestionBuilder) -> {
                    for (String mode : Arrays.asList("toggle", "hold")) {
                        if (mode.startsWith(suggestionBuilder.getRemaining().toLowerCase())) {
                            suggestionBuilder.suggest(mode);
                        }
                    }
                    return suggestionBuilder.buildFuture();
                })
                .executes(ctx -> {
                    String mode = StringArgumentType.getString(ctx, "mode");
                    if (Arrays.asList("toggle", "hold").contains(mode)) {
                        ClientConfig.REQUIRE_KEY_HOLD.set(mode.equals("hold"));
                        ConfigUtils.saveConfig(ClientConfig.SPEC);
                        ctx.getSource().getPlayerOrException().sendSystemMessage(
                            Component.translatable("command.onekeyminer.config.keyhold.set", mode));
                        return Command.SINGLE_SUCCESS;
                    } else {
                        ctx.getSource().getPlayerOrException().sendSystemMessage(
                            Component.translatable("command.onekeyminer.invalid_option"));
                        return 0;
                    }
                })));
         builder.then(Commands.literal("Debug")
            .executes(ctx -> showConfigValue(ctx, "Debug", ClientConfig.DEBUG.get()))
            .then(Commands.argument("value", BoolArgumentType.bool())
                .executes(ctx -> setCommonConfigBoolValue(ctx, "Debug", 
                    ClientConfig.DEBUG,
                    BoolArgumentType.getBool(ctx, "value")))));
        builder.then(Commands.literal("frozen timer")
            .executes(ctx -> showConfigValue(ctx, "frozen timer", ClientConfig.FROZEN_TIMER.get()))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 32767))
                .executes(ctx -> setCommonConfigIntValue(ctx, "frozen timer", 
                    ClientConfig.FROZEN_TIMER,
                    IntegerArgumentType.getInteger(ctx, "value")))));
        return builder;
    }

    /**
     * 注册服务器配置命令
     */
    private static LiteralArgumentBuilder<CommandSourceStack> registerServerConfigCommands() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("options");
        
        // 工具耐久阈值
        builder.then(Commands.literal("toolDurabilityThreshold")
            .executes(ctx -> showConfigValue(ctx, "toolDurabilityThreshold", ServerConfig.TOOL_DURABILITY_THRESHOLD.get()))
            .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 1000))
                .executes(ctx -> {
                    double value = DoubleArgumentType.getDouble(ctx, "value");
                    ServerConfig.TOOL_DURABILITY_THRESHOLD.set(value);
                    ConfigUtils.saveConfig(ServerConfig.SPEC);
                    ctx.getSource().getPlayerOrException().sendSystemMessage(
                        Component.translatable("command.onekeyminer.config.tool_durability.set", value));
                    return Command.SINGLE_SUCCESS;
                })));
        
        // 饥饿度阈值
        builder.then(Commands.literal("hungerThreshold")
            .executes(ctx -> showConfigValue(ctx, "hungerThreshold", ServerConfig.HUNGER_THRESHOLD.get()))
            .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 20))
                .executes(ctx -> {
                    double value = DoubleArgumentType.getDouble(ctx, "value");
                    ServerConfig.HUNGER_THRESHOLD.set(value);
                    ConfigUtils.saveConfig(ServerConfig.SPEC);
                    ctx.getSource().getPlayerOrException().sendSystemMessage(
                        Component.translatable("command.onekeyminer.config.hunger.set", value));
                    return Command.SINGLE_SUCCESS;
                })));
        
        return builder;
    }
    
    /**
     * 显示配置值（通用方法）
     */
    private static int showConfigValue(CommandContext<CommandSourceStack> context, 
                                     String name, 
                                     Object value) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.sendSystemMessage(Component.translatable("command.onekeyminer.config.value", name, value));
        return Command.SINGLE_SUCCESS;
    }
} 