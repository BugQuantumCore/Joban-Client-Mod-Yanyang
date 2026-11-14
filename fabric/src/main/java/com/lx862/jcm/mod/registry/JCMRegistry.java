package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.mtr.block.BlockEntityExtension;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketHandler;
import org.mtr.registry.ObjectHolder;
import org.mtr.registry.RegistryServer;

import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;

/**
 * MTR 4.1 registration facade. The 4.0-era instance-based
 * {@code org.mtr.mapping.registry.Registry} facade is gone; this class keeps
 * the JCM call surface and delegates to the static {@link RegistryServer} /
 * {@code EventRegistryServer} APIs.
 */
public class JCMRegistry {

    public static ObjectHolder<Block> registerBlockItem(String id, Function<BlockBehaviour.Properties, Block> factory, String itemGroup) {
        final ObjectHolder<Block> block = RegistryServer.registerBlock(id, factory);
        if (itemGroup != null) {
            RegistryServer.registerItem(id, settings -> new BlockItem(block.get(), settings), itemGroup);
        }
        return block;
    }

    public static ObjectHolder<Block> registerBlockItem(ResourceLocation id, Function<BlockBehaviour.Properties, Block> factory, String itemGroup) {
        return registerBlockItem(id.getPath(), factory, itemGroup);
    }

    public static ObjectHolder<Block> registerBlock(String id, Function<BlockBehaviour.Properties, Block> factory) {
        return RegistryServer.registerBlock(id, factory);
    }

    public static ObjectHolder<Block> registerBlock(ResourceLocation id, Function<BlockBehaviour.Properties, Block> factory) {
        return registerBlock(id.getPath(), factory);
    }

    public static ObjectHolder<Item> registerItem(String id, Function<Item.Properties, Item> callback, String itemGroup) {
        return RegistryServer.registerItem(id, callback, itemGroup);
    }

    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> ObjectHolder<BlockEntityType<T>> registerBlockEntity(String id, BiFunction<BlockPos, BlockState, T> constructor, ObjectHolder<Block> associatedBlock) {
        return RegistryServer.registerBlockEntityType(id, constructor, associatedBlock::get);
    }

    public static String registerItemGroup(String id, java.util.function.Supplier<net.minecraft.world.item.ItemStack> iconSupplier) {
        return RegistryServer.registerItemGroup(id, iconSupplier);
    }

    public static void setupPacket() {
        RegistryServer.setupPackets();
    }

    public static <T extends PacketHandler> void registerPacket(Class<T> classObject, Function<PacketBufferReceiver, T> getInstance) {
        RegistryServer.registerPacket(classObject, getInstance);
    }

    /**
     * Mirrors MTR's default block settings (push reaction by piston interaction
     * plus default strength); the 4.0-era equivalent lived in
     * {@code org.mtr.registry.Blocks} and is private in MTR 4.1.
     */
    public static BlockBehaviour.Properties createDefaultBlockSettings(boolean blockPiston) {
        return BlockBehaviour.Properties.of().pushReaction(blockPiston ? PushReaction.BLOCK : PushReaction.NORMAL).strength(3);
    }

    public static BlockBehaviour.Properties createDefaultBlockSettings(boolean blockPiston, java.util.function.ToIntFunction<BlockState> luminance) {
        return createDefaultBlockSettings(blockPiston).lightLevel(luminance);
    }

    public static void register() {
        Blocks.register();
        BlockEntities.register();
        Items.register();
        Events.register();
        Networking.register();
        RegistryServer.init();
    }
}
