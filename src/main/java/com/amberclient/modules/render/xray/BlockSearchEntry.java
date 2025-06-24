package com.amberclient.modules.render.xray;

import net.minecraft.block.BlockState;

public record BlockSearchEntry(
        BlockState state,
        BasicColor color,
        boolean isDefault,
        int minY,
        int maxY,
        int size,
        int count,
        float rarity
) {
    public BlockSearchEntry(BlockState state, BasicColor color, boolean isDefault) {
        this(state, color, isDefault, -64, 320, 1, 1, 1.0f);
    }

    public String getName() {
        return state.getBlock().getTranslationKey();
    }
}