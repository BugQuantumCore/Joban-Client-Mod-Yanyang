package com.lx862.jcm.mapping;

import com.lx862.jcm.mixin.mtrpatch.WorldRendererAccessor;
import com.lx862.jcm.mixin.mtrscripting.NativeImageAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.AbstractClientPlayer;
import java.util.List;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImplClient {
    public static long getNativeImagePointer(NativeImage nativeImage) {
        return ((NativeImageAccessor)(Object)nativeImage).getPixels();
    }

    public static List<Player> getWorldPlayers(ClientLevel world) {
        List<Player> players = new ObjectArrayList<>();
        for(AbstractClientPlayer entity : world.players()) {
            players.add(new Player(entity));
        }
        return players;
    }

    public static Vec3 getEntityVelocity(Entity entity) {
        return new Vec3(entity.getDeltaMovement());
    }

    public static void addPackOverride(ResourceLocation id, String modid) {
    }

    public static int getCameraMode() {
        CameraType perspective = Minecraft.getInstance().options.getCameraType();
        return perspective == CameraType.FIRST_PERSON ? 1 : perspective == CameraType.THIRD_PERSON_BACK ? 2 : 3;
    }

    public static boolean isFrustumVisible(Box box) {
        return ((WorldRendererAccessor)Minecraft.getInstance().levelRenderer).jsblock$getFrustum().isVisible(box);
    }
}
