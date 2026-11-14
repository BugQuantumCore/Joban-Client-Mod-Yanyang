package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.jcm.mod.util.BlockUtil;
import net.minecraft.world.level.block.Block;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class JCMBlock extends BlockExtension {
    public JCMBlock(BlockBehaviour.Properties settings) {
        super(LoaderImpl.getSolidBlockSettings(settings));
    }

    /**
     * Loop through each block that should be associated with this block (i.e. Multiblock structure)<br>
     * The callback can be called multiple times on, for example a multi-block, and that multiple block entity should be updated to create a consistent state.<br>
     */
    public void loopStructure(BlockState state, Level world, BlockPos sourcePos, BiConsumer<BlockState, BlockEntity> callback) {
        for(BlockPos bPos : getAllPos(state, world, sourcePos)) {
            BlockState bs = world.getBlockState(sourcePos);
            BlockEntity be = BlockUtil.getBlockEntityOrNull(world, bPos);
            if(be != null) callback.accept(bs, be);
        }
    }

    protected void breakWithoutDropIfCreative(Level world, BlockPos pos, BlockState state, Player player, BlockExtension blockInstance, GetLootDropPositionCallback getLootPos) {
        if(player.isCreative()) {
            BlockPos dropPos = getLootPos.get(state, pos);
            if(world.getBlockState(dropPos).getBlock().equals(blockInstance)) {
                world.destroyBlock(dropPos, false);
            }
        }
    }

    /* Get all pos of the entire block structure */
    public BlockPos[] getAllPos(BlockState state, LevelAccessor world, BlockPos sourcePos) {
        return new BlockPos[]{sourcePos};
    }

    public BlockPos[] getAllPos(BlockState state, Level world, BlockPos sourcePos) {
        return getAllPos(state, (LevelAccessor)world, sourcePos);
    }

    @FunctionalInterface
    public interface GetLootDropPositionCallback {
        BlockPos get(BlockState state, BlockPos pos);
    }
}
