package com.lx862.jcm.mod.compat;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import com.lx862.jcm.mod.compat.TextHelper;

/**
 * Compatibility shim for the removed MTR 4.0-era
 * {@code com.lx862.jcm.mod.compat.TextHelper}. Static call surface preserved.
 */
public final class TextHelper {

	public static MutableComponent translatable(String key, Object... args) {
		return net.minecraft.network.chat.Component.translatable(key, args);
	}

	public static MutableComponent literal(String text) {
		return net.minecraft.network.chat.Component.literal(text);
	}

	public static MutableComponent setStyle(MutableComponent text, Style style) {
		return text.setStyle(style);
	}

	public static FormattedCharSequence mutableTextToOrderedText(MutableComponent text) {
		return text.getVisualOrderText();
	}

	public static MutableComponent append(MutableComponent text, MutableComponent... others) {
		for (final MutableComponent other : others) {
			text.append(other);
		}
		return text;
	}
}
