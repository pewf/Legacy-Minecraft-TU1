package wily.legacy.neoforge.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin {
    @Inject(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V", at = @At("HEAD"), remap = false,cancellable = true)
    private static void renderTooltipBackground(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int backgroundTop, int backgroundBottom, int borderTop, int borderBottom, CallbackInfo ci){
        ScreenUtil.renderPointerPanel(guiGraphics,i - (int)(3 * ScreenUtil.getTextScale()),j - (int)(5 *  ScreenUtil.getTextScale()),(int)((k + 6) *  ScreenUtil.getTextScale()),(int)((l + 9) *  ScreenUtil.getTextScale()));
        ci.cancel();
    }
}
