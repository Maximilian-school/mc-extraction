package ca.maximilian.mineshaft.command;

import ca.maximilian.mineshaft.core.RunningGame;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;

public class KillAllCommand extends Command {

    public KillAllCommand() {
        super("killall");

        setDefaultExecutor((sender, commandContext) -> {
            if (!(sender instanceof Player player)) return;

            RunningGame runningGame = player.getInstance().getTag(RunningGame.TAG);

            if (runningGame == null) return;

            for (Entity entity : player.getInstance().getEntities()) {
                if (!(entity instanceof LivingEntity livingEntity)) continue;
                if (entity instanceof Player) continue;
                if ((entity.getCustomName() != null ? entity.getCustomName() : "").toString().contains("keeper")) continue;
                livingEntity.kill();
            }
        });
    }
}
