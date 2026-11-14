package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.APGDoorDRLBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.mtr.block.BlockAPGGlass;
import org.mtr.block.BlockAPGGlassEnd;
import org.mtr.block.BlockPSDAPGDoorBase;
import org.mtr.block.IBlock;
import org.mtr.block.TripleHorizontalBlock;
import org.mtr.block.IBlock;

import org.mtr.render.BlockEntityRendererExtension;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.tool.Drawing;

import java.util.function.Consumer;

/**
 * DRL APG door renderer, ported to the MTR 4.1 render pipeline. The
 * 4.0-era mapping-layer EntityModel/ModelPart wrappers are replaced by the
 * same vanilla-ModelPart pattern MTR 4.1's own RenderPSDAPGDoor uses.
 */
public class RenderDRLAPGDoor<T extends APGDoorDRLBlockEntity> extends BlockEntityRendererExtension<T> {

    private static final ResourceLocation TEXTURE_APG_DOOR_LIGHT_OFF = ResourceLocation.fromNamespaceAndPath("mtr", "textures/block/apg_door_light_off.png");
    private static final ResourceLocation TEXTURE_APG_DOOR_LIGHT_ON = ResourceLocation.fromNamespaceAndPath("mtr", "textures/block/apg_door_light_on.png");
    private static final ResourceLocation TEXTURE_APG_DOOR_TOP_LEFT = ResourceLocation.fromNamespaceAndPath("jsblock", "textures/block/apg_door_1_top_left.png");
    private static final ResourceLocation TEXTURE_APG_DOOR_TOP_RIGHT = ResourceLocation.fromNamespaceAndPath("jsblock", "textures/block/apg_door_1_top_right.png");
    private static final ResourceLocation TEXTURE_APG_DOOR_BOTTOM_LEFT = ResourceLocation.fromNamespaceAndPath("jsblock", "textures/block/apg_door_1_bottom_left.png");
    private static final ResourceLocation TEXTURE_APG_DOOR_BOTTOM_RIGHT = ResourceLocation.fromNamespaceAndPath("jsblock", "textures/block/apg_door_1_bottom_right.png");
    private static final ResourceLocation TEXTURE_APG_DOOR_LOCKED = ResourceLocation.fromNamespaceAndPath("jsblock", "textures/block/apg_door_1_locked.png");

    private static ResourceLocation drlapgDoorTexture(boolean half, boolean side) {
        if (half) return side ? TEXTURE_APG_DOOR_TOP_RIGHT : TEXTURE_APG_DOOR_TOP_LEFT;
        return side ? TEXTURE_APG_DOOR_BOTTOM_RIGHT : TEXTURE_APG_DOOR_BOTTOM_LEFT;
    }

    private final int type;

    private static final ModelPart MODEL_APG_TOP = createSingleCube(34, 9, 0, 15, 1, 16, 8, 1);
    private static final ModelPart MODEL_APG_BOTTOM = createModelPart(parent -> {
        createModelPartData(parent, 0, 0, -8, -16, -7, 16, 16, 1, 0, 0, 0, 0, 0, 0, 0);
        createModelPartData(parent, 0, 17, -8, -6, -8, 16, 6, 1, 0, 0, 0, 0, 0, 0, 0);
        createModelPartData(parent, 0, 24, -8, -2, 0, 16, 2, 1, 0, 0, -6, -8, -0.7854F, 0, 0);
    }, 34, 27);
    private static final ModelPart MODEL_APG_LIGHT = createModelPart(parent -> {
        createModelPartData(parent, 0, 4, -0.5F, -9, -7, 1, 1, 3, 0.05F, 0, 0, 0, 0, 0, 0);
        createModelPartData(parent, 0, 0, -0.5F, 0.05F, -3.05F, 1, 1, 3, 0.05F, 0, -9.05F, -4.95F, 0.3927F, 0, 0);
    }, 8, 8);
    private static final ModelPart MODEL_APG_DOOR_LOCKED = createSingleCube(6, 6, 5, 17, 1, 6, 6, 0);

    public RenderDRLAPGDoor(BlockEntityRendererProvider.Context dispatcher, int type) {
        this.type = type;
    }

    @Override
    public void render(T entity, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, ClientLevel clientWorld, LocalPlayer clientPlayer, float tickDelta, int light, int overlay) {
        final Level world = entity.getLevel();
        if (world == null) {
            return;
        }

        final BlockPos blockPos = entity.getBlockPos();
        final Direction facing = IBlock.getStatePropertySafe(world, blockPos, com.lx862.jcm.mod.data.BlockProperties.FACING);
        final boolean side = IBlock.getStatePropertySafe(world, blockPos, IBlock.SIDE) == IBlock.EnumSide.RIGHT;
        final boolean half = IBlock.getStatePropertySafe(world, blockPos, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
        final boolean unlocked = IBlock.getStatePropertySafe(world, blockPos, BlockPSDAPGDoorBase.UNLOCKED);
        final double open = Math.min(entity.getDoorValue(), type >= 3 ? 0.75F : 1);

        final StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations(0.5 + blockPos.getX(), blockPos.getY(), 0.5 + blockPos.getZ());
        storedMatrixTransformations.add(matrixStackNew -> {
            Drawing.rotateYDegrees(matrixStackNew, -facing.toYRot());
            Drawing.rotateXDegrees(matrixStackNew, 180);
        });
        final StoredMatrixTransformations storedMatrixTransformationsLight = storedMatrixTransformations.copy();

        switch (type) {
            case 2:
                if (half) {
                    final Block block = world.getBlockState(blockPos.relative(side ? facing.getClockWise() : facing.getCounterClockWise())).getBlock();
                    if (block instanceof BlockAPGGlass || block instanceof BlockAPGGlassEnd) {
                        MainRenderer.scheduleRender(open > 0 ? TEXTURE_APG_DOOR_LIGHT_ON : TEXTURE_APG_DOOR_LIGHT_OFF, false, open > 0 ? QueuedRenderLayer.LIGHT_TRANSLUCENT : QueuedRenderLayer.EXTERIOR, (matrixStackNew, vertexConsumer, offset) -> {
                            storedMatrixTransformationsLight.transform(matrixStackNew, offset);
                            matrixStackNew.translate(side ? -0.515625 : 0.515625, 0, 0);
                            matrixStackNew.scale(0.5F, 1, 1);
                            MODEL_APG_LIGHT.render(matrixStackNew, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
                            matrixStackNew.popPose();
                        });
                    }
                }
                break;
        }

        storedMatrixTransformations.add(matrixStackNew -> matrixStackNew.translate(open * (side ? -1 : 1), 0, 0));

        switch (type) {
            case 2:
                MainRenderer.scheduleRender(drlapgDoorTexture(half, side), false, QueuedRenderLayer.EXTERIOR, (matrixStackNew, vertexConsumer, offset) -> {
                    storedMatrixTransformations.transform(matrixStackNew, offset);
                    (half ? MODEL_APG_TOP : MODEL_APG_BOTTOM).render(matrixStackNew, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
                    matrixStackNew.popPose();
                });
                if (half && !unlocked) {
                    MainRenderer.scheduleRender(TEXTURE_APG_DOOR_LOCKED, false, QueuedRenderLayer.EXTERIOR, (matrixStackNew, vertexConsumer, offset) -> {
                        storedMatrixTransformations.transform(matrixStackNew, offset);
                        MODEL_APG_DOOR_LOCKED.render(matrixStackNew, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
                        matrixStackNew.popPose();
                    });
                }
                break;
            case 4:
                if (IBlock.getStatePropertySafe(world, blockPos, TripleHorizontalBlock.CENTER)) {
                    break;
                }
                storedMatrixTransformations.add(matrixStackNew -> matrixStackNew.translate(side ? 0.5 : -0.5, 0, 0));
        }
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 position) {
        return true;
    }

    /* The same ModelPart factories MTR 4.1's RenderPSDAPGDoor uses, kept
       package-local here so the DRL door keeps rendering its textured cuboids
       through the vanilla part renderer. */
    private static ModelPart createSingleCube(int textureWidth, int textureHeight, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        return createModelPart(parent -> createModelPartData(parent, 0, 0, x - 8, y - 16, z - 8, sizeX, sizeY, sizeZ, 0, 0, 0, 0, 0, 0, 0), textureWidth, textureHeight);
    }

    private static ModelPart createModelPart(Consumer<PartDefinition> builder, int textureWidth, int textureHeight) {
        final net.minecraft.client.model.geom.builders.MeshDefinition modelData = new net.minecraft.client.model.geom.builders.MeshDefinition();
        builder.accept(modelData.getRoot());
        return net.minecraft.client.model.geom.builders.LayerDefinition.create(modelData, textureWidth, textureHeight).bakeRoot();
    }

    private static void createModelPartData(PartDefinition parent, int textureX, int textureY, float offsetX, float offsetY, float offsetZ, float sizeX, float sizeY, float sizeZ, float dilation, float pivotX, float pivotY, float pivotZ, float rotateX, float rotateY, float rotateZ) {
        parent.addOrReplaceChild("part", CubeListBuilder.create()
                .texOffs(textureX, textureY)
                .addBox(offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, new net.minecraft.client.model.geom.builders.CubeDeformation(dilation)),
                PartPose.offsetAndRotation(pivotX, pivotY, pivotZ, rotateX, rotateY, rotateZ));
    }
}
