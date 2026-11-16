package com.lx862.jcm.mod.block.base;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility base class replacing the removed MTR 4.0-era
 * {@code org.mtr.mapping.mapper.BlockExtension}.
 *
 * <p>In MTR 4.1 the mapping layer is gone and every hook the old
 * {@code BlockExtension} exposed is a plain vanilla {@link Block} override.
 * JCM blocks that previously extended the mapping class now extend this shim
 * and override the vanilla methods directly; the "2"-suffixed mapped hook
 * names have been renamed to their vanilla counterparts throughout the port
 * ({@code onUse2} -> {@code useWithoutItem}, {@code onPlaced2} ->
 * {@code setPlacedBy}, {@code getStateForNeighborUpdate2} -> {@code updateShape},
 * {@code getPlacementState2} -> {@code getStateForPlacement}, and so on).</p>
 */
public abstract class BlockExtension extends Block {

        public BlockExtension(BlockBehaviour.Properties settings) {
                super(settings);
        }

        /**
         * Bridges JCM's 4.0-era {@code addBlockProperties(List)} convention to
         * vanilla {@code createBlockStateDefinition(StateDefinition.Builder)}.
         *
         * <p>The {@link Block} constructor invokes this while assembling the
         * block's state definition, so every property contributed by
         * {@link #addBlockProperties(List)} (this class and all subclasses)
         * becomes part of the block's possible states. Without this bridge no
         * JCM block would carry any property, and any constructor calling
         * {@code defaultBlockState().setValue(...)} would crash with
         * {@code IllegalArgumentException: Cannot set property ... as it does
         * not exist}.</p>
         */
        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                final List<Property<?>> properties = new ArrayList<>();
                addBlockProperties(properties);
                builder.add(properties.toArray(new Property[0]));
        }

        /**
         * Declares the block state properties of this block. Kept from the 4.0-era
         * mapping layer; the port keeps the JCM convention of declaring all
         * properties in one place ({@code BlockProperties}).
         */
        public void addBlockProperties(List<Property<?>> properties) {
        }
}
