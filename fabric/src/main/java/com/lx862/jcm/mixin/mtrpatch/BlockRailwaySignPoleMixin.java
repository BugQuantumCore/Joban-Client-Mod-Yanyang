package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.data.BlockProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.mtr.block.BlockRailwaySignPole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Registers JCM's slab-migration property on the railway sign pole. MTR 4.1
 * defines block state properties in vanilla
 * {@code createBlockStateDefinition(StateDefinition.Builder)} instead of the
 * removed mapping-layer {@code addBlockProperties(List)}.
 */
@Mixin(value = BlockRailwaySignPole.class)
public class BlockRailwaySignPoleMixin {
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void jsblock$addIsSlabProperties(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(BlockProperties.IS_SLAB_MIGRATE);
    }
}
