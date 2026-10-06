package com.amaro.stellarodyssey.api.ecology;

import net.minecraft.world.entity.Mob;

/**
 * Interface contract for alien creature behavioral adaptations (low gravity, vacuum, etc.).
 * Allows the Ecology satellite to define modular AI adaptations without hardcoding mob classes.
 */
public interface IAlienEntityBehavior {
    /**
     * Ticks or applies behavioral adaptations to the given entity.
     *
     * @param entity The mob receiving the behavior.
     */
    void applyBehavior(Mob entity);

    /**
     * Determines whether this behavior is active under the specified atmospheric conditions.
     *
     * @param atmosphere The local atmospheric condition.
     * @return {@code true} if active, {@code false} otherwise.
     */
    boolean isActiveIn(IAtmosphereCondition atmosphere);
}
