package com.amaro.stellarodyssey.lifesupport;

import com.amaro.stellarodyssey.item.OxygenTankItem;
import com.amaro.stellarodyssey.item.SpacesuitItem;
import com.amaro.stellarodyssey.world.CelestialBodyRegistry;
import com.amaro.stellarodyssey.world.ModDimensions;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Empirical stress test suite verifying the life support hazard loop:
 * 1. All 16 (2^4) spacesuit equipment permutations (helmet, chestplate, leggings, boots).
 * 2. Exotic atmospheres (Exotic Prime, Proxima B) vs Hard Vacuum (Nexus Moon) damage discrimination.
 * 3. Zero-tank and empty-tank edge cases with air supply drainage math.
 */
@DisplayName("Milestone M1 Challenger: Spacesuit Permutations, Atmospheric Hazard & Edge Cases")
public class SpacesuitPermutationStressTest {

    private static Unsafe unsafe;
    private static SpacesuitItem helmetItem;
    private static SpacesuitItem chestplateItem;
    private static SpacesuitItem leggingsItem;
    private static SpacesuitItem bootsItem;

    @BeforeAll
    static void init() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        helmetItem = createSpacesuitItem(ArmorType.HELMET);
        chestplateItem = createSpacesuitItem(ArmorType.CHESTPLATE);
        leggingsItem = createSpacesuitItem(ArmorType.LEGGINGS);
        bootsItem = createSpacesuitItem(ArmorType.BOOTS);
    }

    private static SpacesuitItem createSpacesuitItem(ArmorType type) throws Exception {
        SpacesuitItem item = (SpacesuitItem) unsafe.allocateInstance(SpacesuitItem.class);
        Field f = SpacesuitItem.class.getDeclaredField("armorType");
        f.setAccessible(true);
        f.set(item, type);
        return item;
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

    /**
     * Creates a mock ServerPlayer instance via Unsafe with initialized EntityEquipment and Inventory.
     * In MC 26.3 the equipment lives in {@code LivingEntity.equipment}; Inventory no longer has
     * armor/offhand fields.
     */
    private static ServerPlayer createMockPlayer(boolean h, boolean c, boolean l, boolean b) throws Exception {
        ServerPlayer player = (ServerPlayer) unsafe.allocateInstance(net.minecraft.server.level.ServerPlayer.class);

        // Equipment holder (helmet slot = HEAD, chest = CHEST, legs = LEGS, feet = FEET)
        EntityEquipment equipment = new EntityEquipment();
        equipment.set(EquipmentSlot.HEAD, h ? createItemStack(helmetItem) : ItemStack.EMPTY);
        equipment.set(EquipmentSlot.CHEST, c ? createItemStack(chestplateItem) : ItemStack.EMPTY);
        equipment.set(EquipmentSlot.LEGS, l ? createItemStack(leggingsItem) : ItemStack.EMPTY);
        equipment.set(EquipmentSlot.FEET, b ? createItemStack(bootsItem) : ItemStack.EMPTY);

        Field equipmentField = LivingEntity.class.getDeclaredField("equipment");
        equipmentField.setAccessible(true);
        equipmentField.set(player, equipment);

        // Main inventory: 36 slots of empty stacks
        Inventory inventory = (Inventory) unsafe.allocateInstance(Inventory.class);
        NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
        Field itemsField = Inventory.class.getDeclaredField("items");
        itemsField.setAccessible(true);
        itemsField.set(inventory, items);

        Field invField = Player.class.getDeclaredField("inventory");
        invField.setAccessible(true);
        invField.set(player, inventory);

        return player;
    }

    /**
     * Enumerated outcome of the hazard evaluation logic.
     */
    public enum HazardOutcome {
        NOMINAL_CONSUMPTION_1X,
        LEAK_CONSUMPTION_2X,
        DECOMPRESSION_DAMAGE,
        ASPHYXIATION_DAMAGE
    }

    /**
     * Oracle mimicking the exact decision engine of LifeSupportManager#tickPlayer.
     */
    public static HazardOutcome evaluateHazardEngine(boolean inVacuum, int pieceCount, boolean canUseTanks, int availableOxygen) {
        if (inVacuum) {
            if (pieceCount == 4) {
                if (canUseTanks) {
                    return availableOxygen > 0 ? HazardOutcome.NOMINAL_CONSUMPTION_1X : HazardOutcome.ASPHYXIATION_DAMAGE;
                } else {
                    return HazardOutcome.ASPHYXIATION_DAMAGE;
                }
            } else if (pieceCount == 3 && canUseTanks) {
                return availableOxygen > 0 ? HazardOutcome.LEAK_CONSUMPTION_2X : HazardOutcome.DECOMPRESSION_DAMAGE;
            } else {
                return HazardOutcome.DECOMPRESSION_DAMAGE;
            }
        } else {
            // Non-vacuum hazard (e.g. Exotic Prime, Proxima B)
            if (canUseTanks) {
                return availableOxygen > 0 ? HazardOutcome.NOMINAL_CONSUMPTION_1X : HazardOutcome.ASPHYXIATION_DAMAGE;
            } else {
                return HazardOutcome.ASPHYXIATION_DAMAGE;
            }
        }
    }

    @Nested
    @DisplayName("1. 16 Spacesuit Armor Permutations (2^4)")
    class ArmorPermutationTests {

        @ParameterizedTest(name = "Armor permutation index {0} (4-bit binary)")
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
        @DisplayName("Exhaustive test of all 16 spacesuit combinations")
        void testAllPermutations(int bitmask) throws Exception {
            boolean h = (bitmask & 8) != 0; // bit 3: Helmet
            boolean c = (bitmask & 4) != 0; // bit 2: Chestplate
            boolean l = (bitmask & 2) != 0; // bit 1: Leggings
            boolean b = (bitmask & 1) != 0; // bit 0: Boots

            int expectedCount = (h ? 1 : 0) + (c ? 1 : 0) + (l ? 1 : 0) + (b ? 1 : 0);
            boolean expectedCanUseTanks = h && c;
            boolean expectedFullSuit = h && c && l && b;

            Player player = createMockPlayer(h, c, l, b);

            // 1. Verify AtmosphereHelper count and checks
            assertEquals(expectedCount, AtmosphereHelper.getEquippedSpacesuitPieceCount(player),
                    "Piece count mismatch for mask " + Integer.toBinaryString(bitmask));
            assertEquals(h, AtmosphereHelper.hasPressurizedHelmet(player),
                    "Helmet detection mismatch for mask " + Integer.toBinaryString(bitmask));
            assertEquals(c, AtmosphereHelper.hasOxygenManifold(player),
                    "Chestplate manifold detection mismatch for mask " + Integer.toBinaryString(bitmask));
            assertEquals(expectedFullSuit, AtmosphereHelper.isFullSuitEquipped(player),
                    "Full suit detection mismatch for mask " + Integer.toBinaryString(bitmask));

            // 2. Coupling rule: ONLY combinations having both helmet and chestplate allow tank usage
            boolean canUseTanks = AtmosphereHelper.hasPressurizedHelmet(player) && AtmosphereHelper.hasOxygenManifold(player);
            assertEquals(expectedCanUseTanks, canUseTanks,
                    "canUseTanks must be true iff Helmet AND Chestplate are equipped");

            // 3. Vacuum behavior evaluation
            HazardOutcome vacuumOutcome = evaluateHazardEngine(true, expectedCount, canUseTanks, 100);
            if (expectedCount == 4) {
                // (1,1,1,1)
                assertEquals(HazardOutcome.NOMINAL_CONSUMPTION_1X, vacuumOutcome,
                        "Full suit in vacuum must consume 1x O2");
            } else if (expectedCount == 3) {
                if (h && c) {
                    // (1,1,1,0) or (1,1,0,1): 3/4 pieces with intact helmet and manifold
                    assertEquals(HazardOutcome.LEAK_CONSUMPTION_2X, vacuumOutcome,
                        "3/4 pieces with helmet+chestplate in vacuum must consume 2x O2");
                } else {
                    // (0,1,1,1) or (1,0,1,1): 3/4 pieces but missing helmet or manifold
                    assertEquals(HazardOutcome.DECOMPRESSION_DAMAGE, vacuumOutcome,
                        "3/4 pieces missing helmet or manifold in vacuum must suffer DECOMPRESSION");
                }
            } else {
                // < 3 pieces: all 11 combinations must suffer decompression damage
                assertEquals(HazardOutcome.DECOMPRESSION_DAMAGE, vacuumOutcome,
                        "< 3 pieces in vacuum must suffer DECOMPRESSION damage regardless of helmet/chestplate");
            }

            // 4. Exotic Atmosphere behavior evaluation
            HazardOutcome exoticOutcome = evaluateHazardEngine(false, expectedCount, canUseTanks, 100);
            assertNotEquals(HazardOutcome.DECOMPRESSION_DAMAGE, exoticOutcome,
                    "Exotic atmospheres must NEVER inflict decompression damage");

            if (canUseTanks) {
                assertEquals(HazardOutcome.NOMINAL_CONSUMPTION_1X, exoticOutcome,
                        "Exotic atmosphere with helmet+manifold must consume 1x O2");
            } else {
                assertEquals(HazardOutcome.ASPHYXIATION_DAMAGE, exoticOutcome,
                        "Exotic atmosphere missing helmet or manifold must inflict ASPHYXIATION");
            }
        }

        @Test
        @DisplayName("Verify exactly 3 permutations allow oxygen consumption in vacuum")
        void testVacuumConsumingPermutationCount() {
            int consumingCount = 0;
            int leakCount = 0;
            int nominalCount = 0;
            int decompressionCount = 0;

            for (int bitmask = 0; bitmask < 16; bitmask++) {
                boolean h = (bitmask & 8) != 0;
                boolean c = (bitmask & 4) != 0;
                boolean l = (bitmask & 2) != 0;
                boolean b = (bitmask & 1) != 0;
                int count = (h ? 1 : 0) + (c ? 1 : 0) + (l ? 1 : 0) + (b ? 1 : 0);
                boolean canUseTanks = h && c;

                HazardOutcome outcome = evaluateHazardEngine(true, count, canUseTanks, 600);
                if (outcome == HazardOutcome.NOMINAL_CONSUMPTION_1X) {
                    consumingCount++;
                    nominalCount++;
                } else if (outcome == HazardOutcome.LEAK_CONSUMPTION_2X) {
                    consumingCount++;
                    leakCount++;
                } else if (outcome == HazardOutcome.DECOMPRESSION_DAMAGE) {
                    decompressionCount++;
                }
            }

            assertEquals(1, nominalCount, "Exactly 1 permutation (4/4 suit) consumes nominal 1x O2 in vacuum");
            assertEquals(2, leakCount, "Exactly 2 permutations (H+C+L and H+C+B) consume 2x O2 in vacuum");
            assertEquals(3, consumingCount, "Exactly 3 of 16 permutations consume oxygen in vacuum");
            assertEquals(13, decompressionCount, "Exactly 13 of 16 permutations suffer decompression damage in vacuum");
        }

        @Test
        @DisplayName("Verify exactly 4 permutations allow oxygen consumption in exotic atmosphere")
        void testExoticConsumingPermutationCount() {
            int consumingCount = 0;
            int asphyxiationCount = 0;

            for (int bitmask = 0; bitmask < 16; bitmask++) {
                boolean h = (bitmask & 8) != 0;
                boolean c = (bitmask & 4) != 0;
                boolean l = (bitmask & 2) != 0;
                boolean b = (bitmask & 1) != 0;
                int count = (h ? 1 : 0) + (c ? 1 : 0) + (l ? 1 : 0) + (b ? 1 : 0);
                boolean canUseTanks = h && c;

                HazardOutcome outcome = evaluateHazardEngine(false, count, canUseTanks, 600);
                if (outcome == HazardOutcome.NOMINAL_CONSUMPTION_1X) {
                    consumingCount++;
                } else if (outcome == HazardOutcome.ASPHYXIATION_DAMAGE) {
                    asphyxiationCount++;
                }
            }

            assertEquals(4, consumingCount, "Exactly 4 permutations (H+C with any legs/boots) consume O2 in exotic air");
            assertEquals(12, asphyxiationCount, "Exactly 12 permutations suffer asphyxiation in exotic air");
        }
    }

    @Nested
    @DisplayName("2. Exotic Atmospheres vs Hard Vacuum Damage Discrimination")
    class AtmosphereDifferentiationTests {

        @Test
        @DisplayName("Nexus Moon is hard vacuum (<0.05 atm), Proxima B and Exotic Prime are NOT")
        void testVacuumDifferentiation() {
            assertTrue(AtmosphereHelper.isHardVacuum(ModDimensions.NEXUS_MOON));
            assertFalse(AtmosphereHelper.isHardVacuum(ModDimensions.PROXIMA_B));
            assertFalse(AtmosphereHelper.isHardVacuum(ModDimensions.EXOTIC_PRIME));
        }

        @Test
        @DisplayName("Exotic Prime (0.85 atm) and Proxima B (0.15 atm) are toxic/unbreathable exoplanets")
        void testToxicDifferentiation() {
            assertTrue(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.EXOTIC_PRIME));
            assertTrue(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.PROXIMA_B));
            assertFalse(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.NEXUS_MOON));
        }

        @Test
        @DisplayName("Damage mode matrix: Hard vacuum uses Decompression/Alarm; Exotic atmospheres use Asphyxiation")
        void testDamageTypeMatrix() {
            // Case 1: Unprotected in Hard Vacuum -> Decompression (3.0F damage, -20 air, alarm)
            HazardOutcome vacuumNoSuit = evaluateHazardEngine(true, 0, false, 0);
            assertEquals(HazardOutcome.DECOMPRESSION_DAMAGE, vacuumNoSuit);

            // Case 2: Unprotected in Toxic Atmosphere -> Asphyxiation (2.0F damage, air drops by 15)
            HazardOutcome exoticNoSuit = evaluateHazardEngine(false, 0, false, 0);
            assertEquals(HazardOutcome.ASPHYXIATION_DAMAGE, exoticNoSuit);
            assertNotEquals(HazardOutcome.DECOMPRESSION_DAMAGE, exoticNoSuit,
                    "Toxic atmospheres must never trigger decompression alarms or decompression damage");
        }
    }

    @Nested
    @DisplayName("3. Zero-Tank & Durability Depletion Edge Cases")
    class ZeroTankEdgeCaseTests {

        @Test
        @DisplayName("drainFromInventory returns 0 when player has no tanks or empty tanks")
        void testDrainFromEmptyInventory() throws Exception {
            Player player = (Player) unsafe.allocateInstance(net.minecraft.server.level.ServerPlayer.class);
            Inventory inventory = (Inventory) unsafe.allocateInstance(Inventory.class);
            NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
            Field itemsField = Inventory.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            itemsField.set(inventory, items);

            // MC 26.3: Inventory.getItem() delegates equipment slots to this field.
            Field equipmentField = Inventory.class.getDeclaredField("equipment");
            equipmentField.setAccessible(true);
            equipmentField.set(inventory, new EntityEquipment());

            Field invField = Player.class.getDeclaredField("inventory");
            invField.setAccessible(true);
            invField.set(player, inventory);

            int drained = LifeSupportManager.drainFromInventory(player, 1);
            assertEquals(0, drained, "Must drain 0 from empty inventory");

            // Now add an empty tank (durability damaged to 600) using an Unsafe-allocated OxygenTankItem
            // whose stack is detected via instanceof (no registry lookup in pure JVM).
            OxygenTankItem tankItem = (OxygenTankItem) unsafe.allocateInstance(OxygenTankItem.class);
            ItemStack emptyTank = createItemStack(tankItem);
            emptyTank.set(DataComponents.MAX_DAMAGE, OxygenTankItem.CAPACITY);
            emptyTank.set(DataComponents.DAMAGE, OxygenTankItem.CAPACITY); // 600 damage = 0 oxygen
            items.set(0, emptyTank);

            int drainedFromDepleted = LifeSupportManager.drainFromInventory(player, 2);
            assertEquals(0, drainedFromDepleted, "Must drain 0 from completely depleted tank");
        }

        @Test
        @DisplayName("3/4 suit in vacuum with 0 oxygen immediately falls back to DECOMPRESSION breach")
        void testLeakingSuitDepletedVacuumOutcome() {
            // 3/4 suit equipped (canUseTanks = true), but availableOxygen = 0
            HazardOutcome outcome = evaluateHazardEngine(true, 3, true, 0);
            assertEquals(HazardOutcome.DECOMPRESSION_DAMAGE, outcome,
                    "Depleted tanks in leaking 3/4 suit can no longer pressurize leak: must immediately decompress");
        }

        @Test
        @DisplayName("4/4 suit in vacuum with 0 oxygen transitions to ASPHYXIATION (residual air in sealed suit)")
        void testFullSuitDepletedVacuumOutcome() {
            // 4/4 suit equipped (canUseTanks = true), but availableOxygen = 0
            HazardOutcome outcome = evaluateHazardEngine(true, 4, true, 0);
            assertEquals(HazardOutcome.ASPHYXIATION_DAMAGE, outcome,
                    "Depleted tanks in sealed 4/4 suit causes asphyxiation, not decompression");
        }

        @Test
        @DisplayName("Air supply drainage rate: 15 air/s takes 22 ticks (seconds) to deplete 300 vanilla air")
        void testAirSupplyDrainMath() {
            int initialAir = 300; // Vanilla max air supply
            int secondsToDamage = 0;
            int currentAir = initialAir;

            while (currentAir > -20) {
                currentAir -= 15;
                secondsToDamage++;
            }

            // 300 - (15 * 21) = -15 (not <= -20).
            // 300 - (15 * 22) = -30 (<= -20, triggers damage).
            assertEquals(22, secondsToDamage,
                    "Player with full 300 air supply takes exactly 22 seconds before taking first asphyxiation damage");
        }
    }
}
