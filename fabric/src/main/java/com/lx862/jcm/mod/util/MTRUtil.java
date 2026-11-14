package com.lx862.jcm.mod.util;

import org.mtr.MTR;
import org.mtr.block.BlockPSDAPGDoorBase;
import org.mtr.block.PlatformHelper;
import org.mtr.render.PositionAndRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MTRUtil {
    private static final int CHECK_DOOR_RADIUS_XZ = 1;
    private static final int CHECK_DOOR_RADIUS_Y = 2;

    /**
     * Passive version of {@link org.mtr.render.RenderVehicleHelper#canOpenDoors(AABB, PositionAndRotation, double)}
     * This does not open the PSD/APG door upon checking.
     */
    public static boolean canOpenDoors(AABB doorway, PositionAndRotation positionAndRotation) {
        final ClientLevel clientWorld = Minecraft.getInstance().level;
        if (clientWorld == null) {
            return false;
        }

        final Vec3 doorwayPosition1 = positionAndRotation.transformForwards(new Vec3(doorway.minX, doorway.maxY, doorway.minZ), Vec3::xRot, Vec3::yRot, Vec3::zRot, Vec3::add);
        final Vec3 doorwayPosition2 = positionAndRotation.transformForwards(new Vec3(doorway.maxX, doorway.maxY, doorway.minZ), Vec3::xRot, Vec3::yRot, Vec3::zRot, Vec3::add);
        final Vec3 doorwayPosition3 = positionAndRotation.transformForwards(new Vec3(doorway.maxX, doorway.maxY, doorway.maxZ), Vec3::xRot, Vec3::yRot, Vec3::zRot, Vec3::add);
        final Vec3 doorwayPosition4 = positionAndRotation.transformForwards(new Vec3(doorway.minX, doorway.maxY, doorway.maxZ), Vec3::xRot, Vec3::yRot, Vec3::zRot, Vec3::add);
        final double minX = Math.min(Math.min(doorwayPosition1.x, doorwayPosition2.x), Math.min(doorwayPosition3.x, doorwayPosition4.x));
        final double maxX = Math.max(Math.max(doorwayPosition1.x, doorwayPosition2.x), Math.max(doorwayPosition3.x, doorwayPosition4.x));
        final double minY = Math.min(Math.min(doorwayPosition1.y, doorwayPosition2.y), Math.min(doorwayPosition3.y, doorwayPosition4.y));
        final double maxY = Math.max(Math.max(doorwayPosition1.y, doorwayPosition2.y), Math.max(doorwayPosition3.y, doorwayPosition4.y));
        final double minZ = Math.min(Math.min(doorwayPosition1.z, doorwayPosition2.z), Math.min(doorwayPosition3.z, doorwayPosition4.z));
        final double maxZ = Math.max(Math.max(doorwayPosition1.z, doorwayPosition2.z), Math.max(doorwayPosition3.z, doorwayPosition4.z));
        boolean canOpenDoors = false;

        for (double checkX = minX - CHECK_DOOR_RADIUS_XZ; checkX <= maxX + CHECK_DOOR_RADIUS_XZ; checkX++) {
            for (double checkY = minY - CHECK_DOOR_RADIUS_Y; checkY <= maxY + CHECK_DOOR_RADIUS_Y; checkY++) {
                for (double checkZ = minZ - CHECK_DOOR_RADIUS_XZ; checkZ <= maxZ + CHECK_DOOR_RADIUS_XZ; checkZ++) {
                    final BlockPos checkPos = new BlockPos((int)checkX, (int)checkY, (int)checkZ);
                    final BlockState blockState = clientWorld.getBlockState(checkPos);
                    final Block block = blockState.getBlock();
                    if (block instanceof PlatformHelper) {
                        canOpenDoors = true;
                    } else if (block instanceof BlockPSDAPGDoorBase && blockState.getValue(BlockPSDAPGDoorBase.UNLOCKED)) {
                        canOpenDoors = true;
                    }
                }
            }
        }
        return canOpenDoors;
    }
}
