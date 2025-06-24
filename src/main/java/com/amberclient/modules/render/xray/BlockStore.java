package com.amberclient.modules.render.xray;

import net.minecraft.block.Blocks;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BlockStore {
    private static BlockStore instance;
    private final Set<BlockSearchEntry> cache;

    private BlockStore() {
        cache = new HashSet<>();
        initializeDefaultOres();
    }

    private void initializeDefaultOres() {
        // Diamond
        cache.add(new BlockSearchEntry(Blocks.DIAMOND_ORE.getDefaultState(), new BasicColor(51, 236, 255), true, -64, 16, 4, 7, 1.0f));
        cache.add(new BlockSearchEntry(Blocks.DEEPSLATE_DIAMOND_ORE.getDefaultState(), new BasicColor(51, 236, 255), true, -64, 16, 4, 7, 1.0f));

        // Gold
        cache.add(new BlockSearchEntry(Blocks.GOLD_ORE.getDefaultState(), new BasicColor(255, 252, 51), true, -64, 32, 9, 4, 1.0f));
        cache.add(new BlockSearchEntry(Blocks.DEEPSLATE_GOLD_ORE.getDefaultState(), new BasicColor(255, 252, 51), true, -64, 32, 9, 4, 1.0f));

        // Iron
        cache.add(new BlockSearchEntry(Blocks.IRON_ORE.getDefaultState(), new BasicColor(201, 201, 183), true, -64, 72, 9, 20, 1.0f));
        cache.add(new BlockSearchEntry(Blocks.DEEPSLATE_IRON_ORE.getDefaultState(), new BasicColor(201, 201, 183), true, -64, 72, 9, 20, 1.0f));

        // Coal
        cache.add(new BlockSearchEntry(Blocks.COAL_ORE.getDefaultState(), new BasicColor(64, 64, 64), true, -64, 320, 17, 30, 1.0f));
        cache.add(new BlockSearchEntry(Blocks.DEEPSLATE_COAL_ORE.getDefaultState(), new BasicColor(64, 64, 64), true, -64, 320, 17, 30, 1.0f));

        // Copper
        cache.add(new BlockSearchEntry(Blocks.COPPER_ORE.getDefaultState(), new BasicColor(255, 140, 0), true, -16, 112, 10, 16, 1.0f));
        cache.add(new BlockSearchEntry(Blocks.DEEPSLATE_COPPER_ORE.getDefaultState(), new BasicColor(255, 140, 0), true, -16, 112, 10, 16, 1.0f));

        // Redstone
        cache.add(new BlockSearchEntry(Blocks.REDSTONE_ORE.getDefaultState(), new BasicColor(255, 0, 0), true, -64, 15, 8, 8, 1.0f));
        cache.add(new BlockSearchEntry(Blocks.DEEPSLATE_REDSTONE_ORE.getDefaultState(), new BasicColor(255, 0, 0), true, -64, 15, 8, 8, 1.0f));

        // Lapis
        cache.add(new BlockSearchEntry(Blocks.LAPIS_ORE.getDefaultState(), new BasicColor(0, 0, 255), true, -32, 32, 7, 4, 1.0f));
        cache.add(new BlockSearchEntry(Blocks.DEEPSLATE_LAPIS_ORE.getDefaultState(), new BasicColor(0, 0, 255), true, -32, 32, 7, 4, 1.0f));

        // Emerald
        cache.add(new BlockSearchEntry(Blocks.EMERALD_ORE.getDefaultState(), new BasicColor(0, 255, 0), true, -16, 480, 3, 100, 0.01f));
        cache.add(new BlockSearchEntry(Blocks.DEEPSLATE_EMERALD_ORE.getDefaultState(), new BasicColor(0, 255, 0), true, -16, 480, 3, 100, 0.01f));
    }

    public static BlockStore getInstance() {
        if (instance == null) {
            instance = new BlockStore();
        }
        return instance;
    }

    public Cache<Set<BlockSearchEntry>> getCache() {
        return new Cache<>(Collections.unmodifiableSet(cache));
    }

    public void addBlock(BlockSearchEntry entry) {
        cache.add(entry);
    }

    public void removeBlock(BlockSearchEntry entry) {
        cache.remove(entry);
    }

    public static class Cache<T> {
        private final T cache;

        public Cache(T cache) {
            this.cache = cache;
        }

        public T get() {
            return cache;
        }
    }
}