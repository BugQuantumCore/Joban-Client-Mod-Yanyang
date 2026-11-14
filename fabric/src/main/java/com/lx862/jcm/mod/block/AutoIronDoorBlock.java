package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.AutoIronDoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.DoorBlock;

import java.util.function.Consumer;

public class AutoIronDoorBlock extends DoorBlock implements EntityBlock {
    public AutoIronDoorBlock(Consumer<BlockBehaviour.Properties> consumer) {
        super(BlockSetType.IRON, buildSettings(consumer));
    }

    private static BlockBehaviour.Properties buildSettings(Consumer<BlockBehaviour.Properties> consumer) {
        final BlockBehaviour.Properties properties = BlockBehaviour.Properties.of();
        consumer.accept(properties);
        return properties;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new AutoIronDoorBlockEntity(blockPos, blockState);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == com.lx862.jcm.mod.registry.BlockEntities.AUTO_IRON_DOOR.get() ? (level1, pos, state1, blockEntity) -> ((com.lx862.jcm.mod.block.entity.JCMBlockEntityBase) blockEntity).tick() : null;
    }
}
