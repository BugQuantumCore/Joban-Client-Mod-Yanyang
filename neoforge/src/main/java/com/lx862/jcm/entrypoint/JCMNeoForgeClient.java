package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge entrypoint (client). The Fabric twin is
 * {@code com.lx862.jcm.entrypoint.MainClient}.
 *
 * <p>Timing note: client registrations made from {@link JCMClient#initialize()}
 * go through MTR 4.1's static {@code RegistryClient} facade, which on this
 * loader collects them for {@code RegisterKeyMappingsEvent} /
 * {@code EntityRenderersEvent} / {@code RegisterColorHandlersEvent} - all of
 * which fire after mod construction, so this constructor is a safe place to
 * start. The builtin resource pack list flushed by
 * {@link JCMNeoForgeEvents} is likewise populated here before
 * {@code AddPackFindersEvent} fires.
 */
@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public final class JCMNeoForgeClient {

    public JCMNeoForgeClient(ModContainer container) {
        JCMClient.initialize();
        /* NeoForge half of native vehicle-script frustum culling (the
           Fabric twin registers WorldRenderEvents; this one installs a
           live frustum reader - see NeoForgeVehicleCulling). */
        NeoForgeVehicleCulling.init();
        /* The mods-list config screen, replacing Fabric's ModMenu entry. */
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (modContainer, parentScreen) -> JCMClient.getClientConfigScreen(parentScreen));
    }
}
