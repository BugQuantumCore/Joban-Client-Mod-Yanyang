package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.block.base.SlabExtendableBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.mtr.block.BlockRailwaySignPole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
#if MC_VERSION == "12104"
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
#endif

/**
 * Extends railway sign poles for slabs placed above them (NTE-style slab
 * migration).
 *
 * <p>MTR 4.1's {@code BlockRailwaySignPole} no longer overrides the
 * neighbour-update method, so the pole inherits the vanilla
 * {@link BlockBehaviour#updateShape} implementation. The hook therefore
 * lives on {@link BlockBehaviour} itself and is guarded with an
 * {@code instanceof BlockRailwaySignPole} check — the pole (and every other
 * block that relies on the inherited implementation) routes through it,
 * while blocks that override {@code updateShape} never reach this code.</p>
 *
 * <p>The 1.21.2+ overload reorders the arguments and adds
 * {@code ScheduledTickAccess}/{@code RandomSource} (see
 * {@code SlabExtendableBlock#updateShape} for the same version split).</p>
 */
@Mixin(BlockBehaviour.class)
public class SignPoleNeighborUpdateMixin {

#if MC_VERSION == "12104"
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void jsblock$updatePoles(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
#else
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void jsblock$updatePoles(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
#endif
        if (direction == Direction.UP && (Object) this instanceof BlockRailwaySignPole) {
            if (SlabExtendableBlock.shouldExtendForSlab(world, neighborPos, SlabType.TOP)) {
                cir.setReturnValue(state.setValue(BlockProperties.IS_SLAB_MIGRATE, BlockProperties.InvertBoolean.TRUE));
            } else {
                cir.setReturnValue(state.setValue(BlockProperties.IS_SLAB_MIGRATE, BlockProperties.InvertBoolean.FALSE));
            }
        }
    }
}
