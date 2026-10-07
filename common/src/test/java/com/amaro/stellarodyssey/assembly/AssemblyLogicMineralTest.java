package com.amaro.stellarodyssey.assembly;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.registry.tiers.AlienMineralTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTiers;
import com.amaro.stellarodyssey.rocket.AssemblyLogic;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssemblyLogic Mineral & Ingot Requirement Specification Tests")
class AssemblyLogicMineralTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    @DisplayName("1. Required Alien Minerals Specification")
    class RequiredMineralsTests {

        @Test
        @DisplayName("Tier 1 rocket requires no alien minerals (Overworld iron/copper)")
        void testTier1RequiredMinerals() {
            RocketTier t1 = RocketTiers.TIER_1.get();
            List<AlienMineralTier> minerals = AssemblyLogic.getRequiredMinerals(t1);
            assertTrue(minerals.isEmpty(), "Tier 1 rocket should not require alien minerals");
            assertTrue(AssemblyLogic.getRequiredMinerals(1).isEmpty());
        }

        @Test
        @DisplayName("Tier 2 rocket requires Celidium from Nexus Moon")
        void testTier2RequiredMinerals() {
            RocketTier t2 = RocketTiers.TIER_2.get();
            List<AlienMineralTier> minerals = AssemblyLogic.getRequiredMinerals(t2);
            assertEquals(1, minerals.size(), "Tier 2 rocket should require exactly 1 alien mineral");
            assertEquals(AlienMineralTier.CELIDIUM, minerals.get(0));
            assertEquals(List.of(AlienMineralTier.CELIDIUM), AssemblyLogic.getRequiredMinerals(2));
        }

        @Test
        @DisplayName("Tier 3 rocket requires both Verdantite and Astralite")
        void testTier3RequiredMinerals() {
            RocketTier t3 = RocketTiers.TIER_3.get();
            List<AlienMineralTier> minerals = AssemblyLogic.getRequiredMinerals(t3);
            assertEquals(2, minerals.size(), "Tier 3 rocket should require 2 alien minerals");
            assertTrue(minerals.contains(AlienMineralTier.VERDANTITE), "Tier 3 must require Verdantite");
            assertTrue(minerals.contains(AlienMineralTier.ASTRALITE), "Tier 3 must require Astralite");
            assertEquals(List.of(AlienMineralTier.VERDANTITE, AlienMineralTier.ASTRALITE), AssemblyLogic.getRequiredMinerals(3));
        }

        @Test
        @DisplayName("Null tier or unregistered levels return empty mineral list")
        void testUnknownTiersReturnEmptyMinerals() {
            assertTrue(AssemblyLogic.getRequiredMinerals((RocketTier) null).isEmpty());
            assertTrue(AssemblyLogic.getRequiredMinerals(0).isEmpty());
            assertTrue(AssemblyLogic.getRequiredMinerals(4).isEmpty());
            assertTrue(AssemblyLogic.getRequiredMinerals(-1).isEmpty());
        }
    }

    @Nested
    @DisplayName("2. Required Ingot Item Identifiers Specification")
    class RequiredIngotsTests {

        @Test
        @DisplayName("Tier 1 rocket requires vanilla iron ingot")
        void testTier1Ingots() {
            RocketTier t1 = RocketTiers.TIER_1.get();
            List<Identifier> ingots = AssemblyLogic.getRequiredIngotIds(t1);
            assertEquals(1, ingots.size());
            assertEquals(Identifier.parse("minecraft:iron_ingot"), ingots.get(0));
            assertEquals(List.of(Identifier.parse("minecraft:iron_ingot")), AssemblyLogic.getRequiredIngotIds(1));
        }

        @Test
        @DisplayName("Tier 2 rocket requires celidium_ingot")
        void testTier2Ingots() {
            RocketTier t2 = RocketTiers.TIER_2.get();
            List<Identifier> ingots = AssemblyLogic.getRequiredIngotIds(t2);
            assertEquals(1, ingots.size());
            assertEquals(StellarOdyssey.id("celidium_ingot"), ingots.get(0));
            assertEquals(List.of(StellarOdyssey.id("celidium_ingot")), AssemblyLogic.getRequiredIngotIds(2));
        }

        @Test
        @DisplayName("Tier 3 rocket requires verdantite_ingot and astralite_ingot")
        void testTier3Ingots() {
            RocketTier t3 = RocketTiers.TIER_3.get();
            List<Identifier> ingots = AssemblyLogic.getRequiredIngotIds(t3);
            assertEquals(2, ingots.size());
            assertTrue(ingots.contains(StellarOdyssey.id("verdantite_ingot")));
            assertTrue(ingots.contains(StellarOdyssey.id("astralite_ingot")));
            assertEquals(List.of(StellarOdyssey.id("verdantite_ingot"), StellarOdyssey.id("astralite_ingot")),
                    AssemblyLogic.getRequiredIngotIds(3));
        }

        @Test
        @DisplayName("Null tier or invalid levels return empty ingot list")
        void testUnknownTiersReturnEmptyIngots() {
            assertTrue(AssemblyLogic.getRequiredIngotIds((RocketTier) null).isEmpty());
            assertTrue(AssemblyLogic.getRequiredIngotIds(0).isEmpty());
            assertTrue(AssemblyLogic.getRequiredIngotIds(-1).isEmpty());
            assertTrue(AssemblyLogic.getRequiredIngotIds(4).isEmpty());
        }
    }

    @Nested
    @DisplayName("3. isMineralUsedInTier Predicate Validation")
    class MineralUsagePredicateTests {

        @Test
        @DisplayName("Celidium is used in Tier 2 only")
        void testCelidiumUsage() {
            RocketTier t1 = RocketTiers.TIER_1.get();
            RocketTier t2 = RocketTiers.TIER_2.get();
            RocketTier t3 = RocketTiers.TIER_3.get();

            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.CELIDIUM, t1));
            assertTrue(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.CELIDIUM, t2));
            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.CELIDIUM, t3));

            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.CELIDIUM, 1));
            assertTrue(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.CELIDIUM, 2));
            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.CELIDIUM, 3));
        }

        @Test
        @DisplayName("Verdantite and Astralite are used in Tier 3")
        void testTier3MineralUsage() {
            RocketTier t1 = RocketTiers.TIER_1.get();
            RocketTier t2 = RocketTiers.TIER_2.get();
            RocketTier t3 = RocketTiers.TIER_3.get();

            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.VERDANTITE, t1));
            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.VERDANTITE, t2));
            assertTrue(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.VERDANTITE, t3));

            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.ASTRALITE, t1));
            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.ASTRALITE, t2));
            assertTrue(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.ASTRALITE, t3));

            assertTrue(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.VERDANTITE, 3));
            assertTrue(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.ASTRALITE, 3));
        }

        @Test
        @DisplayName("Higher tier minerals (Voidstalker, Chronostone) are not used in current rockets")
        void testHigherTierMineralsNotUsed() {
            for (int level = 1; level <= 3; level++) {
                assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.VOIDSTALKER, level));
                assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.CHRONOSTONE, level));
            }
        }

        @Test
        @DisplayName("Null parameters safely evaluate to false without exceptions")
        void testNullHandling() {
            assertFalse(AssemblyLogic.isMineralUsedInTier(null, RocketTiers.TIER_1.get()));
            assertFalse(AssemblyLogic.isMineralUsedInTier(AlienMineralTier.CELIDIUM, (RocketTier) null));
            assertFalse(AssemblyLogic.isMineralUsedInTier(null, (RocketTier) null));
            assertFalse(AssemblyLogic.isMineralUsedInTier(null, 1));
        }
    }
}
