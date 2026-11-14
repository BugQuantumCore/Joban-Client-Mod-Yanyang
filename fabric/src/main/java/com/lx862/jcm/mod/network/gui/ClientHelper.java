package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.data.EnquiryScreenType;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.gui.screen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Because Network Packet class is shared between Client and Server, they must not contain any client code
 * This serves as an abstraction to perform client-specific operation.
 */
public final class ClientHelper {

    public static void openButterflyLightScreen(BlockPos blockPos, int secondsToBlink) {
		Minecraft.getInstance().setScreen(new ButterflyLightScreen(blockPos, secondsToBlink));
	}

	public static void openFareSaverGUIScreen(BlockPos blockPos, String prefix, int discount) {
		Minecraft.getInstance().setScreen(new FareSaverScreen(blockPos, prefix, discount));
	}

	public static void openPIDSGUIScreen(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
		Minecraft.getInstance().setScreen(new PIDSScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId));
	}

	public static void openPIDSProjectorGUIScreen(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId, double x, double y, double z, double rotateX, double rotateY, double rotateZ, double scale) {
		Minecraft.getInstance().setScreen(new PIDSProjectorScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId, x, y, z, rotateX, rotateY, rotateZ, scale));
	}

	public static void openSoundLooperGUIScreen(BlockPos blockPos, BlockPos corner1, BlockPos corner2, String soundId, int soundCategory, float soundVolume, int interval, boolean needRedstone, boolean limitRange) {
		Minecraft.getInstance().setScreen(new SoundLooperScreen(blockPos, corner1, corner2, soundId, soundCategory, soundVolume, interval, needRedstone, limitRange));
	}

	public static void openSubsidyMachineGUIScreen(BlockPos blockPos, int pricePerUse,int cooldown) {
		Minecraft.getInstance().setScreen(new SubsidyMachineScreen(blockPos, pricePerUse, cooldown));
	}

	public static void openOperatorButtonGUIScreen(BlockPos blockPos, boolean[] keyRequirements) {
		Minecraft.getInstance().setScreen(new OperatorButtonScreen(blockPos, keyRequirements));
	}

	public static void openEnquiryScreen(EnquiryScreenType type, BlockPos pos, List<TransactionEntry> entries, int remainingBalance) {
		if(type == EnquiryScreenType.RV) {
			Minecraft.getInstance().setScreen(new RVEnquiryScreen(pos, entries, remainingBalance));
		}
		if(type == EnquiryScreenType.CLASSIC) {
			Minecraft.getInstance().setScreen(new EnquiryScreen(pos, entries, remainingBalance));
		}
	}
}
