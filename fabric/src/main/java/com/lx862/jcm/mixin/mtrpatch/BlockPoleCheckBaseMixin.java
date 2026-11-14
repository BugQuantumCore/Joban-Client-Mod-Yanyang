package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.block.base.SlabExtendableBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.block.BlockPoleCheckBase;
import org.mtr.block.BlockRailwaySignPole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

@Mixin(value = BlockPoleCheckBase.class)
public class BlockPoleCheckBaseMixin {

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    public void getStateForPlacement(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {

        if((Object)this instanceof BlockRailwaySignPole) {
            BlockState state = cir.getReturnValue();
            if(state != null) {
                BlockPos slabCheckPos = ctx.getClickedPos().above();
                if(SlabExtendableBlock.shouldExtendForSlab((LevelAccessor)ctx.getLevel(), slabCheckPos, SlabType.TOP)) {
                    cir.setReturnValue(
                            state
                            .setValue(BlockProperties.IS_SLAB_MIGRATE, BlockProperties.InvertBoolean.TRUE)
                    );
                }
            }
        }
    }
}
