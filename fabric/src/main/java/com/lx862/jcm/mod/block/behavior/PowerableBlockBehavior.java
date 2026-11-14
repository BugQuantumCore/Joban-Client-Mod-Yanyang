package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public interface PowerableBlockBehavior  {
    BooleanProperty UNPOWERED = BlockProperties.UNPOWERED;

    default void updateRedstone(Level world, BlockPos pos, Block block, BlockState state) {
        world.updateNeighborsAt(pos, block);
        world.updateNeighborsAt(pos.relative(IBlock.getStatePropertySafe(state, BlockProperties.FACING)), block);
    }

    default void updateAllRedstone(Level world, BlockPos pos, Block block, BlockState state) {
        world.updateNeighborsAt(pos, block);

        for(Direction direction : Direction.values()) {
            world.updateNeighborsAt(pos.relative(direction), block);
        }
    }
}
