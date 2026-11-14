package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.block.entity.SubsidyMachineBlockEntity;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.network.gui.SubsidyMachineGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.world.level.block.EntityBlock;
import org.mtr.block.IBlock;
import org.mtr.data.TicketSystem;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SubsidyMachineBlock extends WallAttachedBlock implements EntityBlock {
    private static final Object2LongOpenHashMap<UUID> cooldownMap = new Object2LongOpenHashMap<>();
    public SubsidyMachineBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(4, 1.25, 0, 12, 14.75, 2, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        if(be == null) return InteractionResult.FAIL;

        SubsidyMachineBlockEntity sbe = (SubsidyMachineBlockEntity)be;

        return IBlock.checkHoldingBrush(world, player, () -> {
            Networking.sendPacketToClient(player, new SubsidyMachineGUIPacket(pos, sbe.getSubsidyAmount(), sbe.getCooldown()));
        }, () -> {
            if(cooldownExpired(player, sbe.getCooldown())) {
                updateCooldown(player);
                int finalBalance = addMTRBalanceToPlayer(world, player, sbe.getSubsidyAmount());
                player.displayClientMessage((TextUtil.translatable(TextCategory.HUD, "subsidy_machine.success", sbe.getSubsidyAmount(), finalBalance)), true);
            } else {
                int remainingSec = Math.round(sbe.getCooldown() - getCooldown(player));
                player.displayClientMessage((TextUtil.translatable(TextCategory.HUD, "subsidy_machine.fail", remainingSec).withStyle(ChatFormatting.RED)), true);
            }
        });
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SubsidyMachineBlockEntity(blockPos, blockState);
    }

    private static int addMTRBalanceToPlayer(Level world, Player player, int amount) {
        TicketSystem.addBalance(world, player, amount);
        return TicketSystem.getBalance(world, player);
    }

    private static boolean cooldownExpired(Player player, int cooldown) {
        return getCooldown(player) >= cooldown;
    }

    private static long getCooldown(Player player) {
        return (JCMServerStats.getGameTick() - cooldownMap.getOrDefault(player.getUUID(), 0)) / Constants.MC_TICK_PER_SECOND;
    }

    private static void updateCooldown(Player player) {
        cooldownMap.put(player.getUUID(), JCMServerStats.getGameTick());
    }
}