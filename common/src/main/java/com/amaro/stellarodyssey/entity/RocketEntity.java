package com.amaro.stellarodyssey.entity;

import com.amaro.stellarodyssey.registry.ModEntities;
import com.amaro.stellarodyssey.registry.ModItems;
import com.amaro.stellarodyssey.registry.ModSoundEvents;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTierRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Tiered rocket entity. Deployed on a complete 3x3 launch pad, it runs a
 * countdown when a player mounts it, then lifts off vertically and teleports
 * its passengers to the tier's destination dimension.
 */
public class RocketEntity extends VehicleEntity {
    private static final EntityDataAccessor<Integer> DATA_TIER =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LAUNCHING =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LAUNCH_TICKS =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    /** Altitude at which the rocket transitions to the destination dimension. */
    public static final double LAUNCH_ALTITUDE = 300.0;
    /** Countdown length in ticks (5 seconds). */
    public static final int COUNTDOWN_TICKS = 100;

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public RocketEntity(Level level, double x, double y, double z, int tierLevel) {
        this(ModEntities.ROCKET.get(), level);
        this.setPos(x, y, z);
        this.setTierLevel(tierLevel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TIER, 1);
        builder.define(DATA_LAUNCHING, false);
        builder.define(DATA_LAUNCH_TICKS, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setTierLevel(input.getIntOr("Tier", 1));
        this.setLaunching(input.getBooleanOr("Launching", false));
        this.setLaunchTicks(input.getIntOr("LaunchTicks", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Tier", this.getTierLevel());
        output.putBoolean("Launching", this.isLaunching());
        output.putInt("LaunchTicks", this.getLaunchTicks());
    }

    @Override
    protected Item getDropItem() {
        return ModItems.ROCKETS.get("rocket_t" + this.getTierLevel()).get();
    }

    public int getTierLevel() {
        return this.entityData.get(DATA_TIER);
    }

    public void setTierLevel(int tier) {
        this.entityData.set(DATA_TIER, Math.clamp(tier, 1, 3));
    }

    public boolean isLaunching() {
        return this.entityData.get(DATA_LAUNCHING);
    }

    public void setLaunching(boolean launching) {
        this.entityData.set(DATA_LAUNCHING, launching);
    }

    public int getLaunchTicks() {
        return this.entityData.get(DATA_LAUNCH_TICKS);
    }

    public void setLaunchTicks(int ticks) {
        this.entityData.set(DATA_LAUNCH_TICKS, ticks);
    }

    public Optional<RocketTier> getTier() {
        return RocketTierRegistry.getTier(this.getTierLevel());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isLaunching()) {
            this.tickLaunchSequence();
        } else {
            // Resting on the pad: no gravity drift
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void tickLaunchSequence() {
        int ticks = this.getLaunchTicks() + 1;
        this.setLaunchTicks(ticks);

        // Countdown announcement
        if (ticks % 20 == 0 && ticks <= COUNTDOWN_TICKS) {
            int secondsLeft = (COUNTDOWN_TICKS - ticks) / 20 + 1;
            this.level().players().forEach(p -> {
                if (p instanceof ServerPlayer sp && sp.getVehicle() == this) {
                    sp.sendSystemMessage(Component.translatable("message.stellarodyssey.launch_countdown", secondsLeft));
                }
            });
        }

        // Exhaust particles
        if (this.level().isClientSide()) {
            for (int i = 0; i < 6; i++) {
                double ox = (this.random.nextDouble() - 0.5) * 0.8;
                double oz = (this.random.nextDouble() - 0.5) * 0.8;
                this.level().addParticle(ParticleTypes.FLAME, this.getX() + ox, this.getY() - 0.5, this.getZ() + oz,
                        0.0, -0.3, 0.0);
            }
            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() - 0.5, this.getZ(),
                    0.0, 0.1, 0.0);
        }

        if (ticks >= COUNTDOWN_TICKS) {
            // Liftoff: accelerate upward
            Vec3 vel = this.getDeltaMovement();
            this.setDeltaMovement(vel.x * 0.9, vel.y + 0.12, vel.z * 0.9);
            this.move(MoverType.SELF, this.getDeltaMovement());

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, this.blockPosition(), ModSoundEvents.STARSHIP_THRUST.get(),
                        SoundSource.AMBIENT, 1.0F, 0.8F);
            }

            if (this.getY() >= LAUNCH_ALTITUDE) {
                this.teleportPassengersToDestination();
            }
        }
    }

    private void teleportPassengersToDestination() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Optional<RocketTier> tier = this.getTier();
        if (tier.isEmpty()) {
            return;
        }
        ResourceKey<Level> destination = tier.get().destination();
        ServerLevel target = serverLevel.getServer().getLevel(destination);
        if (target == null) {
            return;
        }

        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                Vec3 pos = new Vec3(this.getX(), 320.0, this.getZ());
                player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, player.getYRot(), player.getXRot(),
                        TeleportTransition.DO_NOTHING));
                player.sendSystemMessage(Component.translatable("message.stellarodyssey.arrived",
                        Component.translatable("dimension.stellarodyssey." + destination.identifier().getPath())));
            }
        }
        this.discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.isLaunching()) {
            return InteractionResult.SUCCESS;
        }
        if (this.getPassengers().isEmpty()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        // A player is already seated: begin the launch countdown
        this.setLaunching(true);
        this.setLaunchTicks(0);
        this.level().playSound(null, this.blockPosition(), ModSoundEvents.STARSHIP_THRUST.get(),
                SoundSource.AMBIENT, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
