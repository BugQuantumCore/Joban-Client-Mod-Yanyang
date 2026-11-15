package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalDoubleBlockBehavior;
import com.lx862.jcm.mod.block.behavior.VerticallyAttachedBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.HolderBase;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
#if MC_VERSION == "12104"
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
#endif

public abstract class CeilingAttachedDirectional2Block extends CeilingAttachedDirectionalBlock implements HorizontalDoubleBlockBehavior {
    public static final int width = 2;
    public static final BooleanProperty IS_LEFT = BlockProperties.HORIZONTAL_IS_LEFT;
    private final boolean canAttachTop;
    private final boolean canAttachBottom;

    public CeilingAttachedDirectional2Block(BlockBehaviour.Properties settings, boolean canAttachTop, boolean canAttachBottom, boolean enforceLogicalPattern) {
        super(settings, enforceLogicalPattern);
        this.canAttachTop = canAttachTop;
        this.canAttachBottom = canAttachBottom;
    }

    public boolean canPlace(BlockState state, BlockPos pos, BlockPlaceContext ctx) {
        boolean canPlaceHorizontally = HorizontalDoubleBlockBehavior.canBePlaced(ctx);
        boolean canPlaceVertically = VerticallyAttachedBlock.canPlace(canAttachTop, canAttachBottom, ctx) && VerticallyAttachedBlock.canPlace(canAttachTop, canAttachBottom, pos.relative(IBlock.getStatePropertySafe(state, FACING).getClockWise()), ctx);
        return this.enforceLogicalPattern ? canPlaceHorizontally && canPlaceVertically : canPlaceHorizontally;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return canPlace(super.getStateForPlacement(ctx), ctx.getClickedPos(), ctx) ? super.getStateForPlacement(ctx) : null;
    }

    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        HorizontalDoubleBlockBehavior.placeBlock(world, pos, state, IS_LEFT, (state.getValue(FACING)).getClockWise(), width);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos breakPos, BlockState breakState, Player player) {
        breakWithoutDropIfCreative(world, breakPos, breakState, player, this, HorizontalDoubleBlockBehavior::getLootDropPos);
        return super.playerWillDestroy(world, breakPos, breakState, player);
    }

    @Override
    #if MC_VERSION == "12104"
public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
#else
public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
#endif
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);

        if(!HorizontalDoubleBlockBehavior.blockIsValid(pos, state, direction, world, isLeft)) {
            return Blocks.AIR.defaultBlockState();
        }

#if MC_VERSION == "12104"
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
#else
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
#endif
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(IS_LEFT);
    }
}