package com.lx862.mtrscripting.core.integration;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/MinecraftClientUtil.java */

import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.mojang.text2speech.Narrator;
import java.util.List;
import com.lx862.jcm.mod.compat.MinecraftClientHelper;
import com.lx862.jcm.mod.compat.MinecraftServerHelper;
import com.lx862.jcm.mod.compat.TextHelper;
import com.lx862.jcm.mod.compat.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public class MinecraftClientWrapper {

    private MinecraftClientWrapper() {}

    public static boolean worldIsRaining() {
        return Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.isRaining();
    }

    public static boolean worldIsThundering() {
        return Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.isThundering();
    }

    public static boolean worldIsRainingAt(ScriptVector3f pos) {
        return Minecraft.getInstance().level != null
                && LoaderImpl.isRainingAt((Level)Minecraft.getInstance().level, pos.rawBlockPos());
    }

    public static int worldDayTime() {
        return Minecraft.getInstance().level != null
                ? (int) WorldHelper.getTimeOfDay(Minecraft.getInstance().level) : 0;
    }

    public static boolean getDirectSignal(ScriptVector3f pos) {
        for(Direction direction : Direction.values()) {
            if(getDirectSignal(pos, direction)) return true;
        }
        return false;
    }

    public static boolean getDirectSignal(ScriptVector3f pos, Direction direction) {
        return Minecraft.getInstance().level != null &&
                Minecraft.getInstance().level.getDirectSignal(pos.rawBlockPos(), direction) > 0;
    }

    public static void narrate(String message) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getNarrator().say(net.minecraft.network.chat.Component.literal(message));
        });
    }

    public static int blockLightAt(ScriptVector3f pos) {
        return Minecraft.getInstance().level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos.rawBlockPos());
    }

    public static int skyLightAt(ScriptVector3f pos) {
        return Minecraft.getInstance().level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos.rawBlockPos());
    }

    public static int lightAt(ScriptVector3f pos) {
        int blockLight = blockLightAt(pos);
        int skyLight = skyLightAt(pos);
        return net.minecraft.client.renderer.LightTexture.pack(blockLight, skyLight);
    }

    public static int lightLevelAt(ScriptVector3f pos) {
        return Math.min(blockLightAt(pos), skyLightAt(pos));
    }

    public static int getRedstoneLevel(ScriptVector3f pos) {
        return LoaderImpl.getRedstoneLevel((Level)Minecraft.getInstance().level, pos.rawBlockPos());
    }

    public static String getCurrentWorldId() {
        ResourceLocation id = MinecraftServerHelper.getWorldId((Level)Minecraft.getInstance().level);
        return id.getNamespace() + ":" + id.getPath();
    }

    public static String getWorldBlockState(ScriptVector3f blockPos, String stateName) {
        BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos.rawBlockPos());
        return LoaderImpl.getBlockStateValue(blockState, stateName);
    }

    public static PlayerEntityWrapper localPlayer() {
        LocalPlayer clientPlayerEntity = Minecraft.getInstance().player;
        if(clientPlayerEntity == null) return null;
        return new PlayerEntityWrapper((Player)clientPlayerEntity);
    }

    public static List<PlayerEntityWrapper> getWorldPlayers() {
        LocalPlayer clientPlayerEntity = Minecraft.getInstance().player;
        if(clientPlayerEntity == null) return null;

        int maxDistance = (renderDistance() + 2) * 16;
        return LoaderImplClient.getWorldPlayers(Minecraft.getInstance().level).stream().filter(e -> {
            return e.position().distanceTo(clientPlayerEntity.position()) <= maxDistance;
        }).map(e -> new PlayerEntityWrapper((Player)e)).toList();
    }

    public static Integer getScoreboardScore(String objectiveName, String playerName) {
        final ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return null;
        final net.minecraft.world.scores.Scoreboard scoreboard = world.getScoreboard();
        final net.minecraft.world.scores.Objective objective = scoreboard.getObjective(objectiveName);
        if(objective == null) return null;
        return scoreboard.getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly(playerName), objective).value();
    }

    public static int getCameraMode() {
        return LoaderImplClient.getCameraMode();
    }

    public static boolean gamePaused() {
        return Minecraft.getInstance().isPaused();
    }

    public static void displayMessage(String message, boolean actionBar) {
        displayMessage((TextHelper.literal(message)), actionBar);
    }

    public static void displayMessage(VanillaTextWrapper vanillaTextWrapper, boolean actionBar) {
        displayMessage((vanillaTextWrapper.impl()), actionBar);
    }

    public static int renderDistance() {
        return MinecraftClientHelper.getRenderDistance();
    }

    public static void spawnParticleInWorld(ResourceLocation particleId, ScriptVector3f pos, ScriptVector3f velocity) {
        spawnParticleInWorld(particleId, pos, velocity, false);
    }

    public static void spawnParticleInWorld(ResourceLocation particleId, ScriptVector3f pos, ScriptVector3f velocity, boolean alwaysSpawn) {
        if(Minecraft.getInstance().level != null) {
            ParticleOptions particleEffect = MinecraftParticleData.getData(particleId);
            if(particleEffect != null) {
                Minecraft.getInstance().submit(() -> {
                    Minecraft.getInstance().level.addParticle(particleEffect, alwaysSpawn, pos.x(), pos.y(), pos.z(), velocity.x(), velocity.y(), velocity.z());
                });
            } else {
                throw new IllegalArgumentException("Unknown particle " + particleId + "!");
            }
        }
    }

    private static void displayMessage(net.minecraft.network.chat.Component text, boolean actionBar) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Minecraft.getInstance().execute(() -> {
                player.displayClientMessage(text, actionBar);
            });
        }
    }
}