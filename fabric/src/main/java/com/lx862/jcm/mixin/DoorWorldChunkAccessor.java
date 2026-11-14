package com.lx862.jcm.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelChunk.class)
public interface DoorWorldChunkAccessor {
    @Accessor("level")
    Level jcm$getLevel();
}
