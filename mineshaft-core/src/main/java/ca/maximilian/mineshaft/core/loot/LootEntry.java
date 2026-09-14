package ca.maximilian.mineshaft.core.loot;

import net.minestom.server.item.ItemStack;

public record LootEntry(ItemStack itemStack, int weight, int minAmount, int maxAmount) { }
