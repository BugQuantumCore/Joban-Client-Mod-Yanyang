package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.registry.BlockEntities;
import com.lx862.jcm.mod.compat.MinecraftServerHelper;
import com.lx862.jcm.mod.compat.SoundHelper;
import org.mtr.block.IBlock;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

public class AutoIronDoorBlockEntity extends JCMBlockEntityBase {
    public static final int DETECT_RADIUS = 3;
    public AutoIronDoorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.AUTO_IRON_DOOR.get(), blockPos, blockState);
    }

    public void tick() {
        Level world = getLevel();

        if(world != null && !world.isClientSide() && JCMServerStats.getGameTick() % 5 == 0) {
            BlockPos pos = getBlockPos();
            BlockState state = world.getBlockState(pos);

            AABB box = new AABB(pos.getX() - DETECT_RADIUS, pos.getY() - DETECT_RADIUS, pos.getZ() - DETECT_RADIUS, pos.getX() + DETECT_RADIUS, pos.getY() + DETECT_RADIUS, pos.getZ() + DETECT_RADIUS);
            AtomicBoolean haveNearbyPlayer = new AtomicBoolean(false);

            MinecraftServerHelper.iteratePlayers((ServerLevel)getLevel(), (player) -> {
                if(!player.isSpectator() && box.contains(player.position())) {
                    boolean alreadyOpened = IBlock.getStatePropertySafe(state, net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN);

                    if(!alreadyOpened) {
                        world.playSound(null, pos, SoundHelper.createSoundEvent(ResourceLocation.parse("minecraft:block.iron_door.open")), SoundSource.BLOCKS, 1, 1);
                        world.setBlock(pos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN, true), 3);
                    }
                    haveNearbyPlayer.set(true);
                }
            });

            if(!haveNearbyPlayer.get() && IBlock.getStatePropertySafe(state, net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
                world.playSound((Player) null, getBlockPos(), SoundHelper.createSoundEvent(ResourceLocation.parse("minecraft:block.iron_door.close")), SoundSource.BLOCKS, 1, 1);
                world.setBlock(getBlockPos(), state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN, false), 3);
            }
        }
    }
}