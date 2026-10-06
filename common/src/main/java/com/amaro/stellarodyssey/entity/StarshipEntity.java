package com.amaro.stellarodyssey.entity;

import com.amaro.stellarodyssey.item.OxygenTankItem;
import com.amaro.stellarodyssey.registry.ModEntities;
import com.amaro.stellarodyssey.registry.ModItems;
import com.amaro.stellarodyssey.world.ModDimensions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Controllable exploratory starship entity inspired by No Man's Sky.
 * Features vector flight physics, hyperdrive fuel, particle thrusters,
 * and seamless atmospheric escape / planetary orbital transitions.
 */
public class StarshipEntity extends VehicleEntity {
    private static final EntityDataAccessor<Float> DATA_FUEL =
            SynchedEntityData.defineId(StarshipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_THRUSTING =
            SynchedEntityData.defineId(StarshipEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float MAX_FUEL = 100.0F;
    public static final double ORBITAL_WARP_ALTITUDE = 350.0;

    public StarshipEntity(EntityType<? extends StarshipEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public StarshipEntity(Level level, double x, double y, double z) {
        this(ModEntities.STARSHIP.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUEL, MAX_FUEL);
        builder.define(DATA_THRUSTING, false);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setFuel(input.getFloatOr("Fuel", MAX_FUEL));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Fuel", this.getFuel());
    }

    @Override
    protected Item getDropItem() {
        return ModItems.STARSHIP.get();
    }

    public float getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    public void setFuel(float fuel) {
        this.entityData.set(DATA_FUEL, Math.clamp(fuel, 0.0F, MAX_FUEL));
    }

    public boolean isThrusting() {
        return this.entityData.get(DATA_THRUSTING);
    }

    public void setThrusting(boolean thrusting) {
        this.entityData.set(DATA_THRUSTING, thrusting);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        ItemStack held = player.getItemInHand(hand);

        // Refueling mechanic: Oxygen Tank or Coal restores ship fuel
        if (held.is(ModItems.OXYGEN_TANK.get()) || held.is(Items.COAL) || held.is(Items.BLAZE_POWDER)) {
            float current = getFuel();
            if (current < MAX_FUEL) {
                float restored = held.is(ModItems.OXYGEN_TANK.get()) ? 40.0F : 20.0F;
                setFuel(current + restored);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                player.sendOverlayMessage(
                        Component.literal("§b✦ Hyperdrive Refueled ✦ §fFuel: " + Math.round(getFuel()) + "%")
                );
                return InteractionResult.SUCCESS;
            }
        }

        // Mount the starship cockpit
        if (!this.level().isClientSide() && this.getPassengers().isEmpty()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }

        return super.interact(player, hand, location);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity rider = this.getControllingPassenger();

        if (rider instanceof Player player) {
            // Synchronise orientation with rider look direction
            this.setYRot(player.getYRot());
            this.setXRot(player.getXRot() * 0.65F);
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();

            float forward = player.zza; // Forward control
            float strafe = player.xxa;  // Strafe control

            if (forward > 0 && getFuel() > 0) {
                this.setThrusting(true);
                Vec3 look = player.getLookAngle();
                double thrust = 0.16;
                this.setDeltaMovement(this.getDeltaMovement().add(look.scale(thrust)));

                // Fuel consumption
                if (this.tickCount % 8 == 0) {
                    this.setFuel(this.getFuel() - 0.15F);
                }

                // Thruster particles on both client and server
                if (this.level().isClientSide()) {
                    spawnThrusterParticles();
                }
            } else {
                this.setThrusting(false);
                // Inertial dampening and gentle cruise glide
                Vec3 vel = this.getDeltaMovement();
                this.setDeltaMovement(vel.x * 0.94, Math.max(-0.25, vel.y * 0.96), vel.z * 0.94);
            }

            // Check for Planetary Orbital Warp Transition
            if (!this.level().isClientSide() && this.getY() >= ORBITAL_WARP_ALTITUDE) {
                triggerOrbitalWarp(player);
            }
        } else {
            this.setThrusting(false);
            // Empty ship descent with landing dampener
            Vec3 vel = this.getDeltaMovement();
            this.setDeltaMovement(vel.x * 0.85, Math.max(-0.4, vel.y - 0.05), vel.z * 0.85);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private void spawnThrusterParticles() {
        Vec3 back = Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(-1.2);
        double px = this.getX() + back.x;
        double py = this.getY() + 0.3 + back.y;
        double pz = this.getZ() + back.z;

        this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz,
                -back.x * 0.2, -back.y * 0.2, -back.z * 0.2);
        this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz,
                (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1);
    }

    private void triggerOrbitalWarp(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(this.level() instanceof ServerLevel currentLevel)) {
            return;
        }

        ResourceKey<Level> targetKey = currentLevel.dimension().equals(ModDimensions.PROXIMA_B)
                ? Level.OVERWORLD
                : ModDimensions.PROXIMA_B;

        ServerLevel targetLevel = currentLevel.getServer().getLevel(targetKey);
        if (targetLevel == null) {
            return;
        }

        double targetX = this.getX();
        double targetZ = this.getZ();
        double targetY = 320.0;

        TeleportTransition shipTransition = new TeleportTransition(
                targetLevel,
                new Vec3(targetX, targetY, targetZ),
                new Vec3(0, -0.4, 0),
                this.getYRot(),
                this.getXRot(),
                TeleportTransition.DO_NOTHING
        );

        serverPlayer.stopRiding();

        TeleportTransition playerTransition = new TeleportTransition(
                targetLevel,
                new Vec3(targetX, targetY + 0.35, targetZ),
                new Vec3(0, -0.4, 0),
                this.getYRot(),
                this.getXRot(),
                TeleportTransition.DO_NOTHING
        );

        serverPlayer.teleport(playerTransition);
        Entity newShip = this.teleport(shipTransition);

        if (newShip != null) {
            serverPlayer.startRiding(newShip);
        }

        // No Man's Sky hyperspace warp HUD titles
        String destinationName = targetKey.equals(ModDimensions.PROXIMA_B)
                ? "PROXIMA CENTAURI B"
                : "EARTH ATMOSPHERE";

        serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 15));
        serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§b✦ HYPERSPACE DESCENT ✦")));
        serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Entering: §f" + destinationName)));
    }
}
