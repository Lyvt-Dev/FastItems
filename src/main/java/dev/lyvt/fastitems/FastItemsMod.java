package dev.lyvt.fastitems;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FastItemsMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("fastitems");
    public static FastItemsConfig config = new FastItemsConfig();

    @Override
    public void onInitializeClient() {
        config = FastItemsConfig.load();
        LOGGER.info("Fast Items 1.21.11 Port initialized");
    }
}
