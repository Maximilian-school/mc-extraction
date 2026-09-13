package ca.maximilian.extraction_game.command;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class GameModeCommand extends Command {

    private enum GameModeHuman {
        creative,
        survival,
        adventure,
        spectator
    }

    public GameModeCommand() {
        super("gamemode", "gm");

        var gameModeArg = ArgumentType.Enum("gamemode", GameModeHuman.class);

        addSyntax((sender, context) -> {
            final GameModeHuman gameModeHuman = context.get(gameModeArg);

            GameMode gameMode = switch (gameModeHuman) {
                case creative -> GameMode.CREATIVE;
                case survival -> GameMode.SURVIVAL;
                case adventure -> GameMode.ADVENTURE;
                case spectator -> GameMode.SPECTATOR;
            };

            if (sender instanceof Player player) {
                player.setGameMode(gameMode);
            }

        }, gameModeArg);

        setCondition((sender, commandString) -> {
            if (sender instanceof Player player) {
                return player.getPermissionLevel() >= 0;
            }
            return true;
        });
    }
}
