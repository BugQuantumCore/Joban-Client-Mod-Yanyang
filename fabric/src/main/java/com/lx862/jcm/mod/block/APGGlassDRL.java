package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.APGGlassDRLBlockEntity;
import com.lx862.jcm.mod.registry.Items;
import org.mtr.block.BlockAPGGlass;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class APGGlassDRL extends BlockAPGGlass {

    public APGGlassDRL(net.minecraft.world.level.block.state.BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public Item asItem() {
        return Items.APG_GLASS_DRL.get();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new APGGlassDRLBlockEntity(blockPos, blockState);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockView, BlockPos pos, CollisionContext shapeContext) {
        int height = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? 2 : 16;
        return IBlock.getVoxelShapeByDirection(0.0, 0.0, 0.0, 16.0, height, 4.0, IBlock.getStatePropertySafe(state, com.lx862.jcm.mod.data.BlockProperties.FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter blockView, BlockPos pos, CollisionContext shapeContext) {
        int height = IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? 9 : 16;
        return IBlock.getVoxelShapeByDirection(0.0, 0.0, 0.0, 16.0, height, 4.0, IBlock.getStatePropertySafe(state, com.lx862.jcm.mod.data.BlockProperties.FACING));
    }
}
