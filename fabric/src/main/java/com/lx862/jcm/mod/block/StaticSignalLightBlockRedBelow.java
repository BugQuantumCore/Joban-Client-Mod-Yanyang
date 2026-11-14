package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StaticSignalLightBlockRedBelow extends StaticSignalLightBlock {
    public StaticSignalLightBlockRedBelow(BlockBehaviour.Properties settings) {
        super(settings, 2, 14);
    }

    @Override
    public StaticSignalLightBlockEntity.SignalType getSignalType() {
        return StaticSignalLightBlockEntity.SignalType.RED_BELOW;
    }
}
