package com.amberclient.events.world;

import net.minecraft.world.chunk.WorldChunk;

public record ChunkDataEvent(WorldChunk chunk) {}
