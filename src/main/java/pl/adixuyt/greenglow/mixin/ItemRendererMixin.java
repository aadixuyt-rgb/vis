package pl.adixuyt.greenglow.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws an additional green item outline and animated circular waves.
 * The original item model, texture, tint, and resource-pack assets are retained.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("TAIL")
    )
    private void greenglow$renderGuiAndWorldItems(
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
        if (!stack.isEmpty()) {
            greenglow$drawItemEffect(matrices, consumers, mode);
        }
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("TAIL")
    )
    private void greenglow$renderLocalPlayerHeldItems(
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
        // Never add effects to other players or entities.
        if (entity == MinecraftClient.getInstance().player && !stack.isEmpty()) {
            greenglow$drawItemEffect(matrices, consumers, mode);
        }
    }

    private static void greenglow$drawItemEffect(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            ModelTransformationMode mode
    ) {
        if (matrices == null || consumers == null) {
            return;
        }

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        boolean gui = mode == ModelTransformationMode.GUI;

        // A compact outline follows the item's render transform and leaves its texture visible.
        Box outline = new Box(-0.53, -0.53, -0.53, 0.53, 0.53, 0.53);
        VertexRendering.drawBox(matrices, lines, outline.expand(0.025),
                0.05f, 1.0f, 0.24f, 0.20f);
        VertexRendering.drawBox(matrices, lines, outline,
                0.07f, 1.0f, 0.28f, 0.88f);

        // Four animated wavefronts travel outward from each item.
        double cycle = (System.nanoTime() / 1_000_000L % 2200L) / 2200.0;
        double startRadius = gui ? 0.43 : 0.56;
        double expansion = gui ? 0.46 : 0.92;

        for (int wave = 0; wave < 4; wave++) {
            double phase = (cycle + wave * 0.25) % 1.0;
            double radius = startRadius + phase * expansion;
            float alpha = (float) (0.78 * (1.0 - phase));

            // The outer, low-opacity traces give the bright ring a soft neon edge.
            drawWaveRing(lines, entry, radius + 0.055, gui, alpha * 0.16f);
            drawWaveRing(lines, entry, radius + 0.025, gui, alpha * 0.34f);
            drawWaveRing(lines, entry, radius, gui, alpha);
        }
    }

    private static void drawWaveRing(
            VertexConsumer lines,
            MatrixStack.Entry entry,
            double radius,
            boolean gui,
            float alpha
    ) {
        final int segments = 48;
        for (int segment = 0; segment < segments; segment++) {
            double a0 = Math.PI * 2.0 * segment / segments;
            double a1 = Math.PI * 2.0 * (segment + 1) / segments;

            float x0 = (float) (Math.cos(a0) * radius);
            float x1 = (float) (Math.cos(a1) * radius);
            float z0 = (float) (Math.sin(a0) * radius);
            float z1 = (float) (Math.sin(a1) * radius);

            if (gui) {
                // GUI icons face the camera, so their waves are drawn in the front plane.
                addLine(lines, entry, x0, z0, 0.60f, x1, z1, 0.60f, alpha);
            } else {
                // World and held items get ground-style rings below the model.
                addLine(lines, entry, x0, -0.54f, z0, x1, -0.54f, z1, alpha);
            }
        }
    }

    private static void addLine(
            VertexConsumer lines,
            MatrixStack.Entry entry,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float alpha
    ) {
        lines.vertex(entry, x1, y1, z1)
                .color(0.05f, 1.0f, 0.24f, alpha)
                .normal(entry, 0.0f, 1.0f, 0.0f);
        lines.vertex(entry, x2, y2, z2)
                .color(0.05f, 1.0f, 0.24f, alpha)
                .normal(entry, 0.0f, 1.0f, 0.0f);
    }
}
