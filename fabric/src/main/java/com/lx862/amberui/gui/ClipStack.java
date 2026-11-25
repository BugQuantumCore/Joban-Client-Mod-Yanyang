package com.lx862.amberui.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Keep track of applied OpenGL Scissors, this should be the primary way to apply clipping to the GUI.<br>
 * Use {@link ClipStack#push(int, int, int, int)} to apply a new clip, and {@link ClipStack#pop()} after finish clipping.
 */
public class ClipStack {
    private static final List<Rectangle> stacks = new ArrayList<>();

    /**
     * Push and apply a new clipping area.<br>
     * GUI scaling are already taken account for, there's no need to multiply the scale by yourself for the parameters.
     */
    public static void push(int x, int y, int width, int height) {
        stacks.add(new Rectangle(x, y, width, height));
        enableClip(x, y, width, height);
    }

    /**
     * Push and apply a new clipping area.<br>
     * The clipping area is clamped *within* the previous clipped stack on both
     * axes (origin and end), so the effective clip is the intersection with the
     * enclosing clip. An empty intersection is clamped to a 1-pixel area: a
     * zero/negative scissor rectangle would be rejected by GL
     * (INVALID_VALUE) and drop the scissor state entirely.<br>
     * At least one clip area should be applied via {@link ClipStack#push(int, int, int, int)} before using.
     */
    public static void add(int x, int y, int width, int height) {
        /* Guard: add() clamps against the enclosing clip; with an empty
         * stack there is nothing to clamp against (and stacks.get(-1)
         * would throw) — degrade to a plain push. */
        if (stacks.isEmpty()) {
            push(x, y, width, height);
            return;
        }
        Rectangle oldStack = stacks.get(stacks.size()-1);
        int clampedX = Math.max(oldStack.x, x);
        int clampedY = Math.max(oldStack.y, y);
        int clampedWidth = Math.min(x + width, oldStack.x + oldStack.width) - clampedX;
        int clampedHeight = Math.min(y + height, oldStack.y + oldStack.height) - clampedY;
        /* Empty intersection (or negative extents): clamp to 1px so the
         * scissor rectangle stays valid for GL — everything is clipped
         * away either way. */
        if (clampedWidth <= 0) clampedWidth = 1;
        if (clampedHeight <= 0) clampedHeight = 1;
        Rectangle rect = new Rectangle(clampedX, clampedY, clampedWidth, clampedHeight);
        stacks.add(rect);
        enableClip(rect.x, rect.y, rect.width, rect.height);
    }

    public static void pop() {
        if(stacks.isEmpty()) {
            throw new IllegalStateException("No more clip stack to be popped!");
        }

        if(stacks.size() > 1) {
            stacks.remove(stacks.size() - 1);
            Rectangle remainingLast = stacks.get(stacks.size() - 1);
            enableClip(remainingLast.x, remainingLast.y, remainingLast.width, remainingLast.height);
        } else {
            stacks.remove(0);
            disableClip();
        }
    }

    /**
     * Enable clipping to only render content within boundary
     * @param x Start X of your rectangle
     * @param y Start Y of your rectangle
     * @param width Width of your rectangle
     * @param height Height of your rectangle
     */
    private static void enableClip(int x, int y, int width, int height) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int windowHeight = Minecraft.getInstance().getWindow().getHeight();
        RenderSystem.enableScissor((int)(x * scale), (int)(windowHeight - (y + height) * scale), (int)(width * scale), (int)(height * scale));
    }

    /**
     * Ensure the clip is applied appropriately, yes this is a hack.
     */
    public static void ensureStateCorrect() {
        if(stacks.isEmpty()) {
            disableClip();
        } else {
            Rectangle latestStacks = stacks.get(stacks.size()-1);
            enableClip(latestStacks.x, latestStacks.y, latestStacks.width, latestStacks.height);
        }
    }

    private static void disableClip() {
        RenderSystem.disableScissor();
    }
}
