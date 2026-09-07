package ca.maximilian.extraction_game.core.handlers.loot;

import net.minestom.server.item.ItemStack;

public record LootEntry(ItemStack itemStack, int weight, int minAmount, int maxAmount) { }
