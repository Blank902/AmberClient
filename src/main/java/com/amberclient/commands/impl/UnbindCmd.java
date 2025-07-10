package com.amberclient.commands.impl;

import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ModuleManager;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UnbindCmd {
    public static final String MOD_ID = "amberclient-unbindcmd";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static int execute(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerCommandSource source = ctx.getSource();
            if (!source.isExecutedByPlayer()) {
                source.sendError(Text.literal("This command can only be executed by a player."));
                return 0;
            }

            source.getPlayerOrThrow();
            String moduleName = ctx.getArgument("module", String.class);

            return unbindModule(moduleName,
                    message -> source.sendFeedback(() -> message, false),
                    source::sendError
            );

        } catch (Exception e) {
            LOGGER.error("Error unbinding module: {}", e.getMessage(), e);
            ctx.getSource().sendError(Text.literal("Error unbinding module: " + e.getMessage()));
            return 0;
        }
    }

    public static int executeClient(CommandContext<FabricClientCommandSource> ctx) {
        try {
            String moduleName = ctx.getArgument("module", String.class);
            MinecraftClient client = MinecraftClient.getInstance();

            return unbindModule(moduleName,
                    message -> {
                        assert client.player != null;
                        client.player.sendMessage(message, false);
                    },
                    error -> {
                        assert client.player != null;
                        client.player.sendMessage(error, false);
                    }
            );

        } catch (Exception e) {
            LOGGER.error("Error unbinding module: {}", e.getMessage(), e);
            assert MinecraftClient.getInstance().player != null;
            MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("Error unbinding module: " + e.getMessage()), false
            );
            return 0;
        }
    }

    private static int unbindModule(String moduleName,
                                    java.util.function.Consumer<Text> sendFeedback,
                                    java.util.function.Consumer<Text> sendError) {

        ModuleManager moduleManager = ModuleManager.getInstance();
        Module module = moduleManager.findModuleByName(moduleName);

        if (module == null) {
            sendError.accept(Text.literal("Module '" + moduleName + "' not found."));
            return 0;
        }

        String currentKey = moduleManager.getModuleKeyName(module);
        if (currentKey.equals("Not bound")) {
            sendFeedback.accept(
                    Text.literal("§4[§cAmberClient§4] §cModule §4'" + moduleName + "' §cis not bound to any key.")
            );
        } else {
            moduleManager.unbindModule(module);
            sendFeedback.accept(
                    Text.literal("§4[§cAmberClient§4] §cUnbound module §4'" + moduleName + "' §cfrom key §4'" + currentKey + "'.")
            );
        }
        return 1;
    }
}