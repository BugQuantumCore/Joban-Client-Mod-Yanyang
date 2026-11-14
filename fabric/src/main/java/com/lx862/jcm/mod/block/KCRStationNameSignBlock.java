package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectionalBlock;
import com.lx862.jcm.mod.block.entity.KCRStationNameSignBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.world.level.block.EntityBlock;
import com.lx862.jcm.mod.render.HolderBase;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class KCRStationNameSignBlock extends CeilingAttachedDirectionalBlock implements EntityBlock {
    public static final BooleanProperty EXIT_ON_LEFT = BlockProperties.EXIT_ON_LEFT;
    private final boolean stationColored;
    public KCRStationNameSignBlock(BlockBehaviour.Properties settings, boolean stationColored) {
        super(settings, true);
        this.stationColored = stationColored;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(-7, 2, 5.5, 23, 16, 10.5, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new KCRStationNameSignBlockEntity(blockPos, blockState, stationColored);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            world.setBlock(pos, state.cycle(EXIT_ON_LEFT), 3);
            player.displayClientMessage((TextUtil.translatable(TextCategory.HUD, "kcr_name_sign.success")), true);
        });
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(EXIT_ON_LEFT);
    }
}
