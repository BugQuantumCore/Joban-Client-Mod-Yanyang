package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.registry.Items;
import org.mtr.block.BlockPSDAPGGlassEndBase;
import net.minecraft.world.item.Item;

public class APGGlassEndDRL extends BlockPSDAPGGlassEndBase {

    public APGGlassEndDRL(net.minecraft.world.level.block.state.BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public Item asItem() {
        return Items.APG_GLASS_END_DRL.get();
    }

    @Override
    public boolean isAPG() {
        return true;
    }
}
