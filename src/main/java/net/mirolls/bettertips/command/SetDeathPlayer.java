package net.mirolls.bettertips.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.mirolls.bettertips.death.DeathConfig;
import net.mirolls.bettertips.death.DeathConfigYaml;
import net.mirolls.bettertips.death.DeathMessage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.mirolls.bettertips.BetterTips.LOGGER;
import static net.mirolls.bettertips.command.file.Comment.readComments;
import static net.mirolls.bettertips.command.file.Comment.writeComments;
import static net.mirolls.bettertips.command.file.Verifier.isValidDeathYamlKey;

public class SetDeathPlayer {
    public static void registerCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("setDeath")
                // 任何人都可以调用完全不需要所谓都require
                .then(literal("message")
                        .then(argument("deathID", StringArgumentType.string()) // 理论上来说，ID是String类型的
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(SetDeathPlayer::setMessageHandle))))
                .then(literal("color")
                        .then(argument("deathID", StringArgumentType.string())
                                .then(argument("color", StringArgumentType.greedyString())
                                        .executes(SetDeathPlayer::setColorHandle))))));
    }

    // 创建这个方法的目的是缩进太难受了要爆炸了
    private static int setMessageHandle(CommandContext<ServerCommandSource> context) {
        String deathID = StringArgumentType.getString(context, "deathID");
        String message = StringArgumentType.getString(context, "message");
        String player = Objects.requireNonNull(context.getSource().getPlayer()).getName().getString(); // 好长一表达式

        // 先进行一个校验，防止整个配置文件崩坏
        if (isValidDeathYamlKey(deathID)) {
            try {
                final String CONFIG_FILE_PATH = "bettertips/death.config.yaml";
                ObjectMapper mapper = DeathConfig.getConfigMapper();
                File configYaml = new File(CONFIG_FILE_PATH);
                String comments = readComments(CONFIG_FILE_PATH);
                DeathConfigYaml config = mapper.readValue(configYaml, DeathConfigYaml.class);

                config = updatePlayerMessage(config, player, deathID, message);

                mapper.writeValue(configYaml, config);
                // 写入注释
                writeComments(comments + System.lineSeparator(), CONFIG_FILE_PATH);

                context.getSource().sendFeedback(() -> Text.literal("写入" + deathID + "消息成功"), false);
            } catch (IOException e) {
                context.getSource().sendFeedback(() -> Text.translatable("command.failed"), false);
                LOGGER.error("[BetterTips]: Cannot get the death config");
                return 0;
            }
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.translatable("command.unknown.argument"), false);
            return 0;
        }
    }

    private static int setColorHandle(CommandContext<ServerCommandSource> context) {
        String deathID = StringArgumentType.getString(context, "deathID");
        String color = StringArgumentType.getString(context, "color");
        String player = Objects.requireNonNull(context.getSource().getPlayer()).getName().getString(); // 好长一表达式


        // 先进行一个校验，防止整个配置文件崩坏
        if (isValidDeathYamlKey(deathID)) {
            try {
                final String CONFIG_FILE_PATH = "bettertips/death.config.yaml";
                ObjectMapper mapper = DeathConfig.getConfigMapper();
                File configYaml = new File(CONFIG_FILE_PATH);
                String comments = readComments(CONFIG_FILE_PATH);
                DeathConfigYaml config = mapper.readValue(configYaml, DeathConfigYaml.class);

                config = updatePlayerColor(config, player, deathID, color);

                mapper.writeValue(configYaml, config);
                // 写入注释
                writeComments(comments + System.lineSeparator(), CONFIG_FILE_PATH);

                context.getSource().sendFeedback(() -> Text.literal("写入" + deathID + "颜色成功"), false);
            } catch (IOException e) {
                context.getSource().sendFeedback(() -> Text.translatable("command.failed"), false);
                LOGGER.error("[BetterTips]: Cannot get the death config");
                return 0;
            }
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.translatable("command.unknown.argument"), false);
            return 0;
        }
    }


    public static DeathConfigYaml updatePlayerMessage(DeathConfigYaml config, String playerName, String key, String newMessage) {
        DeathConfigYaml updatedConfig = new DeathConfigYaml();
        updatedConfig.setGlobal(new HashMap<>(config.getGlobal())); // 复制原始 global map
        updatedConfig.setPlayer(new HashMap<>(config.getPlayer())); // 复制原始 player map

        Map<String, Map<String, DeathMessage>> player = updatedConfig.getPlayer();

        Map<String, DeathMessage> playerMessages;
        if (player.containsKey(playerName)) {
            playerMessages = player.get(playerName);
        } else {
            // 如果玩家名称不存在，创建一个新的玩家消息映射
            playerMessages = new HashMap<>();
            player.put(playerName, playerMessages);
        }

        DeathMessage deathMessage;
        if (playerMessages.containsKey(key)) {
            deathMessage = playerMessages.get(key);
        } else {
            // 如果 key 不存在，创建一个新的 DeathMessage 对象并添加到玩家消息映射中
            deathMessage = new DeathMessage();
        }
        deathMessage.setMessage(newMessage);
        playerMessages.put(key, deathMessage);

        return updatedConfig;
    }

    public static DeathConfigYaml updatePlayerColor(DeathConfigYaml config, String playerName, String key, String newColor) {
        DeathConfigYaml updatedConfig = new DeathConfigYaml();
        updatedConfig.setGlobal(new HashMap<>(config.getGlobal())); // 复制原始 global map
        updatedConfig.setPlayer(new HashMap<>(config.getPlayer())); // 复制原始 player map

        Map<String, Map<String, DeathMessage>> player = updatedConfig.getPlayer();

        Map<String, DeathMessage> playerMessages;
        if (player.containsKey(playerName)) {
            playerMessages = player.get(playerName);
        } else {
            // 如果玩家名称不存在，创建一个新的玩家消息映射
            playerMessages = new HashMap<>();
            player.put(playerName, playerMessages);
        }

        DeathMessage deathMessage;
        if (playerMessages.containsKey(key)) {
            deathMessage = playerMessages.get(key);
        } else {
            // 如果 key 不存在，创建一个新的 DeathMessage 对象并添加到玩家消息映射中
            deathMessage = new DeathMessage();
        }
        deathMessage.setColor(newColor);
        playerMessages.put(key, deathMessage);

        return updatedConfig;
    }
}
