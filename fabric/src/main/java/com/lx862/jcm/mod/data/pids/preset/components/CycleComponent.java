package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import net.minecraft.core.Direction;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;
import org.mtr.MTRClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CycleComponent extends PIDSComponent {
    private final List<PIDSComponent> components;
    private final double cycleTime;

    public CycleComponent(KVPair additionalParam, PIDSComponent... components) {
        super(0, 0, 0, 0);
        this.cycleTime = additionalParam.getInt("cycleTime", 20);
        this.components = new ArrayList<>();
        JsonArray array = additionalParam.get("components", new JsonArray());
        for(int i = 0; i < array.size(); i++) {
            this.components.add(PIDSComponent.parse(array.get(i).getAsJsonObject()));
        }
        this.components.addAll(Arrays.asList(components));
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        if(components.isEmpty()) return;
        List<PIDSComponent> filteredComponents = components.stream().filter(e -> e.canRender(context)).collect(Collectors.toList());
        if(filteredComponents.isEmpty()) return;

        if(cycleTime == -1) {
            filteredComponents.get(0).render(graphicsHolder, guiDrawing, facing, context); // Render first available component
        } else if(cycleTime <= 0) {
            /* cycleTime 0 divides by zero below (JSON-supplied value); treat it
               like the -1 branch — always show the first available component. */
            filteredComponents.get(0).render(graphicsHolder, guiDrawing, facing, context);
        } else {
            int currentComponentIndex = (int) ((int)(MTRClient.getGameTick() % (cycleTime * filteredComponents.size())) / cycleTime);

            PIDSComponent component = filteredComponents.get(currentComponentIndex);
            component.render(graphicsHolder, guiDrawing, facing, context);
        }
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new CycleComponent(new KVPair(jsonObject));
    }
}