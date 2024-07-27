package wily.legacy.fabric.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DefaultTooltipPositioner.class)
public class MenuTooltipPositionerMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void sus(CallbackInfo ci) {
    }

    @Inject(method = "positionTooltip(IIIIII)Lorg/joml/Vector2ic;", at = @At("HEAD"))
    private void repositionTooltip(int i, int j, int k, int l, int m, int n, CallbackInfoReturnable<Vector2ic> cir) {
        return new Vector2i(1, 1).add(0, 0);
    }
}
