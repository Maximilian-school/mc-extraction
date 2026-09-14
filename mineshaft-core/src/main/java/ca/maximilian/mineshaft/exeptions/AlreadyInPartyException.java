package ca.maximilian.mineshaft.exeptions;

import net.kyori.adventure.text.Component;

public class AlreadyInPartyException extends PartyException {
    public AlreadyInPartyException(Component component) {
        super(component);
    }
}