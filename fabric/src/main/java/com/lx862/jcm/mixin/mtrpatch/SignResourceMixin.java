package com.lx862.jcm.mixin.mtrpatch;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.lx862.jcm.mod.extra.SignExtraAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import org.mtr.font.FontRenderHelper;
import org.mtr.font.FontRenderOptions;
import org.mtr.libraries.it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair;
import org.mtr.resource.SignResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.awt.Color;
import java.lang.reflect.Field;

/**
 * Applies the per-sign custom {@code textColor} (NTE resource pack extension,
 * parsed by SignResourceSchemaMixin) to the deferred custom-text render.
 *
 * <p>MTR 4.1 renders sign custom text from {@code SignResource.lambda$render$0}
 * through {@link FontRenderHelper#render(PoseStack, String, FontRenderOptions)}.
 * {@link FontRenderOptions} is an immutable Lombok builder, so the color is
 * swapped reflectively on the options instance just for this draw.</p>
 */
@Mixin(value = SignResource.class, remap = false)
public class SignResourceMixin {

    @Unique
    private static final Field JSBLOCK_COLOR_FIELD = jsblock$colorField();

    private static Field jsblock$colorField() {
        try {
            final Field field = FontRenderOptions.class.getDeclaredField("color");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @WrapOperation(method = "lambda$render$0", at = @At(value = "INVOKE", target = "Lorg/mtr/font/FontRenderHelper;render(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;Lorg/mtr/font/FontRenderOptions;)Lorg/mtr/libraries/it/unimi/dsi/fastutil/doubles/FloatFloatImmutablePair;"))
    private static FloatFloatImmutablePair jsblock$applyTextColor(PoseStack poseStack, String text, FontRenderOptions fontRenderOptions, Operation<FloatFloatImmutablePair> original, @Local(argsOnly = true) SignResource signResource) {
        final int textColor = ((SignExtraAccessor) (Object) signResource).jsblock$getSignTextColor();
        if (textColor != -1 && JSBLOCK_COLOR_FIELD != null) {
            final Color previous;
            try {
                previous = (Color) JSBLOCK_COLOR_FIELD.get(fontRenderOptions);
                JSBLOCK_COLOR_FIELD.set(fontRenderOptions, new Color(textColor, true));
            } catch (ReflectiveOperationException ignored) {
                return original.call(poseStack, text, fontRenderOptions);
            }
            try {
                return original.call(poseStack, text, fontRenderOptions);
            } finally {
                try {
                    JSBLOCK_COLOR_FIELD.set(fontRenderOptions, previous);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return original.call(poseStack, text, fontRenderOptions);
    }
}
