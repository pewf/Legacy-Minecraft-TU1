package wily.legacy.forge.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DefaultTooltipPositioner.class)
public class DefaultTooltipPositionerMixin {
    @ModifyVariable(method = "positionTooltip(IIIIII)Lorg/joml/Vector2ic;", at = @At("STORE"), index = 7)
    private Vector2i repositionTooltip(Vector2i original) {
        return original.sub(6, 2);
    }
}
