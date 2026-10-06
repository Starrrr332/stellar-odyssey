package com.amaro.stellarodyssey.registry.tiers;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * Standard built-in alien mineral progression tiers (Tiers 1 to 5).
 * <p>
 * Implements {@link IAlienMineralTier} and pre-registers all 5 canonical
 * mineral tiers into {@link AlienMineralTierRegistry}.
 * </p>
 */
public enum AlienMineralTier implements IAlienMineralTier {

    /**
     * Tier 1: Celidium
     * Scorched / Desert exoplanets, Amber Copper (#D97724), hardness 4.0, blast 6.0, sound AMETHYST, light 5.
     */
    CELIDIUM(
            1,
            "celidium",
            0xD97724,
            "Scorched / Desert Exoplanets",
            4.0F,
            6.0F,
            SoundType.AMETHYST,
            5,
            450,
            6.5F,
            2.5F,
            14,
            ModArmorMaterials.CELIDIUM,
            BlockTags.NEEDS_IRON_TOOL
    ),

    /**
     * Tier 2: Verdantite
     * Toxic / Jungle exoplanets, Toxic Emerald (#22C55E), hardness 6.0, blast 9.0, sound COPPER, light 7.
     */
    VERDANTITE(
            2,
            "verdantite",
            0x22C55E,
            "Toxic / Jungle Exoplanets",
            6.0F,
            9.0F,
            SoundType.COPPER,
            7,
            850,
            7.5F,
            3.5F,
            16,
            ModArmorMaterials.VERDANTITE,
            BlockTags.NEEDS_DIAMOND_TOOL
    ),

    /**
     * Tier 3: Astralite
     * Glacial / Deep Ocean asteroids, Celestial Cyan (#06B6D4), hardness 9.0, blast 15.0, sound DEEPSLATE, light 9.
     */
    ASTRALITE(
            3,
            "astralite",
            0x06B6D4,
            "Glacial / Deep Ocean Asteroids",
            9.0F,
            15.0F,
            SoundType.DEEPSLATE,
            9,
            1650,
            9.0F,
            4.5F,
            18,
            ModArmorMaterials.ASTRALITE,
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("neoforge", "needs_netherite_tool"))
    ),

    /**
     * Tier 4: Voidstalker
     * High-Gravity Black Hole Accretion Disks, Void Violet (#7C3AED), hardness 15.0, blast 30.0, sound SCULK_CATALYST, light 12.
     */
    VOIDSTALKER(
            4,
            "voidstalker",
            0x7C3AED,
            "High-Gravity Black Hole Accretion Disks",
            15.0F,
            30.0F,
            SoundType.SCULK_CATALYST,
            12,
            2500,
            11.5F,
            6.0F,
            22,
            ModArmorMaterials.VOIDSTALKER,
            TagKey.create(Registries.BLOCK, StellarOdyssey.id("needs_tier_3_tool"))
    ),

    /**
     * Tier 5: Chronostone
     * Exotic Quantum Anomaly Planets, Hyper-Chromic Gold (#F59E0B), hardness 25.0, blast 50.0, sound HEAVY_CORE, light 15.
     */
    CHRONOSTONE(
            5,
            "chronostone",
            0xF59E0B,
            "Exotic Quantum Anomaly Planets",
            25.0F,
            50.0F,
            SoundType.HEAVY_CORE,
            15,
            3600,
            14.5F,
            8.0F,
            26,
            ModArmorMaterials.CHRONOSTONE,
            TagKey.create(Registries.BLOCK, StellarOdyssey.id("needs_tier_4_tool"))
    );

    // --- Aliases for numeric indexing --------------------------------------------------------
    public static final AlienMineralTier TIER_1 = CELIDIUM;
    public static final AlienMineralTier TIER_2 = VERDANTITE;
    public static final AlienMineralTier TIER_3 = ASTRALITE;
    public static final AlienMineralTier TIER_4 = VOIDSTALKER;
    public static final AlienMineralTier TIER_5 = CHRONOSTONE;

    // --- Tag references ----------------------------------------------------------------------
    public static final TagKey<Block> INCORRECT_FOR_TIER_1_TOOL = CELIDIUM.getIncorrectBlocksForDropsTag();
    public static final TagKey<Block> INCORRECT_FOR_TIER_2_TOOL = VERDANTITE.getIncorrectBlocksForDropsTag();
    public static final TagKey<Block> INCORRECT_FOR_TIER_3_TOOL = ASTRALITE.getIncorrectBlocksForDropsTag();
    public static final TagKey<Block> INCORRECT_FOR_TIER_4_TOOL = VOIDSTALKER.getIncorrectBlocksForDropsTag();
    public static final TagKey<Block> INCORRECT_FOR_TIER_5_TOOL = CHRONOSTONE.getIncorrectBlocksForDropsTag();

    public static final TagKey<Block> NEEDS_TIER_1_TOOL = CELIDIUM.getRequiredMiningTierTag();
    public static final TagKey<Block> NEEDS_TIER_2_TOOL = VERDANTITE.getRequiredMiningTierTag();
    public static final TagKey<Block> NEEDS_TIER_3_TOOL = ASTRALITE.getRequiredMiningTierTag();
    public static final TagKey<Block> NEEDS_TIER_4_TOOL = VOIDSTALKER.getRequiredMiningTierTag();
    public static final TagKey<Block> NEEDS_TIER_5_TOOL = CHRONOSTONE.getRequiredMiningTierTag();

    // --- Fields ------------------------------------------------------------------------------
    private final int tierLevel;
    private final String name;
    private final Identifier id;
    private final Component displayName;
    private final int colorHex;
    private final String celestialOrigin;

    private final float blockHardness;
    private final float explosionResistance;
    private final SoundType soundType;
    private final int luminance;

    private final TagKey<Block> incorrectBlocksForDropsTag;
    private final TagKey<Block> requiredMiningTierTag;
    private final TagKey<Item> repairItemTag;
    private final ToolMaterial toolMaterial;
    private final ArmorMaterial armorMaterial;

    private static volatile boolean registered = false;

    AlienMineralTier(
            int tierLevel,
            String name,
            int colorHex,
            String celestialOrigin,
            float blockHardness,
            float explosionResistance,
            SoundType soundType,
            int luminance,
            int toolDurability,
            float toolSpeed,
            float toolAttackDamageBonus,
            int toolEnchantmentValue,
            ArmorMaterial armorMaterial,
            TagKey<Block> requiredMiningTierTag
    ) {
        this.tierLevel = tierLevel;
        this.name = name;
        this.id = StellarOdyssey.id(name);
        this.displayName = Component.translatable("tier.stellarodyssey." + name);
        this.colorHex = colorHex;
        this.celestialOrigin = celestialOrigin;

        this.blockHardness = blockHardness;
        this.explosionResistance = explosionResistance;
        this.soundType = soundType;
        this.luminance = luminance;

        this.incorrectBlocksForDropsTag = TagKey.create(Registries.BLOCK, StellarOdyssey.id("incorrect_for_tier_" + tierLevel + "_tool"));
        this.requiredMiningTierTag = requiredMiningTierTag;
        this.repairItemTag = TagKey.create(Registries.ITEM, StellarOdyssey.id(name + "_repair"));

        this.toolMaterial = new ToolMaterial(
                this.incorrectBlocksForDropsTag,
                toolDurability,
                toolSpeed,
                toolAttackDamageBonus,
                toolEnchantmentValue,
                this.repairItemTag
        );
        this.armorMaterial = armorMaterial;
    }

    /**
     * Idempotent bootstrap method to pre-register built-in Tiers 1-5.
     */
    public static synchronized void registerBuiltinTiers() {
        if (registered) {
            return;
        }
        registered = true;
        for (AlienMineralTier tier : values()) {
            AlienMineralTierRegistry.registerTier(tier);
        }
    }

    static {
        registerBuiltinTiers();
    }

    @Override
    public int getTierLevel() {
        return this.tierLevel;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public Component getDisplayName() {
        return this.displayName;
    }

    @Override
    public ToolMaterial getToolMaterial() {
        return this.toolMaterial;
    }

    @Override
    public ArmorMaterial getArmorMaterial() {
        return this.armorMaterial;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDropsTag() {
        return this.incorrectBlocksForDropsTag;
    }

    @Override
    public TagKey<Block> getRequiredMiningTierTag() {
        return this.requiredMiningTierTag;
    }

    @Override
    public float getBlockHardness() {
        return this.blockHardness;
    }

    @Override
    public float getExplosionResistance() {
        return this.explosionResistance;
    }

    @Override
    public SoundType getSoundType() {
        return this.soundType;
    }

    @Override
    public int getLuminance() {
        return this.luminance;
    }

    @Override
    public int getColorHex() {
        return this.colorHex;
    }

    @Override
    public String getCelestialOrigin() {
        return this.celestialOrigin;
    }

    @Override
    public TagKey<Item> getRepairItemTag() {
        return this.repairItemTag;
    }
}
