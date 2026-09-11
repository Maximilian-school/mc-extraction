package ca.maximilian.extraction_game.exeptions;

import lombok.Getter;
import net.kyori.adventure.text.Component;

public class AlreadyInPartyException extends RuntimeException {

    @Getter
    private static Component component;

    public AlreadyInPartyException(Component component) {
        super(component.toString());
        AlreadyInPartyException.component = component;
    }
}
