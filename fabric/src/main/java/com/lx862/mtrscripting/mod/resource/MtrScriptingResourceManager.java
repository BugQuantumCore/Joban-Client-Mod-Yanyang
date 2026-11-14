package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.core.rhinojs.RhinoParsedScript;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.ante.ConfigResponderStorage;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.core.util.ConsoleJS;
import org.mtr.resource.ResourceManagerHelper;
import com.lx862.jcm.mod.compat.TextHelper;
import net.minecraft.resources.ResourceLocation;
import org.mtr.MTR;
import org.mtr.client.CustomResourceLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class MtrScriptingResourceManager {
    public static final VehicleResourceProvider vehicle = new VehicleResourceProvider();
    public static final EyecandyResourceProvider eyecandy = new EyecandyResourceProvider();
    public static final LiftResourceProvider lift = new LiftResourceProvider();
    public static final PSDResourceProvider psd = new PSDResourceProvider();

    public static void reload() {
        MTRScriptingModClient.LOGGER.info("[MTR Scripting via JCM] Loading MTR scripts...");

        /* Yanyang: route native-script hints (platform-skipped C++
           declarations) into the JCM debug mode interface — yellow
           chat line via scriptErrorNotifier, only when scripting
           debug mode is on. latest.log always gets them through
           JCMLogger inside NativeScriptManager. */
        NativeScriptManager.setDebugNotifier(message -> {
            if (!JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) return;
            MTRContentScripting.getScriptManager().scriptErrorNotifier.queue(() -> {
                final LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage((TextHelper.setStyle(
                            TextHelper.literal("[C++] " + message),
                            Style.EMPTY.withColor(ChatFormatting.YELLOW))), false);
                }
            });
        });

        vehicle.reset();
        eyecandy.reset();
        lift.reset();
        psd.reset();
        ScriptResourceUtil.resetCache();
        RhinoParsedScript.clearScriptCache();
        ConfigResponderStorage.reset();

        MTRContentScripting.getScriptManager().scriptErrorNotifier.reset();

        if(JCMClientConfig.INSTANCE.scripting.skipScriptParsing.value()) {
            MTRScriptingModClient.LOGGER.info("[MTR Scripting via JCM] Scripting has been disabled, not parsing any script.");
        } else {
            ConsoleJS consoleJS = new ConsoleJS();
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
                consoleJS.time("MTR Script Load Time");
            }
            vehicle.parseCustom();
            eyecandy.parseCustom();
            lift.parseCustom();
            parseCustomResources(false);
            parseCustomResources(true);
            if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
                consoleJS.timeEnd("MTR Script Load Time");
            }
            /* Yanyang: one-line census of the C++ modules after discovery, so a
               "nothing renders" report can be triaged from the log. */
            com.lx862.jcm.nativeapi.NativeScriptManager.logCensus();
        }

        if(Minecraft.getInstance().player != null) {
            MTRContentScripting.getScriptManager().scriptErrorNotifier.flush();
        }
    }

    private static void parseCustomResources(boolean pendingMigration) {
        String fileName = pendingMigration ? CustomResourceLoader.CUSTOM_RESOURCES_PENDING_MIGRATION_ID + ".json" : CustomResourceLoader.CUSTOM_RESOURCES_ID + ".json";

        ResourceManagerHelper.readAllResources(ResourceLocation.fromNamespaceAndPath(MTR.MOD_ID, fileName), (inputStream) -> {
            try(InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                final JsonObject rootObject = new JsonParser().parse(reader).getAsJsonObject();
                final boolean isLegacyResource = rootObject.has("custom_trains") || rootObject.has("custom_signs");

                vehicle.parseMtrResources(rootObject, isLegacyResource);
                eyecandy.parseMtrResources(rootObject, isLegacyResource);
                lift.parseMtrResources(rootObject, isLegacyResource);
                psd.parseMtrResources(rootObject, isLegacyResource);
            } catch (Exception e) {
                ScriptResourceProvider.logError("parsing scripts in mtr_custom_resources.json", e);
            }
        });

        validateScriptEntries();
    }

    private static void validateScriptEntries() {
        vehicle.validate();
        eyecandy.validate();
        lift.validate();
        psd.validate();
    }
}
