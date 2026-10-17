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
