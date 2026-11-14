package com.lx862.jcm.mixin;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import org.mtr.block.PlatformHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.mtr.block.BlockPSDAPGDoorBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Heightmap.class)
public class DoorHeightmapMixin {
    @Shadow
    @Final
    net.minecraft.world.level.chunk.ChunkAccess chunk;

    @Inject(method = "update(IIILnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("HEAD"))
    private void jsblock$trackDoorRelevantHeightmapUpdate(int x, int y, int z, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!DoorDirectionManager.isServerThread()) return;
        if (!(this.chunk instanceof LevelChunk)) return;
        final Level world = ((DoorWorldChunkAccessor) this.chunk).jcm$getLevel();
        if (world == null) return;
        final ChunkPos chunkPos = ((LevelChunk) this.chunk).getPos();
        final Object newBlock = state.getBlock();
        if (newBlock instanceof PlatformHelper || newBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(world, chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
            return;
        }
        if (!DoorDirectionManager.chunkMayContainPlatforms(world, chunkPos.x, chunkPos.z)) return;
        DoorDirectionManager.onDoorBlockChange(world, chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
    }
}
