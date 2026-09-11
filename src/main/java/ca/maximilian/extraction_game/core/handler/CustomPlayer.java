package ca.maximilian.extraction_game.core.handler;

import lombok.Getter;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

public class CustomPlayer extends Player {

    @Getter
    private double money = 0;

    public CustomPlayer(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    public void increaseMoney(double money) {
        this.money += money;
    }
    public void increaseMoney() {
        this.increaseMoney(1);
    }
    public void decreaseMoney(double money) {
        this.money -= money;
    }
    public void decreaseMoney() {
        this.decreaseMoney(1);
    }
    public void resetMoney() {
        this.money = 0;
    }
}