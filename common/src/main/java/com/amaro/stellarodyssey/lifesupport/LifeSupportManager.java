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
 * consumes oxygen from carried tanks, deals decompression/asphyxiation damage,
 * and synchronises status to clients.
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
        boolean suitSealed = AtmosphereHelper.isFullSuitEquipped(player);

        Inventory inv = player.getInventory();
        int totalOxygen = 0;
        int maxCapacity = 0;
        ItemStack firstAvailableTank = ItemStack.EMPTY;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ModItems.OXYGEN_TANK.get())) {
                int o2 = OxygenTankItem.getOxygen(stack);
                totalOxygen += o2;
                maxCapacity += OxygenTankItem.CAPACITY;
                if (firstAvailableTank.isEmpty() && o2 > 0) {
                    firstAvailableTank = stack;
                }
            }
        }

        if (!creative && inHazard) {
            // Once every second (20 game ticks)
            if (player.tickCount % 20 == 0) {
                ServerLevel serverLevel = (ServerLevel) player.level();

                if (inVacuum && !suitSealed) {
                    // Hard vacuum breach: decompression damage regardless of oxygen supply
                    player.hurtServer(serverLevel, serverLevel.damageSources().drown(), 3.0F);
                    player.setAirSupply(-20);
                    // Klaxon every 2s while the breach persists
                    if (player.tickCount % 40 == 0) {
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                ModSoundEvents.DECOMPRESSION_ALARM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                } else if (!firstAvailableTank.isEmpty()) {
                    // Sealed suit or emergency rebreather with active oxygen
                    OxygenTankItem.drain(firstAvailableTank, 1);
                    totalOxygen = Math.max(0, totalOxygen - 1);
                    player.setAirSupply(player.getMaxAirSupply());
                } else {
                    // Depleted / unsupplied: asphyxiation damage
                    int currentAir = player.getAirSupply() - 15;
                    if (currentAir <= -20) {
                        player.hurtServer(serverLevel, serverLevel.damageSources().drown(), 2.0F);
                        currentAir = 0;
                    }
                    player.setAirSupply(currentAir);
                }
            }
        }

        // Synchronise with client every 10 ticks (0.5s)
        if (player.tickCount % 10 == 0) {
            NetworkManager.sendToPlayer(serverPlayer, new OxygenSyncPayload(totalOxygen, maxCapacity, inHazard));
        }
    }
}
