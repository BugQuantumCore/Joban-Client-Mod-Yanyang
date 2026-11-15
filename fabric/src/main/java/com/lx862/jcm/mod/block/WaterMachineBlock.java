package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.compat.PlayerHelper;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WaterMachineBlock extends Vertical2Block {
    public WaterMachineBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(2.5, 0, 2.5, 13.5, 16, 13.5, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        // 1.21.4 dropped the useItemOn(ItemStack,...) ItemInteractionResult
        // override surface this block used on 1.20.5-1.21.1; useWithoutItem
        // has the same signature across the whole supported range (vanilla
        // only forwards to it after the held-item use passes through, which
        // is exactly this block's PASS branch).
        final ItemStack held = player.getMainHandItem();
        if(held.is(Items.GLASS_BOTTLE)) {
            fillBottleForPlayer(player, InteractionHand.MAIN_HAND);
            return InteractionResult.SUCCESS;
        }

        if(held.is(Items.BUCKET)) {
            fillBucketForPlayer(player, InteractionHand.MAIN_HAND);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static void fillBottleForPlayer(Player player, InteractionHand hand) {
        ItemStack newWaterBottle = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
        offerOrDrop(player, hand, newWaterBottle);
        playSplashSoundToPlayer(player);
    }

    private static void fillBucketForPlayer(Player player, InteractionHand hand) {
        ItemStack newWaterBucket = new ItemStack(Items.WATER_BUCKET);
        offerOrDrop(player, hand, newWaterBucket);
        playSplashSoundToPlayer(player);
    }

    private static void offerOrDrop(Player player, InteractionHand hand, ItemStack stack) {
        ItemStack playerHolding = player.getItemInHand(hand);
        playerHolding.shrink(1);

        if(playerHolding.isEmpty()) {
            player.setItemInHand(hand, stack);
        } else {
            PlayerHelper.getPlayerInventory(player).add(stack);
        }
    }

    private static void playSplashSoundToPlayer(Player player) {
        player.playSound(SoundEvents.BUCKET_FILL, 1F, 1F);
    }
}