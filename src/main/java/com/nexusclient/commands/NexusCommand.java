package com.nexusclient.commands;

import com.nexusclient.commands.impl.DummyCmd;
import com.nexusclient.commands.impl.TopCmd;
import com.nexusclient.commands.impl.BindCmd;
import com.nexusclient.commands.impl.UnbindCmd;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class NexusCommand {
    public static void register() {
        // Register server-side commands (for single-player and server ops)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("nexus")
                            .then(CommandManager.literal("top")
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        return TopCmd.teleportToTop(source);
                                    })
                            )
                            .then(CommandManager.literal("dummy")
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        return DummyCmd.spawnDummy(source);
                                    })
                            )
                            .then(CommandManager.literal("bind")
                                    .then(CommandManager.argument("module", StringArgumentType.string())
                                            .then(CommandManager.argument("key", StringArgumentType.string())
                                                    .executes(BindCmd::execute)
                                            )
                                    )
                            )
                            .then(CommandManager.literal("unbind")
                                    .then(CommandManager.argument("module", StringArgumentType.string())
                                            .executes(UnbindCmd::execute)
                                    )
                            )
            );
        });

        // Register client-side commands (for multiplayer)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("nexus")
                            .then(ClientCommandManager.literal("bind")
                                    .then(ClientCommandManager.argument("module", StringArgumentType.string())
                                            .then(ClientCommandManager.argument("key", StringArgumentType.string())
                                                    .executes(BindCmd::executeClient)
                                            )
                                    )
                            )
                            .then(ClientCommandManager.literal("unbind")
                                    .then(ClientCommandManager.argument("module", StringArgumentType.string())
                                            .executes(UnbindCmd::executeClient)
                                    )
                            )
            );
        });
    }
}