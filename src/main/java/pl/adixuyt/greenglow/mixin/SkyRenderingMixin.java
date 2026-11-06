package pl.adixuyt.greenglow.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns the Overworld sky black. The green aurora ribbons are drawn by
 * GreenGlowClient; other dimensions keep their normal sky behavior.
 */
@Mixin(WorldRenderer.class)
abstract class WorldRendererSkyMixin {
    @Inject(method = "isSkyDark(F)Z", at = @At("HEAD"), cancellable = true)
    private void greenglow$forceBlackOverworldSky(float tickDelta, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.world.getRegistryKey() == World.OVERWORLD) {
            cir.setReturnValue(true);
        }
    }
}

@Mixin(SkyRendering.class)
abstract class SkyRenderingMixin {
    @ModifyVariable(method = "renderSky(FFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float greenglow$blackSkyRed(float red) {
        return isOverworld() ? 0.0f : red;
    }

    @ModifyVariable(method = "renderSky(FFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float greenglow$blackSkyGreen(float green) {
        return isOverworld() ? 0.0f : green;
    }

    @ModifyVariable(method = "renderSky(FFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float greenglow$blackSkyBlue(float blue) {
        return isOverworld() ? 0.0f : blue;
    }

    @ModifyVariable(method = "renderGlowingSky", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int greenglow$greenGlowingSky(int color) {
        return isOverworld() ? 0x8800FF45 : color;
    }

    private static boolean isOverworld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null && client.world.getRegistryKey() == World.OVERWORLD;
    }
}
