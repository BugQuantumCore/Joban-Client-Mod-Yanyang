package com.lx862.amberui.mapping;

import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImplGUI {
    public static void openURLScreen(Screen parentScreen, String url) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(
                new Screen(new net.minecraft.client.gui.screens.ConfirmLinkScreen((confirmed) -> {
                    if(confirmed) {
                        Util.getOperatingSystem().open(url);
                    }
                    mc.setScreen(new Screen(parentScreen));
                }, url, true)
            )
        );
    }

    public static void setTooltip(Screen screen, FormattedCharSequence orderedText) {
        #if MC_VERSION >= "11903"
        screen.setTooltipForNextRenderPass(List.of(orderedText));
        #endif
    }

    public static FormattedCharSequence asOrderedText(MutableComponent mutableText) {
        return new FormattedCharSequence(mutableText.getVisualOrderText());
    }
}
