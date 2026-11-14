package com.lx862.jcm.mod.data.pids.preset;

import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class PIDSContext {
    public final Level world;
    public final String[] customMessages;
    public final ObjectArrayList<ArrivalResponse> arrivals;
    public final @Nullable BlockPos pos;
    public final double deltaTime;

    public PIDSContext(Level world, BlockPos pos, String[] customMessages, ObjectArrayList<ArrivalResponse> arrivals, double deltaTime) {
        this.world = world;
        this.pos = pos;
        this.arrivals = arrivals;
        this.deltaTime = deltaTime;
        this.customMessages = customMessages;
    }
}
