package ca.maximilian.extraction_game.lobby;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.RunningGame;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.NotInPartyException;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;

public class Party {

    @Getter
    private CustomPlayer host;

    @Getter
    private RunningGame runningGame;

    @Getter
    private List<CustomPlayer> kickedPlayers = new ArrayList<>();

    @Getter
    private List<TemporaryPassword> temporaryPasswords = new ArrayList<>();

    @Getter
    private final List<CustomPlayer> players = new ArrayList<>();

    @Getter
    private final @Nullable String password;

    @Getter
    @Setter
    private int maximumPlayers;

    public Party(CustomPlayer host, @Nullable String password, int maximumPlayers) throws IllegalStateException {
        if (Matchmaking.getPartyWithPlayer(host) != null) {
            throw new IllegalStateException("You are already in a party!");
        }

        this.host = host;
        this.password = password;
        this.maximumPlayers = maximumPlayers;

        this.players.add(host);
    }

    public boolean isPublic() {
        return password == null;
    }

    public boolean isFull() {
        return players.size() == maximumPlayers;
    }

    public enum JoinStatus {
        FULL,
        ALREADY_IN_LOBBY,
        INCORRECT_PASSWORD,
        NOT_ALLOWED,
        PLAYING,

        JOINED,
    }

    public TemporaryPassword addTemporaryPassword() {
        TemporaryPassword temporaryPassword = new TemporaryPassword();

        temporaryPasswords.add(temporaryPassword);

        return temporaryPassword;
    }

    public JoinStatus addPlayer(CustomPlayer player, @Nullable String password) {
        if (this.runningGame != null) {
            player.sendMessage(Component.text("Party is already playing!").color(NamedTextColor.RED));
            player.playSound(Constants.ERROR_SOUND);
            return JoinStatus.PLAYING;
        }

        if (isFull()) {
            player.sendMessage(Component.text("Party is full!").color(NamedTextColor.RED));
            player.playSound(Constants.ERROR_SOUND);
            return JoinStatus.FULL;
        }

        if (Matchmaking.getPartyWithPlayer(player) != null) {
            player.sendMessage(Component.text("You are already in a party!").color(NamedTextColor.RED));
            player.playSound(Constants.ERROR_SOUND);
            return JoinStatus.ALREADY_IN_LOBBY;
        }

        if (this.password != null && !this.password.equals(password)) {
            boolean wasTemporary = false;

            Iterator<TemporaryPassword> iterator = this.temporaryPasswords.iterator();
            while (iterator.hasNext()) {
                TemporaryPassword temporaryPassword = iterator.next();

                if (temporaryPassword.getPassword().equals(password) && temporaryPassword.isValid()) {
                    iterator.remove();
                    wasTemporary = true;
                    break;
                } else if (!temporaryPassword.isValid()) {
                    player.sendMessage(Component.text("Your invite has been invalidated, likely you waited too long!").color(NamedTextColor.RED));
                    player.playSound(Constants.ERROR_SOUND);
                    return JoinStatus.INCORRECT_PASSWORD;
                }
            }

            if (!wasTemporary) {
                player.sendMessage(Component.text("That is not the correct password!").color(NamedTextColor.RED));
                player.playSound(Constants.ERROR_SOUND);
                return JoinStatus.INCORRECT_PASSWORD;
            }
        }

        if (getKickedPlayers().contains(player)) {
            player.sendMessage(Component.text("You have been previously kicked from this party!").color(NamedTextColor.RED));
            player.playSound(Constants.ERROR_SOUND);
            return JoinStatus.NOT_ALLOWED;
        }

        Component playerDisplayName = (player.getDisplayName() != null)
                ? player.getDisplayName()
                : player.getName();

        players.add(player);
        player.sendMessage(Component.text("Successfully joined the party!").color(NamedTextColor.GREEN));
        player.playSound(Constants.SUCCESS_SOUND);

        getAudience().sendMessage(playerDisplayName.append(Component.text(" joined the party! (%s/%s)".formatted(
                getPlayers().size(), maximumPlayers
        ))).color(NamedTextColor.YELLOW));
        getAudience().playSound(Constants.SUCCESS_SOUND);

        return JoinStatus.JOINED;
    }

    public void removePlayer(CustomPlayer player, boolean kick) throws NotInPartyException {
        if (!players.contains(player)) {
            throw new NotInPartyException(Component.text("You are not in this party!"));
        }

        if (kick) {
            getKickedPlayers().add(player);
        } else {
            if (this.runningGame != null) {
                player.sendMessage(Component.text("You cannot leave the party while in match!").color(NamedTextColor.RED));
                player.playSound(Constants.ERROR_SOUND);
                return;
            }
        }

        players.remove(player);

        if (players.isEmpty()) {
            Matchmaking.removeParty(this);

            return;
        }

        Component playerDisplayName = (player.getDisplayName() != null)
                ? player.getDisplayName()
                : player.getName();

        Component oldHostDisplayName = (host.getDisplayName() != null)
                ? host.getDisplayName()
                : host.getName();

        if (getHost() == player) {
            this.host = getPlayers().getFirst();

            Component newHostDisplayName = (host.getDisplayName() != null)
                    ? host.getDisplayName()
                    : host.getName();

            for (CustomPlayer p : players) {
                if (p == getHost()) {
                    p.sendMessage(Component.text("Since ").append(oldHostDisplayName).append(Component.text(" left, you are now the host!")));
                } else {
                    p.sendMessage(Component.text("Since ").append(oldHostDisplayName).append(Component.text(" left, ")).append(newHostDisplayName).append(Component.text(" is the new host!")));
                }
            }
        }

        getAudience().sendMessage(playerDisplayName.append(Component.text(" left the party! (%s/%s)".formatted(
                getPlayers().size(), maximumPlayers
        ))).color(NamedTextColor.YELLOW));
        getAudience().playSound(Constants.SUCCESS_SOUND);

        if (kick && player.getInstance() == this.runningGame.getInstance()) {
            player.kick(Component.text("You have been kicked from this party!"));
        }
    }

    public Audience getAudience() {
        return Audience.audience(getPlayers());
    }

    public void start() {
        if (this.runningGame != null) return;
        this.runningGame = new RunningGame(this);

        for (Player player : getPlayers()) {
            player.setInstance(runningGame.getInstance(), new Pos(0.5, 0, 0.5));
        }
    }
}