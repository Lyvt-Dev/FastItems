# Vollständiger Quellcode Datei für Datei

Der binäre Gradle-Wrapper und die Startskripte liegen zusätzlich im Repository und im Projektarchiv.

## settings.gradle

```groovy
pluginManagement { repositories { maven { url = 'https://maven.fabricmc.net/' }; gradlePluginPortal() } }
rootProject.name = 'fast-items-1.21.11-port'

```

## build.gradle

```groovy
plugins { id 'fabric-loom' version '1.14.10'; id 'maven-publish' }
version = '1.0.0+1.21.11'
group = 'dev.lyvt.fastitems'
base { archivesName = 'fastitems-port' }
repositories { mavenCentral() }
dependencies {
 minecraft 'com.mojang:minecraft:1.21.11'
 mappings loom.officialMojangMappings()
 modImplementation 'net.fabricmc:fabric-loader:0.20.0'
 modImplementation 'net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11'
}
java { toolchain.languageVersion = JavaLanguageVersion.of(21); withSourcesJar() }
tasks.withType(JavaCompile).configureEach { options.release = 21 }
processResources { inputs.property 'version', project.version; filesMatching('fabric.mod.json') { expand version: project.version } }
jar { from('LICENSE') }

fabricApi {
    configureTests {
        createSourceSet = true
        modId = 'fastitems-test'
        enableGameTests = false
        enableClientGameTests = true
    }
}

```

## gradle.properties

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.workers.max=4
org.gradle.parallel=true

```

## gradle/wrapper/gradle-wrapper.properties

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
distributionSha256Sum=72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f
networkTimeout=120000

```

## .gitignore

```properties
.gradle/
build/
run/
.idea/
*.iml

```

## src/gametest/java/dev/lyvt/fastitems/FastItemsClientTest.java

```java
package dev.lyvt.fastitems;

import dev.lyvt.fastitems.mixin.ItemStackRenderStateAccessor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FastItemsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                FastItemsConfig previous = FastItemsMod.config;
                try {
                    FastItemsConfig config = new FastItemsConfig();
                    FastItemsMod.config = config;
                    for (var item : new net.minecraft.world.item.Item[] {Items.POTION, Items.ENDER_PEARL, Items.BOOK, Items.ENCHANTED_BOOK}) {
                        ItemStack stack = new ItemStack(item);
                        config.enabled = false;
                        var vanilla = extract(client, stack);
                        int vanillaCount = quads(vanilla);
                        check(!billboard(vanilla), "disabled must retain vanilla");
                        config.enabled = true;
                        var flat = extract(client, stack);
                        check(billboard(flat), "sprite must face camera: " + item);
                        check(quads(flat) > 0 && quads(flat) < vanillaCount, "sprite geometry must be reduced: " + item);
                        FastItemsMod.LOGGER.info("FASTITEMS: {} quads {} -> {}", item, vanillaCount, quads(flat));
                        var access = (ItemStackRenderStateAccessor) flat.item;
                        for (int i = 0; i < access.fastitems$activeLayerCount(); i++) {
                            check(access.fastitems$layers()[i].prepareQuadList().stream().allMatch(q -> q.direction() == Direction.SOUTH), "only front quads");
                        }
                        config.renderSidesOf2DItems = true;
                        check(quads(extract(client, stack)) == vanillaCount, "sides must restore vanilla quads");
                        config.renderSidesOf2DItems = false;
                    }
                    ItemStack custom = new ItemStack(Items.BOOK);
                    custom.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                            new net.minecraft.world.item.component.CustomModelData(java.util.List.of(42f), java.util.List.of(true), java.util.List.of("test"), java.util.List.of(0xff00ff)));
                    custom.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                    check(billboard(extract(client, custom)) && quads(extract(client, custom)) > 0, "component model fallback and glint");
                    config.affect3DModels = false;
                    check(!billboard(extract(client, new ItemStack(Items.STONE))), "3D opt-out");
                    config.affect3DModels = true;
                    check(billboard(extract(client, new ItemStack(Items.STONE))), "3D opt-in");
                    check(!billboard(extract(client, new ItemStack(Items.CHEST))), "special renderer fallback");
                    check(!billboard(extract(client, ItemStack.EMPTY)), "empty stack fallback");
                    config.keepItemShadows = false;
                    var noShadow = extract(client, new ItemStack(Items.BOOK));
                    check(noShadow.shadowRadius == 0 && noShadow.shadowPieces.isEmpty(), "shadow opt-out");
                    config.filterMode = FastItemsConfig.FilterMode.WHITELIST;
                    config.itemIds.add("minecraft:book");
                    check(billboard(extract(client, new ItemStack(Items.BOOK))), "whitelist include");
                    check(!billboard(extract(client, new ItemStack(Items.STONE))), "whitelist exclude");
                    config.filterMode = FastItemsConfig.FilterMode.BLACKLIST;
                    check(!billboard(extract(client, new ItemStack(Items.BOOK))), "blacklist exclude");
                    check(billboard(extract(client, new ItemStack(Items.STONE))), "blacklist include");
                    FastItemsMod.LOGGER.info("FASTITEMS: all model/config assertions passed");
                } finally {
                    FastItemsMod.config = previous;
                }
            });
            world.getServer().runCommand("gamemode spectator @p");
            world.getServer().runCommand("tp @p 0 4 -4 0 8");
            world.getServer().runCommand("summon item -1 5 0 {Item:{id:\"minecraft:potion\",count:16},PickupDelay:32767,NoGravity:1b}");
            world.getServer().runCommand("summon item 0 5 0 {Item:{id:\"minecraft:book\",count:32},PickupDelay:32767,NoGravity:1b}");
            world.getServer().runCommand("summon item 1 5 0 {Item:{id:\"minecraft:stone\",count:64},PickupDelay:32767,NoGravity:1b}");
            context.waitTicks(40);
            context.takeScreenshot("fastitems-billboard");
            world.getServer().runCommand("tp @p 0 4 4 180 8");
            context.waitTicks(10);
            context.takeScreenshot("fastitems-billboard-reverse");
        }
    }

    private static ItemEntityRenderState extract(Minecraft client, ItemStack stack) {
        ItemEntity entity = new ItemEntity(client.level, 0, 5, 0, stack);
        ItemEntityRenderer renderer = (ItemEntityRenderer) client.getEntityRenderDispatcher().getRenderer(entity);
        ItemEntityRenderState state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, 0);
        return state;
    }
    private static int quads(ItemEntityRenderState state) {
        var access = (ItemStackRenderStateAccessor) state.item;
        int count = 0;
        for (int i = 0; i < access.fastitems$activeLayerCount(); i++) count += access.fastitems$layers()[i].prepareQuadList().size();
        return count;
    }
    private static boolean billboard(ItemEntityRenderState state) { return ((FastItemsState) state).fastitems$isBillboard(); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}

```

## src/main/java/dev/lyvt/fastitems/FastItemsConfig.java

```java
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

```

## src/main/java/dev/lyvt/fastitems/FastItemsMod.java

```java
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

```

## src/main/java/dev/lyvt/fastitems/FastItemsRenderer.java

```java
package dev.lyvt.fastitems;

import dev.lyvt.fastitems.mixin.ItemStackRenderStateAccessor;
import dev.lyvt.fastitems.mixin.LayerRenderStateAccessor;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public final class FastItemsRenderer {
    private FastItemsRenderer() {}

    /**
     * Operates AFTER vanilla resolves components, model selectors, tint and foil.
     * Quad lists belong to this render state, not the shared baked resource model.
     * No hand, GUI, item-frame or item-display rendering is changed.
     */
    public static void prepare(ItemStack stack, ItemEntityRenderState state) {
        FastItemsState extension = (FastItemsState) state;
        extension.fastitems$setBillboard(false); // Render states can be reused: always reset.
        FastItemsConfig config = FastItemsMod.config;
        if (!config.accepts(stack) || state.item.isEmpty()) return;

        ItemStackRenderStateAccessor access = (ItemStackRenderStateAccessor) state.item;
        int count = access.fastitems$activeLayerCount();
        ItemStackRenderState.LayerRenderState[] layers = access.fastitems$layers();
        if (count == 0) return;
        boolean flat = true;
        for (int i = 0; i < count; i++) {
            LayerRenderStateAccessor layer = (LayerRenderStateAccessor) layers[i];
            // Shields, chests, skulls and other special renderers keep vanilla behavior.
            // A single special layer also excludes the rest of a composite model.
            if (layer.fastitems$specialRenderer() != null || layers[i].prepareQuadList().isEmpty()) return;
            flat &= isFlat(layers[i], layer);
        }
        if (!flat && !config.affect3DModels) return;
        extension.fastitems$setBillboard(true);
        if (!config.keepItemShadows) {
            state.shadowRadius = 0;
            state.shadowPieces.clear();
        }
        if (!flat || config.renderSidesOf2DItems) return;

        // Keep vanilla extents for bobbing, stacking and bounding-box calculations.
        // Removing vertices must not shift the entity or collapse stack spacing.
        state.item.getModelBoundingBox();
        for (int i = 0; i < count; i++) {
            layers[i].prepareQuadList().removeIf(quad -> quad.direction() != Direction.SOUTH);
        }
    }

    private static boolean isFlat(ItemStackRenderState.LayerRenderState layer, LayerRenderStateAccessor access) {
        var transform = access.fastitems$transform();
        // Rotated or mirrored custom ground transforms may expose the culled face.
        // Treat those as 3D instead of risking invisible geometry.
        if (access.fastitems$usesBlockLight() || transform.rotation().lengthSquared() > 0.000001f
                || transform.scale().x() <= 0 || transform.scale().y() <= 0 || transform.scale().z() <= 0) return false;
        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        boolean front = false, back = false;
        for (BakedQuad quad : layer.prepareQuadList()) {
            front |= quad.direction() == Direction.SOUTH;
            back |= quad.direction() == Direction.NORTH;
            for (int vertex = 0; vertex < 4; vertex++) {
                var p = quad.position(vertex);
                if (!Float.isFinite(p.x()) || !Float.isFinite(p.y()) || !Float.isFinite(p.z())) return false;
                minZ = Math.min(minZ, p.z());
                maxZ = Math.max(maxZ, p.z());
            }
        }
        // Generated sprites have depth 1/16. A thin custom model is accepted only
        // when it has both front and back faces; arbitrary thick geometry is preserved.
        return front && back && maxZ - minZ <= 0.0626f;
    }
}

```

## src/main/java/dev/lyvt/fastitems/FastItemsState.java

```java
package dev.lyvt.fastitems;

/** Per-entity state, never a global flag shared between deferred render submissions. */
public interface FastItemsState {
    boolean fastitems$isBillboard();
    void fastitems$setBillboard(boolean value);
}

```

## src/main/java/dev/lyvt/fastitems/mixin/ItemEntityRenderStateMixin.java

```java
package dev.lyvt.fastitems.mixin;

import dev.lyvt.fastitems.FastItemsState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements FastItemsState {
    @Unique private boolean fastitems$billboard;
    @Override public boolean fastitems$isBillboard() { return fastitems$billboard; }
    @Override public void fastitems$setBillboard(boolean value) { fastitems$billboard = value; }
}

```

## src/main/java/dev/lyvt/fastitems/mixin/ItemEntityRendererMixin.java

```java
package dev.lyvt.fastitems.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lyvt.fastitems.FastItemsRenderer;
import dev.lyvt.fastitems.FastItemsState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void fastitems$prepare(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
        FastItemsRenderer.prepare(entity.getItem(), state);
    }

    /**
     * 1.21.11 uses extractRenderState -> submit -> deferred drawing. Replace only
     * vanilla's single spin rotation. Bobbing, stack count, lighting, foil, outlines
     * and the base renderer's submission remain untouched. Exact descriptors avoid
     * matching synthetic bridge methods or other renderer overloads.
     */
    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
    private void fastitems$rotate(PoseStack target, Quaternionfc vanillaSpin,
                                 ItemEntityRenderState state, PoseStack poses,
                                 SubmitNodeCollector collector, CameraRenderState camera) {
        if (((FastItemsState) state).fastitems$isBillboard()) {
            target.mulPose(camera.orientation);
            // Camera.setRotation already includes PI - yaw in 1.21.11.
            // An extra Y-180 rotation would expose the culled back face.
        } else {
            target.mulPose(vanillaSpin);
        }
    }
}

```

## src/main/java/dev/lyvt/fastitems/mixin/ItemStackRenderStateAccessor.java

```java
package dev.lyvt.fastitems.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Only active layers belong to the current resolved model; capacity may contain stale layers. */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("activeLayerCount") int fastitems$activeLayerCount();
    @Accessor("layers") ItemStackRenderState.LayerRenderState[] fastitems$layers();
}

```

## src/main/java/dev/lyvt/fastitems/mixin/LayerRenderStateAccessor.java

```java
package dev.lyvt.fastitems.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("specialRenderer") SpecialModelRenderer<?> fastitems$specialRenderer();
    @Accessor("usesBlockLight") boolean fastitems$usesBlockLight();
    @Accessor("transform") ItemTransform fastitems$transform();
}

```

## src/gametest/resources/fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "fastitems-test",
  "version": "1.0.0",
  "name": "Fast Items rendering tests",
  "environment": "client",
  "entrypoints": { "fabric-client-gametest": ["dev.lyvt.fastitems.FastItemsClientTest"] }
}

```

## src/main/resources/fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "fastitems",
  "version": "${version}",
  "name": "Fast Items 1.21.11 Port",
  "description": "Client-side billboard rendering and reduced geometry for dropped items.",
  "authors": ["Lyvt-Dev", "Noryea (original concept and implementation)"],
  "license": "CC0-1.0",
  "environment": "client",
  "entrypoints": { "client": ["dev.lyvt.fastitems.FastItemsMod"] },
  "mixins": ["fastitems.mixins.json"],
  "depends": { "fabricloader": ">=0.20.0", "minecraft": "1.21.11", "java": ">=21", "fabric-api": "*" }
}

```

## src/main/resources/fastitems.mixins.json

```json
{
  "required": true,
  "package": "dev.lyvt.fastitems.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": ["ItemEntityRendererMixin", "ItemEntityRenderStateMixin", "ItemStackRenderStateAccessor", "LayerRenderStateAccessor"],
  "injectors": { "defaultRequire": 1 }
}

```
