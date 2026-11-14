package com.lx862.jcm.mixin.mtrpatch;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.renderer.culling.Frustum;

@Mixin(LevelRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("cullingFrustum")
    Frustum jsblock$getFrustum();
}
