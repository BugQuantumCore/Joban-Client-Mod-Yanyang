package com.lx862.jcm.mapping;

#if MC_VERSION >= "11903"
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
#else
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket;
#endif

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.loading.FMLPaths;
import com.lx862.jcm.mod.compat.SoundHelper;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(Level world, BlockPos pos) {
        return world.isRainingAt(pos);
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }

    /** Get a block settings forcing it to be solid, as we don't want water to break our block. */
    public static BlockBehaviour.Properties getSolidBlockSettings(BlockBehaviour.Properties settings) {
        #if MC_VERSION >= "12001"
            return new BlockBehaviour.Properties(settings.forceSolidOn());
        #else
            return settings;
        #endif
    }

    public static Item getItemFromId(ResourceLocation id) {
        #if MC_VERSION < "11903"
            final Optional<net.minecraft.world.item.Item> itm;
            itm = net.minecraft.core.Registry.ITEM.getOptional(id);
        #else
            final Optional<net.minecraft.world.item.Item> itm;
            itm = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id);
        #endif
        return itm.map(Item::new).orElse(null);
    }

    public static ResourceLocation getIdFromItem(Item itm) {
        #if MC_VERSION < "11903"
            return ResourceLocation.parse(net.minecraft.core.Registry.ITEM.getKey(itm));
        #else
            return ResourceLocation.parse(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itm));
        #endif
    }

    public static int getRedstoneLevel(Level world, BlockPos blockPos) {
        return world.getBestNeighborSignal(blockPos);
    }

    public static Style withClipboardContentText(Style style, String content) {
        return new Style(style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, content)));
    }

    public static Style withURLContentText(Style style, String urlContent) {
        return new Style(style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlContent)));
    }

    public static Style withHoverContentText(Style style, MutableComponent content) {
        return new Style(style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, content)));
    }

    public static void playSound(Level world, ServerPlayer playerEntity, ResourceLocation soundId, SoundSource soundCategory, BlockPos blockPos, float volume, float pitch) {
        #if MC_VERSION >= "11903"
        SoundEvent event = SoundHelper.createSoundEvent(soundId);
        playerEntity.connection.send(
                new ClientboundSoundPacket(Holder.direct(event), soundCategory, blockPos.getX(), blockPos.getY(), blockPos.getZ(), volume, pitch, world.getRandom().nextLong())
        );
        #elif MC_VERSION >= "11900"
        playerEntity.connection.send(new ClientboundCustomSoundPacket(soundId, soundCategory, new net.minecraft.world.phys.Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), volume, 1, world.getRandom().nextLong()));
        #else
        playerEntity.connection.send(new ClientboundCustomSoundPacket(soundId, soundCategory, new net.minecraft.world.phys.Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), volume, 1));
        #endif
    }

    public static String getBlockStateValue(BlockState blockState, String stateName) {
        Property property = blockState.getBlock().getStateDefinition().getProperty(stateName);
        if(property == null) return null;
        return property.getName(blockState.getValue(property));
    }
}
