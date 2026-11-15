package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.jcm.mod.Constants;
import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * NeoForge mod-bus event handlers (client).
 *
 * <p>Flushes the builtin resource pack overrides recorded by
 * {@link LoaderImplClient#addPackOverride} into the pack repository while it
 * is being assembled. The shared code records the packs during client mod
 * construction (before any mod-bus event fires), so by the time
 * {@link AddPackFindersEvent} arrives the list is complete.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class JCMNeoForgeEvents {

    private JCMNeoForgeEvents() {
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            LoaderImplClient.flushPendingPackOverrides(event);
        }
    }
}
