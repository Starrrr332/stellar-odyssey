package com.amaro.stellarodyssey.rocket;

import com.amaro.stellarodyssey.item.RocketComponentItem;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTierRegistry;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
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
            counts.merge(component.getType(), 1, Integer::sum);
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
}
