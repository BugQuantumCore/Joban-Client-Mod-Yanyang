package com.lx862.amberui.mapping;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * Fabric implementation, Mojang mappings (1.21.x)
 */
public class LoaderImplGUI {

    public static void openURLScreen(Screen parentScreen, String url) {
        final Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(url);
            }
            mc.setScreen(parentScreen);
        }, url, true));
    }

    public static void setTooltip(Screen screen, FormattedCharSequence orderedText) {
        // 1.21.x renders tooltips through GuiGraphics; the legacy per-screen
        // setter is a no-op shim (callers pass tooltips via render now).
    }

    public static FormattedCharSequence asOrderedText(MutableComponent mutableText) {
        return mutableText.getVisualOrderText();
    }
}
