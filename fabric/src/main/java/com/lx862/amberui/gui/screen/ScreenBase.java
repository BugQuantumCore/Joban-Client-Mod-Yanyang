package com.lx862.amberui.gui.screen;

import com.lx862.amberui.gui.ClipStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Generic GUI Screen, use {@link ScreenBase#withPreviousScreen} to reference the previously opened screen that would be opened again after closing.
 */
public abstract class ScreenBase extends Screen {
    private Screen previousScreen = null;
    public ScreenBase() {
        super(Component.empty());
    }
    public ScreenBase withPreviousScreen(Screen screen) {
        this.previousScreen = screen;
        return this;
    }

    @Override
    public void onClose() {
        /* Belt-and-braces: a widget that leaked a clip mid-render would
           otherwise leave the scissor state wrong for the next screen. */
        ClipStack.ensureStateCorrect();
        Minecraft.getInstance().setScreen(previousScreen);
    }

    /**
     * Final safety net when this screen is swapped out for another one —
     * the clip stack is static, so leaked entries must not survive into
     * whatever screen opens next.
     */
    @Override
    public void removed() {
        super.removed();
        ClipStack.ensureStateCorrect();
    }
}