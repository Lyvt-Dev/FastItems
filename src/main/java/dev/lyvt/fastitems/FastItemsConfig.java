package dev.lyvt.fastitems;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Edit config/fastitems.json while Minecraft is closed. Invalid files are preserved. */
public final class FastItemsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean enabled = true;
    public boolean keepItemShadows = true;
    public boolean renderSidesOf2DItems = false;
    public boolean affect3DModels = true;
    public FilterMode filterMode = FilterMode.ALL;
    public Set<String> itemIds = new HashSet<>();

    public enum FilterMode { ALL, WHITELIST, BLACKLIST }

    public boolean accepts(ItemStack stack) {
        if (!enabled || stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean listed = itemIds.contains(id);
        return switch (filterMode) {
            case ALL -> true;
            case WHITELIST -> listed;
            case BLACKLIST -> !listed;
        };
    }

    public static FastItemsConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("fastitems.json");
        try {
            if (Files.exists(path)) {
                FastItemsConfig result = GSON.fromJson(Files.readString(path), FastItemsConfig.class);
                if (result == null || result.filterMode == null || result.itemIds == null
                        || result.itemIds.contains(null)) throw new IllegalArgumentException("Invalid config fields");
                return result;
            }
            Files.createDirectories(path.getParent());
            FastItemsConfig defaults = new FastItemsConfig();
            Files.writeString(path, GSON.toJson(defaults) + System.lineSeparator());
            return defaults;
        } catch (IOException | RuntimeException ex) {
            FastItemsMod.LOGGER.warn("Cannot read/write {}; using defaults without overwriting the file", path, ex);
            return new FastItemsConfig();
        }
    }
}
