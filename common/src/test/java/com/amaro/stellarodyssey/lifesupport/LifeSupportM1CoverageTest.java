package com.amaro.stellarodyssey.lifesupport;

import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import com.amaro.stellarodyssey.block.entity.OxygenSealerBlockEntity;
import com.amaro.stellarodyssey.item.OxygenTankItem;
import com.amaro.stellarodyssey.world.CelestialBodyRegistry;
import com.amaro.stellarodyssey.world.ModDimensions;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Restores the M1 coverage lost when {@code LifeSupportSystemTest} was rewritten:
 * oxygen-tank math, celestial atmosphere catalog and the Oxygen Sealer BFS algorithm.
 * Kept in its own file so each brain writes at most one test file (pact rule 1).
 */
@DisplayName("Milestone M1 coverage: oxygen tanks, celestial catalog & sealed-room BFS")
public class LifeSupportM1CoverageTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /**
     * Exposes the exact DataComponent contract of {@code OxygenTankItem}
     * (MAX_DAMAGE = CAPACITY, oxygen stored as remaining durability).
     * <p>
     * The stack is fabricated with Unsafe because in pure-JVM unit tests the item
     * holders have no bound components ({@code NPE: Components not bound yet}) and
     * mod items cannot be registered at all (frozen intrusive holders) — same
     * technique as the proven-good {@code LifeSupportSystemTest} fixtures.
     */
    private static ItemStack newTankStack() throws Exception {
        Field uf = Unsafe.class.getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        Unsafe unsafe = (Unsafe) uf.get(null);

        ItemStack stack = (ItemStack) unsafe.allocateInstance(ItemStack.class);
        for (Field field : ItemStack.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getType().equals(Holder.class)) {
                field.set(stack, Holder.direct(Items.PAPER));
            } else if (field.getType().equals(int.class)) {
                field.set(stack, 1); // count = 1
            } else if (field.getType().equals(PatchedDataComponentMap.class)) {
                field.set(stack, new PatchedDataComponentMap(DataComponentMap.EMPTY));
            }
        }
        stack.set(DataComponents.MAX_DAMAGE, OxygenTankItem.CAPACITY);
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }

    @Nested
    @DisplayName("Oxygen Tank Durability & Respiration Math")
    class OxygenTankTests {

        @Test
        @DisplayName("Verify full capacity and empty state parity")
        void testCapacityParity() throws Exception {
            assertEquals(600, OxygenTankItem.CAPACITY, "Tank capacity must be 600 units (10 min)");
            ItemStack tank = newTankStack();
            assertEquals(600, OxygenTankItem.getOxygen(tank), "Undamaged tank must have 600 units of oxygen");
            assertFalse(OxygenTankItem.isEmpty(tank), "Undamaged tank must not be empty");

            tank.setDamageValue(600);
            assertEquals(0, OxygenTankItem.getOxygen(tank), "Tank with 600 damage must have 0 oxygen");
            assertTrue(OxygenTankItem.isEmpty(tank), "Tank with 600 damage must be empty");
        }

        @Test
        @DisplayName("Verify exact and partial oxygen drain without stack deletion")
        void testDrainLogic() throws Exception {
            ItemStack tank = newTankStack();
            int drained = OxygenTankItem.drain(tank, 15);
            assertEquals(15, drained, "Draining 15 units must report 15 drained");
            assertEquals(585, OxygenTankItem.getOxygen(tank), "Remaining oxygen must be 585 units");
            assertEquals(15, tank.getDamageValue(), "Damage value must match consumed units");

            // Drain remaining amount plus surplus
            int overDrain = OxygenTankItem.drain(tank, 1000);
            assertEquals(585, overDrain, "Over-draining must only drain remaining units");
            assertEquals(0, OxygenTankItem.getOxygen(tank), "Remaining oxygen must be 0");
            assertEquals(600, tank.getDamageValue(), "Damage value must cap at max capacity");
            assertFalse(tank.isEmpty(), "Item stack must never be broken or deleted by drain");
        }

        @Test
        @DisplayName("Verify filling oxygen tanks up to full capacity without overfilling")
        void testFillLogic() throws Exception {
            ItemStack tank = newTankStack();
            tank.setDamageValue(600); // 0 oxygen

            int added = OxygenTankItem.fill(tank, 200);
            assertEquals(200, added, "Filling 200 units must report 200 added");
            assertEquals(200, OxygenTankItem.getOxygen(tank), "Tank must now contain 200 oxygen");
            assertEquals(400, tank.getDamageValue(), "Damage value must drop to 400");

            int surplus = OxygenTankItem.fill(tank, 1000);
            assertEquals(400, surplus, "Overfill must cap at remaining deficit (400)");
            assertEquals(600, OxygenTankItem.getOxygen(tank), "Tank must be completely full");
            assertEquals(0, tank.getDamageValue(), "Damage value must be 0 when full");
        }
    }

    @Nested
    @DisplayName("Planetary Celestial Catalog & Atmosphere Differentiation")
    class CelestialAtmosphereTests {

        @Test
        @DisplayName("Verify hard vacuum vs toxic unbreathable parameters in CelestialBodyRegistry")
        void testCatalogAtmospheres() {
            CelestialBodyRegistry registry = CelestialBodyRegistry.getInstance();

            Optional<ICelestialBody> moon = registry.getBody(ModDimensions.NEXUS_MOON);
            assertTrue(moon.isPresent(), "Nexus Moon must be charted in catalog");
            assertEquals(0.00f, moon.get().atmosphericPressure(), 0.001f);
            assertTrue(moon.get().isVacuum(), "Nexus Moon (< 0.05 atm) must be classified as vacuum");
            assertFalse(moon.get().hasBreathableAtmosphere(), "Nexus Moon air must not be breathable");
            assertTrue(moon.get().isHazardous(), "Nexus Moon must be hazardous");

            Optional<ICelestialBody> proxima = registry.getBody(ModDimensions.PROXIMA_B);
            assertTrue(proxima.isPresent(), "Proxima B must be charted in catalog");
            assertEquals(0.15f, proxima.get().atmosphericPressure(), 0.001f);
            assertFalse(proxima.get().isVacuum(), "Proxima B (0.15 atm) must NOT be hard vacuum");
            assertFalse(proxima.get().hasBreathableAtmosphere(), "Proxima B must be unbreathable");
            assertTrue(proxima.get().isHazardous(), "Proxima B must be hazardous");

            Optional<ICelestialBody> exotic = registry.getBody(ModDimensions.EXOTIC_PRIME);
            assertTrue(exotic.isPresent(), "Exotic Prime must be charted in catalog");
            assertEquals(0.85f, exotic.get().atmosphericPressure(), 0.001f);
            assertFalse(exotic.get().isVacuum(), "Exotic Prime (0.85 atm) must NOT be hard vacuum");
            assertFalse(exotic.get().hasBreathableAtmosphere(), "Exotic Prime must be toxic/unbreathable");
            assertTrue(exotic.get().isHazardous(), "Exotic Prime must be hazardous");

            Optional<ICelestialBody> gliese = registry.getBody(ModDimensions.GLIESE_DEEP);
            assertTrue(gliese.isPresent(), "Gliese Deep must be charted in catalog");
            assertEquals(2.50f, gliese.get().atmosphericPressure(), 0.001f);
            assertFalse(gliese.get().isVacuum(), "Gliese Deep (2.50 atm) must NOT be hard vacuum");
        }

        @Test
        @DisplayName("Verify AtmosphereHelper query helpers for vacuum and toxic exoplanets")
        void testAtmosphereHelperQueries() {
            assertTrue(AtmosphereHelper.isHardVacuum(ModDimensions.NEXUS_MOON), "Nexus Moon must be hard vacuum");
            assertFalse(AtmosphereHelper.isHardVacuum(ModDimensions.EXOTIC_PRIME), "Exotic Prime is not hard vacuum");
            assertFalse(AtmosphereHelper.isHardVacuum(ModDimensions.PROXIMA_B), "Proxima B is not hard vacuum");

            assertTrue(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.EXOTIC_PRIME), "Exotic Prime must be toxic");
            assertTrue(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.PROXIMA_B), "Proxima B must be toxic");
            assertFalse(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.NEXUS_MOON), "Nexus Moon is hard vacuum, not toxic atmosphere");
        }
    }

    @Nested
    @DisplayName("Oxygen Sealer 3D BFS Room Algorithm")
    class OxygenSealerAlgorithmTests {

        private Set<BlockPos> buildBoxBarriers(boolean breachedCeiling) {
            Set<BlockPos> barriers = new HashSet<>();
            // 5x5x5 hollow box: X -2..2, Y -1..3, Z -2..2
            for (int x = -2; x <= 2; x++) {
                for (int y = -1; y <= 3; y++) {
                    for (int z = -2; z <= 2; z++) {
                        if (x == -2 || x == 2 || y == -1 || y == 3 || z == -2 || z == 2) {
                            barriers.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
            // The sealer block itself is solid and not part of the interior
            barriers.add(new BlockPos(0, 0, 0));
            if (breachedCeiling) {
                barriers.remove(new BlockPos(0, 3, 0)); // 1-block breach in the ceiling
            }
            return barriers;
        }

        @Test
        @DisplayName("Verify hermetically sealed cube room resolves to sealed state")
        void testEnclosedRoomSeals() {
            BlockPos sealerPos = new BlockPos(0, 0, 0);
            Set<BlockPos> barriers = buildBoxBarriers(false);

            OxygenSealerBlockEntity.SealedRoomResult result = OxygenSealerBlockEntity.calculateSealedRoom(
                    sealerPos,
                    OxygenSealerBlockEntity.MAX_VOLUME,
                    OxygenSealerBlockEntity.MAX_HORIZONTAL_RADIUS,
                    OxygenSealerBlockEntity.MAX_VERTICAL_RADIUS,
                    barriers::contains
            );

            assertTrue(result.sealed(), "Room bounded by solid walls must be sealed");
            // Interior cavity: x∈[-1,1] × y∈[0,2] × z∈[-1,1] = 27 positions minus the sealer block = 26
            assertEquals(26, result.volume(), "Interior room volume must match enclosed cavity count");
            assertNotNull(result.bounds(), "Sealed room must calculate bounding box");
            assertTrue(result.interiorPositions().contains(new BlockPos(0, 1, 0)), "Interior must contain air above sealer");
            assertTrue(result.bounds().contains(0.5, 1.5, 0.5), "Bounds must contain interior positions");
        }

        @Test
        @DisplayName("Verify room with a 1-block breach leaks and fails sealing")
        void testBreachedRoomFails() {
            BlockPos sealerPos = new BlockPos(0, 0, 0);
            Set<BlockPos> barriers = buildBoxBarriers(true);

            OxygenSealerBlockEntity.SealedRoomResult result = OxygenSealerBlockEntity.calculateSealedRoom(
                    sealerPos,
                    OxygenSealerBlockEntity.MAX_VOLUME,
                    OxygenSealerBlockEntity.MAX_HORIZONTAL_RADIUS,
                    OxygenSealerBlockEntity.MAX_VERTICAL_RADIUS,
                    barriers::contains
            );

            assertFalse(result.sealed(), "Breached room must fail to seal");
            assertEquals(0, result.volume(), "Unsealed room must report 0 volume");
            assertNull(result.bounds(), "Unsealed room must have null bounds");
        }

        @Test
        @DisplayName("Verify room exceeding MAX_VOLUME leaks and aborts")
        void testVolumeExceededFails() {
            OxygenSealerBlockEntity.SealedRoomResult result = OxygenSealerBlockEntity.calculateSealedRoom(
                    new BlockPos(0, 0, 0),
                    50, // Small volume limit for test
                    16,
                    10,
                    pos -> false
            );

            assertFalse(result.sealed(), "Open space exceeding volume limit must fail to seal");
            assertEquals(0, result.volume());
        }

        @Test
        @DisplayName("Verify AtmosphereHelper sealed room tracking and coordinate lookup")
        void testAtmosphereHelperSealedLookup() {
            AtmosphereHelper.clearSealers();
            BlockPos testPos = new BlockPos(100, 64, 100);

            assertFalse(AtmosphereHelper.isRoomSealed(ModDimensions.NEXUS_MOON, testPos),
                    "No sealers registered initially, must report false");
        }
    }
}
