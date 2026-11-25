package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.network.chat.MutableComponent;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.MTRClient;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper around vanilla text rendering
 */
@Deprecated
public class VanillaTextRenderer implements RenderHelper {
    /**
     * Per-glyph width cache for the marquee, keyed by (font, content). The
     * renderer is static and its TextInfo instances are recreated every frame,
     * so without this cache a scrolling string would re-measure every glyph
     * twice per frame. Bounded: cleared wholesale past the cap (PIDS screens
     * have a handful of distinct marquee strings, the cap is never hit in
     * practice).
     */
    private static final ConcurrentHashMap<String, int[]> MARQUEE_WIDTH_CACHE = new ConcurrentHashMap<>();
    private static final int MARQUEE_CACHE_CAP = 256;

    /** Extra blank tail on the marquee cycle, in pixels, so the text fully
     *  exits before re-entering from the right. */
    private static final double MARQUEE_TAIL_PADDING = 16;

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
        drawInternal(graphicsHolder, text, x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
        /* Branch BEFORE the full-string measurement: the scrolling path
           measures per glyph (see marqueeCharWidths) and never used the
           plain width, so scrolling text no longer pays for a full-string
           measure each frame. The plain path formats the component once and
           reuses it for both measuring and drawing. */
        if(text.isForScrollingText()) {
            /* Alignment does not apply to scrolling text — it moves through
               the region instead of sitting at a fixed offset. */
            drawScrollingText(graphicsHolder, text, x, y);
        } else {
            final MutableComponent finalText = text.toMutableText();
            final int textWidth = GraphicsHolder.getTextWidth(finalText);
            double finalX = text.getTextAlignment().getX(x, textWidth);
            graphicsHolder.drawText(finalText, (int)finalX, (int)y, text.getTextColor(), false, MAX_RENDER_LIGHT);
        }
    }

    /**
     * Marquee rendering for {@code textOverflowMode: "MARQUEE"}: scrolls the
     * string through the component's visible region, one glyph at a time, in
     * the same style the scripting-side TextWrapper marquee has always used.
     * (This method used to be an empty "HEAVY WIP" stub — marquee-mode JSON
     * PIDS text rendered nothing at all.)
     */
    public static void drawScrollingText(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
        final String content = text.getContent();
        if(content.isEmpty()) return;

        final int[] charWidths = marqueeCharWidths(text);
        int textWidth = 0;
        for(final int w : charWidths) textWidth += w;

        final double regionWidth = text.getScrollWidth() > 0 ? text.getScrollWidth() : textWidth;
        if(regionWidth <= 0) return;

        final double fullWidth = textWidth + regionWidth + MARQUEE_TAIL_PADDING;
        final double cycleDuration = Math.max(1, content.length() * 10);
        final double progress = (MTRClient.getGameTick() % cycleDuration) / cycleDuration;

        /* Start fully right of the region, slide left, exit past the left
           edge, then wrap. */
        double wSoFar = regionWidth - (fullWidth * progress);
        for(int i = 0; i < content.length(); i++) {
            final int charWidth = charWidths[i];
            if(charWidth > 0 && wSoFar + charWidth > 0 && wSoFar < regionWidth) {
                graphicsHolder.push();
                graphicsHolder.translate(x + wSoFar, y, 0);
                graphicsHolder.drawText(text.copy(String.valueOf(content.charAt(i))).toMutableText(), 0, 0, text.getTextColor(), false, MAX_RENDER_LIGHT);
                graphicsHolder.pop();
            }
            wSoFar += charWidth;
        }
    }

    private static int[] marqueeCharWidths(TextInfo text) {
        final String content = text.getContent();
        final String fontKey = text.toMutableText().getStyle().getFont().toString();
        final String key = fontKey + '\0' + content;
        final int[] cached = MARQUEE_WIDTH_CACHE.get(key);
        if(cached != null && cached.length == content.length()) return cached;

        final int[] widths = new int[content.length()];
        for(int i = 0; i < content.length(); i++) {
            widths[i] = GraphicsHolder.getTextWidth(text.copy(String.valueOf(content.charAt(i))).toMutableText());
        }
        if(MARQUEE_WIDTH_CACHE.size() >= MARQUEE_CACHE_CAP) MARQUEE_WIDTH_CACHE.clear();
        MARQUEE_WIDTH_CACHE.put(key, widths);
        return widths;
    }
}
