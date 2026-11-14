package com.lx862.jcm.mod.resource;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.rhinojs.RhinoParsedScript;
import org.apache.commons.io.IOUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import org.mtr.resource.ResourceManagerHelper;
import org.mtr.client.CustomResourceLoader;

public class JCMResourceManager {
    private static final ResourceLocation CUSTOM_RESOURCE_PATH = Constants.id("joban_custom_resources.json");

    public static void reload() {
        JCMClient.getMcMetaManager().reset();
        PIDSManager.reset();
        ComplexModelStorage.reset();
        RhinoParsedScript.clearScriptCache();
        loadCustomResources();
    }

    private static void loadCustomResources() {
        JCMScripting.getScriptManager().scriptErrorNotifier.reset();

        ResourceManagerHelper.readAllResources(CUSTOM_RESOURCE_PATH, (inputStream -> {
            try {
                String str = IOUtils.toString(inputStream, Charsets.UTF_8);
                JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
                PIDSManager.loadJson(jsonObject);
            } catch (Exception e) {
                JCMLogger.error("Failed to parse custom resource file!", e);
            }
        }));
        ComplexModelStorage.beginReload();

        if(Minecraft.getInstance().player != null) {
            JCMScripting.getScriptManager().scriptErrorNotifier.flush();
        }
    }
}
