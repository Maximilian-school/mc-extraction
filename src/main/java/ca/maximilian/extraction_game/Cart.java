package ca.maximilian.extraction_game;

import ca.maximilian.extraction_game.core.handlers.block.BlockHandlers;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.minecart.MinecartMeta;
import net.minestom.server.event.Event;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.inventory.*;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;

import java.util.Arrays;

public class Cart {

    private final Inventory inventory = new Inventory(InventoryType.CHEST_1_ROW, Component.text("Minecart"));

    private Player holdingPlayer;

    private Entity minecart;

    public Cart() {}

    private boolean canPlayerRelease() {
        if (minecart == null) {
            return false; // Nothing to let go of
        }

        Point pos = this.minecart.getPosition();
        Block block = this.minecart.getInstance().getBlock(pos);

        if (block.solid()) {
            return false;
        }

        return true;
    }

    public void spawnCart(Instance instance, Point position) {
        minecart = new Entity(EntityType.MINECART);

        MinecartMeta minecartMeta = (MinecartMeta) minecart.getEntityMeta();
        minecartMeta.setHasNoGravity(false);
        minecartMeta.setHasGlowingEffect(true);

        minecart.setInstance(instance, position);

        minecart.eventNode().addListener(EntityTickEvent.class, this::tick);

        instance.eventNode().addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getTarget() == this.minecart) {
                event.getPlayer().openInventory(this.inventory);
            }
        });

        instance.eventNode().addListener(PlayerDeathEvent.class, event -> {
           if (event.getPlayer() == this.getHoldingPlayer()) {
               this.setHoldingPlayer(null);
           }
        });

        instance.eventNode().addListener(EntityAttackEvent.class, event -> {
            if (event.getEntity() instanceof Player player) {
                Entity target = event.getTarget();

                boolean isCartOrPassenger = target == this.minecart ||
                        (target.getVehicle() != null && target.getVehicle() == this.minecart);

                if (isCartOrPassenger) {
                    if (this.getHoldingPlayer() == player) {
                        boolean canPlayerRelease = this.canPlayerRelease();

                        Sound sound = Sound.sound(
                                SoundEvent.BLOCK_NOTE_BLOCK_BIT,
                                Sound.Source.PLAYER,
                                1.0F,
                                0.01F
                        );

                        if (canPlayerRelease) {
                            this.setHoldingPlayer(null);
                        } else {
                            Audience.audience(this.getHoldingPlayer()).playSound(sound);
                        }
                    } else {
                        this.setHoldingPlayer(player);
                    }
                }
            }
        });

        inventory.eventNode().addListener(InventoryItemChangeEvent.class, this::updatePassengers);

        instance.eventNode().addListener(PlayerBlockBreakEvent.class, event -> {
            Player player = event.getPlayer();
            if (player == this.getHoldingPlayer()) {
                event.setCancelled(true);
            }

            BlockHandlers.breakBlockPostCart(event);
        });

        instance.eventNode().addListener(PlayerStartDiggingEvent.class, event -> {
            Player player = event.getPlayer();
            if (player == this.getHoldingPlayer()) {
                event.setCancelled(true);
            }

            BlockHandlers.startDiggingPostCart(event);
        });
    }

    public void despawnCart() throws IllegalStateException {
        if (this.minecart == null) {
            throw new IllegalStateException("Minecart is null");
        }

        this.clearItems();
        this.minecart.remove();
        this.minecart = null;
    }

    public ItemStack[] getItems() {
        return Arrays.stream(inventory.getItemStacks()).filter(itemStack -> !itemStack.isAir()).toArray(ItemStack[]::new);
    }

    private void updatePassengers() {
        for (Entity passenger : this.minecart.getPassengers()) {
            this.minecart.removePassenger(passenger);
            passenger.remove();
        }

        for (ItemStack item : this.getItems()) {
            ItemEntity itemEntity = new ItemEntity(item);
            itemEntity.setPickable(false);
            itemEntity.setHasPhysics(false);
            itemEntity.setInstance(this.minecart.getInstance(), this.minecart.getPosition());

            this.minecart.addPassenger(itemEntity);
        }
    }

    private void updatePassengers(Event event) {
        this.updatePassengers();
    }

    public boolean addItem(ItemStack itemStack) {
        if (getItems().length >= inventory.getInnerSize()) {
            return false;
        }

        inventory.addItemStack(itemStack);
        updatePassengers();

        return true;
    }

    public void clearItems() {
        inventory.clear();
        updatePassengers();
    }

    public Player getHoldingPlayer() {
        return holdingPlayer;
    }

    public void setHoldingPlayer(Player holdingPlayer) {this.holdingPlayer = holdingPlayer;}

    public void tick(Event _event) {
        if (minecart != null) {
            Player holdingPlayer = getHoldingPlayer();

            if (holdingPlayer != null) {
                Pos playerPos = holdingPlayer.getPosition().withPitch(0);
                Vec forward = playerPos.direction();
                Pos targetPos = playerPos.add(forward);

                Pos upPos = targetPos.add(0, 1.5, 0);
                Pos downPos = targetPos.add(0, -1, 0);

                Block inBlock = this.minecart.getInstance().getBlock(targetPos);
                Block upBlock = this.minecart.getInstance().getBlock(upPos);
                Block downBlock = this.minecart.getInstance().getBlock(downPos);

                int yOffset = 0;

                if (inBlock.solid()) {
                    if (!upBlock.solid()) {
                        yOffset = 1;
                    }
                } else if (!downBlock.solid()) {
                    Pos doubleDownPos = targetPos.add(0, -2, 0);
                    if (this.minecart.getInstance().getBlock(doubleDownPos).solid()) {
                        yOffset = -1;
                    }
                }

                minecart.teleport(targetPos.add(0, yOffset, 0).withYaw(playerPos.yaw() + 90));
            }

            if (minecart.getAliveTicks() % 4 != 0) {
                return;
            }

            Instance instance = minecart.getInstance();

            Point cartPos = minecart.getPosition();
            double searchRadius = 1.5;

            var nearbyEntities = instance.getNearbyEntities(cartPos, searchRadius);
            for (Entity nearby : nearbyEntities) {
                if (nearby instanceof ItemEntity itemEntity) {
                    if (minecart.getPassengers().contains(itemEntity)) continue;
                    if ( addItem(itemEntity.getItemStack())) {
                        itemEntity.remove();
                    }
                    break;
                }
            }
        } else {
            throw new IllegalStateException("CartMinecart is null");
        }
    }
}
