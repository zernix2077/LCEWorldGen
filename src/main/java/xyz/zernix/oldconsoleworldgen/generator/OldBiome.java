package xyz.zernix.oldconsoleworldgen.generator;

import org.allaymc.api.world.biome.BiomeType;
import org.allaymc.api.world.biome.BiomeTypes;

import java.util.HashMap;
import java.util.Map;

final class OldBiome {
    private static final OldBiome[] BIOMES = new OldBiome[256];
    private static final Map<BiomeType, OldBiome> BY_ALLAY = new HashMap<>();

    static final OldBiome OCEAN = register(0, "Ocean", OldBlockIds.GRASS, OldBlockIds.DIRT, -1.0f, 0.4f, 0.5f, 0.5f, BiomeTypes.OCEAN);
    static final OldBiome PLAINS = register(1, "Plains", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.1f, 0.3f, 0.8f, 0.4f, BiomeTypes.PLAINS);
    static final OldBiome DESERT = register(2, "Desert", OldBlockIds.SAND, OldBlockIds.SAND, 0.1f, 0.2f, 2.0f, 0.0f, BiomeTypes.DESERT);
    static final OldBiome EXTREME_HILLS = register(3, "Extreme Hills", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.3f, 1.5f, 0.2f, 0.3f, BiomeTypes.EXTREME_HILLS);
    static final OldBiome FOREST = register(4, "Forest", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.1f, 0.3f, 0.7f, 0.8f, BiomeTypes.FOREST);
    static final OldBiome TAIGA = register(5, "Taiga", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.1f, 0.4f, 0.05f, 0.8f, BiomeTypes.TAIGA);
    static final OldBiome SWAMPLAND = register(6, "Swampland", OldBlockIds.GRASS, OldBlockIds.DIRT, -0.2f, 0.1f, 0.8f, 0.9f, BiomeTypes.SWAMPLAND);
    static final OldBiome RIVER = register(7, "River", OldBlockIds.GRASS, OldBlockIds.DIRT, -0.5f, 0.0f, 0.5f, 0.5f, BiomeTypes.RIVER);
    static final OldBiome HELL = register(8, "Hell", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.1f, 0.3f, 2.0f, 0.0f, BiomeTypes.HELL);
    static final OldBiome SKY = register(9, "Sky", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.1f, 0.3f, 0.5f, 0.5f, BiomeTypes.THE_END);
    static final OldBiome FROZEN_OCEAN = register(10, "FrozenOcean", OldBlockIds.GRASS, OldBlockIds.DIRT, -1.0f, 0.5f, 0.0f, 0.5f, BiomeTypes.LEGACY_FROZEN_OCEAN);
    static final OldBiome FROZEN_RIVER = register(11, "FrozenRiver", OldBlockIds.GRASS, OldBlockIds.DIRT, -0.5f, 0.0f, 0.0f, 0.5f, BiomeTypes.FROZEN_RIVER);
    static final OldBiome ICE_FLATS = register(12, "Ice Plains", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.1f, 0.3f, 0.0f, 0.5f, BiomeTypes.ICE_PLAINS);
    static final OldBiome ICE_MOUNTAINS = register(13, "Ice Mountains", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.3f, 1.3f, 0.0f, 0.5f, BiomeTypes.ICE_MOUNTAINS);
    static final OldBiome MUSHROOM_ISLAND = register(14, "MushroomIsland", OldBlockIds.MYCELIUM, OldBlockIds.DIRT, 0.2f, 1.0f, 0.9f, 1.0f, BiomeTypes.MUSHROOM_ISLAND);
    static final OldBiome MUSHROOM_ISLAND_SHORE = register(15, "MushroomIslandShore", OldBlockIds.MYCELIUM, OldBlockIds.DIRT, -1.0f, 0.1f, 0.9f, 1.0f, BiomeTypes.MUSHROOM_ISLAND_SHORE);
    static final OldBiome BEACH = register(16, "Beach", OldBlockIds.SAND, OldBlockIds.SAND, 0.0f, 0.1f, 0.8f, 0.4f, BiomeTypes.BEACH);
    static final OldBiome DESERT_HILLS = register(17, "DesertHills", OldBlockIds.SAND, OldBlockIds.SAND, 0.3f, 0.8f, 2.0f, 0.0f, BiomeTypes.DESERT_HILLS);
    static final OldBiome FOREST_HILLS = register(18, "ForestHills", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.3f, 0.7f, 0.7f, 0.8f, BiomeTypes.FOREST_HILLS);
    static final OldBiome TAIGA_HILLS = register(19, "TaigaHills", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.3f, 0.8f, 0.05f, 0.8f, BiomeTypes.TAIGA_HILLS);
    static final OldBiome EXTREME_HILLS_EDGE = register(20, "Extreme Hills Edge", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.2f, 0.8f, 0.2f, 0.3f, BiomeTypes.EXTREME_HILLS_EDGE);
    static final OldBiome JUNGLE = register(21, "Jungle", OldBlockIds.GRASS, OldBlockIds.DIRT, 0.2f, 0.4f, 1.2f, 0.9f, BiomeTypes.JUNGLE);
    static final OldBiome JUNGLE_HILLS = register(22, "JungleHills", OldBlockIds.GRASS, OldBlockIds.DIRT, 1.8f, 0.5f, 1.2f, 0.9f, BiomeTypes.JUNGLE_HILLS);

    private final int id;
    private final String name;
    private final int topBlockId;
    private final int fillerBlockId;
    private final float depth;
    private final float scale;
    private final float temperature;
    private final float downfall;
    private final BiomeType biomeType;

    private OldBiome(
            int id,
            String name,
            int topBlockId,
            int fillerBlockId,
            float depth,
            float scale,
            float temperature,
            float downfall,
            BiomeType biomeType
    ) {
        this.id = id;
        this.name = name;
        this.topBlockId = topBlockId;
        this.fillerBlockId = fillerBlockId;
        this.depth = depth;
        this.scale = scale;
        this.temperature = temperature;
        this.downfall = downfall;
        this.biomeType = biomeType;
    }

    static OldBiome byId(int id) {
        return BIOMES[id & 255];
    }

    static OldBiome byAllayBiome(BiomeType biomeType) {
        return BY_ALLAY.getOrDefault(biomeType, PLAINS);
    }

    int id() {
        return id;
    }

    int topBlockId() {
        return topBlockId;
    }

    int fillerBlockId() {
        return fillerBlockId;
    }

    float depth() {
        return depth;
    }

    float scale() {
        return scale;
    }

    float temperature() {
        return temperature;
    }

    float downfall() {
        return downfall;
    }

    BiomeType biomeType() {
        return biomeType;
    }

    boolean hasSnow() {
        return temperature < 0.15f;
    }

    @Override
    public String toString() {
        return name;
    }

    private static OldBiome register(
            int id,
            String name,
            int topBlockId,
            int fillerBlockId,
            float depth,
            float scale,
            float temperature,
            float downfall,
            BiomeType biomeType
    ) {
        var biome = new OldBiome(id, name, topBlockId, fillerBlockId, depth, scale, temperature, downfall, biomeType);
        BIOMES[id] = biome;
        BY_ALLAY.put(biomeType, biome);
        return biome;
    }
}
