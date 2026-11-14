package com.lx862.jcm.mod.data;

import org.mtr.block.IBlock;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Stores all block properties JCM uses. Block classes from JCM should reference the block properties in here
 */
public final class BlockProperties {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UNPOWERED = BooleanProperty.create("unpowered");
    public static final BooleanProperty HORIZONTAL_IS_LEFT = BooleanProperty.create("left");
    public static final EnumProperty<IBlock.DoubleBlockHalf> VERTICAL_2 = IBlock.HALF;
    public static final EnumProperty<IBlock.EnumThird> VERTICAL_PART_3 = IBlock.THIRD;
    public static final EnumProperty<InvertBoolean> IS_SLAB_MIGRATE = EnumProperty.create("is_slab", InvertBoolean.class);
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("level", 0, 15);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty IS_SLAB = BooleanProperty.create("is_slab");
    public static final BooleanProperty TOP = BooleanProperty.create("ceiling");
    public static final BooleanProperty EXIT_ON_LEFT = BooleanProperty.create("exit_on_left");
    public static final BooleanProperty POINT_TO_LEFT = BooleanProperty.create("right");
    public static final IntegerProperty BARRIER_FENCE_TYPE = IntegerProperty.create("type", 0, 10);
    public static final BooleanProperty BARRIER_FLIPPED = BooleanProperty.create("flipped");

    public enum InvertBoolean implements StringRepresentable {
        FALSE("false"),
        TRUE("true");

        private final String name;

        InvertBoolean(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
