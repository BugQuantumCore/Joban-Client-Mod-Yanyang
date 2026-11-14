package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;
import org.mtr.data.ArrivalsCacheClient;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ArrivalETAComponent extends TextComponent {
    private final boolean useAbsoluteTime;
    private final boolean showDeparture;
    private final int arrivalIndex;
    public ArrivalETAComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height, additionalParam);
        this.arrivalIndex = additionalParam.getInt("arrivalIndex", 0);
        this.showDeparture = additionalParam.get("showDeparture", false);
        this.useAbsoluteTime = additionalParam.get("useAbsTime", false);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        ObjectArrayList<ArrivalResponse> arrivals = context.arrivals;
        if(arrivalIndex >= arrivals.size()) return;

        ArrivalResponse arrival = arrivals.get(arrivalIndex);
        long arrDepTime = showDeparture ? arrival.getDeparture() : arrival.getArrival();

        long remTime = arrDepTime - ArrivalsCacheClient.INSTANCE.getMillisOffset() - System.currentTimeMillis();
        int remSec = (int)(remTime / 1000L);
        if(remSec <= 0) return;

        final MutableComponent etaText;

        if(useAbsoluteTime) {
            etaText = TextUtil.literal(new SimpleDateFormat("HH:mm").format(new Date(arrival.getArrival())));
        } else {
            if(remSec >= 60) {
                etaText = TextUtil.translatable(cycleStringTranslation("gui.mtr.arrival_min_cjk", "gui.mtr.arrival_min"), remSec / 60);
            } else {
                etaText = TextUtil.translatable(cycleStringTranslation("gui.mtr.arrival_sec_cjk", "gui.mtr.arrival_sec"), remSec % 60);
            }
        }

        drawText(graphicsHolder, guiDrawing, facing, new TextInfo(etaText));
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new ArrivalETAComponent(x, y, width, height, new KVPair(jsonObject));
    }
}
