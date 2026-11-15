package com.lx862.jcm.mapping;

import com.lx862.jcm.mixin.mtrscripting.NativeImageAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.culling.Frustum;

/**
 * Fabric implementation, Mojang mappings (1.21.x)
 */
public class LoaderImplClient {
    public static List<Player> getWorldPlayers(ClientLevel world) {
        List<Player> players = new ArrayList<>();
        for (AbstractClientPlayer entity : world.players()) {
            players.add(entity);
        }
        return players;
    }

    public static Vec3 getEntityVelocity(Entity entity) {
        return entity.getDeltaMovement();
    }

    public static long getNativeImagePointer(NativeImage nativeImage) {
        return ((NativeImageAccessor) (Object) nativeImage).getPointer();
    }

    public static void addPackOverride(ResourceLocation id, String modid) {
        ResourceManagerHelper.registerBuiltinResourcePack(id, FabricLoader.getInstance().getModContainer(modid).orElseThrow(), ResourcePackActivationType.ALWAYS_ENABLED);
    }

    public static int getCameraMode() {
        final net.minecraft.client.CameraType cameraType = Minecraft.getInstance().options.getCameraType();
        return cameraType == net.minecraft.client.CameraType.FIRST_PERSON ? 1 : cameraType == net.minecraft.client.CameraType.THIRD_PERSON_BACK ? 2 : 3;
    }

    public static boolean isFrustumVisible(AABB box) {
        final net.minecraft.client.renderer.culling.Frustum frustum = ((com.lx862.jcm.mixin.mtrpatch.WorldRendererAccessor) Minecraft.getInstance().levelRenderer).jsblock$getFrustum();
        return frustum == null || frustum.isVisible(box);
    }
}
