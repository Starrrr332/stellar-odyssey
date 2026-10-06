package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.registry.tiers.AlienMineralTier;
import com.amaro.stellarodyssey.registry.tiers.AlienMineralTierRegistry;
import com.amaro.stellarodyssey.registry.tiers.IAlienMineralTier;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.SoundType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Alien Mineral Tier Matrix Contract Tests")
class AlienMineralTierMatrixTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Verify all built-in Tiers 1-5 exist in AlienMineralTierRegistry by level and name")
    void testBuiltinTiersInRegistry() {
        assertEquals(5, AlienMineralTier.values().length, "Exactly 5 built-in canonical tiers must be defined");
        assertTrue(AlienMineralTierRegistry.getTierCount() >= 5, "Registry must contain at least the 5 built-in tiers");

        for (int level = 1; level <= 5; level++) {
            Optional<IAlienMineralTier> tierOpt = AlienMineralTierRegistry.getTier(level);
            assertTrue(tierOpt.isPresent(), "Tier level " + level + " must be present in registry");
            assertEquals(level, tierOpt.get().getTierLevel());
        }

        String[] expectedNames = {"celidium", "verdantite", "astralite", "voidstalker", "chronostone"};
        for (String name : expectedNames) {
            assertTrue(AlienMineralTierRegistry.getTier(name).isPresent(), "Tier name '" + name + "' must be present");
            // Test case-insensitivity
            assertTrue(AlienMineralTierRegistry.getTier(name.toUpperCase()).isPresent(),
                    "Lookup for '" + name.toUpperCase() + "' must be case-insensitive");
        }
    }

    @Test
    @DisplayName("Verify Tier 1: Celidium properties and contracts")
    void testTier1CelidiumProperties() {
        AlienMineralTier celidium = AlienMineralTier.CELIDIUM;
        assertEquals(1, celidium.getTierLevel());
        assertEquals("celidium", celidium.getName());
        assertEquals("stellarodyssey:celidium", celidium.getId().toString());
        assertEquals(4.0F, celidium.getBlockHardness(), 0.001F);
        assertEquals(6.0F, celidium.getExplosionResistance(), 0.001F);
        assertEquals(SoundType.AMETHYST, celidium.getSoundType());
        assertEquals(5, celidium.getLuminance());
        assertEquals(0xD97724, celidium.getColorHex());

        ToolMaterial tm = celidium.getToolMaterial();
        assertNotNull(tm);
        assertEquals(450, tm.durability());
        assertEquals(6.5F, tm.speed(), 0.001F);
        assertEquals(2.5F, tm.attackDamageBonus(), 0.001F);
        assertEquals(14, tm.enchantmentValue());

        assertNotNull(celidium.getArmorMaterial());
        assertNotNull(celidium.getIncorrectBlocksForDropsTag());
        assertNotNull(celidium.getRequiredMiningTierTag());
        assertNotNull(celidium.getRepairItemTag());
    }

    @Test
    @DisplayName("Verify Tier 2: Verdantite properties and contracts")
    void testTier2VerdantiteProperties() {
        AlienMineralTier verdantite = AlienMineralTier.VERDANTITE;
        assertEquals(2, verdantite.getTierLevel());
        assertEquals("verdantite", verdantite.getName());
        assertEquals("stellarodyssey:verdantite", verdantite.getId().toString());
        assertEquals(6.0F, verdantite.getBlockHardness(), 0.001F);
        assertEquals(9.0F, verdantite.getExplosionResistance(), 0.001F);
        assertEquals(SoundType.COPPER, verdantite.getSoundType());
        assertEquals(7, verdantite.getLuminance());
        assertEquals(0x22C55E, verdantite.getColorHex());

        ToolMaterial tm = verdantite.getToolMaterial();
        assertNotNull(tm);
        assertEquals(850, tm.durability());
        assertEquals(7.5F, tm.speed(), 0.001F);
        assertEquals(3.5F, tm.attackDamageBonus(), 0.001F);
        assertEquals(16, tm.enchantmentValue());

        assertNotNull(verdantite.getArmorMaterial());
        assertNotNull(verdantite.getIncorrectBlocksForDropsTag());
        assertNotNull(verdantite.getRequiredMiningTierTag());
        assertNotNull(verdantite.getRepairItemTag());
    }

    @Test
    @DisplayName("Verify Tier 3: Astralite properties and contracts")
    void testTier3AstraliteProperties() {
        AlienMineralTier astralite = AlienMineralTier.ASTRALITE;
        assertEquals(3, astralite.getTierLevel());
        assertEquals("astralite", astralite.getName());
        assertEquals("stellarodyssey:astralite", astralite.getId().toString());
        assertEquals(9.0F, astralite.getBlockHardness(), 0.001F);
        assertEquals(15.0F, astralite.getExplosionResistance(), 0.001F);
        assertEquals(SoundType.DEEPSLATE, astralite.getSoundType());
        assertEquals(9, astralite.getLuminance());
        assertEquals(0x06B6D4, astralite.getColorHex());

        ToolMaterial tm = astralite.getToolMaterial();
        assertNotNull(tm);
        assertEquals(1650, tm.durability());
        assertEquals(9.0F, tm.speed(), 0.001F);
        assertEquals(4.5F, tm.attackDamageBonus(), 0.001F);
        assertEquals(18, tm.enchantmentValue());

        assertNotNull(astralite.getArmorMaterial());
        assertNotNull(astralite.getIncorrectBlocksForDropsTag());
        assertNotNull(astralite.getRequiredMiningTierTag());
        assertNotNull(astralite.getRepairItemTag());
    }

    @Test
    @DisplayName("Verify Tier 4: Voidstalker properties and contracts")
    void testTier4VoidstalkerProperties() {
        AlienMineralTier voidstalker = AlienMineralTier.VOIDSTALKER;
        assertEquals(4, voidstalker.getTierLevel());
        assertEquals("voidstalker", voidstalker.getName());
        assertEquals("stellarodyssey:voidstalker", voidstalker.getId().toString());
        assertEquals(15.0F, voidstalker.getBlockHardness(), 0.001F);
        assertEquals(30.0F, voidstalker.getExplosionResistance(), 0.001F);
        assertEquals(SoundType.SCULK_CATALYST, voidstalker.getSoundType());
        assertEquals(12, voidstalker.getLuminance());
        assertEquals(0x7C3AED, voidstalker.getColorHex());

        ToolMaterial tm = voidstalker.getToolMaterial();
        assertNotNull(tm);
        assertEquals(2500, tm.durability());
        assertEquals(11.5F, tm.speed(), 0.001F);
        assertEquals(6.0F, tm.attackDamageBonus(), 0.001F);
        assertEquals(22, tm.enchantmentValue());

        assertNotNull(voidstalker.getArmorMaterial());
        assertNotNull(voidstalker.getIncorrectBlocksForDropsTag());
        assertNotNull(voidstalker.getRequiredMiningTierTag());
        assertNotNull(voidstalker.getRepairItemTag());
    }

    @Test
    @DisplayName("Verify Tier 5: Chronostone properties and contracts")
    void testTier5ChronostoneProperties() {
        AlienMineralTier chronostone = AlienMineralTier.CHRONOSTONE;
        assertEquals(5, chronostone.getTierLevel());
        assertEquals("chronostone", chronostone.getName());
        assertEquals("stellarodyssey:chronostone", chronostone.getId().toString());
        assertEquals(25.0F, chronostone.getBlockHardness(), 0.001F);
        assertEquals(50.0F, chronostone.getExplosionResistance(), 0.001F);
        assertEquals(SoundType.HEAVY_CORE, chronostone.getSoundType());
        assertEquals(15, chronostone.getLuminance());
        assertEquals(0xF59E0B, chronostone.getColorHex());

        ToolMaterial tm = chronostone.getToolMaterial();
        assertNotNull(tm);
        assertEquals(3600, tm.durability());
        assertEquals(14.5F, tm.speed(), 0.001F);
        assertEquals(8.0F, tm.attackDamageBonus(), 0.001F);
        assertEquals(26, tm.enchantmentValue());

        assertNotNull(chronostone.getArmorMaterial());
        assertNotNull(chronostone.getIncorrectBlocksForDropsTag());
        assertNotNull(chronostone.getRequiredMiningTierTag());
        assertNotNull(chronostone.getRepairItemTag());
    }

    @Test
    @DisplayName("Verify Monotonic Progression Invariants (VR3) across Tiers 1 through 5")
    void testMonotonicProgressionInvariants() {
        AlienMineralTier[] tiers = AlienMineralTier.values();

        for (int i = 0; i < tiers.length - 1; i++) {
            AlienMineralTier current = tiers[i];
            AlienMineralTier next = tiers[i + 1];

            // 1. Tool durability must strictly increase
            assertTrue(current.getToolMaterial().durability() < next.getToolMaterial().durability(),
                    String.format("Durability must increase from %s (%d) to %s (%d)",
                            current.getName(), current.getToolMaterial().durability(),
                            next.getName(), next.getToolMaterial().durability()));

            // 2. Tool speed must strictly increase
            assertTrue(current.getToolMaterial().speed() < next.getToolMaterial().speed(),
                    String.format("Tool speed must increase from %s (%.1f) to %s (%.1f)",
                            current.getName(), current.getToolMaterial().speed(),
                            next.getName(), next.getToolMaterial().speed()));

            // 3. Attack damage bonus must strictly increase
            assertTrue(current.getToolMaterial().attackDamageBonus() < next.getToolMaterial().attackDamageBonus(),
                    String.format("Damage bonus must increase from %s (%.1f) to %s (%.1f)",
                            current.getName(), current.getToolMaterial().attackDamageBonus(),
                            next.getName(), next.getToolMaterial().attackDamageBonus()));

            // 4. Block hardness must strictly increase
            assertTrue(current.getBlockHardness() < next.getBlockHardness(),
                    String.format("Hardness must increase from %s (%.1f) to %s (%.1f)",
                            current.getName(), current.getBlockHardness(),
                            next.getName(), next.getBlockHardness()));

            // 5. Explosion resistance must strictly increase
            assertTrue(current.getExplosionResistance() < next.getExplosionResistance(),
                    String.format("Blast resistance must increase from %s (%.1f) to %s (%.1f)",
                            current.getName(), current.getExplosionResistance(),
                            next.getName(), next.getExplosionResistance()));

            // 6. Tool enchantability must strictly increase
            assertTrue(current.getToolMaterial().enchantmentValue() < next.getToolMaterial().enchantmentValue(),
                    String.format("Enchantability must increase from %s (%d) to %s (%d)",
                            current.getName(), current.getToolMaterial().enchantmentValue(),
                            next.getName(), next.getToolMaterial().enchantmentValue()));

            // 7. Luminance must strictly increase
            assertTrue(current.getLuminance() < next.getLuminance(),
                    String.format("Luminance must increase from %s (%d) to %s (%d)",
                            current.getName(), current.getLuminance(),
                            next.getName(), next.getLuminance()));
        }
    }

    @ParameterizedTest
    @EnumSource(AlienMineralTier.class)
    @DisplayName("Verify tag integrity for all built-in tiers")
    void testTagsNonNullAndProperNamespace(AlienMineralTier tier) {
        assertNotNull(tier.getIncorrectBlocksForDropsTag(), "Incorrect tag must not be null for " + tier.getName());
        assertEquals("stellarodyssey", tier.getIncorrectBlocksForDropsTag().location().getNamespace());
        assertTrue(tier.getIncorrectBlocksForDropsTag().location().getPath().startsWith("incorrect_for_tier_"));

        assertNotNull(tier.getRequiredMiningTierTag(), "Required tier tag must not be null for " + tier.getName());
        assertNotNull(tier.getRepairItemTag(), "Repair item tag must not be null for " + tier.getName());
        assertEquals("stellarodyssey", tier.getRepairItemTag().location().getNamespace());
        assertTrue(tier.getRepairItemTag().location().getPath().endsWith("_repair"));
    }
}
