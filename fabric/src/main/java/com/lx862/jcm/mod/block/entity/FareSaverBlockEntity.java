package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

public class FareSaverBlockEntity extends JCMBlockEntityBase {
    private String prefix = "$";
    private int discount = 2;
    public FareSaverBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.FARE_SAVER.get(), blockPos, blockState);
    }

    @Override
    public void readNbt(CompoundTag compoundTag) {
        super.readNbt(compoundTag);
        this.discount = compoundTag.getInt("discount");
        this.prefix = compoundTag.contains("currency") ? compoundTag.getString("currency") : "$";
    }

    @Override
    public void writeNbt(CompoundTag compoundTag) {
        super.writeNbt(compoundTag);
        compoundTag.putInt("discount", discount);
        compoundTag.putString("prefix", prefix);
    }

    public void setData(String prefix, int discount) {
        this.prefix = prefix;
        this.discount = discount;
        this.setChanged();
    }

    public int getDiscount() {
        return discount;
    }

    public String getPrefix() {
        return prefix;
    }
}
