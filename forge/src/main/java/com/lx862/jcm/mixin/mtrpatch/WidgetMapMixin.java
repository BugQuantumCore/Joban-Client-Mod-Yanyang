package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SavedRailBase;
import org.mtr.core.data.Siding;
import com.lx862.jcm.mod.render.GuiDrawing;
import com.lx862.jcm.mod.compat.TextHelper;
import org.mtr.client.MinecraftClientData;
import org.mtr.screen.WidgetMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static com.lx862.jcm.mod.render.RenderHelper.ARGB_BLACK;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

@Mixin(WidgetMap.class)
public class WidgetMapMixin {
    @Inject(method = "lambda$render$2", at = @At("HEAD"), remap = false)
    private void renderPlatformTooltip(GuiDrawing guiDrawing, SavedRailBase savedRail, double x1, double z1, double x2, double z2, CallbackInfo ci) {
        if(!JCMClientConfig.INSTANCE.mtrPatch.showDashboardTooltip.value()) return;

        Platform platform = (Platform)savedRail;
        final MutableComponent platformText = TextUtil.literal("Platform " + platform.getName()).withStyle(TextFormatting.RED);
        final MutableComponent keyDwell = TextUtil.literal("Dwell: ").withStyle(TextFormatting.GOLD);
        final MutableComponent valueDwell = TextUtil.literal((platform.getDwellTime() / 1000d) + "s").withStyle(TextFormatting.WHITE);
        final MutableComponent keyRouteVia = TextUtil.literal("Route Via: ").withStyle(TextFormatting.GOLD);

        final List<MutableComponent> tooltipComponents = new ArrayList<>();
        tooltipComponents.add(platformText);
        tooltipComponents.add(TextHelper.append(keyDwell, valueDwell));
        tooltipComponents.add(keyRouteVia);

        for (Route route : MinecraftClientData.getDashboardInstance().routes) {
            if (route.getRoutePlatforms().stream().anyMatch(e -> e.getPlatform().getId() == platform.getId())) {
                MutableComponent routeText = TextUtil.literal(route.getName().replace("|", " "));
                TextHelper.setStyle(routeText, Style.getEmptyMapped().withColor(TextColor.fromRgb(ARGB_BLACK | route.getColor())));
                if(route.getHidden()) {
                    TextHelper.append(routeText, TextUtil.literal(" (Hidden)").withStyle(TextFormatting.GRAY));
                }
                tooltipComponents.add(routeText);
            }
        }

        if(tooltipComponents.isEmpty()) {
            tooltipComponents.add(TextUtil.literal("(None)"));
        }

        #if MC_VERSION >= "11903"
        Minecraft.getInstance().getCurrentScreenMapped().setTooltipForNextRenderPass(tooltipComponents.stream().map(e -> e.getVisualOrderText()).toList());
        #endif
    }

    @Inject(method = "lambda$render$4", at = @At("HEAD"), remap = false)
    private void renderSidingTooltip(GuiDrawing guiDrawing, SavedRailBase savedRail, double x1, double z1, double x2, double z2, CallbackInfo ci) {
        if(!JCMClientConfig.INSTANCE.mtrPatch.showDashboardTooltip.value()) return;

        final Siding siding = (Siding)savedRail;
        final MutableComponent platformText = TextUtil.literal("Siding " + siding.getName()).withStyle(TextFormatting.YELLOW);
        final MutableComponent keyRailLength = TextUtil.literal("Rail Length: ").withStyle(TextFormatting.GOLD);
        final MutableComponent valueRailLength = TextUtil.literal((int)Math.round(siding.getRailLength()) + "m").withStyle(TextFormatting.WHITE);
        final MutableComponent keyVehicleLength = TextUtil.literal("Vehicle Length: ").withStyle(TextFormatting.GOLD);

        int roundedVehicleLength = (int)Math.round(Siding.getTotalVehicleLength(siding.getVehicleCars()));
        String carPlural = siding.getVehicleCars().size() == 1 ? "car" : "cars";
        final MutableComponent valueVehicleLength = TextUtil.literal(String.format("%dm (%d-%s)", roundedVehicleLength, siding.getVehicleCars().size(), carPlural)).withStyle(TextFormatting.WHITE);

        final MutableComponent keyMaxVehicles = TextUtil.literal("Max Vehicle(s): ").withStyle(TextFormatting.GOLD);
        final MutableComponent valueMaxVehicles = TextUtil.literal(siding.getIsUnlimited() ? "Unlimited" : String.valueOf(siding.getMaxVehicles())).withStyle(siding.getIsUnlimited() ? TextFormatting.GREEN : TextFormatting.WHITE);
        final MutableComponent keyManual = TextUtil.literal("Manual: ").withStyle(TextFormatting.GOLD);
        final MutableComponent valueManual = TextUtil.literal("Yes").withStyle(TextFormatting.GREEN);

        final List<MutableComponent> tooltipComponents = new ArrayList<>();
        tooltipComponents.add(platformText);
        tooltipComponents.add(TextHelper.append(keyVehicleLength, valueVehicleLength));
        tooltipComponents.add(TextHelper.append(keyRailLength, valueRailLength));
        tooltipComponents.add(TextHelper.append(keyMaxVehicles, valueMaxVehicles));

        if(siding.getIsManual()) {
            tooltipComponents.add(TextHelper.append(keyManual, valueManual));
        }

        #if MC_VERSION >= "11903"
        Minecraft.getInstance().getCurrentScreenMapped().setTooltipForNextRenderPass(tooltipComponents.stream().map(e -> e.getVisualOrderText()).toList());
        #endif
    }
}
