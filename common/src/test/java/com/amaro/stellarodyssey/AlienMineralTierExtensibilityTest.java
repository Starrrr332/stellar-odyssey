package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.block.AlienMineralBlock;
import com.amaro.stellarodyssey.registry.tiers.AlienMineralTier;
import com.amaro.stellarodyssey.registry.tiers.AlienMineralTierRegistry;
import com.amaro.stellarodyssey.registry.tiers.IAlienMineralTier;
import com.amaro.stellarodyssey.registry.tiers.ModArmorMaterials;
import com.amaro.stellarodyssey.registry.tiers.SimpleAlienMineralTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.SoundType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Alien Mineral Tier Extensibility Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AlienMineralTierExtensibilityTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final int TIER_6_LEVEL = 6;
    private static final String TIER_6_NAME = "neutronium";

    private static SimpleAlienMineralTier createNeutroniumTier() {
        ArmorMaterial neutroniumArmor = ModArmorMaterials.create(
                60,
                Map.of(
                        ArmorType.BOOTS, 6,
                        ArmorType.LEGGINGS, 9,
                        ArmorType.CHESTPLATE, 12,
                        ArmorType.HELMET, 6
                ),
                30,
                SoundEvents.ARMOR_EQUIP_NETHERITE,
                6.0F,
                0.35F,
                ModArmorMaterials.repairTag("neutronium_repair"),
                ModArmorMaterials.asset("neutronium")
        );

        ToolMaterial neutroniumTool = new ToolMaterial(
                TagKey.create(Registries.BLOCK, StellarOdyssey.id("incorrect_for_tier_6_tool")),
                5000,
                18.0F,
                10.0F,
                30,
                TagKey.create(Registries.ITEM, StellarOdyssey.id("neutronium_repair"))
        );

        return new SimpleAlienMineralTier(
                TIER_6_LEVEL,
                TIER_6_NAME,
                StellarOdyssey.id(TIER_6_NAME),
                Component.literal("Neutronium"),
                neutroniumTool,
                neutroniumArmor,
                TagKey.create(Registries.BLOCK, StellarOdyssey.id("incorrect_for_tier_6_tool")),
                TagKey.create(Registries.BLOCK, StellarOdyssey.id("needs_tier_5_tool")),
                40.0F,
                100.0F,
                SoundType.HEAVY_CORE,
                15,
                0x00E5FF,
                "Dense Neutron Star Remnant"
        );
    }

    @Test
    @Order(1)
    @DisplayName("Verify dynamic registration of custom Tier 6 Neutronium into AlienMineralTierRegistry")
    void testDynamicRegistrationOfTier6() {
        SimpleAlienMineralTier neutronium = createNeutroniumTier();

        // Register if not already present
        if (!AlienMineralTierRegistry.hasTier(TIER_6_LEVEL)) {
            assertDoesNotThrow(() -> AlienMineralTierRegistry.registerTier(neutronium),
                    "AlienMineralTierRegistry must support runtime registration of custom tiers without engine modifications");
        }

        // Verify lookup by level
        Optional<IAlienMineralTier> byLevel = AlienMineralTierRegistry.getTier(TIER_6_LEVEL);
        assertTrue(byLevel.isPresent(), "Custom Tier 6 must be retrievable by level");
        assertEquals(TIER_6_LEVEL, byLevel.get().getTierLevel());
        assertEquals(TIER_6_NAME, byLevel.get().getName());

        // Verify lookup by name (both lowercase and uppercase)
        Optional<IAlienMineralTier> byName = AlienMineralTierRegistry.getTier(TIER_6_NAME);
        assertTrue(byName.isPresent(), "Custom Tier 6 must be retrievable by name");
        assertEquals(TIER_6_NAME, byName.get().getName());

        Optional<IAlienMineralTier> byNameUpper = AlienMineralTierRegistry.getTier("NEUTRONIUM");
        assertTrue(byNameUpper.isPresent(), "Custom Tier 6 name lookup must be case-insensitive");

        // Verify boolean query methods
        assertTrue(AlienMineralTierRegistry.hasTier(TIER_6_LEVEL));
        assertTrue(AlienMineralTierRegistry.hasTier(TIER_6_NAME));
        assertTrue(AlienMineralTierRegistry.hasTier("Neutronium"));

        // Verify getAllTiers includes custom tier
        assertTrue(AlienMineralTierRegistry.getAllTiers().contains(byLevel.get()));
        assertTrue(AlienMineralTierRegistry.getTierCount() >= 6);
    }

    @Test
    @Order(2)
    @DisplayName("Verify custom Tier 6 properties match expected specification")
    void testTier6PropertiesRetrieval() {
        IAlienMineralTier tier6 = AlienMineralTierRegistry.getTier(TIER_6_LEVEL).orElseThrow();

        assertEquals(6, tier6.getTierLevel());
        assertEquals("neutronium", tier6.getName());
        assertEquals("stellarodyssey:neutronium", tier6.getId().toString());
        assertEquals(40.0F, tier6.getBlockHardness(), 0.001F);
        assertEquals(100.0F, tier6.getExplosionResistance(), 0.001F);
        assertEquals(SoundType.HEAVY_CORE, tier6.getSoundType());
        assertEquals(15, tier6.getLuminance());
        assertEquals(0x00E5FF, tier6.getColorHex());
        assertEquals("Dense Neutron Star Remnant", tier6.getCelestialOrigin());

        ToolMaterial tm = tier6.getToolMaterial();
        assertNotNull(tm);
        assertEquals(5000, tm.durability());
        assertEquals(18.0F, tm.speed(), 0.001F);
        assertEquals(10.0F, tm.attackDamageBonus(), 0.001F);
        assertEquals(30, tm.enchantmentValue());

        // Verify Tier 6 continues monotonic progression above Tier 5 (Chronostone)
        AlienMineralTier tier5 = AlienMineralTier.CHRONOSTONE;
        assertTrue(tier6.getBlockHardness() > tier5.getBlockHardness());
        assertTrue(tier6.getExplosionResistance() > tier5.getExplosionResistance());
        assertTrue(tier6.getToolMaterial().durability() > tier5.getToolMaterial().durability());
        assertTrue(tier6.getToolMaterial().speed() > tier5.getToolMaterial().speed());
        assertTrue(tier6.getToolMaterial().attackDamageBonus() > tier5.getToolMaterial().attackDamageBonus());
        assertTrue(tier6.getToolMaterial().enchantmentValue() > tier5.getToolMaterial().enchantmentValue());
    }

    @Test
    @Order(3)
    @DisplayName("Verify conflict rejection and fail-fast invariants on duplicate registrations")
    void testConflictRejection() {
        // 1. Conflict on duplicate tier level
        SimpleAlienMineralTier duplicateLevelTier = new SimpleAlienMineralTier(
                TIER_6_LEVEL,
                "different_name",
                StellarOdyssey.id("different_name"),
                Component.literal("Different"),
                AlienMineralTier.CHRONOSTONE.getToolMaterial(),
                ModArmorMaterials.CHRONOSTONE,
                AlienMineralTier.INCORRECT_FOR_TIER_5_TOOL,
                AlienMineralTier.NEEDS_TIER_5_TOOL,
                30.0F, 60.0F, SoundType.STONE, 0, 0x000000, "Origin"
        );
        IllegalArgumentException levelEx = assertThrows(IllegalArgumentException.class,
                () -> AlienMineralTierRegistry.registerTier(duplicateLevelTier),
                "Registering a tier with an already registered level must throw IllegalArgumentException");
        assertTrue(levelEx.getMessage().contains("Duplicate mineral tier level: " + TIER_6_LEVEL));

        // 2. Conflict on duplicate tier name
        SimpleAlienMineralTier duplicateNameTier = new SimpleAlienMineralTier(
                7,
                TIER_6_NAME, // "neutronium"
                StellarOdyssey.id("neutronium_v2"),
                Component.literal("Neutronium V2"),
                AlienMineralTier.CHRONOSTONE.getToolMaterial(),
                ModArmorMaterials.CHRONOSTONE,
                AlienMineralTier.INCORRECT_FOR_TIER_5_TOOL,
                AlienMineralTier.NEEDS_TIER_5_TOOL,
                30.0F, 60.0F, SoundType.STONE, 0, 0x000000, "Origin"
        );
        IllegalArgumentException nameEx = assertThrows(IllegalArgumentException.class,
                () -> AlienMineralTierRegistry.registerTier(duplicateNameTier),
                "Registering a tier with an already registered name must throw IllegalArgumentException");
        assertTrue(nameEx.getMessage().contains("Duplicate mineral tier name"));

        // 3. Conflict on built-in tier level
        SimpleAlienMineralTier duplicateBuiltin = new SimpleAlienMineralTier(
                1,
                "new_tier_1",
                StellarOdyssey.id("new_tier_1"),
                Component.literal("New Tier 1"),
                AlienMineralTier.CELIDIUM.getToolMaterial(),
                ModArmorMaterials.CELIDIUM,
                AlienMineralTier.INCORRECT_FOR_TIER_1_TOOL,
                AlienMineralTier.NEEDS_TIER_1_TOOL,
                1.0F, 1.0F, SoundType.STONE, 0, 0x000000, "Origin"
        );
        assertThrows(IllegalArgumentException.class,
                () -> AlienMineralTierRegistry.registerTier(duplicateBuiltin),
                "Overwriting built-in tier level 1 must be rejected");

        // 4. Null tier rejection
        assertThrows(NullPointerException.class,
                () -> AlienMineralTierRegistry.registerTier(null),
                "Registering null must throw NullPointerException");
    }

    @Test
    @Order(4)
    @DisplayName("Verify AlienMineralBlock binds dynamically to custom registered tier")
    void testAlienMineralBlockWithCustomTier() {
        IAlienMineralTier tier6 = AlienMineralTierRegistry.getTier(TIER_6_LEVEL).orElseThrow();

        AlienMineralBlock block = new AlienMineralBlock(tier6);
        assertNotNull(block);
        assertEquals(tier6, block.getTier());

        assertDoesNotThrow(() -> AlienMineralBlock.propertiesForTier(tier6),
                "AlienMineralBlock.propertiesForTier must construct without errors for custom tiers");
    }
}
