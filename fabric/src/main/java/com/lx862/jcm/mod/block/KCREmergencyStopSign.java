package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.jcm.mod.render.HolderBase;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class KCREmergencyStopSign extends WallAttachedBlock {
    public static final BooleanProperty POINT_TO_LEFT = BlockProperties.POINT_TO_LEFT;
    public KCREmergencyStopSign(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 6.5, 26, 7, 9.5, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx) == null ? null : super.getStateForPlacement(ctx).setValue(FACING, Direction.from2DDataValue(ctx.getNearestLookingDirection().get2DDataValue()));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            world.setBlock(pos, state.cycle(POINT_TO_LEFT), 3);
            player.displayClientMessage((TextUtil.translatable(TextCategory.HUD, "kcr_emg_stop_sign.success")), true);
        });
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(POINT_TO_LEFT);
    }

    @Override
    public Direction getWallDirection(Direction defaultDirection) {
        return defaultDirection.getCounterClockWise();
    }
}
