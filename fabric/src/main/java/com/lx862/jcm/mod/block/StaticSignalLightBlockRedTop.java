package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.StaticSignalLightBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StaticSignalLightBlockRedTop extends StaticSignalLightBlock {
    public StaticSignalLightBlockRedTop(BlockBehaviour.Properties settings) {
        super(settings, 2, 14);
    }

    @Override
    public StaticSignalLightBlockEntity.SignalType getSignalType() {
        return StaticSignalLightBlockEntity.SignalType.RED_TOP;
    }
}
