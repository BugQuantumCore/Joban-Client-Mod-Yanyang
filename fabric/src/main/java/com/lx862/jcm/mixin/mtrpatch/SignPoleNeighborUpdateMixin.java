package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.block.base.SlabExtendableBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import org.mtr.block.BlockRailwaySignPole;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends railway sign poles for slabs placed above them (NTE-style slab
 * migration). Replaces the 4.0-era BlockExtensionMixin that hooked the removed
 * mapping-layer {@code getStateForNeighborUpdate2}; MTR 4.1 exposes vanilla
 * {@code updateShape} instead.
 */
@Mixin(value = BlockRailwaySignPole.class)
public class SignPoleNeighborUpdateMixin {
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void jsblock$updatePoles(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        if (direction == Direction.UP) {
            if (SlabExtendableBlock.shouldExtendForSlab(world, neighborPos, SlabType.TOP)) {
                cir.setReturnValue(state.setValue(BlockProperties.IS_SLAB_MIGRATE, BlockProperties.InvertBoolean.TRUE));
            } else {
                cir.setReturnValue(state.setValue(BlockProperties.IS_SLAB_MIGRATE, BlockProperties.InvertBoolean.FALSE));
            }
        }
    }
}
