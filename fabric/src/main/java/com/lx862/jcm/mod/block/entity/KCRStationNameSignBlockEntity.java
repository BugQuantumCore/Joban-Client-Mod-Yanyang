package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockEntityExtension;

public class KCRStationNameSignBlockEntity extends BlockEntityExtension {
    public KCRStationNameSignBlockEntity(BlockPos blockPos, BlockState blockState, boolean stationColored) {
        super(stationColored ? BlockEntities.KCR_STATION_NAME_SIGN_STATION_COLOR.get() : BlockEntities.KCR_STATION_NAME_SIGN.get(), blockPos, blockState);
    }
}
