package com.amaro.stellarodyssey.rocket;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Re-export of RocketEntity under the rocket package for modular architecture.
 */
public class RocketEntity extends com.amaro.stellarodyssey.entity.RocketEntity {
    public RocketEntity(EntityType<? extends com.amaro.stellarodyssey.entity.RocketEntity> type, Level level) {
        super(type, level);
    }

    public RocketEntity(Level level, double x, double y, double z, int tierLevel) {
        super(level, x, y, z, tierLevel);
    }
}
