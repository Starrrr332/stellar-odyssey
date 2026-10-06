package com.amaro.stellarodyssey.lifesupport;

import com.amaro.stellarodyssey.item.OxygenTankItem;
import com.amaro.stellarodyssey.network.OxygenSyncPayload;
import com.amaro.stellarodyssey.registry.ModItems;
import com.amaro.stellarodyssey.registry.ModSoundEvents;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Manages life support logic: checks atmospheric conditions and spacesuit seal,
 * consumes oxygen from carried tanks coupled to the spacesuit manifold and helmet,
 * deals decompression/asphyxiation damage, and synchronises status to clients.
 */
public final class LifeSupportManager {
    private LifeSupportManager() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(LifeSupportManager::tickPlayer);
    }

    private static void tickPlayer(Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean creative = player.isCreative() || player.isSpectator();
        boolean inVacuum = AtmosphereHelper.isVacuumEnvironment(player);
        boolean inHazard = AtmosphereHelper.lacksOxygen(player);

        Inventory inv = player.getInventory();
        int totalOxygen = 0;
        int maxCapacity = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isOxygenTank(stack)) {
                int o2 = OxygenTankItem.getOxygen(stack);
                totalOxygen += o2;
                maxCapacity += OxygenTankItem.CAPACITY;
            }
        }

        if (!creative && inHazard) {
            // Once every second (20 game ticks)
            if (player.tickCount % 20 == 0) {
                ServerLevel serverLevel = (ServerLevel) player.level();
                boolean hasManifold = AtmosphereHelper.hasOxygenManifold(player);
                boolean hasHelmet = AtmosphereHelper.hasPressurizedHelmet(player);
                boolean canUseTanks = hasManifold && hasHelmet;
                int suitPieceCount = AtmosphereHelper.getEquippedSpacesuitPieceCount(player);

                if (inVacuum) {
                    if (suitPieceCount == 4) {
                        // Full hermetic seal: nominal oxygen consumption (1 unit/s)
                        if (canUseTanks) {
                            int drained = drainFromInventory(player, 1);
                            if (drained > 0) {
                                totalOxygen = Math.max(0, totalOxygen - drained);
                                player.setAirSupply(player.getMaxAirSupply());
                            } else {
                                applyAsphyxiation(player, serverLevel);
                            }
                        } else {
                            applyAsphyxiation(player, serverLevel);
                        }
                    } else if (suitPieceCount == 3 && canUseTanks) {
                        // Minor seal breach (3/4 pieces equipped, helmet and manifold intact):
                        // 2x oxygen consumption rate as pressurized air leaks into space
                        int drained = drainFromInventory(player, 2);
                        if (drained > 0) {
                            totalOxygen = Math.max(0, totalOxygen - drained);
                            player.setAirSupply(player.getMaxAirSupply());
                        } else {
                            // Depleted tanks can no longer counter leak: decompression breach
                            applyDecompression(player, serverLevel);
                        }
                    } else {
                        // Unsealed / catastrophic breach (< 3 pieces or missing helmet/chestplate in vacuum)
                        applyDecompression(player, serverLevel);
                    }
                } else {
                    // Non-vacuum hazard (unbreathable/toxic exoplanet atmosphere or water immersion)
                    if (canUseTanks) {
                        int drained = drainFromInventory(player, 1);
                        if (drained > 0) {
                            totalOxygen = Math.max(0, totalOxygen - drained);
                            player.setAirSupply(player.getMaxAirSupply());
                        } else {
                            applyAsphyxiation(player, serverLevel);
                        }
                    } else {
                        // Missing helmet or oxygen manifold: toxic inhalation / asphyxiation
                        applyAsphyxiation(player, serverLevel);
                    }
                }
            }
        }

        // Synchronise with client every 10 ticks (0.5s)
        if (player.tickCount % 10 == 0) {
            NetworkManager.sendToPlayer(serverPlayer, new OxygenSyncPayload(totalOxygen, maxCapacity, inHazard));
        }
    }

    /**
     * Drains up to {@code amount} oxygen units from available tanks in the player's inventory.
     *
     * @return the total amount actually drained
     */
    public static int drainFromInventory(Player player, int amount) {
        Inventory inv = player.getInventory();
        int needed = amount;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isOxygenTank(stack)) {
                int drained = OxygenTankItem.drain(stack, needed);
                needed -= drained;
                if (needed <= 0) {
                    break;
                }
            }
        }
        return amount - needed;
    }

    /**
     * Detects oxygen tanks without requiring the registry lookup to succeed.
     * The {@code ModItems.OXYGEN_TANK.get()} call throws in pure-JVM unit tests
     * (mod items are never registered there), so the type check runs first and
     * the registry fallback is guarded for exotic item copies.
     */
    private static boolean isOxygenTank(ItemStack stack) {
        if (stack.getItem() instanceof OxygenTankItem) {
            return true;
        }
        try {
            return stack.is(ModItems.OXYGEN_TANK.get());
        } catch (RuntimeException ignored) {
            // Registry not populated: falls back to the instanceof check above.
            return false;
        }
    }

    private static void applyDecompression(Player player, ServerLevel serverLevel) {
        player.hurtServer(serverLevel, serverLevel.damageSources().drown(), 3.0F);
        player.setAirSupply(-20);
        if (player.tickCount % 40 == 0) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSoundEvents.DECOMPRESSION_ALARM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void applyAsphyxiation(Player player, ServerLevel serverLevel) {
        int currentAir = player.getAirSupply() - 15;
        if (currentAir <= -20) {
            player.hurtServer(serverLevel, serverLevel.damageSources().drown(), 2.0F);
            currentAir = 0;
        }
        player.setAirSupply(currentAir);
    }
}
