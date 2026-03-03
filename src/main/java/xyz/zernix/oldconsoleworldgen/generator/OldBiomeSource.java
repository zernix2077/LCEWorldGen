package xyz.zernix.oldconsoleworldgen.generator;

final class OldBiomeSource {
    private final OldLayer layer;
    private final OldLayer zoomedLayer;

    OldBiomeSource(long seed, boolean largeBiomes) {
        var layers = OldLayer.createDefaultLayers(seed, largeBiomes);
        this.layer = layers[0];
        this.zoomedLayer = layers[1];
    }

    OldBiome[] getRawBiomeBlock(int x, int z, int w, int h) {
        int[] ids = layer.getArea(x, z, w, h);
        OldBiome[] biomes = new OldBiome[w * h];
        for (int i = 0; i < biomes.length; i++) {
            biomes[i] = OldBiome.byId(ids[i]);
        }
        return biomes;
    }

    OldBiome[] getBiomeBlock(int x, int z, int w, int h) {
        int[] ids = zoomedLayer.getArea(x, z, w, h);
        OldBiome[] biomes = new OldBiome[w * h];
        for (int i = 0; i < biomes.length; i++) {
            biomes[i] = OldBiome.byId(ids[i]);
        }
        return biomes;
    }

    OldBiome getBiome(int x, int z) {
        return OldBiome.byId(zoomedLayer.getArea(x, z, 1, 1)[0]);
    }
}
