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

/**
 * Client-only green world geometry. Deliberately does not render player/entity
 * outlines, names, labels, or HUD elements.
 */
public final class GreenGlowClient implements ClientModInitializer {
    private static final int GRID_SPACING = 8;
    private static final int GRID_RADIUS = 2;
    private static final double BOX_SIZE = 2.6;
    private static final double[] HEIGHT_OFFSETS = {-4.0, 0.0, 4.0};

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(GreenGlowClient::renderWorldGeometry);
    }

    private static void renderWorldGeometry(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || context.world() == null) {
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
                // Leave the immediate player area clear.
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

                    // Subtle outer edge and a crisp neon-green outline.
                    VertexRendering.drawBox(matrices, lines, box.expand(0.035),
                            0.08f, 1.0f, 0.24f, 0.22f);
                    VertexRendering.drawBox(matrices, lines, box,
                            0.12f, 1.0f, 0.30f, 0.95f);
                }
            }
        }

        matrices.pop();
    }
}
