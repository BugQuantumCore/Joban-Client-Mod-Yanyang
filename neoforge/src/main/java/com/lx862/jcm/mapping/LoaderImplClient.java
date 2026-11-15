package com.lx862.jcm.mapping;

import com.lx862.jcm.mixin.mtrscripting.NativeImageAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge implementation, Mojang mappings (1.21.x)
 */
public class LoaderImplClient {

    /**
     * Builtin pack overrides recorded by {@link #addPackOverride} while the
     * client mod is still constructing; flushed into the pack repository by
     * JCMNeoForgeEvents when {@code AddPackFindersEvent} fires.
     */
    private static final List<ResourceLocation> PENDING_PACK_OVERRIDES = new ArrayList<>();

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

    /**
     * Records a builtin resource pack for registration. The Fabric twin
     * registers into the {@code ResourceManagerHelper} immediately; on
     * NeoForge the pack repository does not exist yet during mod
     * construction, so the pack is recorded here and entered into the
     * repository from {@code AddPackFindersEvent}.
     *
     * <p>The pack is expected to live inside the mod jar under
     * {@code resourcepacks/<id path>}, exactly where the Fabric build ships
     * it.
     */
    public static void addPackOverride(ResourceLocation id, String modid) {
        PENDING_PACK_OVERRIDES.add(id);
    }

    /**
     * Enters all recorded packs into the pack repository. Runs from
     * {@code JCMNeoForgeEvents.addPackFinders} on the mod event bus, after
     * every mod has been constructed, so the pending list is final.
     */
    public static void flushPendingPackOverrides(AddPackFindersEvent event) {
        for (ResourceLocation id : PENDING_PACK_OVERRIDES) {
            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "resourcepacks/" + id.getPath()),
                    PackType.CLIENT_RESOURCES,
                    Component.literal("JCM Resource Override"),
                    PackSource.BUILT_IN,
                    true,
                    Pack.Position.TOP
            );
        }
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
