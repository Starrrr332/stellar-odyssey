package com.amaro.stellarodyssey.registry.tiers;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Alien mineral armor material configurations for Stellar Odyssey progression.
 */
public final class ModArmorMaterials {

    // --- Equipment Assets -------------------------------------------------------------------
    public static final ResourceKey<EquipmentAsset> CELIDIUM_ASSET = asset("celidium");
    public static final ResourceKey<EquipmentAsset> VERDANTITE_ASSET = asset("verdantite");
    public static final ResourceKey<EquipmentAsset> ASTRALITE_ASSET = asset("astralite");
    public static final ResourceKey<EquipmentAsset> VOIDSTALKER_ASSET = asset("voidstalker");
    public static final ResourceKey<EquipmentAsset> CHRONOSTONE_ASSET = asset("chronostone");

    // --- Repair Tags ------------------------------------------------------------------------
    public static final TagKey<Item> CELIDIUM_REPAIR = repairTag("celidium_repair");
    public static final TagKey<Item> VERDANTITE_REPAIR = repairTag("verdantite_repair");
    public static final TagKey<Item> ASTRALITE_REPAIR = repairTag("astralite_repair");
    public static final TagKey<Item> VOIDSTALKER_REPAIR = repairTag("voidstalker_repair");
    public static final TagKey<Item> CHRONOSTONE_REPAIR = repairTag("chronostone_repair");

    // --- Materials --------------------------------------------------------------------------

    /**
     * Tier 1: Celidium armor (Amber Copper, Scorched Exoplanets).
     */
    public static final ArmorMaterial CELIDIUM = new ArmorMaterial(
            22,
            Map.of(
                    ArmorType.BOOTS, 2,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.CHESTPLATE, 7,
                    ArmorType.HELMET, 3
            ),
            14,
            SoundEvents.ARMOR_EQUIP_COPPER,
            1.0F,
            0.0F,
            CELIDIUM_REPAIR,
            CELIDIUM_ASSET
    );

    /**
     * Tier 2: Verdantite armor (Toxic Emerald, Toxic/Jungle Exoplanets).
     */
    public static final ArmorMaterial VERDANTITE = new ArmorMaterial(
            28,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 3
            ),
            16,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            2.0F,
            0.05F,
            VERDANTITE_REPAIR,
            VERDANTITE_ASSET
    );

    /**
     * Tier 3: Astralite armor (Celestial Cyan, Glacial/Deep Ocean Asteroids).
     */
    public static final ArmorMaterial ASTRALITE = new ArmorMaterial(
            35,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 4
            ),
            18,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0F,
            0.1F,
            ASTRALITE_REPAIR,
            ASTRALITE_ASSET
    );

    /**
     * Tier 4: Voidstalker armor (Void Violet, Black Hole Accretion Disks).
     */
    public static final ArmorMaterial VOIDSTALKER = new ArmorMaterial(
            42,
            Map.of(
                    ArmorType.BOOTS, 4,
                    ArmorType.LEGGINGS, 7,
                    ArmorType.CHESTPLATE, 9,
                    ArmorType.HELMET, 4
            ),
            22,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            4.0F,
            0.15F,
            VOIDSTALKER_REPAIR,
            VOIDSTALKER_ASSET
    );

    /**
     * Tier 5: Chronostone armor (Hyper-Chromic Gold, Quantum Anomalies).
     */
    public static final ArmorMaterial CHRONOSTONE = new ArmorMaterial(
            50,
            Map.of(
                    ArmorType.BOOTS, 5,
                    ArmorType.LEGGINGS, 8,
                    ArmorType.CHESTPLATE, 10,
                    ArmorType.HELMET, 5
            ),
            26,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            5.0F,
            0.25F,
            CHRONOSTONE_REPAIR,
            CHRONOSTONE_ASSET
    );

    private ModArmorMaterials() {
    }

    public static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, StellarOdyssey.id(name));
    }

    public static TagKey<Item> repairTag(String name) {
        return TagKey.create(Registries.ITEM, StellarOdyssey.id(name));
    }

    /**
     * Dynamic factory method for add-ons or expansions creating custom alien armor materials.
     */
    public static ArmorMaterial create(int durability, Map<ArmorType, Integer> defense, int enchantmentValue,
                                       Holder<SoundEvent> sound, float toughness, float knockbackResistance,
                                       TagKey<Item> repair, ResourceKey<EquipmentAsset> asset) {
        return new ArmorMaterial(durability, defense, enchantmentValue, sound, toughness, knockbackResistance, repair, asset);
    }
}
