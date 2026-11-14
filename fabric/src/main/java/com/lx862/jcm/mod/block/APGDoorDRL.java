package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.APGDoorDRLBlockEntity;
import com.lx862.jcm.mod.registry.Items;
import net.minecraft.world.level.block.EntityBlock;
import org.mtr.block.BlockPSDAPGDoorBase;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class APGDoorDRL extends BlockPSDAPGDoorBase implements EntityBlock {

    public APGDoorDRL(net.minecraft.world.level.block.state.BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public Item asItem() {
        return Items.APG_DOOR_DRL.get();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new APGDoorDRLBlockEntity(blockPos, blockState);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockView, BlockPos pos, CollisionContext shapeContext) {
        int height = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? 2 : 16;
        return IBlock.getVoxelShapeByDirection(0.0, 0.0, 0.0, 16.0, height, 4.0, IBlock.getStatePropertySafe(state, com.lx862.jcm.mod.data.BlockProperties.FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter blockView, BlockPos pos, CollisionContext shapeContext) {
        final BlockEntity blockEntity = blockView.getBlockEntity(pos);
        final Level world = blockEntity == null ? null : blockEntity.getLevel();
        if(world == null || !world.isClientSide()) return Shapes.empty();

        if(((BlockEntityBase) blockEntity).getDoorValue() == 0.0) {
            int height = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? 9 : 16;
            return IBlock.getVoxelShapeByDirection(0.0, 0.0, 0.0, 16.0, height, 4.0, IBlock.getStatePropertySafe(state, com.lx862.jcm.mod.data.BlockProperties.FACING));
        }
        return Shapes.empty();
    }
}
