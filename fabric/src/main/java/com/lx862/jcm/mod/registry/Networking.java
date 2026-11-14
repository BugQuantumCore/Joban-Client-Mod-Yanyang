package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.network.block.*;
import com.lx862.jcm.mod.network.gui.*;
import com.lx862.jcm.mod.util.JCMLogger;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.mtr.packet.PacketHandler;
import org.mtr.packet.PacketBufferReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.mtr.registry.RegistryClient;
import org.mtr.registry.RegistryServer;

public class Networking {
    private static final List<String> registeredPackets = new ArrayList<>();

    public static void register() {
        JCMLogger.debug("Registering network packets...");
        JCMRegistry.setupPacket();

        // Block Entity Update
        registerPacket(ButterflyLightUpdatePacket.class, ButterflyLightUpdatePacket::new);
        registerPacket(EnquiryUpdateGUIPacket.class, EnquiryUpdateGUIPacket::new);
        registerPacket(FareSaverUpdatePacket.class, FareSaverUpdatePacket::new);
        registerPacket(OperatorButtonUpdatePacket.class, OperatorButtonUpdatePacket::new);
        registerPacket(PIDSUpdatePacket.class, PIDSUpdatePacket::new);
        registerPacket(PIDSProjectorUpdatePacket.class, PIDSProjectorUpdatePacket::new);
        registerPacket(SoundLooperUpdatePacket.class, SoundLooperUpdatePacket::new);
        registerPacket(SubsidyMachineUpdatePacket.class, SubsidyMachineUpdatePacket::new);

        // GUI Screen
        registerPacket(ButterflyLightGUIPacket.class, ButterflyLightGUIPacket::new);
        registerPacket(FareSaverGUIPacket.class, FareSaverGUIPacket::new);
        registerPacket(OperatorButtonGUIPacket.class, OperatorButtonGUIPacket::new);
        registerPacket(PIDSGUIPacket.class, PIDSGUIPacket::new);
        registerPacket(PIDSProjectorGUIPacket.class, PIDSProjectorGUIPacket::new);
        registerPacket(SoundLooperGUIPacket.class, SoundLooperGUIPacket::new);
        registerPacket(SubsidyMachineGUIPacket.class, SubsidyMachineGUIPacket::new);
    }

    public static void registerClient() {
        JCMRegistryClient.setupPacketClient();
    }

    public static <T extends PacketHandler> void registerPacket(Class<T> classObject, Function<PacketBufferReceiver, T> getInstance) {
        // Keep track of the registered packets, so we can warn if we try to send a non-registered packet
        // I believe Minecraft Mapping itself already keeps track of the registered packets, it just doesn't warn
        registeredPackets.add(classObject.getName());
        JCMRegistry.registerPacket(classObject, getInstance);
    }

    public static <T extends PacketHandler> void sendPacketToServer(T data) {
        if(!registeredPackets.contains(data.getClass().getName())) {
            JCMLogger.warn("Non-registered packets is sent: " + data.getClass().getName());
            JCMLogger.warn("Consider registering in method: mod/registry/Networking.register()");
        }
        RegistryClient.sendPacketToServer(data);
    }

    public static <T extends PacketHandler> void sendPacketToClient(Player player, T data) {
        if(!registeredPackets.contains(data.getClass().getName())) {
            JCMLogger.warn("Non-registered packets is sent: " + data.getClass().getName());
            JCMLogger.warn("Consider registering in method: mod/registry/Networking.register()");
        }
        RegistryServer.sendPacketToClient((ServerPlayer)player, data);
    }
}
