package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class Vertical3Block extends DirectionalBlock {
    private static final int HEIGHT = 3;
    public static final EnumProperty<IBlock.EnumThird> THIRD = BlockProperties.VERTICAL_PART_3;

    public Vertical3Block(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return BlockUtil.isReplacable(ctx.getLevel(), ctx.getClickedPos(), Direction.UP, ctx, HEIGHT) ? super.getStateForPlacement(ctx) : null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlock(pos.above(1), state.setValue(THIRD, IBlock.EnumThird.MIDDLE), 3);
        world.setBlock(pos.above(2), state.setValue(THIRD, IBlock.EnumThird.UPPER), 3);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos breakPos, BlockState breakState, Player player) {
        breakWithoutDropIfCreative(world, breakPos, breakState, player, this, this::getLootDropPos);
        return super.playerWillDestroy(world, breakPos, breakState, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if(!BlockUtil.canSurvive(state.getBlock(), world, pos, Direction.UP, getPart(IBlock.getStatePropertySafe(state, THIRD)), HEIGHT)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(THIRD);
    }

    @Override
    public BlockPos[] getAllPos(BlockState state, Level world, BlockPos pos) {
        switch(IBlock.getStatePropertySafe(state, THIRD)) {
            case LOWER:
                return new BlockPos[]{
                        pos,
                        pos.above(),
                        pos.above(2)};
            case MIDDLE:
                return new BlockPos[]{
                        pos.below(),
                        pos,
                        pos.above()};
            case UPPER:
                return new BlockPos[]{
                        pos.below(2),
                        pos.below(),
                        pos};
            default:
                return super.getAllPos(state, world, pos);
        }
    }

    private BlockPos getLootDropPos(BlockState state, BlockPos pos) {
        switch(IBlock.getStatePropertySafe(state, THIRD)) {
            case LOWER: return pos;
            case MIDDLE: return pos.below();
            case UPPER: return pos.below(2);
            default: return pos;
        }
    }

    public int getPart(IBlock.EnumThird e) {
        if(e == IBlock.EnumThird.LOWER) {
            return 0;
        } else if(e == IBlock.EnumThird.MIDDLE) {
            return 1;
        } else {
            return 2;
        }
    }
}
