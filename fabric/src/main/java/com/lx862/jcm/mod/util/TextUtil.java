package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.compat.TextHelper;
import org.mtr.config.Config;
import org.mtr.data.IGui;

import java.util.ArrayList;
import java.util.List;

public class TextUtil {
    public static MutableComponent literal(String content) {
        return TextHelper.literal(content);
    }

    public static MutableComponent translatable(TextCategory textCategory, String id, Object... variables) {
        return TextHelper.translatable(textCategory.prefix + "." + Constants.MOD_ID + "." + id, variables);
    }

    public static MutableComponent translatable(String id, Object... variables) {
        return TextHelper.translatable(id, variables);
    }

    public static Style withFontStyle(ResourceLocation fontId) {
        return Config.getClient().getUseMTRFont() ? Style.EMPTY.withFont(fontId) : Style.EMPTY;
    }

    /** Set a custom font style to MutableText, this respects the client config. */
    public static MutableComponent withFont(MutableComponent text, ResourceLocation fontId) {
        return TextHelper.setStyle(text, withFontStyle(fontId));
    }

    public static boolean haveNonCjk(String str) {
        for(String dest : str.split("\\|")) {
            if(!IGui.isCjk(dest)) {
                return true;
            }
        }
        return false;
    }

    public static List<FormattedCharSequence> wrapText(MutableComponent text, int width) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String content = text.getString();
        for(String manualSplitLine : content.split("\n")) {
            GraphicsHolder.wrapLines(TextUtil.literal(manualSplitLine), width).forEach(line -> {
                lines.add(line);
            });
        }

        return lines;
    }
}