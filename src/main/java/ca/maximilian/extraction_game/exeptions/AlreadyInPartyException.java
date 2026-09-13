package ca.maximilian.extraction_game.exeptions;

import net.kyori.adventure.text.Component;

public class AlreadyInPartyException extends PartyException {
    public AlreadyInPartyException(Component component) {
        super(component);
    }
}