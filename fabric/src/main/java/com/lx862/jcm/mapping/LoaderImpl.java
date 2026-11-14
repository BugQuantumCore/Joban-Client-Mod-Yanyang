package com.lx862.jcm.mapping;

import com.lx862.jcm.mod.compat.SoundHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Fabric implementation, Mojang mappings (1.21.x)
 */
public class LoaderImpl {
    public static boolean isRainingAt(Level world, BlockPos pos) {
        return world.isRainingAt(pos);
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }

    /** Get a block settings forcing it to be solid, as we don't want water to break our block. */
    public static net.minecraft.world.level.block.state.BlockBehaviour.Properties getSolidBlockSettings(net.minecraft.world.level.block.state.BlockBehaviour.Properties settings) {
        return settings;
    }

    public static Item getItemFromId(ResourceLocation id) {
        final Optional<Item> itm = Optional.ofNullable(BuiltInRegistries.ITEM.get(id));
        return itm.orElse(null);
    }

    public static ResourceLocation getIdFromItem(Item itm) {
        return BuiltInRegistries.ITEM.getKey(itm);
    }

    public static int getRedstoneLevel(Level world, BlockPos blockPos) {
        return world.getBestNeighborSignal(blockPos);
    }

    public static Style withClipboardContentText(Style style, String content) {
        return style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, content));
    }

    public static Style withURLContentText(Style style, String urlContent) {
        return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlContent));
    }

    public static Style withHoverContentText(Style style, MutableComponent content) {
        return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, content));
    }

    public static void playSound(Level world, ServerPlayer playerEntity, ResourceLocation soundId, SoundSource soundCategory, BlockPos blockPos, float volume, float pitch) {
        final SoundEvent event = SoundHelper.createSoundEvent(soundId);
        playerEntity.connection.send(
                new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(event), soundCategory, blockPos.getX(), blockPos.getY(), blockPos.getZ(), volume, pitch, world.getRandom().nextLong())
        );
    }

    public static String getBlockStateValue(BlockState blockState, String stateName) {
        Property<?> property = blockState.getBlock().getStateDefinition().getProperty(stateName);
        if (property == null) return null;
        return getNameFromProperty(blockState, property);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String getNameFromProperty(BlockState blockState, Property property) {
        final Comparable<?> value = blockState.getValue(property);
        return property.getName(value);
    }
}
