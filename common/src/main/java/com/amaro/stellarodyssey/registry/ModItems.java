package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.item.OxygenTankItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

/** Unified item registry. Block items live here too so blocks stay item-agnostic. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(StellarOdyssey.MOD_ID, Registries.ITEM);

    // --- Block items -------------------------------------------------------------------------
    public static final RegistrySupplier<BlockItem> ALIEN_ORE = register("alien_ore",
            props -> new BlockItem(ModBlocks.ALIEN_ORE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties());

    // --- Items -------------------------------------------------------------------------------
    /** Portable O2 reserve. Durability = stored oxygen units (full when undamaged). */
    public static final RegistrySupplier<OxygenTankItem> OXYGEN_TANK = register("oxygen_tank", OxygenTankItem::new,
            new Item.Properties()
                    .durability(OxygenTankItem.CAPACITY)
                    .rarity(Rarity.UNCOMMON));

    private ModItems() {
    }

    /** Sets the mandatory item id (MC 1.21.2+) and adds the item to the mod's creative tab. */
    private static <T extends Item> RegistrySupplier<T> register(String name, Function<Item.Properties, T> factory,
                                                                 Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, StellarOdyssey.id(name));
        return ITEMS.register(name, () -> factory.apply(properties.setId(key).arch$tab(ModCreativeTabs.MAIN)));
    }

    public static void register() {
        ITEMS.register();
    }
}
