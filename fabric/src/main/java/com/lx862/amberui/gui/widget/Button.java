package com.lx862.amberui.gui.widget;

import net.minecraft.network.chat.Component;

/**
 * Compatibility button keeping the 4.0-era mapping constructor
 * {@code (x, y, width, height, text, onPress)} that the JCM GUI code uses.
 */
public class Button extends net.minecraft.client.gui.components.Button {

	public Button(int x, int y, int width, int height, Component text, OnPress onPress) {
		super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
	}

	public void setActiveMapped(boolean value) {
		active = value;
	}

	public void setVisibleMapped(boolean value) {
		visible = value;
	}
}
