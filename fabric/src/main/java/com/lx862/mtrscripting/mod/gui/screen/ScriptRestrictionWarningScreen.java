package com.lx862.mtrscripting.mod.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.amberui.gui.screen.TitledScreen;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import com.lx862.amberui.gui.widget.Button;
import com.lx862.jcm.mod.render.GraphicsHolder;

public class ScriptRestrictionWarningScreen extends TitledScreen implements RenderHelper, GuiHelper {
    private final Runnable acknowledgeWarningCallback;
    public ScriptRestrictionWarningScreen(Runnable acknowledgeWarningCallback) {
        super(false);
        this.acknowledgeWarningCallback = acknowledgeWarningCallback;
    }

    @Override
    public MutableComponent getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "script_restriction.title").withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public MutableComponent getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "script_restriction.subtitle");
    }

    @Override
    protected void init() {
        super.init();

        Button yesButton = new Button(0, height - 30, 150, 20, TextUtil.translatable("gui.yes"), (b) -> {
                acknowledgeWarningCallback.run();
                onClose();
            }
        );
        yesButton.setX((width / 2) - (TEXT_PADDING/2) - yesButton.getWidth());
        addRenderableWidget(yesButton);

        Button noButton = new Button(0, height - 30, 150, 20, TextUtil.translatable("gui.no"), (b) -> onClose());
        noButton.setX((width / 2) + (TEXT_PADDING/2));
        addRenderableWidget(noButton);
    }

    @Override
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.drawBackground(graphicsHolder, mouseX, mouseY, tickDelta);

        int lineHeight = 9;
        int lines = 5;

        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, height / 2.0, 0);
        graphicsHolder.translate(0, -(lines * lineHeight) / 2.0, 0);
        RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "script_restriction.about"), 0, 0, ARGB_WHITE);
        RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "script_restriction.consequence").withStyle(ChatFormatting.UNDERLINE), 0, 10, ARGB_WHITE);
        graphicsHolder.translate(0, lineHeight, 0);
        RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "script_restriction.note"), 0, 20, ARGB_WHITE);
        RenderHelper.drawCenteredText(graphicsHolder, TextUtil.translatable(TextCategory.GUI, "script_restriction.caution"), 0, 30, ARGB_WHITE);
        graphicsHolder.pop();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
