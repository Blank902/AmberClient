package com.amberclient.modules.render.xray;

import com.amberclient.utils.general.BasicColor;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanTask implements Runnable {
    public static Set<BlockPosWithColor> renderQueue = Collections.synchronizedSet(new HashSet<>());
    private static final AtomicBoolean isScanning = new AtomicBoolean(false);
    private static ChunkPos playerLastChunk;
    private static boolean forceNextScan = false;
    private final ChunkPos centerChunk;
    private final int range;

    public ScanTask(ChunkPos centerChunk, int range) {
        this.centerChunk = centerChunk;
        this.range = range;
    }

    public static void runTask(ChunkPos centerChunk, int range) {
        runTask(centerChunk, range, false);
    }

    public static void runTask(ChunkPos centerChunk, int range, boolean forceScan) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !SettingsStore.getInstance().get().isActive()) {
            return;
        }

        if (forceScan || forceNextScan || playerLocationChanged(client.player, centerChunk, range) || playerLastChunk == null) {
            playerLastChunk = centerChunk;
            forceNextScan = false;
            client.execute(new ScanTask(centerChunk, range));
        }
    }

    public static void requestForcedScan() {
        forceNextScan = true;
    }

    public static void resetLocationTracking() {
        playerLastChunk = null;
        forceNextScan = true;
    }

    public static void blockBroken(World world, PlayerEntity player, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        if (!(player instanceof ClientPlayerEntity clientPlayer)) return;
        if (!SettingsStore.getInstance().get().isActive()) return;
        if (renderQueue.stream().anyMatch(e -> e.pos().equals(blockPos))) {
            runTask(clientPlayer.getChunkPos(), SettingsStore.getInstance().get().getHalfRange(), true);
        }
    }

    private static boolean playerLocationChanged(ClientPlayerEntity player, ChunkPos currentChunk, int range) {
        return playerLastChunk == null ||
                currentChunk.x > playerLastChunk.x + range || currentChunk.x < playerLastChunk.x - range ||
                currentChunk.z > playerLastChunk.z + range || currentChunk.z < playerLastChunk.z - range;
    }

    @Override
    public void run() {
        if (isScanning.get()) {
            return;
        }

        isScanning.set(true);
        Set<BlockPosWithColor> blocks = collectBlocks();
        renderQueue.clear();
        renderQueue.addAll(blocks);
        isScanning.set(false);
        RenderOutlines.requestedRefresh.set(true);
    }

    private Set<BlockPosWithColor> collectBlocks() {
        Set<BlockSearchEntry> blocks = BlockStore.getInstance().getCache().get();
        if (blocks.isEmpty()) {
            return new HashSet<>();
        }
        MinecraftClient instance = MinecraftClient.getInstance();
        final World world = instance.world;
        final ClientPlayerEntity player = instance.player;
        if (world == null || player == null) {
            return new HashSet<>();
        }
        final Set<BlockPosWithColor> renderQueue = new HashSet<>();
        int cX = centerChunk.x;
        int cZ = centerChunk.z;
        boolean exposedOnly = SettingsStore.getInstance().get().isExposedOnly();
        for (int i = cX - range; i <= cX + range; i++) {
            for (int j = cZ - range; j <= cZ + range; j++) {
                if (!world.isChunkLoaded(i, j)) {
                    continue;
                }
                int chunkStartX = i << 4;
                int chunkStartZ = j << 4;
                for (int k = chunkStartX; k < chunkStartX + 16; k++) {
                    for (int l = chunkStartZ; l < chunkStartZ + 16; l++) {
                        int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, k, l);
                        for (int m = world.getBottomY(); m < topY; m++) {
                            BlockPos pos = new BlockPos(k, m, l);
                            BasicColor color = isValidBlock(pos, world, blocks);
                            if (color != null) {
                                if (!exposedOnly || isBlockExposed(pos, world)) {
                                    renderQueue.add(new BlockPosWithColor(pos, color));
                                }
                            }
                        }
                    }
                }
            }
        }
        return renderQueue;
    }

    private BasicColor isValidBlock(BlockPos pos, World world, Set<BlockSearchEntry> blocks) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        if (SettingsStore.getInstance().get().isShowLava() && state.getBlock() == Blocks.LAVA) {
            return new BasicColor(255, 69, 0);
        }

        BlockState defaultState = state.getBlock().getDefaultState();

        return blocks.stream()
                .filter(localState -> localState.isDefault() && defaultState == localState.state() ||
                        !localState.isDefault() && state == localState.state())
                .findFirst()
                .map(BlockSearchEntry::color)
                .orElse(null);
    }

    private boolean isBlockExposed(BlockPos pos, World world) {
        BlockPos[] adjacentPositions = {
                pos.up(),    // Y+1
                pos.down(),  // Y-1
                pos.north(), // Z-1
                pos.south(), // Z+1
                pos.east(),  // X+1
                pos.west()   // X-1
        };

        for (BlockPos adjacentPos : adjacentPositions) {
            BlockState adjacentState = world.getBlockState(adjacentPos);

            if (adjacentState.isAir() ||
                    !adjacentState.isOpaque() ||
                    !adjacentState.getFluidState().isEmpty() ||
                    isNaturalCavity(adjacentPos, world)) {
                return true;
            }
        }

        return false;
    }

    private boolean isNaturalCavity(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);

        if (state.isAir()) {
            int airCount = 0;
            int checkRadius = 2;

            for (int x = -checkRadius; x <= checkRadius; x++) {
                for (int y = -checkRadius; y <= checkRadius; y++) {
                    for (int z = -checkRadius; z <= checkRadius; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos checkPos = pos.add(x, y, z);
                        if (world.getBlockState(checkPos).isAir()) {
                            airCount++;
                        }
                    }
                }
            }

            return airCount > (checkRadius * 2 + 1) * (checkRadius * 2 + 1) * (checkRadius * 2 + 1) * 0.3;
        }

        return false;
    }

    private boolean isBlockNearSurface(BlockPos pos, World world) {
        int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ());
        int maxDepth = 10;

        return pos.getY() >= (surfaceY - maxDepth);
    }

    private boolean isBlockExposedAndNearSurface(BlockPos pos, World world) {
        if (!isBlockExposed(pos, world)) {
            return false;
        }

        int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ());
        int maxDepthFromSurface = 15;

        return pos.getY() >= (surfaceY - maxDepthFromSurface);
    }

    private boolean isBlockInLargeAirPocket(BlockPos pos, World world) {
        int airBlockCount = 0;
        int totalBlocksChecked = 0;
        int radius = 2;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos checkPos = pos.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);

                    if (state.isAir()) {
                        airBlockCount++;
                    }
                    totalBlocksChecked++;
                }
            }
        }

        return (airBlockCount / (double) totalBlocksChecked) > 0.3;
    }
}