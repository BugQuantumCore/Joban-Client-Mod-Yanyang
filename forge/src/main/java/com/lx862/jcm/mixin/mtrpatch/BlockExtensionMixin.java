package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.block.base.SlabExtendableBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.block.BlockRailwaySignPole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = BlockAbstractMapping.class, remap = false)
public class BlockExtensionMixin {
    @Inject(method = "getStateForNeighborUpdate2", at = @At("HEAD"), remap = false, cancellable = true)
    private void jsblock$updatePoles(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        if((Object)this instanceof BlockRailwaySignPole) {
            if(direction == Direction.UP) {
                if(SlabExtendableBlock.shouldExtendForSlab(world, neighborPos, SlabType.TOP)) {
                    cir.setReturnValue(
                            state.setValue($1, BlockProperties.InvertBoolean.TRUE)
                    );
                } else {
                    cir.setReturnValue(
                        state.setValue($1, BlockProperties.InvertBoolean.FALSE)
                    );
                }
            }
        }
    }
}
