package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.jcm.mod.Constants;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import org.mtr.MTRClient;
import org.mtr.registry.ObjectHolder;
import org.mtr.registry.RegistryClient;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Client-side registration facade for the MTR 4.1 static registry API.
 */
public class JCMRegistryClient {

    public static void setupPacketClient() {
        RegistryClient.setupPackets();
    }

    public static <U extends BlockEntity> void registerBlockEntityRenderer(ObjectHolder<BlockEntityType<U>> blockEntityType, BlockEntityRendererProvider<? super U> rendererInstance) {
        RegistryClient.registerBlockEntityRenderer(blockEntityType, rendererInstance);
    }

    public static void registerBlockRenderType(RenderType renderLayer, ObjectHolder<Block>... blocks) {
        for(ObjectHolder<Block> block : blocks) {
            RegistryClient.registerBlockRenderType(renderLayer, block);
        }
    }

    public static void registerStationColoredBlock(ObjectHolder<Block>... blocks) {
        RegistryClient.registerBlockColors((blockState, blockRenderView, blockPos, i) -> MTRClient.getStationColor(blockPos), blocks);
    }

    public static void register() {
        Events.registerClient();
        Blocks.registerClient();
        BlockEntityRenderers.registerClient();
        Networking.registerClient();
        KeyBinds.registerClient();
        LoaderImplClient.addPackOverride(Constants.id("jsblock_override"), Constants.MOD_ID);
    }
}
