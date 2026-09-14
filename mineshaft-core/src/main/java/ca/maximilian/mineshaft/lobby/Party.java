package ca.maximilian.mineshaft.lobby;

import ca.maximilian.mineshaft.Constants;
import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.core.RunningGame;
import ca.maximilian.mineshaft.exeptions.NotInPartyException;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Party {

    @Getter
    private ExtractionPlayer host;

    @Getter
    private RunningGame runningGame;

    @Getter
    private List<ExtractionPlayer> kickedPlayers = new ArrayList<>();

    @Getter
    private List<Invite> invites = new ArrayList<>();

    @Getter
    private final List<ExtractionPlayer> players = new ArrayList<>();

    @Getter
    private final @Nullable String password;

    @Getter
    @Setter
    private int maximumPlayers;

    @Getter
    private Sidebar sidebar = new Sidebar(Component.text("Mineshaft"));

    public Party(ExtractionPlayer host, @Nullable String password, int maximumPlayers) throws IllegalStateException {
        if (Matchmaking.getPartyWithPlayer(host) != null) {
            throw new IllegalStateException("You are already in a party!");
        }

        this.host = host;
        this.password = password;
        this.maximumPlayers = maximumPlayers;

        this.players.add(host);

        sidebar.addViewer(host);
        updateSidebar();
    }

    public void updateSidebar() {
        for (Sidebar.ScoreboardLine line : sidebar.getLines()) {
            sidebar.removeLine(line.getId());
        }

        int playerIndex = players.size();

        Stream<ExtractionPlayer> playersSorted = players.stream().sorted((a, b) -> Double.compare(b.getMoney(), a.getMoney()));

        for (ExtractionPlayer player : playersSorted.toList()) {
            int lineScore = playerIndex--;

            sidebar.createLine(new Sidebar.ScoreboardLine(
                    player.getUsername(),
                    player.getName().append(Component.text(" $" + player.getMoney())),
                    lineScore,
                    Sidebar.NumberFormat.blank()
            ));
        }
    }

    public boolean isPublic() {
        return password == null;
    }

    public boolean isRunning() {
        return this.runningGame != null;
    }

    public void setCrashed() {
        if (this.runningGame != null) {
            this.runningGame = null;
        }
    }

    public boolean isFull() {
        return players.size() == maximumPlayers;
    }

    public enum JoinStatus {
        FULL,
        ALREADY_IN_LOBBY,
        INCORRECT_PASSWORD,
        INVITE_EXPIRED,
        NOT_ALLOWED,
        PLAYING,

        JOINED,
    }

    public Invite invitePlayer(ExtractionPlayer player) {
        Invite invite = new Invite(player);

        invites.add(invite);

        return invite;
    }

    public JoinStatus addPlayer(ExtractionPlayer player, @Nullable String password) {
        if (isRunning()) {
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
            @Nullable Invite playersInvite = null;

            for (Invite invite : this.invites) {
                if (invite.getPlayer().equals(player)) {
                    playersInvite = invite;
                    break;
                }
            }

            if (playersInvite != null) {

                if (playersInvite.expired()) {
                    player.sendMessage(Component.text("That invite has expired!").color(NamedTextColor.RED));
                    player.playSound(Constants.ERROR_SOUND);
                    return JoinStatus.INVITE_EXPIRED;
                }

                if (playersInvite.used()) {
                    player.sendMessage(Component.text("That invite has been used!").color(NamedTextColor.RED));
                    player.playSound(Constants.ERROR_SOUND);
                    return JoinStatus.INVITE_EXPIRED;
                }

                playersInvite.use();

            } else {
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

        sidebar.addViewer(player);

        updateSidebar();

        return JoinStatus.JOINED;
    }

    public boolean removePlayer(ExtractionPlayer player, boolean kick) throws NotInPartyException {
        if (!players.contains(player)) {
            throw new NotInPartyException(Component.text("You are not in this party!"));
        }

        if (kick) {
            getKickedPlayers().add(player);
        } else if (isRunning()) {
            player.sendMessage(Component.text("You cannot leave the party while in match!").color(NamedTextColor.RED));
            player.playSound(Constants.ERROR_SOUND);
            return false;
        }

        players.remove(player);

        if (players.isEmpty()) {
            Matchmaking.removeParty(this);
            return true;
        }

        Component playerDisplayName = (player.getDisplayName() != null)
                ? player.getDisplayName()
                : player.getName();

        if (getHost() == player) {
            this.host = getPlayers().getFirst();

            Component newHostDisplayName = (host.getDisplayName() != null)
                    ? host.getDisplayName()
                    : host.getName();

            for (ExtractionPlayer p : players) {
                if (p == getHost()) {
                    p.sendMessage(Component.text("Since ").append(playerDisplayName).append(Component.text(" left, you are now the host!")));
                } else {
                    p.sendMessage(Component.text("Since ").append(playerDisplayName).append(Component.text(" left, ")).append(newHostDisplayName).append(Component.text(" is the new host!")));
                }
            }
        }

        getAudience().sendMessage(playerDisplayName.append(Component.text(" left the party! (%s/%s)".formatted(
                getPlayers().size(), maximumPlayers
        ))).color(NamedTextColor.YELLOW));
        getAudience().playSound(Constants.SUCCESS_SOUND);

        // runningGame can be null here (e.g. kicking someone from a party that never started),
        // so guard it instead of blindly calling .getInstance() on it like before.
        if (kick && isRunning() && player.getInstance() == this.runningGame.getInstance()) {
            player.kick(Component.text("You have been kicked from this party!"));
        }

        sidebar.removeLine(player.getUsername());

        sidebar.removeViewer(player);

        return true;
    }

    public Audience getAudience() {
        return Audience.audience(getPlayers());
    }

    public void start() {
        if (isRunning()) return;
        this.runningGame = new RunningGame(this);
    }

    /**
     * If needed, the party can run code on every tick.
     * When possible, use events.
     */
    public void tickParty() {
        if (isRunning()) {
            if (!this.invites.isEmpty()) {
                this.invites.clear();
            }
        }

        if (players.isEmpty()) {
            Matchmaking.removeParty(this);
        }
    }
}