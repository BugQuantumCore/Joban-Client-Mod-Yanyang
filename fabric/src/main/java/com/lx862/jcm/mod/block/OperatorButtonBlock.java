package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.block.entity.OperatorButtonBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.network.gui.OperatorButtonGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.world.level.block.EntityBlock;
import org.mtr.registry.ObjectHolder;
import com.lx862.jcm.mod.render.HolderBase;
import org.mtr.block.IBlock;
import org.mtr.item.ItemDepotDriverKey;
import org.mtr.item.ItemDriverKey;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.Item;

public class OperatorButtonBlock extends WallAttachedBlock implements EntityBlock {
    @SuppressWarnings("unchecked")
    public static final org.mtr.registry.ObjectHolder<Item>[] ACCEPTED_KEYS = new org.mtr.registry.ObjectHolder[]{org.mtr.registry.Items.BASIC_DRIVER_KEY, org.mtr.registry.Items.GUARD_KEY, org.mtr.registry.Items.ADVANCED_DRIVER_KEY, org.mtr.registry.Items.CREATIVE_DRIVER_KEY};
    public static final BooleanProperty POWERED = BlockProperties.POWERED;
    private final int poweredDuration;

    public OperatorButtonBlock(BlockBehaviour.Properties settings, int poweredDuration) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
        this.poweredDuration = poweredDuration;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(5, 5, 0, 11, 11.5, 0.2, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(world.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity thisBE = world.getBlockEntity(pos);
        OperatorButtonBlockEntity be = thisBE == null ? null : (OperatorButtonBlockEntity)thisBE;

        IBlock.checkHoldingBrush(world, player, () -> {
            Networking.sendPacketToClient(player, new OperatorButtonGUIPacket(pos, be.getKeyRequirements()));
        }, () -> {
            boolean pass = false;
            ItemStack[] itemStacks = new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()};
            for(ItemStack stack : itemStacks) {
                if(be == null) {
                    if(stack.getItem() instanceof ItemDriverKey) {
                        pass = true;
                    }
                } else {
                    if(be.canOpen(stack)) {
                        pass = true;
                    }
                }
                // Expiry check
                if(stack.getItem() instanceof ItemDepotDriverKey && stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).getUnsafe().getLong("expiry_time") < System.currentTimeMillis()) {
                    pass = false;
                }
                if(pass) break;
            }

            if(pass) {
                setPowered(world, state, pos, true);
                world.scheduleTick(pos, this, poweredDuration);
            } else {
                player.displayClientMessage((TextUtil.translatable(TextCategory.HUD, "operator_button.fail").withStyle(ChatFormatting.RED)), true);
            }
        });

        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick(BlockState state, ServerLevel serverWorld, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            setPowered((Level)serverWorld, state, pos, false);
        }
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(POWERED);
    }


    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return IBlock.getStatePropertySafe(state, POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return IBlock.getStatePropertySafe(state, POWERED) ? 15 : 0;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new OperatorButtonBlockEntity(blockPos, blockState);
    }

    private void setPowered(Level world, BlockState blockState, BlockPos pos, boolean powered) {
        world.setBlock(pos, blockState.setValue(POWERED, powered), 3);
        updateNearbyBlock(world, pos, (blockState.getValue(FACING)));
    }

    private void updateNearbyBlock(Level world, BlockPos pos, Direction blockFacing) {
        world.updateNeighborsAt(pos.relative(blockFacing), this);
    }
}
