package ca.maximilian.mineshaft.core.gui;

import eu.koboo.minestom.stomui.api.ViewRegistry;
import eu.koboo.minestom.stomui.core.MinestomUI;
import lombok.Getter;

public class Gui {

    @Getter
    private final ViewRegistry viewRegistry;

    public Gui() {
        this.viewRegistry = MinestomUI.create();

        viewRegistry.enable();
    }
}
