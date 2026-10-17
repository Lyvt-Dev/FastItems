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
