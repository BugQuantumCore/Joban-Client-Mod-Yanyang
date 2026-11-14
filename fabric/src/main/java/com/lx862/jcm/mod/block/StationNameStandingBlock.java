package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.StationNameStandingBlockEntity;
import org.mtr.block.BlockStationNameTallStanding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The old station name standing block. Block is moved to MTR Mod, now this is just a block wrapping the mtr mod variant to retain the Block Entity.
 */
@Deprecated
public class StationNameStandingBlock extends BlockStationNameTallStanding {

    public StationNameStandingBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public Item asItem() {
        // Redirect to official MTR block
        return org.mtr.registry.Blocks.STATION_NAME_TALL_STANDING.get().asItem();
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StationNameStandingBlockEntity(blockPos, blockState);
    }
}
