package ca.maximilian.extraction_game.lobby;

import lombok.Getter;

@Getter
public enum LobbyType {
    ALL("All Lobbies"),
    PRIVATE("Private Only"),
    PUBLIC("Public Only");

    private final String displayName;

    LobbyType(String displayName) {
        this.displayName = displayName;
    }

    public LobbyType getNext() {
        LobbyType[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}