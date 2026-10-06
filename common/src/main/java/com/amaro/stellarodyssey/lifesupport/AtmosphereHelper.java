package com.amaro.stellarodyssey.lifesupport;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import com.amaro.stellarodyssey.block.entity.OxygenSealerBlockEntity;
import com.amaro.stellarodyssey.item.SpacesuitItem;
import com.amaro.stellarodyssey.world.CelestialBodyRegistry;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Determines atmospheric conditions, planetary vacuum/toxicity, spacesuit hermetic seal integrity,
 * and tracks active sealed habitats created by {@link OxygenSealerBlockEntity}.
 */
public final class AtmosphereHelper {
    /** Dimension types tagged as having no breathable atmosphere (vacuum / alien planets). */
    public static final TagKey<DimensionType> VACUUM_DIMENSIONS = TagKey.create(
            Registries.DIMENSION_TYPE,
            StellarOdyssey.id("vacuum")
    );

    /** Altitude in blocks where atmospheric pressure drops and breathing becomes impossible. */
    public static final int VACUUM_ALTITUDE_THRESHOLD = 320;

    /** Active oxygen sealers indexed by level dimension key. */
    private static final Map<ResourceKey<Level>, Set<OxygenSealerBlockEntity>> ACTIVE_SEALERS = new ConcurrentHashMap<>();

    static {
        try {
            LifecycleEvent.SERVER_STOPPING.register(server -> clearSealers());
        } catch (Throwable ignored) {
            // Safely ignored in environments where Architectury lifecycle events are not active
        }
    }

    private AtmosphereHelper() {
    }

    /**
     * Registers an active sealer in the level-scoped tracker.
     */
    public static void registerSealer(OxygenSealerBlockEntity sealer) {
        if (sealer != null && sealer.getLevel() != null) {
            ACTIVE_SEALERS.computeIfAbsent(sealer.getLevel().dimension(), k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(sealer);
        }
    }

    /**
     * Unregisters a sealer from all dimension tracking sets.
     */
    public static void unregisterSealer(OxygenSealerBlockEntity sealer) {
        if (sealer != null) {
            for (Set<OxygenSealerBlockEntity> set : ACTIVE_SEALERS.values()) {
                set.remove(sealer);
            }
        }
    }

    /**
     * Clears all active sealers. Used for test harness resets.
     */
    public static void clearSealers() {
        ACTIVE_SEALERS.clear();
    }

    /**
     * Checks if a world position is within any active hermetically sealed room.
     */
    public static boolean isRoomSealed(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        return isRoomSealed(level.dimension(), pos);
    }

    /**
     * Checks if a dimension coordinate is within an active hermetically sealed room.
     */
    public static boolean isRoomSealed(ResourceKey<Level> dimension, BlockPos pos) {
        if (dimension == null || pos == null) {
            return false;
        }
        Set<OxygenSealerBlockEntity> sealers = ACTIVE_SEALERS.get(dimension);
        if (sealers == null || sealers.isEmpty()) {
            return false;
        }
        for (OxygenSealerBlockEntity sealer : sealers) {
            if (sealer.isSealed() && sealer.containsPos(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the player is in open space, orbit, or an unpressurized vacuum environment (<0.05 atm).
     */
    public static boolean isVacuumEnvironment(Player player) {
        if (player == null) {
            return false;
        }
        Level level = player.level();
        if (level == null) {
            return false;
        }
        BlockPos pos = player.blockPosition();

        // 1. Inside an active sealed room: artificial pressurized air protects against vacuum
        if (pos != null && isRoomSealed(level, pos)) {
            return false;
        }

        // 2. High altitude / mesosphere & orbit
        if (player.getY() >= VACUUM_ALTITUDE_THRESHOLD) {
            return true;
        }

        // 3. Charted celestial bodies take precedence: only < 0.05 atm is hard vacuum (e.g. Nexus Moon).
        // This ensures charted exoplanets like Exotic Prime (0.85 atm) and Proxima B (0.15 atm) are
        // treated as toxic/unbreathable exoplanetary atmospheres rather than hard vacuum.
        Optional<ICelestialBody> body = CelestialBodyRegistry.getInstance().getBody(level.dimension());
        if (body.isPresent()) {
            return body.get().isVacuum();
        }

        // 4. Fallback for uncharted dimensions: vacuum dimension type tag
        try {
            if (level.dimensionTypeRegistration() != null && level.dimensionTypeRegistration().is(VACUUM_DIMENSIONS)) {
                return true;
            }
        } catch (Exception ignored) {
            // Guard against uninitialized registries in headless unit tests
        }

        // 5. Uncharted mod dimensions fall back to namespace heuristic.
        if (level.dimension() != null && level.dimension().identifier() != null) {
            return level.dimension().identifier().getNamespace().equals(StellarOdyssey.MOD_ID);
        }
        return false;
    }

    /**
     * Checks if the local atmosphere cannot be breathed unaided (e.g. Exotic Prime's toxic 0.85 atm shroud).
     */
    public static boolean isUnbreathableAtmosphere(Player player) {
        if (player == null) {
            return false;
        }
        Level level = player.level();
        if (level == null) {
            return false;
        }
        BlockPos pos = player.blockPosition();

        if (pos != null && isRoomSealed(level, pos)) {
            return false;
        }

        return CelestialBodyRegistry.getInstance().getBody(level.dimension())
                .map(body -> !body.hasBreathableAtmosphere() && !body.isVacuum())
                .orElse(false);
    }

    /**
     * Checks if the current environment requires active life support
     * (water immersion, hard vacuum, or an unbreathable alien atmosphere).
     */
    public static boolean lacksOxygen(Player player) {
        if (isRoomSealed(player.level(), player.blockPosition())) {
            return false;
        }
        return player.isEyeInFluid(FluidTags.WATER)
                || isVacuumEnvironment(player)
                || isUnbreathableAtmosphere(player);
    }

    /**
     * Retrieves the charted celestial body for a given dimension resource key.
     */
    public static Optional<ICelestialBody> getCelestialBody(ResourceKey<Level> dimension) {
        return CelestialBodyRegistry.getInstance().getBody(dimension);
    }

    /**
     * Retrieves the charted celestial body for a given level.
     */
    public static Optional<ICelestialBody> getCelestialBody(Level level) {
        return level != null ? getCelestialBody(level.dimension()) : Optional.empty();
    }

    /**
     * Returns whether the given dimension is a hard vacuum environment (< 0.05 atm, e.g. Nexus Moon).
     */
    public static boolean isHardVacuum(ResourceKey<Level> dimension) {
        return getCelestialBody(dimension).map(ICelestialBody::isVacuum).orElse(false);
    }

    /**
     * Returns whether the given dimension has an unbreathable or toxic atmosphere (>= 0.05 atm, e.g. Exotic Prime).
     */
    public static boolean isToxicOrUnbreathable(ResourceKey<Level> dimension) {
        return getCelestialBody(dimension)
                .map(body -> !body.hasBreathableAtmosphere() && !body.isVacuum())
                .orElse(false);
    }

    /**
     * Checks if the player has an intact, fully sealed spacesuit equipped across all 4 armor slots.
     */
    public static boolean isFullSuitEquipped(Player player) {
        return isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.HEAD), ArmorType.HELMET)
                && isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.CHEST), ArmorType.CHESTPLATE)
                && isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.LEGS), ArmorType.LEGGINGS)
                && isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.FEET), ArmorType.BOOTS);
    }

    /**
     * Checks if the player has equipped the Spacesuit Chestplate (the canonical Oxygen Manifold & Atmospheric Regulator).
     */
    public static boolean hasOxygenManifold(Player player) {
        return isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.CHEST), ArmorType.CHESTPLATE);
    }

    /**
     * Checks if the player has equipped the Spacesuit Helmet (Pressurized Visor & Environmental Scanner).
     */
    public static boolean hasPressurizedHelmet(Player player) {
        return isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.HEAD), ArmorType.HELMET);
    }

    /**
     * Returns the total count of valid spacesuit pieces currently equipped (0 to 4).
     */
    public static int getEquippedSpacesuitPieceCount(Player player) {
        int count = 0;
        if (isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.HEAD), ArmorType.HELMET)) count++;
        if (isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.CHEST), ArmorType.CHESTPLATE)) count++;
        if (isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.LEGS), ArmorType.LEGGINGS)) count++;
        if (isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.FEET), ArmorType.BOOTS)) count++;
        return count;
    }

    /**
     * Verifies if an ItemStack is a valid spacesuit piece for a given armor slot.
     */
    public static boolean isSpacesuitPiece(ItemStack stack, ArmorType expectedType) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof SpacesuitItem spacesuitItem) {
            return spacesuitItem.getArmorType() == expectedType;
        }
        return false;
    }
}
