package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

public class ButterflyLightBlockEntity extends JCMBlockEntityBase {
    private int startBlinkingSeconds = 10;
    public ButterflyLightBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.BUTTERFLY_LIGHT.get(), blockPos, blockState);
    }

    @Override
    public void readNbt(CompoundTag compoundTag) {
        super.readNbt(compoundTag);
        this.startBlinkingSeconds = compoundTag.getInt("seconds_to_blink");
    }

    @Override
    public void writeNbt(CompoundTag compoundTag) {
        super.writeNbt(compoundTag);
        compoundTag.putInt("seconds_to_blink", startBlinkingSeconds);
    }

    public void setData(int secondsToBlink) {
        this.startBlinkingSeconds = secondsToBlink;
        this.setChanged();
    }

    public int getStartBlinkingSeconds() {
        return startBlinkingSeconds;
    }
}
