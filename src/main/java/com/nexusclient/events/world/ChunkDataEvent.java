package com.nexusclient.events.world;

import net.minecraft.world.chunk.WorldChunk;

public record ChunkDataEvent(WorldChunk chunk) {}
