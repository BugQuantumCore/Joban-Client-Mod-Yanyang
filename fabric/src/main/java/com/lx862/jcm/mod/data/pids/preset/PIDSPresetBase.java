package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.render.StoredMatrixTransformations;

import javax.annotation.Nullable;
import java.util.List;

public abstract class PIDSPresetBase implements RenderHelper {
    public static final float BASE_SCALE = 1/96F;
    private final String id;
    private final String name;
    private final List<String> blacklist;
    protected final ResourceLocation thumbnail;
    public final boolean builtin;
    private final boolean outOfBound;

    public PIDSPresetBase(String id, @Nullable String name, ResourceLocation thumbnail, List<String> blacklistedPIDS, boolean outOfBound, boolean builtin) {
        this.id = id;
        if(name == null) {
            this.name = id;
        } else {
            this.name = name;
        }
        this.outOfBound = outOfBound;
        this.blacklist = blacklistedPIDS;
        this.builtin = builtin;
        this.thumbnail = thumbnail;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getThumbnail() {
        return thumbnail;
    }

    public boolean outOfBoundAllowed() {
        return this.outOfBound;
    }

    public boolean typeAllowed(String pidsType) {
        return !blacklist.contains(pidsType);
    }

    public abstract int getTextColor();
    public abstract boolean isRowHidden(int row);

    public abstract void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Level world, BlockPos pos, Direction facing, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height, int light);
}