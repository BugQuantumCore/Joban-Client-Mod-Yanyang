package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.mtr.block.IBlock;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;

public class LCDPIDSBlockEntity extends PIDSBlockEntity {
    public LCDPIDSBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.LCD_PIDS.get(), blockPos, blockState);
    }

    @Override
    public String getPIDSType() {
        return "lcd_pids";
    }

    @Override
    public String getDefaultPresetId() {
        return "lcd_pids";
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
