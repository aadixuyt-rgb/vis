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
 * Adds a green outline and animated expanding wave rings after item rendering.
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
        greenglow$drawOutlineAndWaves(matrices, consumers);
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
        greenglow$drawOutlineAndWaves(matrices, consumers);
    }

    private static void greenglow$drawOutlineAndWaves(MatrixStack matrices, VertexConsumerProvider consumers) {
        if (consumers == null || matrices == null) {
            return;
        }

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        Box outline = new Box(-0.53, -0.53, -0.53, 0.53, 0.53, 0.53);

        // Crisp green outline around the rendered item.
        VertexRendering.drawBox(matrices, lines, outline.expand(0.025),
                0.08f, 1.0f, 0.22f, 0.28f);
        VertexRendering.drawBox(matrices, lines, outline,
                0.10f, 1.0f, 0.28f, 0.95f);

        // Three animated square wavefronts expand outward from the item.
        // This is an additional pass: resource-pack models and textures stay intact.
        double cycle = (System.nanoTime() / 1_000_000L % 1800L) / 1800.0;
        for (int i = 0; i < 3; i++) {
            double phase = (cycle + i / 3.0) % 1.0;
            double radius = 0.58 + phase * 0.78;
            float alpha = (float) (0.82 * (1.0 - phase));
            Box wave = new Box(
                    -radius, -0.49 - phase * 0.025, -radius,
                    radius, -0.49 + phase * 0.025, radius
            );

            // A soft outer line and a sharper inner line create a neon-like pulse.
            VertexRendering.drawBox(matrices, lines, wave.expand(0.018, 0.0, 0.018),
                    0.04f, 1.0f, 0.18f, alpha * 0.30f);
            VertexRendering.drawBox(matrices, lines, wave,
                    0.08f, 1.0f, 0.24f, alpha);
        }
    }
}
