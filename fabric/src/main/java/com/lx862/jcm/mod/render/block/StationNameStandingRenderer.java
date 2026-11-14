package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.StationNameStandingBlockEntity;
import org.mtr.block.BlockStationNameTallStanding;
import org.mtr.render.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class StationNameStandingRenderer extends RenderStationNameTall<StationNameStandingBlockEntity> {
    public StationNameStandingRenderer(BlockEntityRendererProvider.Context dispatcher) {
        super(BlockStationNameTallStanding.WIDTH, BlockStationNameTallStanding.HEIGHT, org.mtr.block.BlockStationNameTallBase.OFFSET_Y);
    }
}
