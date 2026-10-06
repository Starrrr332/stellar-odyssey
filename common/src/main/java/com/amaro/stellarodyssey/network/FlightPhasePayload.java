package com.amaro.stellarodyssey.network;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-Client packet syncing the flight phase of a rocket entity to its
 * rider so the client-side overlay, camera controller, and audio controller
 * can mirror the server-authoritative {@code RocketFlightPhase} without
 * relying on synched entity data alone.
 */
public record FlightPhasePayload(int entityId, RocketFlightPhase phase, int phaseTicks) implements CustomPacketPayload {
    public static final Type<FlightPhasePayload> TYPE = new Type<>(StellarOdyssey.id("flight_phase"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlightPhasePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FlightPhasePayload::entityId,
            ByteBufCodecs.VAR_INT.map(ord -> RocketFlightPhase.fromOrdinal(ord, RocketFlightPhase.IDLE), RocketFlightPhase::ordinal), FlightPhasePayload::phase,
            ByteBufCodecs.VAR_INT, FlightPhasePayload::phaseTicks,
            FlightPhasePayload::new
    );

    /**
     * Backward-compatible constructor accepting integer phase ordinal.
     */
    public FlightPhasePayload(int entityId, int phaseOrdinal, int phaseTicks) {
        this(entityId, RocketFlightPhase.fromOrdinal(phaseOrdinal, RocketFlightPhase.IDLE), phaseTicks);
    }

    /**
     * Backward-compatible constructor accepting destination string.
     */
    public FlightPhasePayload(int entityId, int phaseOrdinal, int phaseTicks, String destinationId) {
        this(entityId, RocketFlightPhase.fromOrdinal(phaseOrdinal, RocketFlightPhase.IDLE), phaseTicks);
    }

    public int phaseOrdinal() {
        return this.phase != null ? this.phase.ordinal() : 0;
    }

    public String destinationId() {
        return "";
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}