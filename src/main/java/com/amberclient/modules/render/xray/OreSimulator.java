package com.amberclient.modules.render.xray;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import java.util.*;

public class OreSimulator {
    public record Ore(BlockState blockState, BasicColor color, int minY, int maxY, int count, int size, float rarity,
                      boolean scattered, int index, int point, float discardOnAirChance) {
    }

    public static List<Ore> getOres() {
        List<Ore> ores = new ArrayList<>();
        Set<BlockSearchEntry> entries = BlockStore.getInstance().getCache().get();
        int index = 0;
        for (BlockSearchEntry entry : entries) {
            boolean scattered = entry.state().getBlock() == net.minecraft.block.Blocks.EMERALD_ORE ||
                    entry.state().getBlock() == net.minecraft.block.Blocks.DEEPSLATE_EMERALD_ORE;
            ores.add(new Ore(entry.state(), entry.color(), entry.minY(), entry.maxY(), entry.count(),
                    entry.size(), entry.rarity(), scattered, index, 0, 0.0f));
            index++;
        }
        return ores;
    }

    public static long getWorldSeed() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isIntegratedServerRunning() && mc.getServer() != null) {
            return mc.getServer().getOverworld().getSeed();
        } else {
            throw new RuntimeException("Seed unavailable");
        }
    }

    public static Set<BlockPosWithColor> simulateChunk(ChunkPos chunkPos, List<Ore> ores, long worldSeed) {
        Set<BlockPosWithColor> positions = new HashSet<>();
        int chunkX = chunkPos.x << 4;
        int chunkZ = chunkPos.z << 4;
        ChunkRandom random = new ChunkRandom(ChunkRandom.RandomProvider.XOROSHIRO.create(0));
        long populationSeed = random.setPopulationSeed(worldSeed, chunkX, chunkZ);

        for (Ore ore : ores) {
            random.setDecoratorSeed(populationSeed, ore.index, ore.point);
            int repeat = ore.count;
            for (int i = 0; i < repeat; i++) {
                if (ore.rarity != 1.0f && random.nextFloat() >= ore.rarity) {
                    continue;
                }
                int x = random.nextInt(16) + chunkX;
                int z = random.nextInt(16) + chunkZ;
                int y = random.nextInt(ore.maxY - ore.minY + 1) + ore.minY;
                BlockPos origin = new BlockPos(x, y, z);
                if (ore.scattered) {
                    positions.addAll(generateHidden(random, origin, ore.size, ore.color));
                } else {
                    positions.addAll(generateNormal(random, origin, ore.size, ore.discardOnAirChance, ore.color));
                }
            }
        }
        return positions;
    }

    private static Set<BlockPosWithColor> generateNormal(ChunkRandom random, BlockPos origin, int size,
                                                         float discardOnAirChance, BasicColor color) {
        float f = random.nextFloat() * 3.1415927F;
        float g = (float) size / 8.0F;
        int i = MathHelper.ceil(((float) size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double d = origin.getX() + Math.sin(f) * g;
        double e = origin.getX() - Math.sin(f) * g;
        double h = origin.getZ() + Math.cos(f) * g;
        double j = origin.getZ() - Math.cos(f) * g;
        double l = origin.getY() + random.nextInt(3) - 2;
        double m = origin.getY() + random.nextInt(3) - 2;
        int n = origin.getX() - MathHelper.ceil(g) - i;
        int o = origin.getY() - 2 - i;
        int p = origin.getZ() - MathHelper.ceil(g) - i;
        int q = 2 * (MathHelper.ceil(g) + i);
        int r = 2 * (2 + i);

        return new HashSet<>(generateVeinPart(random, size, d, e, h, j, l, m, n, o, p, q, r, discardOnAirChance, color));
    }

    private static Set<BlockPosWithColor> generateVeinPart(ChunkRandom random, int size, double startX, double endX,
                                                           double startZ, double endZ, double startY, double endY,
                                                           int x, int y, int z, int sizeV, int i, float discardOnAirChance,
                                                           BasicColor color) {
        Set<BlockPosWithColor> positions = new HashSet<>();
        BitSet bitSet = new BitSet(sizeV * i * sizeV);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        double[] ds = new double[size * 4];

        for (int n = 0; n < size; ++n) {
            float f = (float) n / (float) size;
            double p = MathHelper.lerp(f, startX, endX);
            double q = MathHelper.lerp(f, startY, endY);
            double r = MathHelper.lerp(f, startZ, endZ);
            double s = random.nextDouble() * (double) size / 16.0D;
            double m = ((double) (MathHelper.sin(3.1415927F * f) + 1.0F) * s + 1.0D) / 2.0D;
            ds[n * 4] = p;
            ds[n * 4 + 1] = q;
            ds[n * 4 + 2] = r;
            ds[n * 4 + 3] = m;
        }

        for (int n = 0; n < size - 1; ++n) {
            if (ds[n * 4 + 3] <= 0.0D) continue;
            for (int o = n + 1; o < size; ++o) {
                if (ds[o * 4 + 3] <= 0.0D) continue;
                double p = ds[n * 4] - ds[o * 4];
                double q = ds[n * 4 + 1] - ds[o * 4 + 1];
                double r = ds[n * 4 + 2] - ds[o * 4 + 2];
                double s = ds[n * 4 + 3] - ds[o * 4 + 3];
                if (s * s > p * p + q * q + r * r) {
                    if (s > 0.0D) ds[o * 4 + 3] = -1.0D;
                    else ds[n * 4 + 3] = -1.0D;
                }
            }
        }

        for (int n = 0; n < size; ++n) {
            double u = ds[n * 4 + 3];
            if (u < 0.0D) continue;
            double v = ds[n * 4];
            double w = ds[n * 4 + 1];
            double aa = ds[n * 4 + 2];
            int ab = Math.max(MathHelper.floor(v - u), x);
            int ac = Math.max(MathHelper.floor(w - u), y);
            int ad = Math.max(MathHelper.floor(aa - u), z);
            int ae = Math.max(MathHelper.floor(v + u), ab);
            int af = Math.max(MathHelper.floor(w + u), ac);
            int ag = Math.max(MathHelper.floor(aa + u), ad);

            for (int ah = ab; ah <= ae; ++ah) {
                double ai = ((double) ah + 0.5D - v) / u;
                if (ai * ai >= 1.0D) continue;
                for (int aj = ac; aj <= af; ++aj) {
                    double ak = ((double) aj + 0.5D - w) / u;
                    if (ai * ai + ak * ak >= 1.0D) continue;
                    for (int al = ad; al <= ag; ++al) {
                        double am = ((double) al + 0.5D - aa) / u;
                        if (ai * ai + ak * ak + am * am >= 1.0D) continue;
                        int an = ah - x + (aj - y) * sizeV + (al - z) * sizeV * i;
                        if (bitSet.get(an)) continue;
                        bitSet.set(an);
                        mutable.set(ah, aj, al);
                        if (aj >= -64 && aj < 320) {
                            if (discardOnAirChance == 0.0f || random.nextFloat() < discardOnAirChance) {
                                positions.add(new BlockPosWithColor(mutable.toImmutable(), color));
                            }
                        }
                    }
                }
            }
        }
        return positions;
    }

    private static Set<BlockPosWithColor> generateHidden(ChunkRandom random, BlockPos origin, int size, BasicColor color) {
        Set<BlockPosWithColor> positions = new HashSet<>();
        int i = random.nextInt(size + 1);
        for (int j = 0; j < i; ++j) {
            int s = Math.min(j, 7);
            int x = randomCoord(random, s) + origin.getX();
            int y = randomCoord(random, s) + origin.getY();
            int z = randomCoord(random, s) + origin.getZ();
            BlockPos pos = new BlockPos(x, y, z);
            positions.add(new BlockPosWithColor(pos, color));
        }
        return positions;
    }

    private static int randomCoord(ChunkRandom random, int size) {
        return Math.round((random.nextFloat() - random.nextFloat()) * size);
    }
}