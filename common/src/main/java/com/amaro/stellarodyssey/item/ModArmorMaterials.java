package com.amaro.stellarodyssey.item;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Armor materials for Stellar Odyssey.
 */
public final class ModArmorMaterials {
    public static final ResourceKey<EquipmentAsset> SPACESUIT_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            StellarOdyssey.id("spacesuit")
    );

    /**
     * Heavy reinforced pressurized spacesuit material.
     * High defense, built-in radiation and kinetic protection.
     */
    public static final ArmorMaterial SPACESUIT = new ArmorMaterial(
            35,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 3
            ),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.0F,
            0.1F,
            ItemTags.REPAIRS_NETHERITE_ARMOR,
            SPACESUIT_ASSET
    );

    private ModArmorMaterials() {
    }
}
