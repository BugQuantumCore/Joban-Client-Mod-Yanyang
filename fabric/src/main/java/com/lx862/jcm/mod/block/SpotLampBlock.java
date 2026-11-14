package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.SlabExtendableBlock;
import com.lx862.jcm.mod.block.base.VerticallyAttachedBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpotLampBlock extends VerticallyAttachedBlock {
    public static final BooleanProperty LIT = BlockProperties.POWERED;
    public static final EnumProperty<BlockProperties.InvertBoolean> IS_SLAB = BlockProperties.IS_SLAB_MIGRATE;

    public SpotLampBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);
        if(superState == null) return null;
        boolean isTop = IBlock.getStatePropertySafe(superState, BlockProperties.TOP);
        BlockPos targetBlockPos = isTop ? ctx.getClickedPos().above() : ctx.getClickedPos().below();
        boolean hasSlab = SlabExtendableBlock.shouldExtendForSlab(ctx.getLevel(), targetBlockPos, isTop ? net.minecraft.world.level.block.state.properties.SlabType.TOP : net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);

        return superState.setValue(IS_SLAB, hasSlab ? BlockProperties.InvertBoolean.TRUE : BlockProperties.InvertBoolean.FALSE);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        boolean isTop = IBlock.getStatePropertySafe(state, BlockProperties.TOP);

        if((isTop && direction == Direction.UP) || (!isTop && direction == Direction.DOWN)) {
            if(SlabExtendableBlock.shouldExtendForSlab(world, neighborPos, isTop ? net.minecraft.world.level.block.state.properties.SlabType.TOP : net.minecraft.world.level.block.state.properties.SlabType.BOTTOM)) {
                return state.setValue(IS_SLAB, BlockProperties.InvertBoolean.TRUE);
            } else {
                return state.setValue(IS_SLAB, BlockProperties.InvertBoolean.FALSE);
            }
        }

        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        if (IBlock.getStatePropertySafe(state, TOP)) {
            return IBlock.getVoxelShapeByDirection(4, 15.75, 4, 12, 16, 12, Direction.NORTH);
        } else {
            return IBlock.getVoxelShapeByDirection(4, 0, 4, 12, 0.25, 12, Direction.NORTH);
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            BlockState newState = state.cycle(LIT);
            world.setBlock(pos, newState, 3);
        });
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(LIT);
        properties.add(IS_SLAB);
    }
}
