package ca.maximilian.extraction_game.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class ItemPrices {

    private record Price(double price, double sellValue) {}

    private static final Map<Material, Price> PRICES = Map.of(
            Material.STRING, new Price(-1, 1),
            Material.ROTTEN_FLESH, new Price(-1, 3),
            Material.COAL, new Price(20, 10),
            Material.IRON_INGOT, new Price(50, 35)
    );

    private ItemPrices() {}

    public static double price(ItemStack itemStack) {
        double glowingMultiplier = Boolean.TRUE.equals(itemStack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) ? 1.3 : 1;

        return price(itemStack.material()) * itemStack.amount() * glowingMultiplier;
    }

    public static double sellValue(ItemStack itemStack) {
        double glowingMultiplier = Boolean.TRUE.equals(itemStack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) ? 2 : 1;

        return sellValue(itemStack.material()) * itemStack.amount() * glowingMultiplier;
    }

    public static ItemStack withPrices(ItemStack itemStack) {
        Component buyPriceComponent = Component.text()
                .content("Buy: $%s".formatted(price(itemStack)))
                .style(Style.style()
                        .decoration(TextDecoration.ITALIC, false)
                        .color(NamedTextColor.GREEN)
                        .build())
                .build();

        if (price(itemStack) < 0) {
            buyPriceComponent = Component.text("Unpurchaseable").style(Style.style().color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }

        Component sellValueComponent = Component.text()
                .content("Sell: $%s".formatted(sellValue(itemStack)))
                .style(Style.style()
                        .decoration(TextDecoration.ITALIC, false)
                        .color(NamedTextColor.GOLD)
                        .build())
                .build();

        List<Component> lore = List.of(buyPriceComponent, sellValueComponent);

        return itemStack.withLore(lore);
    }

    private static double price(Material material) {
        return get(material).price();
    }

    private static double sellValue(Material material) {
        return get(material).sellValue();
    }

    private static Price get(Material material) {
        return PRICES.getOrDefault(material, new Price(0.0, 0.0));
    }
}