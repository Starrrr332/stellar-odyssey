package com.amaro.stellarodyssey.lifesupport;

import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import com.amaro.stellarodyssey.block.entity.OxygenSealerBlockEntity;
import com.amaro.stellarodyssey.item.OxygenTankItem;
import com.amaro.stellarodyssey.item.SpacesuitItem;
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
import net.minecraft.world.item.equipment.ArmorType;
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

@DisplayName("Milestone M1: Life Support, Planetary Atmosphere & Sealed Habitat Tests")
public class LifeSupportSystemTest {

    private static Unsafe unsafe;

    @BeforeAll
    static void init() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);
    }

    private static SpacesuitItem createSpacesuitItem(ArmorType type) throws Exception {
        SpacesuitItem item = (SpacesuitItem) unsafe.allocateInstance(SpacesuitItem.class);
        Field f = SpacesuitItem.class.getDeclaredField("armorType");
        f.setAccessible(true);
        f.set(item, type);
        return item;
    }

    private static OxygenTankItem createOxygenTankItem() throws Exception {
        return (OxygenTankItem) unsafe.allocateInstance(OxygenTankItem.class);
    }

    private static ItemStack createItemStack(Object item) throws Exception {
        ItemStack stack = (ItemStack) unsafe.allocateInstance(ItemStack.class);
        Field itemField = ItemStack.class.getDeclaredField("item");
        itemField.setAccessible(true);
        itemField.set(stack, Holder.direct(item));

        Field countField = ItemStack.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.set(stack, 1);

        Field componentsField = ItemStack.class.getDeclaredField("components");
        componentsField.setAccessible(true);
        componentsField.set(stack, new PatchedDataComponentMap(DataComponentMap.EMPTY));

        return stack;
    }

    @Nested
    @DisplayName("Oxygen Tank Durability & Respiration Math")
    class OxygenTankTests {

        @Test
        @DisplayName("Verify constant capacity value (600 seconds = 10 minutes)")
        void testCapacityConstant() {
            assertEquals(600, OxygenTankItem.CAPACITY, "Oxygen tank capacity must be exactly 600 units");
        }

        @Test
        @DisplayName("Verify getOxygen, isEmpty, drain, and fill durability contracts")
        void testDurabilityMath() throws Exception {
            OxygenTankItem tankItem = createOxygenTankItem();
            ItemStack stack = createItemStack(tankItem);
            stack.set(DataComponents.MAX_DAMAGE, OxygenTankItem.CAPACITY);
            stack.set(DataComponents.DAMAGE, 0);

            assertEquals(600, stack.getMaxDamage());
            assertEquals(0, stack.getDamageValue());
            assertEquals(600, OxygenTankItem.getOxygen(stack), "Undamaged stack must have full oxygen");
            assertFalse(OxygenTankItem.isEmpty(stack), "Undamaged stack must not be empty");

            // Drain partial
            int drained = OxygenTankItem.drain(stack, 25);
            assertEquals(25, drained, "Must drain requested amount");
            assertEquals(575, OxygenTankItem.getOxygen(stack));
            assertEquals(25, stack.getDamageValue());

            // Fill partial
            int filled = OxygenTankItem.fill(stack, 10);
            assertEquals(10, filled, "Must fill requested amount");
            assertEquals(15, stack.getDamageValue());
            assertEquals(585, OxygenTankItem.getOxygen(stack));

            // Drain remaining
            int remaining = OxygenTankItem.getOxygen(stack);
            int drainedAll = OxygenTankItem.drain(stack, remaining + 500);
            assertEquals(remaining, drainedAll, "Over-drain must cap at remaining units");
            assertEquals(0, OxygenTankItem.getOxygen(stack));
            assertTrue(OxygenTankItem.isEmpty(stack));
            assertFalse(stack.isEmpty(), "Stack must never be destroyed by drain");

            // Fill to full
            int fillAll = OxygenTankItem.fill(stack, OxygenTankItem.CAPACITY + 500);
            assertEquals(OxygenTankItem.CAPACITY, fillAll, "Overfill must cap at max damage");
            assertEquals(0, stack.getDamageValue());
            assertEquals(600, OxygenTankItem.getOxygen(stack));
        }
    }

    @Nested
    @DisplayName("Spacesuit Modular Armor & Equipment Verification")
    class SpacesuitEquipmentTests {

        @Test
        @DisplayName("Verify spacesuit piece type matching and rejection of vanilla/empty items")
        void testSpacesuitPieceMatching() throws Exception {
            ItemStack helmet = createItemStack(createSpacesuitItem(ArmorType.HELMET));
            ItemStack chestplate = createItemStack(createSpacesuitItem(ArmorType.CHESTPLATE));
            ItemStack leggings = createItemStack(createSpacesuitItem(ArmorType.LEGGINGS));
            ItemStack boots = createItemStack(createSpacesuitItem(ArmorType.BOOTS));

            assertTrue(AtmosphereHelper.isSpacesuitPiece(helmet, ArmorType.HELMET));
            assertFalse(AtmosphereHelper.isSpacesuitPiece(helmet, ArmorType.CHESTPLATE));

            assertTrue(AtmosphereHelper.isSpacesuitPiece(chestplate, ArmorType.CHESTPLATE));
            assertFalse(AtmosphereHelper.isSpacesuitPiece(chestplate, ArmorType.HELMET));

            assertTrue(AtmosphereHelper.isSpacesuitPiece(leggings, ArmorType.LEGGINGS));
            assertTrue(AtmosphereHelper.isSpacesuitPiece(boots, ArmorType.BOOTS));

            // Rejection of empty stacks
            assertFalse(AtmosphereHelper.isSpacesuitPiece(ItemStack.EMPTY, ArmorType.HELMET));
        }

        @Test
        @DisplayName("Verify lore roles: manifold chestplate and visor helmet")
        void testSpacesuitRoles() throws Exception {
            SpacesuitItem chest = createSpacesuitItem(ArmorType.CHESTPLATE);
            assertEquals(ArmorType.CHESTPLATE, chest.getArmorType());

            SpacesuitItem helm = createSpacesuitItem(ArmorType.HELMET);
            assertEquals(ArmorType.HELMET, helm.getArmorType());
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

        @Test
        @DisplayName("Verify hermetically sealed cube room (3x3x3 hollow) resolves to sealed state")
        void testEnclosedRoomSeals() {
            BlockPos sealerPos = new BlockPos(0, 0, 0);
            Set<BlockPos> barriers = new HashSet<>();

            // Build a 5x5x5 solid hollow box around sealer:
            // X from -2 to 2, Y from -1 to 3, Z from -2 to 2
            int minX = -2, maxX = 2;
            int minY = -1, maxY = 3;
            int minZ = -2, maxZ = 2;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                            barriers.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
            barriers.add(sealerPos);

            // Algorithm checks if surrounding space is enclosed
            OxygenSealerBlockEntity.SealedRoomResult result = OxygenSealerBlockEntity.calculateSealedRoom(
                    sealerPos,
                    OxygenSealerBlockEntity.MAX_VOLUME,
                    OxygenSealerBlockEntity.MAX_HORIZONTAL_RADIUS,
                    OxygenSealerBlockEntity.MAX_VERTICAL_RADIUS,
                    barriers::contains
            );

            assertTrue(result.sealed(), "Room bounded by solid walls must be sealed");
            // Interior volume: 3 * 3 * 3 = 27 blocks minus 1 for sealer position = 26 interior positions
            assertEquals(26, result.volume(), "Interior room volume must match enclosed cavity count");
            assertNotNull(result.bounds(), "Sealed room must calculate bounding box");
            assertTrue(result.interiorPositions().contains(new BlockPos(0, 1, 0)), "Interior must contain air above sealer");
            assertTrue(result.bounds().contains(0.5, 1.5, 0.5), "Bounds must contain interior positions");
        }

        @Test
        @DisplayName("Verify room with a 1-block breach leaks and fails sealing")
        void testBreachedRoomFails() {
            BlockPos sealerPos = new BlockPos(0, 0, 0);
            Set<BlockPos> barriers = new HashSet<>();

            int minX = -2, maxX = 2;
            int minY = -1, maxY = 3;
            int minZ = -2, maxZ = 2;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                            barriers.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
            barriers.add(sealerPos);

            // Create a breach in the ceiling
            barriers.remove(new BlockPos(0, maxY, 0));

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
            BlockPos sealerPos = new BlockPos(0, 0, 0);
            // No barriers at all (open space)
            OxygenSealerBlockEntity.SealedRoomResult result = OxygenSealerBlockEntity.calculateSealedRoom(
                    sealerPos,
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
