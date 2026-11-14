package com.lx862.jcm.mod.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.screen.AnimatedScreen;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;
import org.mtr.data.IGui;

import java.text.SimpleDateFormat;
import java.util.List;

public class RVEnquiryScreen extends AnimatedScreen {
    private static final ResourceLocation cardScreenTexture = Constants.id("textures/enquiry/card.png");
    private static final ResourceLocation balanceTexture = Constants.id("textures/enquiry/transactions.png");
    private static final ResourceLocation octopusCardTexture = Constants.id("textures/enquiry/octopus_card.png");
    private static final ResourceLocation font = ResourceLocation.fromNamespaceAndPath("mtr", "mtr");
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final List<TransactionEntry> entries;
    private final BlockPos pos;
    private final long remainingBalance;
    private boolean showBalance;

    public RVEnquiryScreen(BlockPos pos, List<TransactionEntry> entries, int remainingBalance) {
        super(false);
        this.pos = pos;
        this.entries = entries;
        this.remainingBalance = remainingBalance;
        this.showBalance = true; // Simplify mechanics
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
        super.renderBackground(guiGraphics, mouseX, mouseY, tickDelta);

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);

        super.render(guiGraphics, mouseX, mouseY, tickDelta);
        int baseWidth = 427;
        double scaledWidth = width;
        double scaledHeight = height;
        int startX = 20;
        int startY = 70;

        int rectX = (screenWidth - 150) / 2;
        int rectY = ((screenHeight - 70) / 2) + 65;
        int rectWidth = 150;
        int rectHeight = 70;

        boolean cursorWithinRectangle = mouseX >= rectX && mouseX <= rectX + rectWidth && mouseY >= rectY && mouseY <= rectY + rectHeight;
        if(cursorWithinRectangle) {
            showBalance = true;
        }

        if (!showBalance) {
            GuiHelper.drawTexture(guiDrawing, cardScreenTexture, (screenWidth - (int) scaledWidth) / 2.0, (screenHeight - (int) scaledHeight) / 2.0, (int) scaledWidth, (int) scaledHeight);
            GuiHelper.drawTexture(guiDrawing, octopusCardTexture, mouseX, mouseY, 140, 86);
        } else {
            GuiHelper.drawTexture(guiDrawing, balanceTexture, (screenWidth - (int) scaledWidth) / 2.0, (screenHeight - (int) scaledHeight) / 2.0, (int) scaledWidth, (int) scaledHeight);

            int renderY = startY;

            graphicsHolder.push();
            graphicsHolder.scale((float)(double)width / baseWidth, (float)(double)width / baseWidth, (float)(double)width / baseWidth);

            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.balance"), font), startX, 20, 0, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("Octopus"), font), startX + 305, 75, 0, false, GraphicsHolder.getDefaultLight());
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.processor"), font), startX + 305, 85, 0, false, GraphicsHolder.getDefaultLight());

            String processorId = String.format("%06d", pos.asLong() % 1000000);
            graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal(processorId), font), startX + 305, 95, 0, false, GraphicsHolder.getDefaultLight());

            for (int i = 0; i < 10; i++) {
                MutableComponent renderText = getEntryText(i);
                if(renderText == null) continue;

                graphicsHolder.drawText(TextUtil.withFont(renderText, font), startX, renderY, 0, false, GraphicsHolder.getDefaultLight());
                renderY += 10;
            }

            if (!entries.isEmpty()) {
                if (remainingBalance >= 0) {
                    graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("$" + remainingBalance), font), startX + 270, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
                } else {
                    graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal("-$" + Math.abs(remainingBalance)), font), startX + 270, 20, 0x000000, false, GraphicsHolder.getDefaultLight());
                }
            }

            long lastDate = 0;

            for (TransactionEntry transactionEntry : entries) {
                if (transactionEntry.amount() > 0) {
                    lastDate = transactionEntry.time();
                }
            }

            if (lastDate != 0) {
                String formattedDate = TIME_FORMATTER.format(lastDate);
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.a"), font), startX + 305, 115, 0, false, GraphicsHolder.getDefaultLight());
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.b"), font), startX + 305, 125, 0, false, GraphicsHolder.getDefaultLight());
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.translatable(TextCategory.GUI, "rvenquiry_screen.last.c"), font), startX + 305, 135, 0, false, GraphicsHolder.getDefaultLight());
                graphicsHolder.drawText(TextUtil.withFont(TextUtil.literal(formattedDate), font), startX + 305, 145, 0x000000, false, GraphicsHolder.getDefaultLight());
            }

            graphicsHolder.pop();
        }
    }

    private MutableComponent getEntryText(int i) {
        if(i >= entries.size()) return null;
        TransactionEntry transactionEntry = entries.get(i);
        String renderTextString;
        if (transactionEntry.amount() < 0) {
            renderTextString = String.format("%s     %s     -$%.2f", TIME_FORMATTER.format(transactionEntry.time()), IGui.formatStationName(transactionEntry.source()), Math.abs((double) transactionEntry.amount()));
        } else if (transactionEntry.amount() > 0) {
            renderTextString = String.format("%s     %s     +$%.2f", TIME_FORMATTER.format(transactionEntry.time()), IGui.formatStationName(transactionEntry.source()), (double) transactionEntry.amount());
        } else {
            renderTextString = String.format("%s     %s     $%.2f", TIME_FORMATTER.format(transactionEntry.time()), IGui.formatStationName(transactionEntry.source()), (double) transactionEntry.amount());
        }
        return TextUtil.literal(renderTextString);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
