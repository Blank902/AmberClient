package com.amberclient.modules.render.xray;

import com.amberclient.utils.general.BasicColor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.feature.OrePlacedFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;

public enum OreType {
    DIAMOND("Diamond",
            new Block[]{Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE},
            new BasicColor(51, 236, 255),
            new RegistryKey[]{OrePlacedFeatures.ORE_DIAMOND, OrePlacedFeatures.ORE_DIAMOND_BURIED,
                    OrePlacedFeatures.ORE_DIAMOND_LARGE, OrePlacedFeatures.ORE_DIAMOND_MEDIUM}),
    GOLD("Gold",
            new Block[]{Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE},
            new BasicColor(255, 252, 51),
            new RegistryKey[]{OrePlacedFeatures.ORE_GOLD, OrePlacedFeatures.ORE_GOLD_LOWER,
                    OrePlacedFeatures.ORE_GOLD_EXTRA, OrePlacedFeatures.ORE_GOLD_NETHER,
                    OrePlacedFeatures.ORE_GOLD_DELTAS}),
    IRON("Iron",
            new Block[]{Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE},
            new BasicColor(201, 201, 183),
            new RegistryKey[]{OrePlacedFeatures.ORE_IRON_MIDDLE, OrePlacedFeatures.ORE_IRON_SMALL,
                    OrePlacedFeatures.ORE_IRON_UPPER}),
    EMERALD("Emerald",
            new Block[]{Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE},
            new BasicColor(27, 209, 45),
            new RegistryKey[]{OrePlacedFeatures.ORE_EMERALD}),
    QUARTZ("Quartz",
            new Block[]{Blocks.NETHER_QUARTZ_ORE},
            new BasicColor(205, 205, 205),
            new RegistryKey[]{OrePlacedFeatures.ORE_QUARTZ_NETHER, OrePlacedFeatures.ORE_QUARTZ_DELTAS}),
    DEBRIS("Ancient Debris",
            new Block[]{Blocks.ANCIENT_DEBRIS},
            new BasicColor(209, 27, 245),
            new RegistryKey[]{OrePlacedFeatures.ORE_DEBRIS_SMALL, OrePlacedFeatures.ORE_ANCIENT_DEBRIS_LARGE});

    public final String name;
    public final Block[] blocks;
    public final BasicColor color;
    public final RegistryKey<PlacedFeature>[] placedFeatures;

    OreType(String name, Block[] blocks, BasicColor color, RegistryKey<PlacedFeature>[] placedFeatures) {
        this.name = name;
        this.blocks = blocks;
        this.color = color;
        this.placedFeatures = placedFeatures;
    }
}