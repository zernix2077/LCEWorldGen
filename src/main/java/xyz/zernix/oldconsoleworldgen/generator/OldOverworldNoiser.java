package xyz.zernix.oldconsoleworldgen.generator;

import org.allaymc.api.world.generator.context.NoiseContext;
import org.allaymc.api.world.generator.function.Noiser;

public final class OldOverworldNoiser implements Noiser {
    private final OldRandomLevelSource levelSource;

    public OldOverworldNoiser(GeneratorConfig config) {
        this.levelSource = new OldRandomLevelSource(config);
    }

    @Override
    public boolean apply(NoiseContext context) {
        var buffer = new OldChunkBuffer();
        levelSource.generateChunk(context.getCurrentChunk().getX(), context.getCurrentChunk().getZ(), buffer);
        buffer.flushToChunk(context.getCurrentChunk());
        return true;
    }
}
