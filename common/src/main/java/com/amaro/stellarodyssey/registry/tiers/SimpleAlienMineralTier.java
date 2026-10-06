package com.amaro.stellarodyssey.registry.tiers;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.util.Objects;

/**
 * Concrete record implementation of {@link IAlienMineralTier} for dynamic,
 * runtime, or add-on tier registration.
 */
public record SimpleAlienMineralTier(
        int tierLevel,
        String name,
        Identifier id,
        Component displayName,
        ToolMaterial toolMaterial,
        ArmorMaterial armorMaterial,
        TagKey<Block> incorrectBlocksForDropsTag,
        TagKey<Block> requiredMiningTierTag,
        float blockHardness,
        float explosionResistance,
        SoundType soundType,
        int luminance,
        int colorHex,
        String celestialOrigin
) implements IAlienMineralTier {

    public SimpleAlienMineralTier {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(toolMaterial, "toolMaterial cannot be null");
        Objects.requireNonNull(armorMaterial, "armorMaterial cannot be null");
        Objects.requireNonNull(incorrectBlocksForDropsTag, "incorrectBlocksForDropsTag cannot be null");
        Objects.requireNonNull(requiredMiningTierTag, "requiredMiningTierTag cannot be null");
        Objects.requireNonNull(soundType, "soundType cannot be null");
    }

    @Override
    public int getTierLevel() {
        return tierLevel;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public ToolMaterial getToolMaterial() {
        return toolMaterial;
    }

    @Override
    public ArmorMaterial getArmorMaterial() {
        return armorMaterial;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDropsTag() {
        return incorrectBlocksForDropsTag;
    }

    @Override
    public TagKey<Block> getRequiredMiningTierTag() {
        return requiredMiningTierTag;
    }

    @Override
    public float getBlockHardness() {
        return blockHardness;
    }

    @Override
    public float getExplosionResistance() {
        return explosionResistance;
    }

    @Override
    public SoundType getSoundType() {
        return soundType;
    }

    @Override
    public int getLuminance() {
        return luminance;
    }

    @Override
    public int getColorHex() {
        return colorHex;
    }

    @Override
    public String getCelestialOrigin() {
        return celestialOrigin;
    }

    @Override
    public TagKey<Item> getRepairItemTag() {
        return toolMaterial.repairItems();
    }
}
