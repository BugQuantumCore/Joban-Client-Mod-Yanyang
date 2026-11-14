package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.mtr.block.IBlock;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;

public class RVPIDSSIL1BlockEntity extends PIDSBlockEntity {

    public RVPIDSSIL1BlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.RV_PIDS_SIL_1.get(), blockPos, blockState);
    }

    @Override
    public String getPIDSType() {
        return "rv_pids_sil_1";
    }

    @Override
    public String getDefaultPresetId() {
        return "rv_pids";
    }

    @Override
    public int getRowAmount() {
        return 4;
    }

    @Override
    public boolean isKeyBlock() {
        Direction dir = IBlock.getStatePropertySafe(getBlockState(), FACING);
        return dir.equals(Direction.NORTH) || dir.equals(Direction.EAST);
    }
}
