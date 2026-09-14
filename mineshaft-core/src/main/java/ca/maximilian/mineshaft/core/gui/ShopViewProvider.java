package ca.maximilian.mineshaft.core.gui;

import eu.koboo.minestom.stomui.api.ViewBuilder;
import eu.koboo.minestom.stomui.api.ViewRegistry;
import eu.koboo.minestom.stomui.api.ViewType;
import eu.koboo.minestom.stomui.api.component.ViewProvider;

public class ShopViewProvider extends ViewProvider {

    public ShopViewProvider(ViewRegistry viewRegistry) {
        super(viewRegistry, ViewBuilder.of(ViewType.SIZE_6_X_9)
                .title("MyMiniMessageTitle"));
    }
}
