package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockPSDAPGDoorBase;

public class APGDoorDRLBlockEntity extends BlockPSDAPGDoorBase.BlockEntityBase {
    public APGDoorDRLBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.APG_DOOR_DRL.get(), pos, state);
    }
}