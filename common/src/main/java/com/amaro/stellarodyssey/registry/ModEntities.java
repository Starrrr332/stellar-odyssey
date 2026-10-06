package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.entity.StarshipEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

/**
 * Entity type registry for Stellar Odyssey.
 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(StellarOdyssey.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<StarshipEntity>> STARSHIP = ENTITIES.<EntityType<StarshipEntity>>register("starship",
            () -> EntityType.Builder.<StarshipEntity>of(StarshipEntity::new, MobCategory.MISC)
                    .sized(2.4F, 1.2F)
                    .clientTrackingRange(12)
                    .passengerAttachments(new Vec3(0.0, 0.35, 0.0))
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, StellarOdyssey.id("starship"))));

    public static final RegistrySupplier<EntityType<RocketEntity>> ROCKET = ENTITIES.<EntityType<RocketEntity>>register("rocket",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(1.2F, 3.0F)
                    .clientTrackingRange(12)
                    .passengerAttachments(new Vec3(0.0, 2.2, 0.0))
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, StellarOdyssey.id("rocket"))));

    private ModEntities() {
    }

    public static void register() {
        ENTITIES.register();
    }
}
