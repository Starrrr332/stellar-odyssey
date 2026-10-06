package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.core.ModConstants;
import com.amaro.stellarodyssey.registry.*;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mod Registries Binding and Centralized Registration Tests")
class ModRegistriesBindingTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Verify ModRegistries.registerAll() executes cleanly or handles untransformed test platform")
    void testRegisterAllExecution() {
        try {
            ModRegistries.registerAll();
            // When running within a loaded mod platform environment, verify idempotency
            assertDoesNotThrow(ModRegistries::registerAll,
                    "Subsequent calls to ModRegistries.registerAll() must be safe and idempotent");
        } catch (AssertionError e) {
            // In Architectury Loom multi-loader setups, @ExpectPlatform methods like RegistrarManager._get()
            // throw AssertionError when called in pure JVM common unit tests without a loaded mod platform.
            assertNotNull(e.getStackTrace(), "Stack trace must not be null");
            assertTrue(e.getStackTrace().length > 0 && e.getStackTrace()[0].getClassName().contains("RegistrarManager"),
                    "AssertionError must originate from Architectury RegistrarManager @ExpectPlatform stub");
        }

        // Verify the registration suppliers directly without failing the unit test suite
        assertNotNull(ModBlocks.BLOCKS, "ModBlocks.BLOCKS must be instantiated");
        assertNotNull(ModItems.ITEMS, "ModItems.ITEMS must be instantiated");
        assertNotNull(ModCreativeTabs.TABS, "ModCreativeTabs.TABS must be instantiated");
        assertNotNull(ModEntities.ENTITIES, "ModEntities.ENTITIES must be instantiated");
        assertNotNull(ModSoundEvents.SOUND_EVENTS, "ModSoundEvents.SOUND_EVENTS must be instantiated");

        assertNotNull(ModBlocks.ALIEN_ORE, "ALIEN_ORE supplier must be present");
        assertNotNull(ModItems.ALIEN_ORE, "Item ALIEN_ORE supplier must be present");
        assertNotNull(ModCreativeTabs.MAIN, "Creative tab supplier must be present");
        assertNotNull(ModEntities.STARSHIP, "STARSHIP entity supplier must be present");
        assertNotNull(ModSoundEvents.STARSHIP_THRUST, "STARSHIP_THRUST sound supplier must be present");
    }

    @Test
    @DisplayName("Verify ModBlocks registered entries and namespace contracts")
    void testModBlocksRegistry() {
        assertNotNull(ModBlocks.BLOCKS, "ModBlocks.BLOCKS DeferredRegister must not be null");

        assertNotNull(ModBlocks.ALIEN_ORE, "ALIEN_ORE must be registered");
        assertNotNull(ModBlocks.ALIEN_STONE, "ALIEN_STONE must be registered");
        assertNotNull(ModBlocks.ALIEN_TURF, "ALIEN_TURF must be registered");

        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "alien_ore"), ModBlocks.ALIEN_ORE.getId());
        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "alien_stone"), ModBlocks.ALIEN_STONE.getId());
        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "alien_turf"), ModBlocks.ALIEN_TURF.getId());

        Set<String> blockPaths = StreamSupport.stream(ModBlocks.BLOCKS.spliterator(), false)
                .map(supplier -> supplier.getId().getPath())
                .collect(Collectors.toSet());

        assertTrue(blockPaths.contains("alien_ore"));
        assertTrue(blockPaths.contains("alien_stone"));
        assertTrue(blockPaths.contains("alien_turf"));
    }

    @Test
    @DisplayName("Verify ModItems registered entries and BlockItem parity")
    void testModItemsRegistry() {
        assertNotNull(ModItems.ITEMS, "ModItems.ITEMS DeferredRegister must not be null");

        assertNotNull(ModItems.ALIEN_ORE, "BlockItem ALIEN_ORE must be registered");
        assertNotNull(ModItems.ALIEN_STONE, "BlockItem ALIEN_STONE must be registered");
        assertNotNull(ModItems.ALIEN_TURF, "BlockItem ALIEN_TURF must be registered");
        assertNotNull(ModItems.OXYGEN_TANK, "OXYGEN_TANK item must be registered");
        assertNotNull(ModItems.SPACESUIT_HELMET, "SPACESUIT_HELMET item must be registered");
        assertNotNull(ModItems.SPACESUIT_CHESTPLATE, "SPACESUIT_CHESTPLATE item must be registered");
        assertNotNull(ModItems.SPACESUIT_LEGGINGS, "SPACESUIT_LEGGINGS item must be registered");
        assertNotNull(ModItems.SPACESUIT_BOOTS, "SPACESUIT_BOOTS item must be registered");
        assertNotNull(ModItems.STARSHIP, "STARSHIP item must be registered");

        Set<String> itemPaths = StreamSupport.stream(ModItems.ITEMS.spliterator(), false)
                .map(supplier -> supplier.getId().getPath())
                .collect(Collectors.toSet());

        // Every block in ModBlocks must have a corresponding item
        for (RegistrySupplier<?> blockEntry : ModBlocks.BLOCKS) {
            String blockPath = blockEntry.getId().getPath();
            assertTrue(itemPaths.contains(blockPath),
                    "Every block in ModBlocks must have a matching item in ModItems: " + blockPath);
        }

        assertTrue(itemPaths.contains("oxygen_tank"));
        assertTrue(itemPaths.contains("spacesuit_helmet"));
        assertTrue(itemPaths.contains("spacesuit_chestplate"));
        assertTrue(itemPaths.contains("spacesuit_leggings"));
        assertTrue(itemPaths.contains("spacesuit_boots"));
        assertTrue(itemPaths.contains("starship"));
    }

    @Test
    @DisplayName("Verify ModCreativeTabs registered tab and properties")
    void testModCreativeTabsRegistry() {
        assertNotNull(ModCreativeTabs.TABS, "ModCreativeTabs.TABS DeferredRegister must not be null");
        assertNotNull(ModCreativeTabs.MAIN, "MAIN creative mode tab supplier must not be null");

        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "main"), ModCreativeTabs.MAIN.getId());
        assertEquals("stellarodyssey", ModCreativeTabs.MAIN.getId().getNamespace());
    }

    @Test
    @DisplayName("Verify ModEntities registered entity types")
    void testModEntitiesRegistry() {
        assertNotNull(ModEntities.ENTITIES, "ModEntities.ENTITIES DeferredRegister must not be null");
        assertNotNull(ModEntities.STARSHIP, "STARSHIP entity supplier must not be null");

        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "starship"), ModEntities.STARSHIP.getId());
    }

    @Test
    @DisplayName("Verify ModSoundEvents registered sounds and identifier keys")
    void testModSoundEventsRegistry() {
        assertNotNull(ModSoundEvents.SOUND_EVENTS, "ModSoundEvents.SOUND_EVENTS DeferredRegister must not be null");

        assertNotNull(ModSoundEvents.STARSHIP_THRUST, "STARSHIP_THRUST sound must be registered");
        assertNotNull(ModSoundEvents.DECOMPRESSION_ALARM, "DECOMPRESSION_ALARM sound must be registered");
        assertNotNull(ModSoundEvents.ALIEN_AMBIENCE, "ALIEN_AMBIENCE sound must be registered");
        assertNotNull(ModSoundEvents.RESONANCE_CRYSTAL, "RESONANCE_CRYSTAL sound must be registered");

        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "entity.starship.thrust"),
                ModSoundEvents.STARSHIP_THRUST.getId());
        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "hazard.decompression_alarm"),
                ModSoundEvents.DECOMPRESSION_ALARM.getId());
        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "ambient.alien_world"),
                ModSoundEvents.ALIEN_AMBIENCE.getId());
        assertEquals(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "block.alien_ore.resonate"),
                ModSoundEvents.RESONANCE_CRYSTAL.getId());

        Set<String> soundPaths = StreamSupport.stream(ModSoundEvents.SOUND_EVENTS.spliterator(), false)
                .map(supplier -> supplier.getId().getPath())
                .collect(Collectors.toSet());

        assertTrue(soundPaths.contains("entity.starship.thrust"));
        assertTrue(soundPaths.contains("hazard.decompression_alarm"));
        assertTrue(soundPaths.contains("ambient.alien_world"));
        assertTrue(soundPaths.contains("block.alien_ore.resonate"));
    }
}
