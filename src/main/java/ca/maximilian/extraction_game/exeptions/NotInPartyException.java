package ca.maximilian.extraction_game.exeptions;

import lombok.Getter;

import net.kyori.adventure.text.Component;

public class NotInPartyException extends RuntimeException {

    @Getter
    private static Component component;

    public NotInPartyException(Component component) {
        super(component.toString());
        NotInPartyException.component = component;
    }
}
