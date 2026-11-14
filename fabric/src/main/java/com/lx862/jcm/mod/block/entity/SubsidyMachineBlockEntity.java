package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

public class SubsidyMachineBlockEntity extends JCMBlockEntityBase {
    private int subsidyAmount = 10;
    private int cooldown = 0;
    public SubsidyMachineBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.SUBSIDY_MACHINE.get(), blockPos, blockState);
    }

    @Override
    public void readNbt(CompoundTag compoundTag) {
        super.readNbt(compoundTag);
        this.subsidyAmount = compoundTag.getInt("price_per_click");
        this.cooldown = compoundTag.getInt("timeout");
    }

    @Override
    public void writeNbt(CompoundTag compoundTag) {
        super.writeNbt(compoundTag);
        compoundTag.putInt("price_per_click", subsidyAmount);
        compoundTag.putInt("timeout", cooldown);
    }

    public void setData(int pricePerUse, int cooldown) {
        this.subsidyAmount = pricePerUse;
        this.cooldown = cooldown;
        this.setChanged();
    }

    public int getSubsidyAmount() {
        return subsidyAmount;
    }

    public int getCooldown() {
        return cooldown;
    }
}
