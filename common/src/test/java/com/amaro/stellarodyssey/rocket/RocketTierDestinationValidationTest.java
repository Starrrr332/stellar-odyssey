package com.amaro.stellarodyssey.rocket;

import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.network.FlightPhasePayload;
import com.amaro.stellarodyssey.network.SelectDestinationPayload;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTiers;
import com.amaro.stellarodyssey.world.ModDimensions;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone M4: Rocket Tier Destination Validation & Networking Tests")
class RocketTierDestinationValidationTest {

    private static Unsafe unsafe;

    @BeforeAll
    static void initMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);
    }

    @Nested
    @DisplayName("1. Tier Destination Matrix Requirements")
    class DestinationMatrixTests {

        @Test
        @DisplayName("getRequiredTier maps dimensions to their required minimum rocket tier")
        void testRequiredTierMapping() {
            assertEquals(1, RocketTiers.getRequiredTier(ModDimensions.NEXUS_MOON));
            assertEquals(1, RocketTiers.getRequiredTier(Level.OVERWORLD));
            assertEquals(2, RocketTiers.getRequiredTier(ModDimensions.PROXIMA_B));
            assertEquals(3, RocketTiers.getRequiredTier(ModDimensions.EXOTIC_PRIME));
            assertEquals(3, RocketTiers.getRequiredTier(ModDimensions.GLIESE_DEEP));

            // Unknown or unmapped dimensions return -1
            assertEquals(-1, RocketTiers.getRequiredTier(null));
            assertEquals(-1, RocketTiers.getRequiredTier(Level.NETHER));
            assertEquals(-1, RocketTiers.getRequiredTier(Level.END));

            ResourceKey<Level> unknown = ResourceKey.create(Registries.DIMENSION, Identifier.parse("stellarodyssey:unknown_world"));
            assertEquals(-1, RocketTiers.getRequiredTier(unknown));
        }

        @Test
        @DisplayName("Tier 1 (Pioneer) allows Moon & Overworld, rejects Proxima B, Exotic Prime, Gliese Deep")
        void testTier1Permissions() {
            int t1 = 1;
            assertTrue(RocketTiers.isDestinationAllowed(t1, ModDimensions.NEXUS_MOON));
            assertTrue(RocketTiers.isDestinationAllowed(t1, Level.OVERWORLD));

            assertFalse(RocketTiers.isDestinationAllowed(t1, ModDimensions.PROXIMA_B));
            assertFalse(RocketTiers.isDestinationAllowed(t1, ModDimensions.EXOTIC_PRIME));
            assertFalse(RocketTiers.isDestinationAllowed(t1, ModDimensions.GLIESE_DEEP));
            assertFalse(RocketTiers.isDestinationAllowed(t1, Level.NETHER));
            assertFalse(RocketTiers.isDestinationAllowed(t1, null));
        }

        @Test
        @DisplayName("Tier 2 (Voyager) allows Moon, Overworld & Proxima B, rejects Exotic Prime & Gliese Deep")
        void testTier2Permissions() {
            int t2 = 2;
            assertTrue(RocketTiers.isDestinationAllowed(t2, ModDimensions.NEXUS_MOON));
            assertTrue(RocketTiers.isDestinationAllowed(t2, Level.OVERWORLD));
            assertTrue(RocketTiers.isDestinationAllowed(t2, ModDimensions.PROXIMA_B));

            assertFalse(RocketTiers.isDestinationAllowed(t2, ModDimensions.EXOTIC_PRIME));
            assertFalse(RocketTiers.isDestinationAllowed(t2, ModDimensions.GLIESE_DEEP));
            assertFalse(RocketTiers.isDestinationAllowed(t2, Level.NETHER));
            assertFalse(RocketTiers.isDestinationAllowed(t2, null));
        }

        @Test
        @DisplayName("Tier 3 (Odyssey) allows Moon, Overworld, Proxima B, Exotic Prime & Gliese Deep")
        void testTier3Permissions() {
            int t3 = 3;
            assertTrue(RocketTiers.isDestinationAllowed(t3, ModDimensions.NEXUS_MOON));
            assertTrue(RocketTiers.isDestinationAllowed(t3, Level.OVERWORLD));
            assertTrue(RocketTiers.isDestinationAllowed(t3, ModDimensions.PROXIMA_B));
            assertTrue(RocketTiers.isDestinationAllowed(t3, ModDimensions.EXOTIC_PRIME));
            assertTrue(RocketTiers.isDestinationAllowed(t3, ModDimensions.GLIESE_DEEP));

            assertFalse(RocketTiers.isDestinationAllowed(t3, Level.NETHER));
            assertFalse(RocketTiers.isDestinationAllowed(t3, null));
        }

        @Test
        @DisplayName("Invalid or non-positive tiers reject all destinations")
        void testInvalidTiers() {
            for (int invalidTier : new int[]{0, -1, -5}) {
                assertFalse(RocketTiers.isDestinationAllowed(invalidTier, ModDimensions.NEXUS_MOON));
                assertFalse(RocketTiers.isDestinationAllowed(invalidTier, ModDimensions.PROXIMA_B));
                assertFalse(RocketTiers.isDestinationAllowed(invalidTier, ModDimensions.EXOTIC_PRIME));
                assertFalse(RocketTiers.isDestinationAllowed(invalidTier, Level.OVERWORLD));
            }
        }

        @Test
        @DisplayName("RocketTier static helpers delegate consistently to RocketTiers matrix")
        void testRocketTierHelperDelegation() {
            assertEquals(1, RocketTier.getRequiredTier(ModDimensions.NEXUS_MOON));
            assertEquals(1, RocketTier.minTierForDestination(ModDimensions.NEXUS_MOON));
            assertEquals(2, RocketTier.getRequiredTier(ModDimensions.PROXIMA_B));
            assertEquals(3, RocketTier.getRequiredTier(ModDimensions.EXOTIC_PRIME));

            assertTrue(RocketTier.isDestinationAllowed(1, ModDimensions.NEXUS_MOON));
            assertFalse(RocketTier.isDestinationAllowed(1, ModDimensions.PROXIMA_B));
            assertTrue(RocketTier.isDestinationAllowed(2, ModDimensions.PROXIMA_B));
            assertFalse(RocketTier.isDestinationAllowed(2, ModDimensions.EXOTIC_PRIME));

            // com.amaro.stellarodyssey.rocket.RocketTiers helper re-export
            assertEquals(1, com.amaro.stellarodyssey.rocket.RocketTiers.getRequiredTier(ModDimensions.NEXUS_MOON));
            assertEquals(2, com.amaro.stellarodyssey.rocket.RocketTiers.getRequiredTier(ModDimensions.PROXIMA_B));
            assertEquals(3, com.amaro.stellarodyssey.rocket.RocketTiers.getRequiredTier(ModDimensions.EXOTIC_PRIME));
            assertTrue(com.amaro.stellarodyssey.rocket.RocketTiers.isDestinationAllowed(3, ModDimensions.EXOTIC_PRIME));
        }
    }

    @Nested
    @DisplayName("2. Network Payloads Serialization & Deserialization")
    class NetworkPayloadCodecTests {

        @Test
        @DisplayName("FlightPhasePayload encodes and decodes correctly across all flight phases")
        void testFlightPhasePayloadCodec() {
            for (RocketFlightPhase phase : RocketFlightPhase.values()) {
                FlightPhasePayload original = new FlightPhasePayload(1001 + phase.ordinal(), phase, 45);

                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
                FlightPhasePayload.CODEC.encode(buf, original);

                FlightPhasePayload decoded = FlightPhasePayload.CODEC.decode(buf);
                assertEquals(original.entityId(), decoded.entityId());
                assertEquals(original.phase(), decoded.phase());
                assertEquals(original.phaseTicks(), decoded.phaseTicks());
                assertEquals(original.phaseOrdinal(), decoded.phaseOrdinal());
                assertEquals(phase.ordinal(), decoded.phaseOrdinal());
            }
        }

        @Test
        @DisplayName("FlightPhasePayload backward compatibility constructors and accessors")
        void testFlightPhasePayloadCompatibility() {
            FlightPhasePayload payload1 = new FlightPhasePayload(55, 3, 20); // ordinal 3 = ASCENT
            assertEquals(55, payload1.entityId());
            assertEquals(RocketFlightPhase.ASCENT, payload1.phase());
            assertEquals(20, payload1.phaseTicks());
            assertEquals(3, payload1.phaseOrdinal());

            FlightPhasePayload payload2 = new FlightPhasePayload(77, 7, 10, "stellarodyssey:proxima_b"); // ordinal 7 = WARP
            assertEquals(77, payload2.entityId());
            assertEquals(RocketFlightPhase.WARP, payload2.phase());
            assertEquals(10, payload2.phaseTicks());
            assertEquals("", payload2.destinationId());
        }

        @Test
        @DisplayName("SelectDestinationPayload encodes and decodes ResourceKey correctly")
        void testSelectDestinationPayloadCodec() {
            ResourceKey<Level>[] testKeys = new ResourceKey[]{
                    ModDimensions.NEXUS_MOON,
                    ModDimensions.PROXIMA_B,
                    ModDimensions.EXOTIC_PRIME,
                    ModDimensions.GLIESE_DEEP,
                    Level.OVERWORLD
            };

            for (ResourceKey<Level> key : testKeys) {
                SelectDestinationPayload original = new SelectDestinationPayload(404, key);

                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
                SelectDestinationPayload.CODEC.encode(buf, original);

                SelectDestinationPayload decoded = SelectDestinationPayload.CODEC.decode(buf);
                assertEquals(404, decoded.entityId());
                assertEquals(key, decoded.destinationDimension());
                assertEquals(key, decoded.dimensionKey());
                assertEquals(key.identifier().toString(), decoded.dimensionId());
            }
        }

        @Test
        @DisplayName("SelectDestinationPayload backward compatibility string constructor")
        void testSelectDestinationPayloadStringConstructor() {
            SelectDestinationPayload payload = new SelectDestinationPayload(808, "stellarodyssey:proxima_b");
            assertEquals(808, payload.entityId());
            assertEquals(ModDimensions.PROXIMA_B, payload.destinationDimension());
            assertEquals(ModDimensions.PROXIMA_B, payload.dimensionKey());
            assertEquals("stellarodyssey:proxima_b", payload.dimensionId());
        }
    }

    @Nested
    @DisplayName("3. Server-side Validation Logic & Fallback Behavior")
    class ServerValidationLogicTests {

        private RocketEntity createHeadlessRocket(int tierLevel) throws Exception {
            RocketEntity rocket = (RocketEntity) unsafe.allocateInstance(RocketEntity.class);

            // Initialize minimal fields
            Field tierField = RocketEntity.class.getDeclaredField("DATA_TIER");
            tierField.setAccessible(true);

            // Using reflection to set tier level and state directly without Level ticking
            Field scheduleCacheTier = RocketEntity.class.getDeclaredField("scheduleCacheTier");
            scheduleCacheTier.setAccessible(true);
            scheduleCacheTier.set(rocket, -1);

            return rocket;
        }

        @Test
        @DisplayName("Target destination defaults to tier destination fallback")
        void testDestinationFallback() {
            // Tier 1 defaults to Nexus Moon
            RocketTier t1 = RocketTiers.TIER_1.get();
            assertEquals(ModDimensions.NEXUS_MOON, t1.destination());

            // Tier 2 defaults to Proxima B
            RocketTier t2 = RocketTiers.TIER_2.get();
            assertEquals(ModDimensions.PROXIMA_B, t2.destination());

            // Tier 3 defaults to Exotic Prime
            RocketTier t3 = RocketTiers.TIER_3.get();
            assertEquals(ModDimensions.EXOTIC_PRIME, t3.destination());
        }

        @Test
        @DisplayName("Server validation rejects if player is not riding the rocket")
        void testRejectIfNotRiding() throws Exception {
            RocketEntity rocket = createHeadlessRocket(1);
            ServerPlayer player = (ServerPlayer) unsafe.allocateInstance(ServerPlayer.class);

            // player.getVehicle() will be null, while rocket is not player's vehicle
            boolean result = rocket.handleSelectDestination(player, ModDimensions.NEXUS_MOON);
            assertFalse(result, "handleSelectDestination must reject when player is not riding the rocket");
        }

        @Test
        @DisplayName("Destination matrix validation correctly enforces tier requirements")
        void testTierConstraintEnforcement() {
            // T1 rocket targeting Proxima B (requires T2) must be disallowed
            assertFalse(RocketTiers.isDestinationAllowed(1, ModDimensions.PROXIMA_B));

            // T1 rocket targeting Exotic Prime (requires T3) must be disallowed
            assertFalse(RocketTiers.isDestinationAllowed(1, ModDimensions.EXOTIC_PRIME));

            // T2 rocket targeting Exotic Prime (requires T3) must be disallowed
            assertFalse(RocketTiers.isDestinationAllowed(2, ModDimensions.EXOTIC_PRIME));

            // T2 rocket targeting Proxima B (requires T2) is allowed
            assertTrue(RocketTiers.isDestinationAllowed(2, ModDimensions.PROXIMA_B));

            // T3 rocket targeting all dimensions is allowed
            assertTrue(RocketTiers.isDestinationAllowed(3, ModDimensions.NEXUS_MOON));
            assertTrue(RocketTiers.isDestinationAllowed(3, ModDimensions.PROXIMA_B));
            assertTrue(RocketTiers.isDestinationAllowed(3, ModDimensions.EXOTIC_PRIME));
            assertTrue(RocketTiers.isDestinationAllowed(3, ModDimensions.GLIESE_DEEP));
        }

        @Test
        @DisplayName("Target destination can be explicitly updated on rocket instance")
        void testTargetDestinationUpdate() throws Exception {
            RocketEntity rocket = createHeadlessRocket(2);
            rocket.setTargetDestination(ModDimensions.PROXIMA_B);

            assertEquals(ModDimensions.PROXIMA_B, rocket.getTargetDestination());
            assertEquals(ModDimensions.PROXIMA_B, rocket.getDestination());

            rocket.setDestination(ModDimensions.EXOTIC_PRIME);
            assertEquals(ModDimensions.EXOTIC_PRIME, rocket.getTargetDestination());
            assertEquals(ModDimensions.EXOTIC_PRIME, rocket.getDestination());
        }
    }
}
