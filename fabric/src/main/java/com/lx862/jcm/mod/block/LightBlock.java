package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.jcm.mod.render.HolderBase;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightBlock extends JCMBlock {
    public static final IntegerProperty LIGHT_LEVEL = BlockProperties.LIGHT_LEVEL;

    public LightBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(LIGHT_LEVEL, 15));
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player != null && player.isHolding(this.asItem())) {
            //TODO: world.addParticle, no particle in mappings yet
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext shapeContext) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(world.isClientSide()) return InteractionResult.SUCCESS;

        if(player.isHolding(this.asItem())) {
            BlockState newState = state.cycle(LIGHT_LEVEL);
            world.setBlock(pos, newState, 3);
            player.displayClientMessage((TextUtil.translatable(TextCategory.HUD, "light_block.success", IBlock.getStatePropertySafe(newState, LIGHT_LEVEL))), true);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.FAIL;
        }
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(LIGHT_LEVEL);
    }
}
