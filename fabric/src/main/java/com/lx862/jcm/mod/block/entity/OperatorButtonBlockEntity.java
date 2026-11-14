package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.block.OperatorButtonBlock;
import com.lx862.jcm.mod.registry.BlockEntities;
import com.lx862.jcm.mod.util.JCMUtil;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class OperatorButtonBlockEntity extends JCMBlockEntityBase {
    private boolean[] keyRequirements = new boolean[OperatorButtonBlock.ACCEPTED_KEYS.length];

    public OperatorButtonBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.OPERATOR_BUTTON.get(), blockPos, blockState);
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            this.keyRequirements[i] = true; // Enable all by default
        }
    }

    @Override
    public void readNbt(CompoundTag compoundTag) {
        super.readNbt(compoundTag);
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            boolean isEnabled = !compoundTag.contains("required_key" + i) || compoundTag.getBoolean("required_key" + i);
            this.keyRequirements[i] = isEnabled;
        }
    }

    @Override
    public void writeNbt(CompoundTag compoundTag) {
        super.writeNbt(compoundTag);
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            compoundTag.putBoolean("required_key" + i, keyRequirements[i]);
        }
    }

    public void setData(boolean[] keyRequirements) {
        this.keyRequirements = keyRequirements;
        this.setChanged();
    }

    public boolean[] getKeyRequirements() {
        return this.keyRequirements;
    }

    public boolean canOpen(ItemStack itemStack) {
        for(int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            if(Objects.equals(OperatorButtonBlock.ACCEPTED_KEYS[i].get(), itemStack.getItem())) {
                return keyRequirements[i];
            }
        }
        return false;
    }
}
