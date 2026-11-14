package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.block.behavior.PowerableBlockBehavior;
import com.lx862.jcm.mod.render.HolderBase;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HelpLineStandingEALBlock extends Vertical2Block implements PowerableBlockBehavior {
    public HelpLineStandingEALBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        switch (IBlock.getStatePropertySafe(state, HALF)) {
            case LOWER:
                return IBlock.getVoxelShapeByDirection(0, 0, 5.5, 16, 16, 10.5, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                return IBlock.getVoxelShapeByDirection(0, 0, 5.5, 16, 24, 10.5, IBlock.getStatePropertySafe(state, FACING));
            default:
                return Shapes.empty();
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        for(BlockPos bPos : getAllPos(state, world, pos)) {
            BlockState bs = world.getBlockState(bPos);
            if(bs.getBlock() instanceof HelpLineStandingEALBlock) {
                world.setBlock(bPos, bs.setValue(UNPOWERED, false), 3);
                updateAllRedstone((Level)world, bPos, this, bs);
                world.scheduleTick(bPos, this, 20);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        world.setBlock(pos, state.setValue(UNPOWERED, true), 3);
        updateAllRedstone((Level)world, pos, this, state);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return IBlock.getStatePropertySafe(state, UNPOWERED) ? 0 : 15;
    }

    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return IBlock.getStatePropertySafe(state, UNPOWERED) ? 0 : 15;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(UNPOWERED);
    }
}