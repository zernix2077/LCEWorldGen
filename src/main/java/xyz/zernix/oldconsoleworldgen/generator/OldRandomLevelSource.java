package xyz.zernix.oldconsoleworldgen.generator;

final class OldRandomLevelSource {
    private static final int CHUNK_WIDTH = 4;
    private static final int CHUNK_HEIGHT = 8;

    private final GeneratorConfig config;
    private final OldBiomeSource biomeSource;
    private final OldJavaRandom random;
    private final OldPerlinNoise lperlinNoise1;
    private final OldPerlinNoise lperlinNoise2;
    private final OldPerlinNoise perlinNoise1;
    private final OldPerlinNoise perlinNoise3;
    private final OldPerlinNoise scaleNoise;
    private final OldPerlinNoise depthNoise;
    private final OldLargeCaveFeature caveFeature = new OldLargeCaveFeature();
    private final OldCanyonFeature canyonFeature = new OldCanyonFeature();
    private float[] pows;

    OldRandomLevelSource(GeneratorConfig config) {
        this.config = config;
        this.biomeSource = new OldBiomeSource(config.seed(), config.largeBiomes());
        this.random = new OldJavaRandom(config.seed());
        this.lperlinNoise1 = new OldPerlinNoise(random, 16);
        this.lperlinNoise2 = new OldPerlinNoise(random, 16);
        this.perlinNoise1 = new OldPerlinNoise(random, 8);
        this.perlinNoise3 = new OldPerlinNoise(random, 4);
        this.scaleNoise = new OldPerlinNoise(random, 10);
        this.depthNoise = new OldPerlinNoise(random, 16);
    }

    synchronized void generateChunk(int chunkX, int chunkZ, OldChunkBuffer chunkBuffer) {
        random.setSeed(chunkX * 341873128712L + chunkZ * 132897987541L);
        prepareHeights(chunkX, chunkZ, chunkBuffer);

        OldBiome[] biomes = biomeSource.getBiomeBlock(chunkX * 16, chunkZ * 16, 16, 16);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                chunkBuffer.setBiome(x, z, biomes[x + z * 16]);
            }
        }

        buildSurfaces(chunkX, chunkZ, chunkBuffer);
        caveFeature.apply(config.seed(), chunkX, chunkZ, chunkBuffer);
        canyonFeature.apply(config.seed(), chunkX, chunkZ, chunkBuffer);
    }

    private void prepareHeights(int chunkX, int chunkZ, OldChunkBuffer chunkBuffer) {
        int xChunks = 16 / CHUNK_WIDTH;
        int yChunks = OldChunkBuffer.GEN_DEPTH / CHUNK_HEIGHT;
        int xSize = xChunks + 1;
        int ySize = yChunks + 1;
        int zSize = xChunks + 1;

        OldBiome[] biomes = biomeSource.getRawBiomeBlock(chunkX * CHUNK_WIDTH - 2, chunkZ * CHUNK_WIDTH - 2, xSize + 5, zSize + 5);
        double[] heights = getHeights(chunkX * xChunks, 0, chunkZ * xChunks, xSize, ySize, zSize, biomes, xSize + 5);

        for (int xc = 0; xc < xChunks; xc++) {
            for (int zc = 0; zc < xChunks; zc++) {
                for (int yc = 0; yc < yChunks; yc++) {
                    double yStep = 1.0 / CHUNK_HEIGHT;
                    double s0 = heights[((xc) * zSize + zc) * ySize + yc];
                    double s1 = heights[((xc) * zSize + (zc + 1)) * ySize + yc];
                    double s2 = heights[((xc + 1) * zSize + zc) * ySize + yc];
                    double s3 = heights[((xc + 1) * zSize + (zc + 1)) * ySize + yc];
                    double s0a = (heights[((xc) * zSize + zc) * ySize + (yc + 1)] - s0) * yStep;
                    double s1a = (heights[((xc) * zSize + (zc + 1)) * ySize + (yc + 1)] - s1) * yStep;
                    double s2a = (heights[((xc + 1) * zSize + zc) * ySize + (yc + 1)] - s2) * yStep;
                    double s3a = (heights[((xc + 1) * zSize + (zc + 1)) * ySize + (yc + 1)] - s3) * yStep;

                    for (int y = 0; y < CHUNK_HEIGHT; y++) {
                        double xStep = 1.0 / CHUNK_WIDTH;
                        double currentS0 = s0;
                        double currentS1 = s1;
                        double currentS0a = (s2 - s0) * xStep;
                        double currentS1a = (s3 - s1) * xStep;

                        for (int x = 0; x < CHUNK_WIDTH; x++) {
                            double zStep = 1.0 / CHUNK_WIDTH;
                            double value = currentS0;
                            double valueStep = (currentS1 - currentS0) * zStep;
                            value -= valueStep;
                            for (int z = 0; z < CHUNK_WIDTH; z++) {
                                value += valueStep;
                                int blockId = OldBlockIds.AIR;
                                int worldY = yc * CHUNK_HEIGHT + y;
                                if (value > 0.0) {
                                    blockId = OldBlockIds.STONE;
                                } else if (worldY < config.seaLevel()) {
                                    blockId = OldBlockIds.WATER;
                                }
                                chunkBuffer.setBlock(x + xc * CHUNK_WIDTH, worldY, z + zc * CHUNK_WIDTH, blockId);
                            }
                            currentS0 += currentS0a;
                            currentS1 += currentS1a;
                        }

                        s0 += s0a;
                        s1 += s1a;
                        s2 += s2a;
                        s3 += s3a;
                    }
                }
            }
        }
    }

    private void buildSurfaces(int chunkX, int chunkZ, OldChunkBuffer chunkBuffer) {
        double scale = 1.0 / 32.0;
        double[] depthBuffer = perlinNoise3.getRegion(null, chunkX * 16, chunkZ * 16, 16, 16, scale * 2.0, scale * 2.0, scale * 2.0);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                OldBiome biome = chunkBuffer.getBiome(x, z);
                float temperature = biome.temperature();
                int runDepth = (int) (depthBuffer[x + z * 16] / 3.0 + 3.0 + random.nextDouble() * 0.25);
                int run = -1;
                int top = biome.topBlockId();
                int filler = biome.fillerBlockId();

                for (int y = OldChunkBuffer.GEN_DEPTH - 1; y >= 0; y--) {
                    if (y <= 1 + random.nextInt(2)) {
                        chunkBuffer.setBlock(x, y, z, OldBlockIds.BEDROCK);
                        continue;
                    }

                    int old = chunkBuffer.getBlock(x, y, z);
                    if (old == OldBlockIds.AIR) {
                        run = -1;
                    } else if (old == OldBlockIds.STONE) {
                        if (run == -1) {
                            if (runDepth <= 0) {
                                top = OldBlockIds.AIR;
                                filler = OldBlockIds.STONE;
                            } else if (y >= config.seaLevel() - 4 && y <= config.seaLevel() + 1) {
                                top = biome.topBlockId();
                                filler = biome.fillerBlockId();
                            }

                            if (y < config.seaLevel() && top == OldBlockIds.AIR) {
                                top = temperature < 0.15f ? OldBlockIds.ICE : OldBlockIds.WATER;
                            }

                            run = runDepth;
                            chunkBuffer.setBlock(x, y, z, y >= config.seaLevel() - 1 ? top : filler);
                        } else if (run > 0) {
                            run--;
                            chunkBuffer.setBlock(x, y, z, filler);
                            if (run == 0 && filler == OldBlockIds.SAND) {
                                run = random.nextInt(4);
                                filler = OldBlockIds.SANDSTONE;
                            }
                        }
                    }
                }
            }
        }
    }

    private double[] getHeights(int x, int y, int z, int xSize, int ySize, int zSize, OldBiome[] biomes, int biomeWidth) {
        if (pows == null) {
            pows = new float[25];
            for (int xb = -2; xb <= 2; xb++) {
                for (int zb = -2; zb <= 2; zb++) {
                    pows[xb + 2 + (zb + 2) * 5] = (float) (10.0 / Math.sqrt(xb * xb + zb * zb + 0.2));
                }
            }
        }

        double s = 684.412;
        double hs = 684.412;
        scaleNoise.getRegion(null, x, z, xSize, zSize, 1.121, 1.121, 0.5);
        double[] dr = depthNoise.getRegion(null, x, z, xSize, zSize, 200.0, 200.0, 0.5);
        double[] pnr = perlinNoise1.getRegion(null, x, y, z, xSize, ySize, zSize, s / 80.0, hs / 160.0, s / 80.0);
        double[] ar = lperlinNoise1.getRegion(null, x, y, z, xSize, ySize, zSize, s, hs, s);
        double[] br = lperlinNoise2.getRegion(null, x, y, z, xSize, ySize, zSize, s, hs, s);
        double[] buffer = new double[xSize * ySize * zSize];

        int p = 0;
        int pp = 0;
        for (int xx = 0; xx < xSize; xx++) {
            for (int zz = 0; zz < zSize; zz++) {
                float weightedScale = 0.0f;
                float weightedDepth = 0.0f;
                float weightTotal = 0.0f;
                OldBiome middleBiome = biomes[(xx + 2) + (zz + 2) * biomeWidth];
                for (int xb = -2; xb <= 2; xb++) {
                    for (int zb = -2; zb <= 2; zb++) {
                        OldBiome biome = biomes[(xx + xb + 2) + (zz + zb + 2) * biomeWidth];
                        float weight = pows[xb + 2 + (zb + 2) * 5] / (biome.depth() + 2.0f);
                        if (biome.depth() > middleBiome.depth()) {
                            weight /= 2.0f;
                        }
                        weightedScale += biome.scale() * weight;
                        weightedDepth += biome.depth() * weight;
                        weightTotal += weight;
                    }
                }

                weightedScale /= weightTotal;
                weightedDepth /= weightTotal;
                weightedScale = weightedScale * 0.9f + 0.1f;
                weightedDepth = (weightedDepth * 4.0f - 1.0f) / 8.0f;

                double randomDepth = dr[pp] / 8000.0;
                if (randomDepth < 0.0) {
                    randomDepth = -randomDepth * 0.3;
                }
                randomDepth = randomDepth * 3.0 - 2.0;
                if (randomDepth < 0.0) {
                    randomDepth /= 2.0;
                    if (randomDepth < -1.0) {
                        randomDepth = -1.0;
                    }
                    randomDepth /= 1.4;
                    randomDepth /= 2.0;
                } else {
                    if (randomDepth > 1.0) {
                        randomDepth = 1.0;
                    }
                    randomDepth /= 8.0;
                }
                pp++;

                for (int yy = 0; yy < ySize; yy++) {
                    double depth = weightedDepth + randomDepth * 0.2;
                    double scaleValue = weightedScale;
                    depth = depth * ySize / 16.0;
                    double yCenter = ySize / 2.0 + depth * 4.0;
                    double yOffset = (yy - yCenter) * 12.0 * 128.0 / OldChunkBuffer.GEN_DEPTH / scaleValue;
                    if (yOffset < 0.0) {
                        yOffset *= 4.0;
                    }

                    double low = ar[p] / 512.0;
                    double high = br[p] / 512.0;
                    double blend = (pnr[p] / 10.0 + 1.0) / 2.0;
                    double value = blend < 0.0 ? low : blend > 1.0 ? high : low + (high - low) * blend;
                    value -= yOffset;

                    if (yy > ySize - 4) {
                        double slide = (yy - (ySize - 4)) / 3.0;
                        value = value * (1.0 - slide) + -10.0 * slide;
                    }

                    buffer[p] = value;
                    p++;
                }
            }
        }

        return buffer;
    }
}
