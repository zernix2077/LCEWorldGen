package xyz.zernix.oldconsoleworldgen.generator;

abstract class OldLayer {
    private final long seedMixup;
    protected OldLayer parent;
    private long seed;
    private long randomValue;

    protected OldLayer(long seedMixup) {
        long value = seedMixup;
        value = mix(value, seedMixup);
        value = mix(value, seedMixup);
        value = mix(value, seedMixup);
        this.seedMixup = value;
    }

    static OldLayer[] createDefaultLayers(long seed, boolean largeBiomes) {
        OldLayer islandLayer = new IslandLayer(1L);
        islandLayer = new FuzzyZoomLayer(2000L, islandLayer);
        islandLayer = new AddIslandLayer(1L, islandLayer);
        islandLayer = new ZoomLayer(2001L, islandLayer);
        islandLayer = new AddIslandLayer(2L, islandLayer);
        islandLayer = new AddSnowLayer(2L, islandLayer);
        islandLayer = new ZoomLayer(2002L, islandLayer);
        islandLayer = new AddIslandLayer(3L, islandLayer);
        islandLayer = new ZoomLayer(2003L, islandLayer);
        islandLayer = new AddIslandLayer(4L, islandLayer);

        int zoomLevel = largeBiomes ? 6 : 4;

        OldLayer riverLayer = islandLayer;
        riverLayer = ZoomLayer.zoom(1000L, riverLayer, 0);
        riverLayer = new RiverInitLayer(100L, riverLayer);
        riverLayer = ZoomLayer.zoom(1000L, riverLayer, zoomLevel + 2);
        riverLayer = new RiverLayer(1L, riverLayer);
        riverLayer = new SmoothLayer(1000L, riverLayer);

        OldLayer biomeLayer = islandLayer;
        biomeLayer = ZoomLayer.zoom(1000L, biomeLayer, 0);
        biomeLayer = new BiomeInitLayer(200L, biomeLayer, false);
        biomeLayer = ZoomLayer.zoom(1000L, biomeLayer, 2);
        biomeLayer = new RegionHillsLayer(1000L, biomeLayer);

        for (int i = 0; i < zoomLevel; i++) {
            biomeLayer = new ZoomLayer(1000L + i, biomeLayer);
            if (i == 0) {
                biomeLayer = new AddIslandLayer(3L, biomeLayer);
                biomeLayer = new AddMushroomIslandLayer(5L, biomeLayer);
            }
            if (i == 1) {
                biomeLayer = new GrowMushroomIslandLayer(5L, biomeLayer);
                biomeLayer = new ShoreLayer(1000L, biomeLayer);
                biomeLayer = new SwampRiversLayer(1000L, biomeLayer);
            }
        }

        biomeLayer = new SmoothLayer(1000L, biomeLayer);
        biomeLayer = new RiverMixerLayer(100L, biomeLayer, riverLayer);

        OldLayer zoomedLayer = new VoronoiZoomLayer(10L, biomeLayer);
        biomeLayer.init(seed);
        zoomedLayer.init(seed);
        return new OldLayer[]{biomeLayer, zoomedLayer};
    }

    void init(long seed) {
        this.seed = seed;
        if (parent != null) {
            parent.init(seed);
        }
        this.seed = mix(this.seed, seedMixup);
        this.seed = mix(this.seed, seedMixup);
        this.seed = mix(this.seed, seedMixup);
    }

    protected void initRandom(long x, long y) {
        randomValue = seed;
        randomValue = mix(randomValue, x);
        randomValue = mix(randomValue, y);
        randomValue = mix(randomValue, x);
        randomValue = mix(randomValue, y);
    }

    protected int nextRandom(int max) {
        int result = (int) ((randomValue >> 24) % max);
        if (result < 0) {
            result += max;
        }
        randomValue = mix(randomValue, seed);
        return result;
    }

    abstract int[] getArea(int xo, int yo, int w, int h);

    private static long mix(long seed, long value) {
        seed *= seed * 6364136223846793005L + 1442695040888963407L;
        seed += value;
        return seed;
    }
}

final class IslandLayer extends OldLayer {
    IslandLayer(long seedMixup) {
        super(seedMixup);
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        var result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                initRandom(xo + x, yo + y);
                result[x + y * w] = nextRandom(10) == 0 ? 1 : 0;
            }
        }

        if (xo > -w && xo <= 0 && yo > -h && yo <= 0) {
            result[-xo + (-yo * w)] = 1;
        }
        return result;
    }
}

abstract class AbstractZoomLayer extends OldLayer {
    protected AbstractZoomLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    protected int[] zoomArea(int xo, int yo, int w, int h, boolean fuzzy) {
        int px = xo >> 1;
        int py = yo >> 1;
        int pw = (w >> 1) + 3;
        int ph = (h >> 1) + 3;
        int[] parentArea = parent.getArea(px, py, pw, ph);

        int[] tmp = new int[(pw * 2) * (ph * 2)];
        int tmpWidth = pw << 1;
        for (int y = 0; y < ph - 1; y++) {
            int row = y << 1;
            int index = row * tmpWidth;
            int ul = parentArea[y * pw];
            int dl = parentArea[(y + 1) * pw];
            for (int x = 0; x < pw - 1; x++) {
                initRandom((long) (x + px) << 1, (long) (y + py) << 1);
                int ur = parentArea[(x + 1) + y * pw];
                int dr = parentArea[(x + 1) + (y + 1) * pw];
                tmp[index] = ul;
                tmp[index++ + tmpWidth] = choose(ul, dl);
                tmp[index] = choose(ul, ur);
                tmp[index++ + tmpWidth] = fuzzy ? chooseFuzzy(ul, ur, dl, dr) : chooseModeOrRandom(ul, ur, dl, dr);
                ul = ur;
                dl = dr;
            }
        }

        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(tmp, (y + (yo & 1)) * (pw << 1) + (xo & 1), result, y * w, w);
        }
        return result;
    }

    private int choose(int a, int b) {
        return nextRandom(2) == 0 ? a : b;
    }

    private int chooseFuzzy(int a, int b, int c, int d) {
        int pick = nextRandom(4);
        return pick == 0 ? a : pick == 1 ? b : pick == 2 ? c : d;
    }

    private int chooseModeOrRandom(int a, int b, int c, int d) {
        if (b == c && c == d) return b;
        if (a == b && a == c) return a;
        if (a == b && a == d) return a;
        if (a == c && a == d) return a;
        if (a == b && c != d) return a;
        if (a == c && b != d) return a;
        if (a == d && b != c) return a;
        if (b == c && a != d) return b;
        if (b == d && a != c) return b;
        if (c == d && a != b) return c;
        return chooseFuzzy(a, b, c, d);
    }
}

final class FuzzyZoomLayer extends AbstractZoomLayer {
    FuzzyZoomLayer(long seedMixup, OldLayer parent) {
        super(seedMixup, parent);
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        return zoomArea(xo, yo, w, h, true);
    }
}

final class ZoomLayer extends AbstractZoomLayer {
    ZoomLayer(long seedMixup, OldLayer parent) {
        super(seedMixup, parent);
    }

    static OldLayer zoom(long seed, OldLayer parent, int count) {
        OldLayer result = parent;
        for (int i = 0; i < count; i++) {
            result = new ZoomLayer(seed + i, result);
        }
        return result;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        return zoomArea(xo, yo, w, h, false);
    }
}

final class AddIslandLayer extends OldLayer {
    AddIslandLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int px = xo - 1;
        int py = yo - 1;
        int pw = w + 2;
        int ph = h + 2;
        int[] parentArea = parent.getArea(px, py, pw, ph);
        int[] result = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int n1 = parentArea[x + y * pw];
                int n2 = parentArea[(x + 2) + y * pw];
                int n3 = parentArea[x + (y + 2) * pw];
                int n4 = parentArea[(x + 2) + (y + 2) * pw];
                int center = parentArea[(x + 1) + (y + 1) * pw];
                initRandom(x + xo, y + yo);

                if (center == 0 && (n1 != 0 || n2 != 0 || n3 != 0 || n4 != 0)) {
                    int odds = 1;
                    int swap = 1;
                    if (n1 != 0 && nextRandom(odds++) == 0) swap = n1;
                    if (n2 != 0 && nextRandom(odds++) == 0) swap = n2;
                    if (n3 != 0 && nextRandom(odds++) == 0) swap = n3;
                    if (n4 != 0 && nextRandom(odds++) == 0) swap = n4;
                    if (nextRandom(3) == 0) {
                        result[x + y * w] = swap;
                    } else {
                        result[x + y * w] = swap == OldBiome.ICE_FLATS.id() ? OldBiome.FROZEN_OCEAN.id() : 0;
                    }
                } else if (center > 0 && (n1 == 0 || n2 == 0 || n3 == 0 || n4 == 0)) {
                    if (nextRandom(5) == 0) {
                        result[x + y * w] = center == OldBiome.ICE_FLATS.id() ? OldBiome.FROZEN_OCEAN.id() : 0;
                    } else {
                        result[x + y * w] = center;
                    }
                } else {
                    result[x + y * w] = center;
                }
            }
        }

        return result;
    }
}

final class AddSnowLayer extends OldLayer {
    AddSnowLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] parentArea = parent.getArea(xo - 1, yo - 1, w + 2, h + 2);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int center = parentArea[(x + 1) + (y + 1) * (w + 2)];
                initRandom(x + xo, y + yo);
                if (center == 0) {
                    result[x + y * w] = 0;
                } else {
                    int pick = nextRandom(5);
                    result[x + y * w] = pick == 0 ? OldBiome.ICE_FLATS.id() : 1;
                }
            }
        }
        return result;
    }
}

final class SmoothLayer extends OldLayer {
    SmoothLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] parentArea = parent.getArea(xo - 1, yo - 1, w + 2, h + 2);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int left = parentArea[x + (y + 1) * (w + 2)];
                int right = parentArea[(x + 2) + (y + 1) * (w + 2)];
                int up = parentArea[(x + 1) + y * (w + 2)];
                int down = parentArea[(x + 1) + (y + 2) * (w + 2)];
                int center = parentArea[(x + 1) + (y + 1) * (w + 2)];
                if (left == right && up == down) {
                    initRandom(x + xo, y + yo);
                    center = nextRandom(2) == 0 ? left : up;
                } else {
                    if (left == right) center = left;
                    if (up == down) center = up;
                }
                result[x + y * w] = center;
            }
        }
        return result;
    }
}

final class RiverInitLayer extends OldLayer {
    RiverInitLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] base = parent.getArea(xo, yo, w, h);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                initRandom(x + xo, y + yo);
                result[x + y * w] = base[x + y * w] > 0 ? nextRandom(2) + 2 : 0;
            }
        }
        return result;
    }
}

final class RiverLayer extends OldLayer {
    RiverLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] parentArea = parent.getArea(xo - 1, yo - 1, w + 2, h + 2);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int left = parentArea[x + (y + 1) * (w + 2)];
                int right = parentArea[(x + 2) + (y + 1) * (w + 2)];
                int up = parentArea[(x + 1) + y * (w + 2)];
                int down = parentArea[(x + 1) + (y + 2) * (w + 2)];
                int center = parentArea[(x + 1) + (y + 1) * (w + 2)];
                result[x + y * w] = (center == 0 || left == 0 || right == 0 || up == 0 || down == 0
                        || center != left || center != right || center != up || center != down)
                        ? OldBiome.RIVER.id()
                        : -1;
            }
        }
        return result;
    }
}

final class BiomeInitLayer extends OldLayer {
    private final OldBiome[] startBiomes;

    BiomeInitLayer(long seedMixup, OldLayer parent, boolean legacy11) {
        super(seedMixup);
        this.parent = parent;
        this.startBiomes = legacy11
                ? new OldBiome[]{OldBiome.DESERT, OldBiome.FOREST, OldBiome.EXTREME_HILLS, OldBiome.SWAMPLAND, OldBiome.PLAINS, OldBiome.TAIGA}
                : new OldBiome[]{OldBiome.DESERT, OldBiome.FOREST, OldBiome.EXTREME_HILLS, OldBiome.SWAMPLAND, OldBiome.PLAINS, OldBiome.TAIGA, OldBiome.JUNGLE};
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] base = parent.getArea(xo, yo, w, h);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                initRandom(x + xo, y + yo);
                int old = base[x + y * w];
                if (old == 0) {
                    result[x + y * w] = 0;
                } else if (old == OldBiome.MUSHROOM_ISLAND.id()) {
                    result[x + y * w] = old;
                } else if (old == 1) {
                    result[x + y * w] = startBiomes[nextRandom(startBiomes.length)].id();
                } else {
                    int pick = startBiomes[nextRandom(startBiomes.length)].id();
                    result[x + y * w] = pick == OldBiome.TAIGA.id() ? pick : OldBiome.ICE_FLATS.id();
                }
            }
        }
        return result;
    }
}

final class RegionHillsLayer extends OldLayer {
    RegionHillsLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] base = parent.getArea(xo - 1, yo - 1, w + 2, h + 2);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                initRandom(x + xo, y + yo);
                int old = base[(x + 1) + (y + 1) * (w + 2)];
                if (nextRandom(3) == 0) {
                    int next = switch (old) {
                        case 2 -> OldBiome.DESERT_HILLS.id();
                        case 4 -> OldBiome.FOREST_HILLS.id();
                        case 5 -> OldBiome.TAIGA_HILLS.id();
                        case 1 -> OldBiome.FOREST.id();
                        case 12 -> OldBiome.ICE_MOUNTAINS.id();
                        case 21 -> OldBiome.JUNGLE_HILLS.id();
                        default -> old;
                    };

                    if (next != old) {
                        int north = base[(x + 1) + y * (w + 2)];
                        int east = base[(x + 2) + (y + 1) * (w + 2)];
                        int west = base[x + (y + 1) * (w + 2)];
                        int south = base[(x + 1) + (y + 2) * (w + 2)];
                        result[x + y * w] = (north == old && east == old && west == old && south == old) ? next : old;
                    } else {
                        result[x + y * w] = old;
                    }
                } else {
                    result[x + y * w] = old;
                }
            }
        }
        return result;
    }
}

final class AddMushroomIslandLayer extends OldLayer {
    AddMushroomIslandLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] parentArea = parent.getArea(xo - 1, yo - 1, w + 2, h + 2);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int n1 = parentArea[x + y * (w + 2)];
                int n2 = parentArea[(x + 2) + y * (w + 2)];
                int n3 = parentArea[x + (y + 2) * (w + 2)];
                int n4 = parentArea[(x + 2) + (y + 2) * (w + 2)];
                int center = parentArea[(x + 1) + (y + 1) * (w + 2)];
                initRandom(x + xo, y + yo);
                if (center == 0 && n1 == 0 && n2 == 0 && n3 == 0 && n4 == 0 && nextRandom(100) == 0) {
                    result[x + y * w] = OldBiome.MUSHROOM_ISLAND.id();
                } else {
                    result[x + y * w] = center;
                }
            }
        }
        return result;
    }
}

final class GrowMushroomIslandLayer extends OldLayer {
    GrowMushroomIslandLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] parentArea = parent.getArea(xo - 1, yo - 1, w + 2, h + 2);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int n1 = parentArea[x + y * (w + 2)];
                int n2 = parentArea[(x + 2) + y * (w + 2)];
                int n3 = parentArea[x + (y + 2) * (w + 2)];
                int n4 = parentArea[(x + 2) + (y + 2) * (w + 2)];
                int center = parentArea[(x + 1) + (y + 1) * (w + 2)];
                result[x + y * w] = (n1 == OldBiome.MUSHROOM_ISLAND.id()
                        || n2 == OldBiome.MUSHROOM_ISLAND.id()
                        || n3 == OldBiome.MUSHROOM_ISLAND.id()
                        || n4 == OldBiome.MUSHROOM_ISLAND.id()) ? OldBiome.MUSHROOM_ISLAND.id() : center;
            }
        }
        return result;
    }
}

final class ShoreLayer extends OldLayer {
    ShoreLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] base = parent.getArea(xo - 1, yo - 1, w + 2, h + 2);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int old = base[(x + 1) + (y + 1) * (w + 2)];
                int north = base[(x + 1) + y * (w + 2)];
                int east = base[(x + 2) + (y + 1) * (w + 2)];
                int west = base[x + (y + 1) * (w + 2)];
                int south = base[(x + 1) + (y + 2) * (w + 2)];
                if (old == OldBiome.MUSHROOM_ISLAND.id()) {
                    result[x + y * w] = (north == OldBiome.OCEAN.id()
                            || east == OldBiome.OCEAN.id()
                            || west == OldBiome.OCEAN.id()
                            || south == OldBiome.OCEAN.id()) ? OldBiome.MUSHROOM_ISLAND_SHORE.id() : old;
                } else if (old != OldBiome.OCEAN.id()
                        && old != OldBiome.RIVER.id()
                        && old != OldBiome.SWAMPLAND.id()
                        && old != OldBiome.EXTREME_HILLS.id()) {
                    result[x + y * w] = (north == OldBiome.OCEAN.id()
                            || east == OldBiome.OCEAN.id()
                            || west == OldBiome.OCEAN.id()
                            || south == OldBiome.OCEAN.id()) ? OldBiome.BEACH.id() : old;
                } else if (old == OldBiome.EXTREME_HILLS.id()) {
                    result[x + y * w] = (north != OldBiome.EXTREME_HILLS.id()
                            || east != OldBiome.EXTREME_HILLS.id()
                            || west != OldBiome.EXTREME_HILLS.id()
                            || south != OldBiome.EXTREME_HILLS.id()) ? OldBiome.EXTREME_HILLS_EDGE.id() : old;
                } else {
                    result[x + y * w] = old;
                }
            }
        }
        return result;
    }
}

final class SwampRiversLayer extends OldLayer {
    SwampRiversLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] base = parent.getArea(xo - 1, yo - 1, w + 2, h + 2);
        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                initRandom(x + xo, y + yo);
                int old = base[(x + 1) + (y + 1) * (w + 2)];
                if ((old == OldBiome.SWAMPLAND.id() && nextRandom(6) == 0)
                        || ((old == OldBiome.JUNGLE.id() || old == OldBiome.JUNGLE_HILLS.id()) && nextRandom(8) == 0)) {
                    result[x + y * w] = OldBiome.RIVER.id();
                } else {
                    result[x + y * w] = old;
                }
            }
        }
        return result;
    }
}

final class RiverMixerLayer extends OldLayer {
    private final OldLayer biomes;
    private final OldLayer rivers;

    RiverMixerLayer(long seedMixup, OldLayer biomes, OldLayer rivers) {
        super(seedMixup);
        this.biomes = biomes;
        this.rivers = rivers;
    }

    @Override
    void init(long seed) {
        biomes.init(seed);
        rivers.init(seed);
        super.init(seed);
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        int[] biomeArea = biomes.getArea(xo, yo, w, h);
        int[] riverArea = rivers.getArea(xo, yo, w, h);
        int[] result = new int[w * h];
        for (int i = 0; i < result.length; i++) {
            if (biomeArea[i] == OldBiome.OCEAN.id()) {
                result[i] = biomeArea[i];
            } else if (riverArea[i] >= 0) {
                if (biomeArea[i] == OldBiome.ICE_FLATS.id()) {
                    result[i] = OldBiome.FROZEN_RIVER.id();
                } else if (biomeArea[i] == OldBiome.MUSHROOM_ISLAND.id() || biomeArea[i] == OldBiome.MUSHROOM_ISLAND_SHORE.id()) {
                    result[i] = OldBiome.MUSHROOM_ISLAND.id();
                } else {
                    result[i] = riverArea[i];
                }
            } else {
                result[i] = biomeArea[i];
            }
        }
        return result;
    }
}

final class VoronoiZoomLayer extends OldLayer {
    VoronoiZoomLayer(long seedMixup, OldLayer parent) {
        super(seedMixup);
        this.parent = parent;
    }

    @Override
    int[] getArea(int xo, int yo, int w, int h) {
        xo -= 2;
        yo -= 2;
        int bits = 2;
        int scale = 1 << bits;
        int px = xo >> bits;
        int py = yo >> bits;
        int pw = (w >> bits) + 3;
        int ph = (h >> bits) + 3;
        int[] parentArea = parent.getArea(px, py, pw, ph);

        int tmpWidth = pw << bits;
        int tmpHeight = ph << bits;
        int[] tmp = new int[tmpWidth * tmpHeight];
        for (int y = 0; y < ph - 1; y++) {
            int ul = parentArea[y * pw];
            int dl = parentArea[(y + 1) * pw];
            for (int x = 0; x < pw - 1; x++) {
                double stretch = scale * 0.9;
                initRandom((long) (x + px) << bits, (long) (y + py) << bits);
                double x0 = (nextRandom(1024) / 1024.0 - 0.5) * stretch;
                double y0 = (nextRandom(1024) / 1024.0 - 0.5) * stretch;
                initRandom((long) (x + px + 1) << bits, (long) (y + py) << bits);
                double x1 = (nextRandom(1024) / 1024.0 - 0.5) * stretch + scale;
                double y1 = (nextRandom(1024) / 1024.0 - 0.5) * stretch;
                initRandom((long) (x + px) << bits, (long) (y + py + 1) << bits);
                double x2 = (nextRandom(1024) / 1024.0 - 0.5) * stretch;
                double y2 = (nextRandom(1024) / 1024.0 - 0.5) * stretch + scale;
                initRandom((long) (x + px + 1) << bits, (long) (y + py + 1) << bits);
                double x3 = (nextRandom(1024) / 1024.0 - 0.5) * stretch + scale;
                double y3 = (nextRandom(1024) / 1024.0 - 0.5) * stretch + scale;

                int ur = parentArea[(x + 1) + y * pw];
                int dr = parentArea[(x + 1) + (y + 1) * pw];
                for (int yy = 0; yy < scale; yy++) {
                    int index = ((y << bits) + yy) * tmpWidth + (x << bits);
                    for (int xx = 0; xx < scale; xx++) {
                        double d0 = sq(yy - y0) + sq(xx - x0);
                        double d1 = sq(yy - y1) + sq(xx - x1);
                        double d2 = sq(yy - y2) + sq(xx - x2);
                        double d3 = sq(yy - y3) + sq(xx - x3);
                        tmp[index++] = d0 < d1 && d0 < d2 && d0 < d3 ? ul : d1 < d0 && d1 < d2 && d1 < d3 ? ur : d2 < d0 && d2 < d1 && d2 < d3 ? dl : dr;
                    }
                }
                ul = ur;
                dl = dr;
            }
        }

        int[] result = new int[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(tmp, (y + (yo & (scale - 1))) * tmpWidth + (xo & (scale - 1)), result, y * w, w);
        }
        return result;
    }

    private static double sq(double value) {
        return value * value;
    }
}
