package com.amberclient.utils.minecraft;

import com.amberclient.utils.core.Dimension;
import net.minecraft.client.MinecraftClient;

public class PlayerUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Dimension getDimension() {
        if (mc.world == null) return Dimension.Overworld;

        return switch (mc.world.getRegistryKey().getValue().getPath()) {
            case "the_nether" -> Dimension.Nether;
            case "the_end" -> Dimension.End;
            default -> Dimension.Overworld;
        };
    }
}
