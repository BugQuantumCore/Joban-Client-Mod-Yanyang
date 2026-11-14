package com.lx862.jcm.mod.scripting.psd;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.integration.MinecraftClientWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.gui.hud.SortableScriptInstance;
import com.lx862.jcm.mod.scripting.pids.PIDSWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.mtr.block.BlockPSDAPGGlassBase;
import org.mtr.block.BlockPSDTop;
import net.minecraft.network.chat.Component;

/**
 * One running "scripted PIDS preset bound to a PSD block" instance — the
 * wrapper the script sees is a synthesized {@link PIDSWrapper} (no
 * {@code PIDSBlockEntity} exists at a PSD), while the drawing context is the
 * PSD context, so the preset's Text/Texture/Rect calls replay onto the PSD
 * surface unchanged.
 */
@ApiInternal
public class PSDPidsScriptInstance extends ScriptInstance<PIDSWrapper> implements SortableScriptInstance {
    private final BlockPos blockPos;
    private final PSDWrapper.PSDType type;
    private final Object worldData;
    private ScriptSoundManager soundManager;
    private ScriptRenderManager renderManager;

    public PSDPidsScriptInstance(PSDBinding binding, ParsedScript script) {
        super(new PSDScriptContext(binding.scriptId), script);
        this.blockPos = binding.pos;
        this.type = binding.type;
        final net.minecraft.client.multiplayer.ClientLevel world = Minecraft.getInstance().level;
        this.worldData = world == null ? null : world;
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public void saveRenderCalls(ScriptRenderManager renderManager) {
        this.renderManager.takeFrom(renderManager);
    }

    public void saveSoundCalls(ScriptSoundManager soundManager) {
        this.soundManager.takeFrom(soundManager);
    }

    public ScriptSoundManager getSoundManager() {
        return this.soundManager;
    }

    public ScriptRenderManager getRenderManager() {
        return this.renderManager;
    }

    @Override
    public boolean shouldInvalidate() {
        final net.minecraft.client.multiplayer.ClientLevel clientWorld = Minecraft.getInstance().level;
        if (clientWorld == null || clientWorld != worldData) return true;
        final Level world = (Level)clientWorld;
        final BlockState state = world.getBlockState(blockPos);
        final Object block = state.getBlock();
        final boolean stillPsd = type == PSDWrapper.PSDType.TOP
                ? block instanceof BlockPSDTop : block instanceof BlockPSDAPGGlassBase;
        return !stillPsd || !PSDClientBindings.isSameBinding(blockPos, getContextObject().getName(), true, type);
    }

    @Override
    public int getSortScore() {
        return (int) (MinecraftClientWrapper.localPlayer().pos().distance(new ScriptVector3f(blockPos)) * 1000);
    }
}
