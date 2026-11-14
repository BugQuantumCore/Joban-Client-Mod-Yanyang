package com.lx862.amberui.gui.widget;
import com.lx862.jcm.mod.render.GraphicsHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * A wrapper around common widget implementations. On 1.21.1 Mojang mappings
 * every legacy widget extension (Button/EditBox/Checkbox/Slider) is an
 * {@link AbstractWidget}, so delegation collapses onto that type plus a
 * nested-wrapper case.
 */
public class MappedWidget {
    private final Object widget;
    public MappedWidget(Object widget) {
        if(!(widget instanceof MappedWidget || widget instanceof AbstractWidget)) {
            throw new IllegalArgumentException("Widget is not an instance of a widget!");
        }

        this.widget = widget;
    }

    private AbstractWidget asWidget() {
        if(widget instanceof MappedWidget) {
            return ((MappedWidget) widget).asWidget();
        }
        return (AbstractWidget) widget;
    }

    public int getX() {
        return asWidget().getX();
    }

    public int getY() {
        return asWidget().getY();
    }

    public int getWidth() {
        if(widget instanceof HorizontalWidgetSet) {
            return ((HorizontalWidgetSet) widget).getWidth();
        }
        return asWidget().getWidth();
    }

    public int getHeight() {
        return asWidget().getHeight();
    }

    public boolean getActive() {
        return asWidget().active;
    }

    public void setWidth(int width) {
        asWidget().setWidth(width);
    }

    public void setX(int newX) {
        if(widget instanceof WidgetsWrapper) {
            ((WidgetsWrapper) widget).setAllX(newX);
        } else {
            asWidget().setX(newX);
        }
    }

    public void setY(int newY) {
        if(widget instanceof WidgetsWrapper) {
            ((WidgetsWrapper) widget).setAllY(newY);
        } else {
            asWidget().setY(newY);
        }
    }

    /** Legacy GraphicsHolder-based render entry used by the list widget code. */
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        final GuiGraphics guiGraphics = graphicsHolder.getGuiGraphics();
        if(guiGraphics != null) {
            renderWidget(guiGraphics, mouseX, mouseY, tickDelta);
        }
    }

    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        if(widget instanceof MappedWidget) {
            ((MappedWidget) widget).renderWidget(guiGraphics, mouseX, mouseY, tickDelta);
        } else {
            ((AbstractWidget) widget).render(guiGraphics, mouseX, mouseY, tickDelta);
        }
    }

    public void setVisible(boolean value) {
        asWidget().visible = value;
    }

    public void setAlpha(float alpha) {
        asWidget().setAlpha(alpha);
    }
}
