package com.lx862.amberui.gui.widget;

import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.TextCase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * Text field with the 4.0-era mapping constructor, ported onto the vanilla
 * {@link EditBox} (font + hint + max length + text-case filter).
 */
public class AlwaysRenderedTextField extends EditBox {

	public AlwaysRenderedTextField(int x, int y, int width, int height, int maxLength, TextCase textCase, String filter, String suggestion) {
		super(Minecraft.getInstance().font, x, y, width, height, Component.empty());
		setup(maxLength, textCase, filter, suggestion);
	}

	public AlwaysRenderedTextField(int x, int y, int width, int height, String message, int maxLength, TextCase textCase, String filter, String suggestion) {
		super(Minecraft.getInstance().font, x, y, width, height, Component.literal(message));
		setup(maxLength, textCase, filter, suggestion);
	}

	public AlwaysRenderedTextField(int x, int y, int width, int height, MutableComponent text, int maxLength, TextCase textCase, String filter, String suggestion) {
		super(Minecraft.getInstance().font, x, y, width, height, text);
		setup(maxLength, textCase, filter, suggestion);
	}

	private void setup(int maxLength, TextCase textCase, String filter, String suggestion) {
		if (maxLength > 0) {
			setMaxLength(maxLength);
		}
		if (textCase == TextCase.UPPER) {
			setFormatter((text, firstCharacterIndex) -> Component.literal(text.toUpperCase(Locale.ROOT)).getVisualOrderText());
		} else if (textCase == TextCase.LOWER) {
			setFormatter((text, firstCharacterIndex) -> Component.literal(text.toLowerCase(Locale.ROOT)).getVisualOrderText());
		}
		if (suggestion != null && !suggestion.isEmpty()) {
			setHint(Component.literal(suggestion));
		}
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
		final boolean wasVisible = visible;
		visible = true;
		super.renderWidget(guiGraphics, mouseX, mouseY, tickDelta);
		visible = wasVisible;
	}
}
