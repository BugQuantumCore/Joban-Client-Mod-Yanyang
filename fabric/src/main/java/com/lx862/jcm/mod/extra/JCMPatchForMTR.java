package com.lx862.jcm.mod.extra;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.mod.impl.mtr.util.MTRWrapperJS;
import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import org.mtr.resource.CustomResourceTools;

import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.lx862.jcm.mod.util.ShaderDetector;

public class JCMPatchForMTR {
    private final static int RAIL_CULLING_AREA_LIMIT = Integer.MAX_VALUE / 2;
    private static boolean cachedRenderingShadow = false;
    private static final Long2IntArrayMap liftInstructions = new Long2IntArrayMap();
    public static ResourceLocation LIFT_DING_SOUND = ResourceLocation.parse("minecraft:block.note_block.pling");
    public static Map<BlockPos, BlockState> cachedRailNodeState = new HashMap<>();
    public static boolean isSitting = false;
    public static boolean wasSneaking = false;

    /**
     * Reload optimized rendering shaders.<br>
     * This was originally a part of {@link OptimizedRenderer#beginReload()}, however reloading shaders is quite heavy and memory intensive.<br>
     * It should only be done once during resource reload, hence it has been moved here to increase model loading speed.
     */
    /**
     * No-op since the MTR 4.1 port: the custom optimized-render shader
     * pipeline (OptimizedRenderer / ShaderManager) was removed upstream and
     * 4.1 renders through vanilla RenderTypes, so there is nothing to reload.
     */
    public static void reloadOptimizedRenderingShader() {
    }

    public static void tick() {
        if(Minecraft.getInstance().player != null) {
            if(JCMClientConfig.INSTANCE.mtrPatch.allowCameraLowering.value()) {
                boolean isPlayerSneaking = Minecraft.getInstance().player.isShiftKeyDown();
                if(MTRWrapperJS.Data.getPlayerMountedVehicle() == null) {
                    JCMPatchForMTR.isSitting = false;
                } else {
                    if(isPlayerSneaking && !JCMPatchForMTR.wasSneaking) {
                        JCMPatchForMTR.isSitting = !JCMPatchForMTR.isSitting;
                    }
                }
                JCMPatchForMTR.wasSneaking = isPlayerSneaking;
            }
        }
    }

    /**
     * Resolve from a relative path to an absolute path.<br>
     * Unlike MTR's method, this can resolve path traversals like ../, which is used in NTE resource packs
     * @param basePath The base identifier path
     * @param resource The relative path
     * @param extension The file extension
     * @return An identifier with the absolute path.
     */
    public static ResourceLocation resolveRelativePath(String basePath, String resource, String extension) {
        if (resource.contains(":")) { // Assume it is already an identifier
            return CustomResourceTools.formatIdentifierWithDefault(resource, extension);
        } else {
            ResourceLocation basePathId = ResourceLocation.parse(basePath);
            String relative = resource.toLowerCase(Locale.ROOT).replace('\\', '/');

            String resolvedPath = FileSystems.getDefault().getPath(basePathId.getPath()).getParent().resolve(relative)
                    .normalize().toString().replace('\\', '/');
            return CustomResourceTools.formatIdentifier(basePathId.getNamespace() + ":" + resolvedPath, extension);
        }
    }

    /** Clear every per-world cache (client join): liftInstructions keyed by
     *  lift id survived server switches and produced stale arrival detection. */
    public static void clearClientCaches() {
        cachedRailNodeState.clear();
        liftInstructions.clear();
    }

    public static void updateRenderingShadow() {
        cachedRenderingShadow = ShaderDetector.isRenderingShadowPass();
    }

    public static boolean isRenderingShadow() {
        return cachedRenderingShadow;
    }

    public static boolean liftJustArrived(long liftId, int currentInstructionSize) {
        long lastValue = liftInstructions.getOrDefault(liftId, currentInstructionSize);
        boolean liftJustArrived = currentInstructionSize == 0 && lastValue == 1;
        liftInstructions.put(liftId, currentInstructionSize);
        return liftJustArrived;
    }

    public static boolean shouldSkipCullingTask(double lowX, double lowY, double lowZ, double highX, double highY, double highZ) {
        // Exceeds max limit, not gonna bother checking
        if(Math.max(0, highX - lowX) * Math.max(0, highY - lowY) * Math.max(0, highZ - lowZ) > RAIL_CULLING_AREA_LIMIT) {
            return true;
        }
        return false;
    }

    public static AABB clampBoundingBoxToRenderDistance(Vec3 pos, int renderDistance, double lowX, double lowY, double lowZ, double highX, double highY, double highZ) {
        int blocksPerSide = (renderDistance * 16) + 1;
        double clampLowX = pos.x() - blocksPerSide;
        double clampLowZ = pos.z() - blocksPerSide;
        double clampHighX = pos.x() + blocksPerSide;
        double clampHighZ = pos.z() + blocksPerSide;

        return new AABB(Math.max(lowX, clampLowX), lowY, Math.max(lowZ, clampLowZ), Math.min(highX, clampHighX), highY, Math.min(highZ, clampHighZ));
    }
}
