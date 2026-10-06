package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.block.AlienMineralBlock;
import com.amaro.stellarodyssey.block.AlienOreBlock;
import com.amaro.stellarodyssey.block.AssemblyTableBlock;
import com.amaro.stellarodyssey.block.LaunchPadBaseBlock;
import com.amaro.stellarodyssey.block.LaunchPadBlock;
import com.amaro.stellarodyssey.block.OxygenRefillerBlock;
import com.amaro.stellarodyssey.block.OxygenSealerBlock;
import com.amaro.stellarodyssey.registry.tiers.AlienMineralTier;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

/** Unified block registry (Architectury {@link DeferredRegister} -> native registry on each loader). */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(StellarOdyssey.MOD_ID, Registries.BLOCK);

    /**
     * Alien Ore: crystalline ore found on hostile planets. The block itself only emits a faint
     * light (level 5); the bright "veins" are drawn by the emissive overlay in its block model.
     */
    public static final RegistrySupplier<AlienOreBlock> ALIEN_ORE = register("alien_ore", AlienOreBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 5));

    /** Alien Stone: dense metamorphic silicate bedrock of hostile celestial bodies. */
    public static final RegistrySupplier<Block> ALIEN_STONE = register("alien_stone", Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    /** Alien Turf: bioluminescent xenomorphic ground cover. Emits faint neon glow. */
    public static final RegistrySupplier<Block> ALIEN_TURF = register("alien_turf", Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.8F, 0.8F)
                    .sound(SoundType.SCULK)
                    .lightLevel(state -> 4));

    // --- Tiered alien mineral ores -----------------------------------------------------------
    public static final RegistrySupplier<AlienMineralBlock> CELIDIUM_ORE = register("celidium_ore",
            props -> new AlienMineralBlock(AlienMineralTier.CELIDIUM, props),
            AlienMineralBlock.propertiesForTier(AlienMineralTier.CELIDIUM));

    public static final RegistrySupplier<AlienMineralBlock> VERDANTITE_ORE = register("verdantite_ore",
            props -> new AlienMineralBlock(AlienMineralTier.VERDANTITE, props),
            AlienMineralBlock.propertiesForTier(AlienMineralTier.VERDANTITE));

    public static final RegistrySupplier<AlienMineralBlock> ASTRALITE_ORE = register("astralite_ore",
            props -> new AlienMineralBlock(AlienMineralTier.ASTRALITE, props),
            AlienMineralBlock.propertiesForTier(AlienMineralTier.ASTRALITE));

    // --- Crafting stations -------------------------------------------------------------------
    /** Rocket Assembly Table: combines tiered components into an assembled rocket. */
    public static final RegistrySupplier<AssemblyTableBlock> ASSEMBLY_TABLE = register("assembly_table",
            AssemblyTableBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // --- Launch infrastructure ---------------------------------------------------------------
    /** Central block of the 3x3 launch pad. */
    public static final RegistrySupplier<LaunchPadBlock> LAUNCH_PAD = register("launch_pad",
            LaunchPadBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(4.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    /** Filler block of the 3x3 launch pad. */
    public static final RegistrySupplier<LaunchPadBaseBlock> LAUNCH_PAD_BASE = register("launch_pad_base",
            LaunchPadBaseBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(4.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // --- Life Support Machines ---------------------------------------------------------------
    /** Oxygen Refiller: recharges portable oxygen tanks. */
    public static final RegistrySupplier<OxygenRefillerBlock> OXYGEN_REFILLER = register("oxygen_refiller",
            OxygenRefillerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    /** Oxygen Sealer: pressurizes and oxygenates enclosed rooms on airless worlds. */
    public static final RegistrySupplier<OxygenSealerBlock> OXYGEN_SEALER = register("oxygen_sealer",
            OxygenSealerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    private ModBlocks() {
    }

    /** Since MC 1.21.2 every block must know its own registry key before construction. */
    private static <T extends Block> RegistrySupplier<T> register(String name, Function<BlockBehaviour.Properties, T> factory,
                                                                  BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, StellarOdyssey.id(name));
        return BLOCKS.register(name, () -> factory.apply(properties.setId(key)));
    }

    public static void register() {
        BLOCKS.register();
    }
}
