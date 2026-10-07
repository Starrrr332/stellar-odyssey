package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.block.entity.AssemblyTableBlockEntity;
import com.amaro.stellarodyssey.block.entity.AssemblyTableMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

/** Menu type registry for Stellar Odyssey. */
public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(StellarOdyssey.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<AssemblyTableMenu>> ASSEMBLY_TABLE =
            MENU_TYPES.register("assembly_table", () -> MenuRegistry.ofExtended((id, inv, buf) -> {
                BlockPos pos = buf.readBlockPos();
                Level level = inv.player.level();
                if (level != null && level.getBlockEntity(pos) instanceof AssemblyTableBlockEntity table) {
                    return new AssemblyTableMenu(id, inv, table, ContainerLevelAccess.create(level, pos));
                }
                return new AssemblyTableMenu(id, inv);
            }));

    private ModMenuTypes() {
    }

    public static void register() {
        MENU_TYPES.register();
    }
}
