package pl.adixuyt.greenglow.mixin;

import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Darkens the vanilla sky and recolors Minecraft's glowing-sky pass green.
 */
@Mixin(WorldRenderer.class)
abstract class WorldRendererSkyMixin {
    @Inject(method = "isSkyDark(F)Z", at = @At("HEAD"), cancellable = true)
    private void greenglow$forceBlackSky(float tickDelta, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}

@Mixin(SkyRendering.class)
abstract class SkyRenderingMixin {
    @ModifyVariable(method = "renderSky(FFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float greenglow$blackSkyRed(float red) {
        return 0.0f;
    }

    @ModifyVariable(method = "renderSky(FFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float greenglow$blackSkyGreen(float green) {
        return 0.0f;
    }

    @ModifyVariable(method = "renderSky(FFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float greenglow$blackSkyBlue(float blue) {
        return 0.0f;
    }

    @ModifyVariable(method = "renderGlowingSky", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int greenglow$greenGlowingSky(int color) {
        return 0x5500FF35;
    }
}
