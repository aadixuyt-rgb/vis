package pl.adixuyt.greenglow.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a separate green outline pass after the normal item model is rendered.
 * The original model, texture, tint, and resource-pack assets are untouched.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("TAIL")
    )
    private void greenglow$outlineGuiAndDroppedItems(
            ItemStack stack,
            ModelTransformationMode mode,
            int light,
            int overlay,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            World world,
            int seed,
            CallbackInfo ci
    ) {
        greenglow$drawOutline(matrices, consumers);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("TAIL")
    )
    private void greenglow$outlineHeldItems(
            LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode mode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            World world,
            int light,
            int overlay,
            int seed,
            CallbackInfo ci
    ) {
        greenglow$drawOutline(matrices, consumers);
    }

    private static void greenglow$drawOutline(MatrixStack matrices, VertexConsumerProvider consumers) {
        if (consumers == null) {
            return;
        }

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        Box outline = new Box(-0.53, -0.53, -0.53, 0.53, 0.53, 0.53);

        // Subtle outer pass plus a crisp green edge; original item texture is untouched.
        VertexRendering.drawBox(matrices, lines, outline.expand(0.025),
                0.08f, 1.0f, 0.22f, 0.28f);
        VertexRendering.drawBox(matrices, lines, outline,
                0.10f, 1.0f, 0.28f, 0.95f);
    }
}
