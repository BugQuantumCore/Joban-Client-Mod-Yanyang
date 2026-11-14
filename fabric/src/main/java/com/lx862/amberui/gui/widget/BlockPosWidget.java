package com.lx862.amberui.gui.widget;

import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.AbstractWidget;
import com.lx862.jcm.mod.render.GraphicsHolder;

import java.util.function.Consumer;

public class BlockPosWidget extends AbstractWidget implements WidgetsWrapper {
    private final CoordTextField posXTextField;
    private final CoordTextField posYTextField;
    private final CoordTextField posZTextField;
    public BlockPosWidget(int x, int y, int width, int height) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty());
        this.posXTextField = new CoordTextField(0, 0, width / 3, height, -29999999, 29999999, 0);
        this.posYTextField = new CoordTextField(width / 3, 0, width / 3, height, -29999999, 29999999, 0);
        this.posZTextField = new CoordTextField((width / 3) * 2, 0, width / 3, height, -29999999, 29999999, 0);

        this.posXTextField.setResponder(this::setPosition);
        this.posYTextField.setResponder(this::setPosition);
        this.posZTextField.setResponder(this::setPosition);
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    	final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
    
        this.posXTextField.render(guiGraphics, mouseX, mouseY, delta);
        this.posYTextField.render(guiGraphics, mouseX, mouseY, delta);
        this.posZTextField.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public void setAllX(int newX) {
        super.setX(newX);
        positionWidgets();
    }

    @Override
    public void setAllY(int newY) {
        super.setY(newY);
        positionWidgets();
    }

    public void setActive(boolean active) {
        this.posXTextField.setEditable(active);
        this.posYTextField.setEditable(active);
        this.posZTextField.setEditable(active);
        this.posXTextField.active = active;
        this.posYTextField.active = active;
        this.posZTextField.active = active;
        super.active = active;
    }

    public void setVisible(boolean visible) {
        this.posXTextField.visible = visible;
        this.posYTextField.visible = visible;
        this.posZTextField.visible = visible;
        super.visible = visible;
    }

    public void positionWidgets() {
        int perWidth = getWidth() / 3;
        this.posXTextField.setX(getX());
        this.posYTextField.setX(getX() + perWidth);
        this.posZTextField.setX(getX() + perWidth + perWidth);
        this.posXTextField.setY(getY());
        this.posYTextField.setY(getY());
        this.posZTextField.setY(getY());
        this.posXTextField.setWidth(perWidth);
        this.posYTextField.setWidth(perWidth);
        this.posZTextField.setWidth(perWidth);
    }

    public void addWidget(Consumer<AbstractWidget> callback) {
        callback.accept(posXTextField);
        callback.accept(posYTextField);
        callback.accept(posZTextField);
    }

    public void setBlockPos(BlockPos newPos) {
        this.posXTextField.setValue(String.valueOf(newPos.getX()));
        this.posYTextField.setValue(String.valueOf(newPos.getY()));
        this.posZTextField.setValue(String.valueOf(newPos.getZ()));
    }

    public BlockPos getBlockPos() {
        return new BlockPos(posXTextField.getNumber(), posYTextField.getNumber(), posZTextField.getNumber());
    }

    private void setPosition(String str) {
        str = str.trim();
        String[] strSplit = str.split("\\s+");

        if(!str.isEmpty() && strSplit.length > 1) {
            for(int i = 0; i < strSplit.length; i++) {
                CoordTextField field = null;
                if(i == 0) field = posXTextField;
                if(i == 1) field = posYTextField;
                if(i == 2) field = posZTextField;

                try {
                    int parsed = Integer.parseInt(strSplit[i]);
                    if(field != null) field.setValue(String.valueOf(parsed));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

	@Override
	public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
	}
}
