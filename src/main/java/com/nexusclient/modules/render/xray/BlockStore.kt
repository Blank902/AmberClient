package com.amberclient.modules.render.xray

import com.amberclient.utils.core.BasicColor
import net.minecraft.block.Blocks
import java.util.Collections

object BlockStore {
    private var instance: BlockStore? = null
    val cache: MutableSet<BlockSearchEntry> = HashSet()

    init {
        // Default blocks to scan
        cache.add(BlockSearchEntry(Blocks.DIAMOND_ORE.defaultState, BasicColor(51, 236, 255), true))
        cache.add(BlockSearchEntry(Blocks.DEEPSLATE_DIAMOND_ORE.defaultState, BasicColor(51, 236, 255), true))
        cache.add(BlockSearchEntry(Blocks.GOLD_ORE.defaultState, BasicColor(255, 252, 51), true))
        cache.add(BlockSearchEntry(Blocks.DEEPSLATE_GOLD_ORE.defaultState, BasicColor(255, 252, 51), true))
        cache.add(BlockSearchEntry(Blocks.IRON_ORE.defaultState, BasicColor(201, 201, 183), true))
        cache.add(BlockSearchEntry(Blocks.DEEPSLATE_IRON_ORE.defaultState, BasicColor(201, 201, 183), true))
    }

    fun getInstance(): BlockStore {
        if (instance == null) {
            instance = BlockStore
        }
        return instance!!
    }

    fun getCache(): Cache<Set<BlockSearchEntry>> {
        return Cache(Collections.unmodifiableSet(cache))
    }

    data class Cache<T>(val cache: T) {
        fun get(): T = cache
    }
}