package pl.adixuyt.greenglow;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    private static final List<LandingWave> LANDING_WAVES = new ArrayList<>();
    private static final long LANDING_WAVE_LIFETIME_NANOS = 1_000_000_000L;
    private static final float MIN_LANDING_FALL_DISTANCE = 0.85f;
    private static World trackedWorld;
    private static boolean wasOnGround = true;
    private static float maximumFallDistance;

    private record LandingWave(double x, double z, double groundY, long startedAtNanos) {}

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(GreenGlowClient::trackPlayerLanding);
        WorldRenderEvents.AFTER_ENTITIES.register(GreenGlowClient::renderWorldGeometry);
    }

    private static void trackPlayerLanding(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            trackedWorld = null;
            wasOnGround = true;
            maximumFallDistance = 0.0f;
            LANDING_WAVES.clear();
            return;
        }

        if (trackedWorld != client.world) {
            trackedWorld = client.world;
            wasOnGround = client.player.isOnGround();
            maximumFallDistance = 0.0f;
            LANDING_WAVES.clear();
            return;
        }

        boolean onGround = client.player.isOnGround();
        if (!onGround) {
            maximumFallDistance = Math.max(maximumFallDistance, client.player.fallDistance);
            wasOnGround = false;
        } else {
            if (!wasOnGround && maximumFallDistance >= MIN_LANDING_FALL_DISTANCE) {
                LANDING_WAVES.add(new LandingWave(
                        client.player.getX(), client.player.getZ(),
                        client.player.getY() + 0.035, System.nanoTime()
                ));
            }
            wasOnGround = true;
            maximumFallDistance = 0.0f;
        }

        long now = System.nanoTime();
        Iterator<LandingWave> iterator = LANDING_WAVES.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().startedAtNanos() > LANDING_WAVE_LIFETIME_NANOS) {
                iterator.remove();
            }
        }
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

        renderLandingWaves(lines, matrices.peek(), System.nanoTime());
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

    /**
     * A splash-like impact: three bright green ripples expand from the
     * landing point, sit just above the ground, and fade away quickly.
     */
    private static void renderLandingWaves(VertexConsumer lines, MatrixStack.Entry entry, long now) {
        final int segments = 80;
        final long delay = 105_000_000L;
        final long duration = 690_000_000L;

        for (LandingWave landing : LANDING_WAVES) {
            long age = now - landing.startedAtNanos();
            for (int ring = 0; ring < 3; ring++) {
                long ringAge = age - ring * delay;
                if (ringAge < 0 || ringAge >= duration) continue;

                double progress = (double) ringAge / duration;
                double eased = 1.0 - (1.0 - progress) * (1.0 - progress);
                double radius = 0.18 + eased * 3.15;
                double lift = 0.025 + Math.sin(progress * Math.PI) * 0.085 + ring * 0.008;
                float alpha = (float) ((1.0 - progress) * (0.88 + 0.12 * Math.sin(progress * Math.PI * 2.0)));

                drawGroundRing(lines, entry, landing, radius + 0.075, lift, alpha * 0.18f, segments);
                drawGroundRing(lines, entry, landing, radius + 0.030, lift, alpha * 0.48f, segments);
                drawGroundRing(lines, entry, landing, radius, lift, alpha * 0.96f, segments);
            }
        }
    }

    private static void drawGroundRing(
            VertexConsumer lines, MatrixStack.Entry entry, LandingWave landing,
            double radius, double lift, float alpha, int segments
    ) {
        for (int segment = 0; segment < segments; segment++) {
            double a0 = Math.PI * 2.0 * segment / segments;
            double a1 = Math.PI * 2.0 * (segment + 1) / segments;
            double x0 = landing.x() + Math.cos(a0) * radius;
            double z0 = landing.z() + Math.sin(a0) * radius;
            double x1 = landing.x() + Math.cos(a1) * radius;
            double z1 = landing.z() + Math.sin(a1) * radius;
            double y = landing.groundY() + lift;
            drawLine(lines, entry, x0, y, z0, x1, y, z1, alpha);
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
