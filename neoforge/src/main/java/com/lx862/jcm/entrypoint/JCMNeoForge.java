package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCM;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entrypoint (common). The Fabric twin is
 * {@code com.lx862.jcm.entrypoint.Main}.
 *
 * <p>All registration flows through MTR 4.1's static {@code RegistryServer}
 * facade, which on this loader forwards to MTR's own DeferredRegisters -
 * populating them during mod construction is exactly the window NeoForge
 * expects, so no local DeferredRegister is needed.
 */
@Mod(Constants.MOD_ID)
public final class JCMNeoForge {

    public JCMNeoForge() {
        JCM.initialize();
    }
}
