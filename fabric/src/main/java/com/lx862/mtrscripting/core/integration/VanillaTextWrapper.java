package com.lx862.mtrscripting.core.integration;

import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.mod.util.JSUtil;
import com.lx862.jcm.mod.compat.TextHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

public class VanillaTextWrapper {
    private final MutableComponent impl;
    private Style style;
    private boolean skipApplyingStyle;

    @ApiInternal
    public VanillaTextWrapper(MutableComponent impl) {
        this.impl = impl;
        this.style = Style.EMPTY;
    }

    public static VanillaTextWrapper literal(String str) {
        return new VanillaTextWrapper(TextHelper.literal(str));
    }

    public static VanillaTextWrapper translatable(String str, Object... placeholders) {
        return new VanillaTextWrapper(TextHelper.translatable(str, placeholders));
    }

    public VanillaTextWrapper append(VanillaTextWrapper other) {
        TextHelper.append(this.impl(), other.impl());
        skipApplyingStyle = true;
        return this;
    }

    public VanillaTextWrapper withBold() {
        this.style = this.style.withBold(true);
        return this;
    }

    public VanillaTextWrapper withItalic() {
        this.style = this.style.withItalic(true);
        return this;
    }

    public VanillaTextWrapper withFont(String id) {
        return withFont(JSUtil.parseId(id));
    }

    public VanillaTextWrapper withFont(ResourceLocation id) {
        this.style = this.style.withFont(id);
        return this;
    }

    public VanillaTextWrapper withColor(int rgb) {
        this.style = this.style.withColor(TextColor.fromRgb(rgb));
        return this;
    }

    public VanillaTextWrapper withColor(String colorName) {
        ChatFormatting chatFormatting = ChatFormatting.getByName(colorName);
        if(chatFormatting == null) throw new IllegalArgumentException("Color " + colorName + " is not a valid text color!");
        this.style = this.style.withColor(chatFormatting);
        return this;
    }

    public VanillaTextWrapper clickToCopyText(String content) {
        this.style = LoaderImpl.withClipboardContentText(style, content);
        return this;
    }

    public VanillaTextWrapper clickToOpenURL(String urlContent) {
        this.style = LoaderImpl.withURLContentText(style, urlContent);
        return this;
    }

    public VanillaTextWrapper withHoverText(String content) {
        this.style = LoaderImpl.withHoverContentText(style, TextHelper.literal(content));
        return this;
    }

    /**
     * MutableText is not the intended argument type, scripts should not be exposed to internal details.
     */
    @Deprecated
    public VanillaTextWrapper withHoverText(MutableComponent content) {
        this.style = LoaderImpl.withHoverContentText(style, content);
        return this;
    }

    public VanillaTextWrapper withHoverText(VanillaTextWrapper content) {
        this.style = LoaderImpl.withHoverContentText(style, content.impl());
        return this;
    }

    public String getString() {
        return this.impl.getString();
    }

    public MutableComponent impl() {
        return skipApplyingStyle ? this.impl : TextHelper.setStyle(this.impl, this.style);
    }
}
