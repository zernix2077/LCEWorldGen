package xyz.zernix.oldconsoleworldgen;

import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import xyz.zernix.oldconsoleworldgen.generator.OldConsoleOverworldGeneratorFactory;

public final class OldConsoleWorldGenPlugin extends Plugin {
    @Override
    public void onLoad() {
        Registries.WORLD_GENERATOR_FACTORIES.register(
            OldConsoleOverworldGeneratorFactory.NAME,
            new OldConsoleOverworldGeneratorFactory()
        );
        pluginLogger.info("Registered {} world generator", OldConsoleOverworldGeneratorFactory.NAME);
    }

    @Override
    public void onEnable() {
        pluginLogger.info("Old console overworld generator plugin enabled");
    }

    @Override
    public void onDisable() {
        pluginLogger.info("Old console overworld generator plugin disabled");
    }
}
