package xyz.zernix.oldconsoleworldgen.generator;

record GeneratorConfig(long seed, int seaLevel, boolean largeBiomes) {
    static GeneratorConfig fromPreset(String preset) {
        long seed = 0L;
        int seaLevel = 64;
        boolean largeBiomes = false;

        if (preset != null && !preset.isBlank()) {
            for (var entry : preset.split("[,;]")) {
                var parts = entry.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }

                var key = parts[0].trim().toLowerCase();
                var value = parts[1].trim();
                try {
                    switch (key) {
                        case "seed" -> seed = Long.parseLong(value);
                        case "sealevel", "sea_level" -> seaLevel = Integer.parseInt(value);
                        case "largebiomes", "large_biomes" -> largeBiomes = Boolean.parseBoolean(value);
                        default -> {
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore malformed preset entries and keep defaults.
                }
            }
        }

        return new GeneratorConfig(seed, seaLevel, largeBiomes);
    }
}
