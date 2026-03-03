package xyz.zernix.oldconsoleworldgen.generator;

import org.allaymc.api.block.type.BlockState;
import org.allaymc.api.block.type.BlockTypes;
import org.allaymc.api.world.chunk.UnsafeChunk;

final class OldChunkBuffer {
    static final int WIDTH = 16;
    static final int DEPTH = 16;
    static final int GEN_DEPTH = 128;

    private static final BlockState AIR = BlockTypes.AIR.getDefaultState();
    private static final BlockState BEDROCK = BlockTypes.BEDROCK.getDefaultState();
    private static final BlockState STONE = BlockTypes.STONE.getDefaultState();
    private static final BlockState GRASS = BlockTypes.GRASS_BLOCK.getDefaultState();
    private static final BlockState DIRT = BlockTypes.DIRT.getDefaultState();
    private static final BlockState WATER = BlockTypes.WATER.getDefaultState();
    private static final BlockState LAVA = BlockTypes.LAVA.getDefaultState();
    private static final BlockState SAND = BlockTypes.SAND.getDefaultState();
    private static final BlockState SANDSTONE = BlockTypes.SANDSTONE.getDefaultState();
    private static final BlockState ICE = BlockTypes.ICE.getDefaultState();
    private static final BlockState MYCELIUM = BlockTypes.MYCELIUM.getDefaultState();

    private final int[] blocks = new int[WIDTH * DEPTH * GEN_DEPTH];
    private final OldBiome[] biomes = new OldBiome[WIDTH * DEPTH];

    int getBlock(int x, int y, int z) {
        return blocks[index(x, y, z)];
    }

    void setBlock(int x, int y, int z, int blockId) {
        blocks[index(x, y, z)] = blockId;
    }

    OldBiome getBiome(int x, int z) {
        return biomes[(z << 4) | x];
    }

    void setBiome(int x, int z, OldBiome biome) {
        biomes[(z << 4) | x] = biome;
    }

    void flushToChunk(UnsafeChunk chunk) {
        int biomeMinY = chunk.getDimensionInfo().minHeight();
        int biomeMaxY = chunk.getDimensionInfo().maxHeight();
        for (int z = 0; z < DEPTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
                var biome = getBiome(x, z);
                if (biome != null) {
                    for (int y = biomeMinY; y <= biomeMaxY; y++) {
                        chunk.setBiome(x, y, z, biome.biomeType());
                    }
                }

                for (int y = 0; y < GEN_DEPTH; y++) {
                    int blockId = getBlock(x, y, z);
                    if (blockId == OldBlockIds.AIR) {
                        continue;
                    }
                    chunk.setBlockState(x, y, z, mapBlock(blockId));
                }
            }
        }
    }

    private static int index(int x, int y, int z) {
        return (((z << 4) | x) * GEN_DEPTH) + y;
    }

    private static BlockState mapBlock(int blockId) {
        return switch (blockId) {
            case OldBlockIds.BEDROCK -> BEDROCK;
            case OldBlockIds.STONE -> STONE;
            case OldBlockIds.GRASS -> GRASS;
            case OldBlockIds.DIRT -> DIRT;
            case OldBlockIds.WATER -> WATER;
            case OldBlockIds.LAVA -> LAVA;
            case OldBlockIds.SAND -> SAND;
            case OldBlockIds.SANDSTONE -> SANDSTONE;
            case OldBlockIds.ICE -> ICE;
            case OldBlockIds.MYCELIUM -> MYCELIUM;
            default -> AIR;
        };
    }
}

final class OldBlockIds {
    static final int AIR = 0;
    static final int STONE = 1;
    static final int GRASS = 2;
    static final int DIRT = 3;
    static final int BEDROCK = 7;
    static final int WATER = 9;
    static final int LAVA = 11;
    static final int SAND = 12;
    static final int SANDSTONE = 24;
    static final int ICE = 79;
    static final int MYCELIUM = 110;

    private OldBlockIds() {
    }
}
