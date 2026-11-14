package com.lx862.jcm.mixin.mtrscripting;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {
    @Accessor("originalImage")
    NativeImage jsblock$getImage();

    @Accessor("name")
    ResourceLocation jsblock$getId();
}
