package com.amaro.stellarodyssey.network;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-Client packet synchronising the local player's oxygen level,
 * total capacity across all carried tanks, and current environmental hazard status.
 */
public record OxygenSyncPayload(int oxygen, int maxOxygen, boolean inHazard) implements CustomPacketPayload {
    public static final Type<OxygenSyncPayload> TYPE = new Type<>(StellarOdyssey.id("oxygen_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OxygenSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OxygenSyncPayload::oxygen,
            ByteBufCodecs.VAR_INT, OxygenSyncPayload::maxOxygen,
            ByteBufCodecs.BOOL, OxygenSyncPayload::inHazard,
            OxygenSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
