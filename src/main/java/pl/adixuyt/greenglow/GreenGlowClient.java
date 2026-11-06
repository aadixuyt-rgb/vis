package pl.adixuyt.greenglow;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Client-only GreenGlow world effects. No player/entity outlines, names,
 * labels, or HUD elements are rendered.
 */
public final class GreenGlowClient implements ClientModInitializer {
    private static final int GRID_SPACING = 8;
    private static final int GRID_RADIUS = 2;
    private static final double BOX_SIZE = 2.6;
    private static final double[] HEIGHT_OFFSETS = {-4.0, 0.0, 4.0};

    private static final float GREEN_R = 0.05f;
    private static final float GREEN_G = 1.0f;
    private static final float GREEN_B = 0.24f;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(GreenGlowClient::renderWorldGeometry);
    }

    private static void renderWorldGeometry(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || context.world() == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }

        Vec3d camera = context.camera().getPos();
        Vec3d player = client.player.getPos();
        int gridX = Math.floorDiv((int) Math.floor(player.x), GRID_SPACING);
        int gridZ = Math.floorDiv((int) Math.floor(player.z), GRID_SPACING);
        double baseY = Math.floor(player.y / GRID_SPACING) * GRID_SPACING;

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        for (int gx = -GRID_RADIUS; gx <= GRID_RADIUS; gx++) {
            for (int gz = -GRID_RADIUS; gz <= GRID_RADIUS; gz++) {
                // Keep the immediate area around the player clear.
                if (gx == 0 && gz == 0) {
                    continue;
                }

                double centerX = (gridX + gx) * GRID_SPACING + GRID_SPACING / 2.0;
                double centerZ = (gridZ + gz) * GRID_SPACING + GRID_SPACING / 2.0;

                for (double heightOffset : HEIGHT_OFFSETS) {
                    double centerY = baseY + heightOffset;
                    Box box = new Box(
                            centerX - BOX_SIZE / 2.0,
                            centerY - BOX_SIZE / 2.0,
                            centerZ - BOX_SIZE / 2.0,
                            centerX + BOX_SIZE / 2.0,
                            centerY + BOX_SIZE / 2.0,
                            centerZ + BOX_SIZE / 2.0
                    );

                    // Faint outer edge and a crisp neon-green inner edge.
                    VertexRendering.drawBox(matrices, lines, box.expand(0.045),
                            GREEN_R, GREEN_G, GREEN_B, 0.13f);
                    VertexRendering.drawBox(matrices, lines, box,
                            GREEN_R, GREEN_G, GREEN_B, 0.92f);
                }
            }
        }

        if (client.world.getRegistryKey() == World.OVERWORLD) {
            renderAurora(lines, matrices.peek(), player);
        }

        matrices.pop();
    }

    /**
     * Builds several slowly moving aurora curtains around the player.
     * The multiple low-alpha strands create a soft neon impression without
     * changing the world's textures or adding a HUD.
     */
    private static void renderAurora(VertexConsumer lines, MatrixStack.Entry entry, Vec3d player) {
        double time = System.nanoTime() / 1_000_000_000.0;
        final int arcSegments = 144;
        final int curtainCount = 28;
        final int curtainSegments = 22;

        // Long, flowing arcs across the sky.
        for (int band = 0; band < 6; band++) {
            double radius = 43.0 + band * 1.35;
            for (int segment = 0; segment < arcSegments; segment++) {
                double a0 = (Math.PI * 2.0 * segment) / arcSegments;
                double a1 = (Math.PI * 2.0 * (segment + 1)) / arcSegments;

                double wave0 = Math.sin(a0 * 3.0 + time * 0.42 + band * 0.72) * 4.2
                        + Math.sin(a0 * 7.0 - time * 0.23 + band) * 1.15;
                double wave1 = Math.sin(a1 * 3.0 + time * 0.42 + band * 0.72) * 4.2
                        + Math.sin(a1 * 7.0 - time * 0.23 + band) * 1.15;

                double y0 = player.y + 27.0 + band * 1.05 + wave0;
                double y1 = player.y + 27.0 + band * 1.05 + wave1;

                double x0 = player.x + Math.cos(a0) * radius;
                double z0 = player.z + Math.sin(a0) * radius;
                double x1 = player.x + Math.cos(a1) * radius;
                double z1 = player.z + Math.sin(a1) * radius;

                float pulse = (float) (0.70 + 0.30 * Math.sin(time * 0.65 + band * 0.8));
                float alpha = (0.16f + band * 0.012f) * pulse;

                // Soft halo strands around a brighter central ribbon.
                drawLine(lines, entry, x0, y0 - 0.16, z0, x1, y1 - 0.16, z1, alpha * 0.38f);
                drawLine(lines, entry, x0, y0, z0, x1, y1, z1, alpha);
                drawLine(lines, entry, x0, y0 + 0.16, z0, x1, y1 + 0.16, z1, alpha * 0.38f);
            }
        }

        // Thin vertical folds make the arcs read like moving aurora curtains.
        for (int curtain = 0; curtain < curtainCount; curtain++) {
            double angle = (Math.PI * 2.0 * curtain) / curtainCount;
            for (int segment = 0; segment < curtainSegments; segment++) {
                double v0 = (double) segment / curtainSegments;
                double v1 = (double) (segment + 1) / curtainSegments;

                double drift0 = Math.sin(v0 * 5.2 + time * 0.52 + curtain * 0.61) * 0.075;
                double drift1 = Math.sin(v1 * 5.2 + time * 0.52 + curtain * 0.61) * 0.075;
                double radius0 = 46.0 + Math.sin(v0 * 4.0 + time * 0.33 + curtain) * 2.0;
                double radius1 = 46.0 + Math.sin(v1 * 4.0 + time * 0.33 + curtain) * 2.0;

                double a0 = angle + drift0;
                double a1 = angle + drift1;
                double y0 = player.y + 12.0 + v0 * 23.0;
                double y1 = player.y + 12.0 + v1 * 23.0;

                double x0 = player.x + Math.cos(a0) * radius0;
                double z0 = player.z + Math.sin(a0) * radius0;
                double x1 = player.x + Math.cos(a1) * radius1;
                double z1 = player.z + Math.sin(a1) * radius1;

                float envelope = (float) Math.sin(Math.PI * (0.08 + v0 * 0.84));
                float alpha = 0.19f * Math.max(0.0f, envelope);
                drawLine(lines, entry, x0, y0, z0, x1, y1, z1, alpha);
            }
        }
    }

    private static void drawLine(
            VertexConsumer lines,
            MatrixStack.Entry entry,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float alpha
    ) {
        lines.vertex(entry, (float) x1, (float) y1, (float) z1)
                .color(GREEN_R, GREEN_G, GREEN_B, alpha)
                .normal(entry, 0.0f, 1.0f, 0.0f);
        lines.vertex(entry, (float) x2, (float) y2, (float) z2)
                .color(GREEN_R, GREEN_G, GREEN_B, alpha)
                .normal(entry, 0.0f, 1.0f, 0.0f);
    }
}
