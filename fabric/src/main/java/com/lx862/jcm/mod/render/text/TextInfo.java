package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

public class TextInfo {
    private final String content;
    private ResourceLocation fontId;
    private TextAlignment textAlignment = TextAlignment.LEFT;
    private int textColor;
    private boolean forScrollingText = false;
    /** Visible width of the marquee scroll region (in the same coordinate space
     *  the caller draws in); 0 = unspecified, the renderer then falls back to
     *  the text's own width. */
    private double scrollWidth = 0;

    public TextInfo(String content) {
        this.content = content;
    }

    public TextInfo(MutableComponent text) {
        this.content = text.getString();
    }

    public String getContent() {
        return content;
    }

    public int getTextColor() {
        return textColor;
    }

    public TextAlignment getTextAlignment() {
        return textAlignment;
    }

    public boolean isForScrollingText() {
        return this.forScrollingText;
    }

    public TextInfo withScrollingText() {
        this.forScrollingText = true;
        return this;
    }

    public TextInfo withScrollingText(double scrollWidth) {
        this.forScrollingText = true;
        this.scrollWidth = scrollWidth;
        return this;
    }

    public double getScrollWidth() {
        return this.scrollWidth;
    }

    public TextInfo withColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextInfo withTextAlignment(TextAlignment textAlignment) {
        this.textAlignment = textAlignment;
        return this;
    }

    public TextInfo withFont(String font) {
        return withFont(ResourceLocation.parse(font));
    }

    public TextInfo withFont(ResourceLocation fontId) {
        this.fontId = fontId;
        return this;
    }

    public MutableComponent toMutableText() {
        MutableComponent text = TextUtil.literal(content);
        if(fontId != null) {
            text = TextUtil.withFont(text, fontId);
        }
        return text;
    }

    public TextInfo copy(String newContent) {
        TextInfo ti = new TextInfo(newContent);
        ti.withColor(textColor);
        ti.withFont(fontId);
        ti.scrollWidth = this.scrollWidth;
        return ti;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TextInfo)) return false;
        TextInfo other = ((TextInfo) o);
        /* Objects.equals: fontId is nullable (null = vanilla MC font), a null
           fontId on either side NPE'd here before. */
        return other.content.equals(content) && other.textColor == textColor && this.forScrollingText == other.forScrollingText && java.util.Objects.equals(this.fontId, other.fontId);
    }
}
