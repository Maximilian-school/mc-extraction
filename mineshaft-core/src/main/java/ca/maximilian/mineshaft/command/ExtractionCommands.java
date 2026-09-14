package ca.maximilian.mineshaft.command;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;

public class ExtractionCommands {
    public static void init() {
        CommandManager commandManager = MinecraftServer.getCommandManager();

        commandManager.register(new PartyCommand());
        commandManager.register(new GameModeCommand());
        commandManager.register(new KillAllCommand());
    }
}
