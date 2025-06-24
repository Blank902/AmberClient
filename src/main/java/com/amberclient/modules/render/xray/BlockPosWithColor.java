package com.amberclient.modules.render.xray;

import com.amberclient.utils.general.BasicColor;
import net.minecraft.util.math.BlockPos;

public record BlockPosWithColor(BlockPos pos, BasicColor color) {
}