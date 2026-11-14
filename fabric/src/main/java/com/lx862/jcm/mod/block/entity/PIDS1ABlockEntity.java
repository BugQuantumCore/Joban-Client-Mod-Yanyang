package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.mtr.block.IBlock;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;

public class PIDS1ABlockEntity extends PIDSBlockEntity {

    public PIDS1ABlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.PIDS_1A.get(), blockPos, blockState);
    }

    @Override
    public String getPIDSType() {
        return "pids_1a";
    }

    @Override
    public String getDefaultPresetId() {
        return "pids_1a";
    }

    @Override
    public int getRowAmount() {
        return 3;
    }

    @Override
    public boolean isKeyBlock() {
        Direction dir = IBlock.getStatePropertySafe(getBlockState(), FACING);
        return dir.equals(Direction.NORTH) || dir.equals(Direction.EAST);
    }
}
