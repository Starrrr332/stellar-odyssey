package com.amaro.stellarodyssey.rocket;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.item.RocketComponentItem;
import com.amaro.stellarodyssey.registry.tiers.AlienMineralTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTierRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure assembly validation logic for the rocket assembly table.
 * <p>
 * Given the items placed in the table, determines whether they form a valid
 * rocket of some tier. Kept free of Minecraft server state so it can be unit
 * tested headlessly.
 * </p>
 */
public final class AssemblyLogic {

    private AssemblyLogic() {
    }

    /**
     * Validates a collection of stacks against every registered rocket tier.
     *
     * @return the tier whose exact component set is present, or empty.
     */
    public static Optional<RocketTier> validate(Collection<ItemStack> stacks) {
        Map<String, Integer> counts = new HashMap<>();
        RocketTier tier = null;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof RocketComponentItem component)) {
                return Optional.empty();
            }
            if (tier == null) {
                tier = component.getTier();
            } else if (tier.tierLevel() != component.getTier().tierLevel()) {
                return Optional.empty(); // mixed tiers are invalid
            }
            // Count actual item quantities, not slot occurrences. Slot-based counting made a
            // slot holding a stack satisfy the "exactly one component" contract, so a valid
            // rocket could be taken repeatedly (one item consumed per slot) — an infinite
            // rocket duplication exploit.
            counts.merge(component.getType(), stack.getCount(), Integer::sum);
        }
        if (tier == null) {
            return Optional.empty();
        }
        java.util.List<String> required = RocketComponentRegistry.getRequiredComponents(tier);
        if (counts.size() != required.size()) {
            return Optional.empty();
        }
        for (String req : required) {
            if (counts.getOrDefault(req, 0) != 1) {
                return Optional.empty();
            }
        }
        return Optional.of(tier);
    }

    /**
     * @return the tier matching the given level, or empty if not registered.
     */
    public static Optional<RocketTier> tierByLevel(int level) {
        return RocketTierRegistry.getTier(level);
    }

    /**
     * Returns the alien mineral tiers required to manufacture the given rocket tier.
     * <ul>
     *   <li>Tier 1: Empty list (Overworld iron/copper)</li>
     *   <li>Tier 2: [CELIDIUM] (Alien Mineral Tier 1 from Nexus Moon)</li>
     *   <li>Tier 3: [VERDANTITE, ASTRALITE] (Alien Mineral Tiers 2 &amp; 3)</li>
     * </ul>
     */
    public static List<AlienMineralTier> getRequiredMinerals(RocketTier tier) {
        if (tier == null) {
            return List.of();
        }
        return getRequiredMinerals(tier.tierLevel());
    }

    /**
     * Returns the alien mineral tiers required to manufacture the given rocket tier level.
     */
    public static List<AlienMineralTier> getRequiredMinerals(int tierLevel) {
        return switch (tierLevel) {
            case 2 -> List.of(AlienMineralTier.CELIDIUM);
            case 3 -> List.of(AlienMineralTier.VERDANTITE, AlienMineralTier.ASTRALITE);
            default -> List.of();
        };
    }

    /**
     * Returns the canonical ingot item IDs required for component fabrication of this tier.
     */
    public static List<Identifier> getRequiredIngotIds(RocketTier tier) {
        if (tier == null) {
            return List.of();
        }
        return getRequiredIngotIds(tier.tierLevel());
    }

    /**
     * Returns the canonical ingot item IDs required for component fabrication of this tier level.
     */
    public static List<Identifier> getRequiredIngotIds(int tierLevel) {
        return switch (tierLevel) {
            case 1 -> List.of(Identifier.parse("minecraft:iron_ingot"));
            case 2 -> List.of(StellarOdyssey.id("celidium_ingot"));
            case 3 -> List.of(StellarOdyssey.id("verdantite_ingot"), StellarOdyssey.id("astralite_ingot"));
            default -> List.of();
        };
    }

    /**
     * Checks whether the given alien mineral is part of the rocket tier's bill of materials.
     */
    public static boolean isMineralUsedInTier(AlienMineralTier mineral, RocketTier tier) {
        if (mineral == null || tier == null) {
            return false;
        }
        return getRequiredMinerals(tier).contains(mineral);
    }

    /**
     * Checks whether the given alien mineral is part of the rocket tier level's bill of materials.
     */
    public static boolean isMineralUsedInTier(AlienMineralTier mineral, int tierLevel) {
        if (mineral == null) {
            return false;
        }
        return getRequiredMinerals(tierLevel).contains(mineral);
    }
}
