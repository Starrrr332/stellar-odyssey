package com.amaro.stellarodyssey.registry.tiers;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * Extensible contract for alien mineral progression tiers in Stellar Odyssey.
 * <p>
 * Bridges Minecraft 26.3 {@link ToolMaterial} and {@link ArmorMaterial} records
 * with No Man's Sky style planetary resource progression.
 * </p>
 */
public interface IAlienMineralTier {

    /**
     * Integer rank for progression scaling and procedural planet generation (1 through 5+).
     */
    int getTierLevel();

    /**
     * Unique path identifier (e.g. "celidium", "verdantite").
     */
    String getName();

    /**
     * Full Namespaced Identifier (e.g. {@code stellarodyssey:celidium}).
     */
    Identifier getId();

    /**
     * Localized or display component for UI, starmap, and tooltips.
     */
    Component getDisplayName();

    /**
     * Minecraft 26.3 {@link ToolMaterial} record defining tool durability,
     * mining speed, attack damage bonus, enchantability, and repair tags.
     */
    ToolMaterial getToolMaterial();

    /**
     * Minecraft 26.3 {@link ArmorMaterial} record defining armor durability,
     * defense distribution, toughness, knockback resistance, and assets.
     */
    ArmorMaterial getArmorMaterial();

    /**
     * Inverted drop denial tag (e.g. {@code #stellarodyssey:incorrect_for_tier_1_tool}).
     */
    TagKey<Block> getIncorrectBlocksForDropsTag();

    /**
     * Mining harvest requirement tag (e.g. {@code #minecraft:needs_iron_tool}).
     */
    TagKey<Block> getRequiredMiningTierTag();

    /**
     * Block hardness / destroy time in seconds.
     */
    float getBlockHardness();

    /**
     * Block explosion / blast resistance.
     */
    float getExplosionResistance();

    /**
     * Block sound type for stepping, placing, hitting, and breaking.
     */
    SoundType getSoundType();

    /**
     * Block light emission level (0-15).
     */
    int getLuminance();

    /**
     * RGB Hex color code representing this mineral's visual palette.
     */
    default int getColorHex() {
        return 0xFFFFFF;
    }

    /**
     * Celestial origin / planetary classification description.
     */
    default String getCelestialOrigin() {
        return "Unknown Exoplanet";
    }

    /**
     * Item repair tag for tools and armor of this tier.
     */
    default TagKey<Item> getRepairItemTag() {
        return getToolMaterial().repairItems();
    }
}
