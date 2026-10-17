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
