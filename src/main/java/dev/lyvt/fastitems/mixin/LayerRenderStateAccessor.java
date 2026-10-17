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
