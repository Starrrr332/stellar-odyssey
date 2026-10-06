package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.block.AlienOreBlock;
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
