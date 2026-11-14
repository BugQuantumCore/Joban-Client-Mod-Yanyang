package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class URLEmergencyStopButtonWallBlockEntity extends JCMBlockEntityBase {
    public URLEmergencyStopButtonWallBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.URL_EMG_STOP_BUTTON_WALL.get(), blockPos, blockState);
    }
}
