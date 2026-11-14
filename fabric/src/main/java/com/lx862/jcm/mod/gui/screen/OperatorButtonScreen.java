package com.lx862.jcm.mod.gui.screen;

import com.lx862.jcm.mod.block.OperatorButtonBlock;
import com.lx862.jcm.mod.network.block.OperatorButtonUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.screen.BlockConfigScreen;
import com.lx862.amberui.gui.widget.WidgetSet;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.lx862.amberui.gui.widget.Button;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;
import com.lx862.jcm.mod.compat.TextHelper;
import net.minecraft.util.FastColor;
import org.mtr.data.IGui;
import org.mtr.item.ItemDriverKey;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import com.lx862.amberui.gui.widget.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class OperatorButtonScreen extends BlockConfigScreen {
    private static final int KEY_BUTTON_SIZE = 50;
    private final boolean[] keyRequirements;
    private KeyToggleButton hoveredButton = null;

    public OperatorButtonScreen(BlockPos blockPos, boolean[] keyRequirements) {
        super(blockPos);
        this.keyRequirements = keyRequirements;
    }

    @Override
    public void init() {
        super.init();
        int centerX = width / 2;
        int contentWidth = getContentWidth();
        int startX = (width - contentWidth) / 2;
        int startY = (int)(height / 2.5f);

        int keyRowWidth = (KEY_BUTTON_SIZE * OperatorButtonBlock.ACCEPTED_KEYS.length) + ((OperatorButtonBlock.ACCEPTED_KEYS.length-1) * 10);
        WidgetSet keyRow = new WidgetSet(KEY_BUTTON_SIZE, 10);

        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            final int keyIdx = i;
            KeyToggleButton keyToggleButton = new KeyToggleButton((ItemDriverKey) OperatorButtonBlock.ACCEPTED_KEYS[keyIdx].get(), keyRequirements[keyIdx], (itm) -> {
                hoveredButton = itm;
            }, 0, 0, KEY_BUTTON_SIZE, KEY_BUTTON_SIZE, (btn) -> {
                keyRequirements[keyIdx] = ((KeyToggleButton)btn).isAllowed();
            });
            keyRow.addWidget(keyToggleButton);
            addRenderableWidget(keyToggleButton);
        }
        keyRow.setXYSize(centerX - (keyRowWidth / 2), startY - (KEY_BUTTON_SIZE / 2), keyRowWidth, KEY_BUTTON_SIZE);
        addRenderableWidget(keyRow);

        addBottomRowEntry(startX, height - 40, contentWidth, 40);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        int startY = (int)(height / 2.5f);

        MutableComponent allowedKeyText = TextUtil.translatable(TextCategory.GUI, "operator_button.keys_title").withStyle(ChatFormatting.UNDERLINE);
        graphicsHolder.push();
        graphicsHolder.translate(width / 2d, startY - 45, 0);
        graphicsHolder.scale(1.5f, 1.5f, 1.5f);
        graphicsHolder.drawCenteredText(allowedKeyText, 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();

        if(hoveredButton != null) {
            ItemDriverKey key = hoveredButton.getKeyItem();
            MutableComponent itemText = TextUtil.translatable(key.getDescriptionId());
            MutableComponent prefixTick = TextUtil.literal("✔ ").withStyle(ChatFormatting.DARK_GREEN);
            MutableComponent prefixCross = TextUtil.literal("✗ ").withStyle(ChatFormatting.DARK_RED);

            graphicsHolder.push();
            graphicsHolder.translate(width / 2d, startY + 40, 0);
            graphicsHolder.translate(0, 0, 0);
            graphicsHolder.scale(1.5f, 1.5f, 1.5f);
            graphicsHolder.drawCenteredText(TextHelper.append((hoveredButton.isAllowed() ? prefixTick : prefixCross), itemText.withStyle(ChatFormatting.WHITE)), 0, 0, key.color);
            graphicsHolder.pop();

            graphicsHolder.push();
            graphicsHolder.translate(width / 2d, startY + 60, 0);

            MutableComponent canDrive = TextHelper.append(TextUtil.translatable(TextCategory.GUI, "operator_button.key.drivable").withStyle(ChatFormatting.YELLOW), getBooleanText(key.canDrive));
            graphicsHolder.drawCenteredText(canDrive, 0, 0, IGui.ARGB_WHITE);
            graphicsHolder.translate(0, 10, 0);

            MutableComponent canOpenDoor = TextHelper.append(TextUtil.translatable(TextCategory.GUI, "operator_button.key.door").withStyle(ChatFormatting.YELLOW), getBooleanText(key.canOpenDoors));
            graphicsHolder.drawCenteredText(canOpenDoor, 0, 0, IGui.ARGB_WHITE);
            graphicsHolder.translate(0, 10, 0);

            MutableComponent canBoardVehicles = TextHelper.append(TextUtil.translatable(TextCategory.GUI, "operator_button.key.board").withStyle(ChatFormatting.YELLOW), getBooleanText(key.canBoardAnyVehicle));
            graphicsHolder.drawCenteredText(canBoardVehicles, 0, 0, IGui.ARGB_WHITE);

            graphicsHolder.pop();
        }
    }

    @Override
    public MutableComponent getScreenTitle() {
        return com.lx862.jcm.mod.registry.Blocks.OPERATOR_BUTTON.get().getName();
    }

    @Override
    protected void onSave() {
        Networking.sendPacketToServer(new OperatorButtonUpdatePacket(blockPos, keyRequirements));
    }

    private MutableComponent getBooleanText(boolean bl) {
        MutableComponent text = TextUtil.translatable(bl ? "gui.yes" : "gui.no");
        return text.withStyle(bl ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    static class KeyToggleButton extends Button {
        private static final ResourceLocation KEY_TEXTURE = ResourceLocation.parse("mtr:textures/item/driver_key.png");
        private static final int BORDER_PADDING = 8;
        private static final int SELECTED_COLOR = 0xFF00FF00;
        private static final int DESELECTED_COLOR = 0xFFFF0000;
        private final ItemDriverKey keyItem;
        private final Consumer<KeyToggleButton> hoverCallback;
        private boolean allowed;
        private boolean lastHovered;

        public KeyToggleButton(ItemDriverKey keyItem, boolean allowed, Consumer<KeyToggleButton> hoverCallback, int x, int y, int width, int height, net.minecraft.client.gui.components.Button.OnPress onPress) {
            super(x, y, width, height, net.minecraft.network.chat.Component.empty(), onPress);
            this.keyItem = keyItem;
            this.hoverCallback = hoverCallback;
            this.allowed = allowed;
        }

        @Override
        public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);

            super.renderWidget(guiGraphics, mouseX, mouseY, delta);

            if (this.visible) {
                GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
                final int argb = GuiHelper.MAX_ALPHA | keyItem.color;
                final int a = (argb >>> 24) & 0xFF, r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;
                RenderSystem.setShaderColor(r / 255f, g / 255f, b / 255f, a / 255f);
                GuiHelper.drawTexture(guiDrawing, KEY_TEXTURE, getX() + BORDER_PADDING, getY() + BORDER_PADDING, getWidth() - (BORDER_PADDING * 2), getHeight() - (BORDER_PADDING * 2));
                RenderSystem.setShaderColor(1, 1, 1, 1);

                int borderColor = allowed ? SELECTED_COLOR : DESELECTED_COLOR;
                GuiHelper.drawRectangle(guiDrawing, getX(), getY(), width, 1, borderColor);
                GuiHelper.drawRectangle(guiDrawing, getX(), getY() + height-1, width, 1, borderColor);
                GuiHelper.drawRectangle(guiDrawing, getX(), getY(), 1, height, borderColor);
                GuiHelper.drawRectangle(guiDrawing, getX() + width-1, getY(), 1, height, borderColor);

                if(this.lastHovered != isHovered()) {
                    hoverCallback.accept(isHovered() ? this : null);
                    this.lastHovered = isHovered();
                }
            }
        }

        @Override
        public void onPress() {
            this.allowed = !this.allowed;
            super.onPress();
        }

        public ItemDriverKey getKeyItem() {
            return keyItem;
        }

        public boolean isAllowed() {
            return this.allowed;
        }
    }
}
