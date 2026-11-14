package com.lx862.jcm.mixin;

import com.lx862.jcm.mod.util.JCMUtil;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import javax.annotation.Nullable;

@Mixin(net.minecraft.core.DefaultedMappedRegistry.class)
public class SimpleDefaultedRegistryMixin {
    @ModifyVariable(at = @At("HEAD"), method = "get(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;", ordinal = 0, argsOnly = true)
    ResourceLocation dataFixerRegistry(@Nullable ResourceLocation id) {
        if (id == null) return null;
        return JCMUtil.getMigrationId(id);
    }
}
