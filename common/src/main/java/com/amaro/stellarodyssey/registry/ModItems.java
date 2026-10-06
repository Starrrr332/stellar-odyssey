package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.item.OxygenTankItem;
import com.amaro.stellarodyssey.item.RocketComponentItem;
import com.amaro.stellarodyssey.item.RocketItem;
import com.amaro.stellarodyssey.item.SpacesuitItem;
import com.amaro.stellarodyssey.item.StarshipItem;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTierRegistry;
import com.amaro.stellarodyssey.rocket.RocketComponentRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Unified item registry. Block items live here too so blocks stay item-agnostic. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(StellarOdyssey.MOD_ID, Registries.ITEM);

    // --- Block items -------------------------------------------------------------------------
    public static final RegistrySupplier<BlockItem> ALIEN_ORE = register("alien_ore",
            props -> new BlockItem(ModBlocks.ALIEN_ORE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties());

    public static final RegistrySupplier<BlockItem> ALIEN_STONE = register("alien_stone",
            props -> new BlockItem(ModBlocks.ALIEN_STONE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties());

    public static final RegistrySupplier<BlockItem> ALIEN_TURF = register("alien_turf",
            props -> new BlockItem(ModBlocks.ALIEN_TURF.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties());

    // --- Items -------------------------------------------------------------------------------
    /** Portable O2 reserve. Durability = stored oxygen units (full when undamaged). */
    public static final RegistrySupplier<OxygenTankItem> OXYGEN_TANK = register("oxygen_tank", OxygenTankItem::new,
            new Item.Properties()
                    .durability(OxygenTankItem.CAPACITY)
                    .rarity(Rarity.UNCOMMON));

    // --- Modular Spacesuit -------------------------------------------------------------------
    public static final RegistrySupplier<SpacesuitItem> SPACESUIT_HELMET = register("spacesuit_helmet",
            props -> new SpacesuitItem(ArmorType.HELMET, props),
            new Item.Properties().rarity(Rarity.RARE));

    public static final RegistrySupplier<SpacesuitItem> SPACESUIT_CHESTPLATE = register("spacesuit_chestplate",
            props -> new SpacesuitItem(ArmorType.CHESTPLATE, props),
            new Item.Properties().rarity(Rarity.RARE));

    public static final RegistrySupplier<SpacesuitItem> SPACESUIT_LEGGINGS = register("spacesuit_leggings",
            props -> new SpacesuitItem(ArmorType.LEGGINGS, props),
            new Item.Properties().rarity(Rarity.RARE));

    public static final RegistrySupplier<SpacesuitItem> SPACESUIT_BOOTS = register("spacesuit_boots",
            props -> new SpacesuitItem(ArmorType.BOOTS, props),
            new Item.Properties().rarity(Rarity.RARE));

    // --- Vehicles ----------------------------------------------------------------------------
    public static final RegistrySupplier<StarshipItem> STARSHIP = register("starship", StarshipItem::new,
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // --- Alien minerals (progression loop) ---------------------------------------------------
    public static final RegistrySupplier<Item> RAW_CELIDIUM = register("raw_celidium", Item::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistrySupplier<Item> RAW_VERDANTITE = register("raw_verdantite", Item::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistrySupplier<Item> RAW_ASTRALITE = register("raw_astralite", Item::new,
            new Item.Properties().rarity(Rarity.RARE));

    /** Raw chunk of the base-tier alien ore (the only ore without a refined ingot). */
    public static final RegistrySupplier<Item> RAW_ALIEN = register("raw_alien", Item::new,
            new Item.Properties().rarity(Rarity.COMMON));

    public static final RegistrySupplier<Item> CELIDIUM_INGOT = register("celidium_ingot", Item::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistrySupplier<Item> VERDANTITE_INGOT = register("verdantite_ingot", Item::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistrySupplier<Item> ASTRALITE_INGOT = register("astralite_ingot", Item::new,
            new Item.Properties().rarity(Rarity.RARE));

    // --- Mineral ore blocks ------------------------------------------------------------------
    public static final RegistrySupplier<BlockItem> CELIDIUM_ORE = register("celidium_ore",
            props -> new BlockItem(ModBlocks.CELIDIUM_ORE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistrySupplier<BlockItem> VERDANTITE_ORE = register("verdantite_ore",
            props -> new BlockItem(ModBlocks.VERDANTITE_ORE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistrySupplier<BlockItem> ASTRALITE_ORE = register("astralite_ore",
            props -> new BlockItem(ModBlocks.ASTRALITE_ORE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().rarity(Rarity.RARE));

    // --- Crafting station block items --------------------------------------------------------
    public static final RegistrySupplier<BlockItem> ASSEMBLY_TABLE = register("assembly_table",
            props -> new BlockItem(ModBlocks.ASSEMBLY_TABLE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().rarity(Rarity.UNCOMMON));

    // --- Launch pad block items --------------------------------------------------------------
    public static final RegistrySupplier<BlockItem> LAUNCH_PAD = register("launch_pad",
            props -> new BlockItem(ModBlocks.LAUNCH_PAD.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final RegistrySupplier<BlockItem> LAUNCH_PAD_BASE = register("launch_pad_base",
            props -> new BlockItem(ModBlocks.LAUNCH_PAD_BASE.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().rarity(Rarity.COMMON));

    // --- Life support machine block items ----------------------------------------------------
    public static final RegistrySupplier<BlockItem> OXYGEN_REFILLER = register("oxygen_refiller",
            props -> new BlockItem(ModBlocks.OXYGEN_REFILLER.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final RegistrySupplier<BlockItem> OXYGEN_SEALER = register("oxygen_sealer",
            props -> new BlockItem(ModBlocks.OXYGEN_SEALER.get(), props.useBlockDescriptionPrefix()),
            new Item.Properties().rarity(Rarity.UNCOMMON));

    // --- Rocket components (tiered) ----------------------------------------------------------
    /** rocket_<type>_t<level> for every tier/type combination. */
    public static final Map<String, RegistrySupplier<RocketComponentItem>> ROCKET_COMPONENTS = new HashMap<>();

    static {
        for (RocketTier tier : RocketTierRegistry.getAllTiers()) {
            for (String type : RocketComponentRegistry.COMPONENT_TYPES) {
                String name = "rocket_" + type + "_t" + tier.tierLevel();
                Rarity rarity = switch (tier.tierLevel()) {
                    case 1 -> Rarity.COMMON;
                    case 2 -> Rarity.UNCOMMON;
                    default -> Rarity.RARE;
                };
                ROCKET_COMPONENTS.put(name, register(name,
                        props -> new RocketComponentItem(tier, type, props),
                        new Item.Properties().rarity(rarity)));
            }
        }
    }

    // --- Assembled rockets (tiered) ---------------------------------------------------------
    /** rocket_t<level> for every tier. Produced by the Rocket Assembly Table. */
    public static final Map<String, RegistrySupplier<RocketItem>> ROCKETS = new HashMap<>();

    static {
        for (RocketTier tier : RocketTierRegistry.getAllTiers()) {
            String name = "rocket_t" + tier.tierLevel();
            Rarity rarity = switch (tier.tierLevel()) {
                case 1 -> Rarity.UNCOMMON;
                case 2 -> Rarity.RARE;
                default -> Rarity.EPIC;
            };
            ROCKETS.put(name, register(name,
                    props -> new RocketItem(tier, props),
                    new Item.Properties().rarity(rarity).stacksTo(1)));
        }
    }

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
