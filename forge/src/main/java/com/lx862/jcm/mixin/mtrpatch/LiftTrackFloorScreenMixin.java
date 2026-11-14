package com.lx862.jcm.mixin.mtrpatch;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import org.mtr.screen.LiftTrackFloorScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LiftTrackFloorScreen.class, remap = false)
public class LiftTrackFloorScreenMixin {
    @Shadow
    @Final
    private Checkbox checkboxShouldDing;

    @Inject(method = "init2", at = @At("TAIL"))
    private void jsblock$addDingCheckbox(CallbackInfo ci) {
        ((LiftTrackFloorScreen)(Object)this).addChild(new ClickableWidget(checkboxShouldDing));
    }
}
