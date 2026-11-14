package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.util.JCMLogger;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import org.mtr.registry.RegistryClient;

public class KeyBinds {
    public static final KeyMapping SCRIPT_DEBUG_SOURCE_PREV = new KeyMapping("gui.jsblock.keybinds.script_debug_source_previous", GLFW.GLFW_KEY_LEFT_BRACKET, "category.jsblock.keybinding");
    public static final KeyMapping SCRIPT_DEBUG_SOURCE_NEXT = new KeyMapping("gui.jsblock.keybinds.script_debug_source_next", GLFW.GLFW_KEY_RIGHT_BRACKET, "category.jsblock.keybinding");
    public static final KeyMapping SCRIPT_DEBUG_RELOAD = new KeyMapping("gui.jsblock.keybinds.script_debug_reload", GLFW.GLFW_KEY_R, "category.jsblock.keybinding");

    public static void registerClient() {
        JCMLogger.debug("Registering keybinds...");
        RegistryClient.registerKeyBinding(SCRIPT_DEBUG_SOURCE_PREV);
        RegistryClient.registerKeyBinding(SCRIPT_DEBUG_SOURCE_NEXT);
        RegistryClient.registerKeyBinding(SCRIPT_DEBUG_RELOAD);
    }
}
