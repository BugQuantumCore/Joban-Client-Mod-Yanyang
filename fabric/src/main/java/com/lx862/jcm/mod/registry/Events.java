package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import com.lx862.mtrscripting.mod.gui.hud.MTRScriptDebugOverlay;
import com.lx862.jcm.mod.resource.JCMResourceManager;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import com.lx862.mtrscripting.mod.impl.mtr.server.PSDServerBindings;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.ServerDoorDirections;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleDataCache;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.registry.EventRegistryServer;
import org.mtr.registry.EventRegistryClient;
import com.lx862.jcm.mod.compat.TextHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Events {
    public static void register() {
        // Start Tick Event for counting tick
        EventRegistryServer.registerStartServerTick(JCMServerStats::incrementGameTick);

        /* Yanyang: server-side door-opening-direction memory (the migrated
           self-learning). First start with this version scans every
           route-served platform of the save once — unloaded chunks included —
           stores the sides per route under the mtr data root, marks the
           scan so later boots never rescan, re-scans incrementally when
           platform blocks change (batch-tool safe), and answers client
           queries from the stored data. */
        EventRegistryServer.registerServerStarted(DoorDirectionManager::onServerStarted);
        EventRegistryServer.registerServerStopping(DoorDirectionManager::onServerStopping);
        EventRegistryServer.registerEndWorldTick(DoorDirectionManager::onWorldTick);

        /* Yanyang/PSD: server-side PSD script binding storage — loads per
           dimension on first use, batches changes per tick and saves at shutdown. */
        EventRegistryServer.registerStartServerTick(PSDServerBindings::onServerTick);
        EventRegistryServer.registerServerStopping(PSDServerBindings::onServerStopping);
    }

    public static void registerClient() {
        EventRegistryClient.registerStartClientTick(() -> {
            JCMClient.getMcMetaManager().tick();
            JCMScripting.tick();
            MTRContentScripting.tick();
            VehicleDataCache.tick();
            JCMPatchForMTR.tick();
            com.lx862.jcm.mod.scripting.psd.PSDClientBindings.tick();

            if(Minecraft.getInstance().level == null) {
                VehicleDataCache.clearData();
                com.lx862.jcm.mod.scripting.psd.PSDWallScanner.reset();
                com.lx862.jcm.mod.scripting.psd.PSDRouteStopCache.reset();
                com.lx862.jcm.mod.scripting.psd.PSDWrapper.PSDStationWrapper.resetCache();
                com.lx862.jcm.mod.scripting.psd.PSDClientBindings.reset();
                /* Yanyang: drop the cached server door-direction answers
                   while out of a world; a fresh join re-requests what the
                   render path actually needs. (The old client-side on-disk
                   learning is gone — the memory now lives on the server.) */
                ServerDoorDirections.reset();
            }
        });

        EventRegistryClient.registerClientJoin(() -> {
            /* On behalf of MTR Scripting */
            MTRContentScripting.getScriptManager().scriptErrorNotifier.flush();
            JCMScripting.getScriptManager().scriptErrorNotifier.flush();
            JCMPatchForMTR.clearClientCaches();
        });

        EventRegistryClient.registerEndClientTick(() -> {
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value() && KeyBinds.SCRIPT_DEBUG_SOURCE_PREV.isDown()) {
                MTRScriptDebugOverlay.previousSource();
            }
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value() && KeyBinds.SCRIPT_DEBUG_SOURCE_NEXT.isDown()) {
                MTRScriptDebugOverlay.nextSource();
            }
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value() && KeyBinds.SCRIPT_DEBUG_RELOAD.isDown()) {
                boolean isShiftPressed = Minecraft.getInstance().options.keyShift.isDown();
                boolean isCtrlPressed = Minecraft.getInstance().options.keySprint.isDown();
                if(!isShiftPressed && !isCtrlPressed) return;
                boolean shouldReloadJCM = isShiftPressed;

                if(Minecraft.getInstance().player != null) {
                    MutableComponent prefix = TextUtil.literal("[JCM] ").withStyle(ChatFormatting.AQUA);

                    if(shouldReloadJCM) {
                        Minecraft.getInstance().player.displayClientMessage((TextHelper.append(prefix, TextUtil.literal("Reloading all JCM PIDS Scripts...").withStyle(ChatFormatting.YELLOW))), false);
                    } else {
                        Minecraft.getInstance().player.displayClientMessage((TextHelper.append(prefix, TextUtil.literal("Reloading all MTR Scripts...").withStyle(ChatFormatting.YELLOW))), false);
                    }
                }

                if(shouldReloadJCM) {
                    JCMScripting.reset();
                    JCMResourceManager.reload();
                } else {
                    /* Yanyang: reset the C++ runtime on this path too — the
                       same reasons onClientReloadResource() does it apply
                       here, only worse: without it this keybind re-parses
                       the JS scripts while every dlopen'd C++ module stays
                       running (an updated .so with the same script id is
                       silently ignored), and the native instance-id cache
                       keeps serving ids the bridge has long forgotten
                       (ERR_INSTANCE, permanently blank train LCDs) — the
                       user pressing this very key to "fix" a dead LCD
                       could never revive it. */
                    com.lx862.jcm.nativeapi.NativeScriptManager.reload();
                    MTRContentScripting.reset();
                    MtrScriptingResourceManager.reload();
                }
            }
        });

        EventRegistryClient.registerHudLayerRenderEvent(guiGraphics -> {
            MTRScriptDebugOverlay.render(new com.lx862.jcm.mod.render.GraphicsHolder(guiGraphics));
        });
    }

    public static void onClientReloadResource() {
        com.lx862.jcm.mod.scripting.psd.PSDScriptRenderer.reset();
        /* Yanyang: reset the C++ script runtime BEFORE the JS side re-parses.
           Without this, F3+T kept every dlopen'd module running (an updated
           .so with the same script id was silently ignored — the declaration
           guard in loadManyFromDeclaration is idempotent), SKIPPED/INPUTS
           entries for removed packs leaked forever, and NativeHost kept
           model handles that ModelManagerJS.reset() below was about to
           close — a use-after-free on the render thread for every native
           draw call after the reload. reload() disposes the modules,
           clears the per-instance states, draw registries and driver
           memos, and drops the NativeHost model/texture references first. */
        com.lx862.jcm.nativeapi.NativeScriptManager.reload();
        JCMScripting.reset();
        MTRContentScripting.reset();
        ModelManagerJS.reset();
        MtrScriptingResourceManager.reload();
        JCMResourceManager.reload();
    }
}
