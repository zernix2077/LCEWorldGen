package xyz.zernix.oldconsoleworldgen.generator;

import org.allaymc.api.world.generator.WorldGenerator;

import java.util.function.Function;

public final class OldConsoleOverworldGeneratorFactory implements Function<String, WorldGenerator> {
    public static final String NAME = "OLD_CONSOLE_OVERWORLD";

    @Override
    public WorldGenerator apply(String preset) {
        var config = GeneratorConfig.fromPreset(preset);
        return WorldGenerator.builder()
                .name(NAME)
                .preset(preset == null ? "" : preset)
                .noisers(new OldOverworldNoiser(config))
                .populators(new OldOverworldPopulator(config))
                .postProcessors(new OldOverworldPostProcessor(config))
                .build();
    }
}
