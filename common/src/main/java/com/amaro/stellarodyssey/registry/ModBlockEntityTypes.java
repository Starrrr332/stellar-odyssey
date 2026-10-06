package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.block.AssemblyTableBlock;
import com.amaro.stellarodyssey.block.entity.AssemblyTableBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/** Block entity type registry for Stellar Odyssey. */
public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(StellarOdyssey.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<AssemblyTableBlockEntity>> ASSEMBLY_TABLE =
            BLOCK_ENTITY_TYPES.register("assembly_table",
                    () -> new BlockEntityType<>(
                            AssemblyTableBlockEntity::new,
                            Set.of(ModBlocks.ASSEMBLY_TABLE.get())));

    private ModBlockEntityTypes() {
    }

    public static void register() {
        BLOCK_ENTITY_TYPES.register();
    }
}
