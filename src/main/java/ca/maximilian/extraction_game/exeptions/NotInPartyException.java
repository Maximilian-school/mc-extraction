package ca.maximilian.extraction_game.exeptions;

import net.kyori.adventure.text.Component;

public class NotInPartyException extends PartyException {
    public NotInPartyException(Component component) {
        super(component);
    }
}