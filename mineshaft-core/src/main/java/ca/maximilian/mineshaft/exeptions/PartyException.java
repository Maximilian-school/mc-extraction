package ca.maximilian.mineshaft.exeptions;

import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Base for all party related exceptions. Holds the message as an instance field
 * (the old subclasses used a static field, meaning every exception of that type
 * shared one component and could stomp on each other under concurrent commands).
 */
public abstract class PartyException extends RuntimeException {

    @Getter
    private final Component component;

    protected PartyException(Component component) {
        super(component.toString());
        this.component = component;
    }
}