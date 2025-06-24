package com.amberclient.modules.render.xray;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BlockStore {
    private static BlockStore instance;
    private final Set<BlockSearchEntry> cache;

    private BlockStore() {
        cache = new HashSet<>();
        for (OreType oreType : OreType.values()) {
            for (var block : oreType.blocks) {
                cache.add(new BlockSearchEntry(block.getDefaultState(), oreType.color, true));
            }
        }
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