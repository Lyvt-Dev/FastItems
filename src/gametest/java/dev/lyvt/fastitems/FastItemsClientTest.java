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
