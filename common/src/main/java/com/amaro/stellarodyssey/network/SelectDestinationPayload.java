package com.amaro.stellarodyssey.network;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Client-to-Server packet: the player riding a rocket confirms a destination
 * from the Star Map UI. {@code entityId} identifies the rocket entity on the
 * server; {@code destinationDimension} is the ResourceKey of the target dimension.
 */
public record SelectDestinationPayload(int entityId, ResourceKey<Level> destinationDimension) implements CustomPacketPayload {
    public static final Type<SelectDestinationPayload> TYPE = new Type<>(StellarOdyssey.id("select_destination"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectDestinationPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectDestinationPayload::entityId,
            ResourceKey.streamCodec(Registries.DIMENSION), SelectDestinationPayload::destinationDimension,
            SelectDestinationPayload::new
    );

    /**
     * Backward-compatible constructor accepting a string dimension ID.
     */
    public SelectDestinationPayload(int entityId, String dimensionId) {
        this(entityId, ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionId)));
    }

    /**
     * Backward-compatible accessor for destination ResourceKey.
     */
    public ResourceKey<Level> dimensionKey() {
        return this.destinationDimension;
    }

    /**
     * Backward-compatible accessor for destination String ID.
     */
    public String dimensionId() {
        return this.destinationDimension.identifier().toString();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}