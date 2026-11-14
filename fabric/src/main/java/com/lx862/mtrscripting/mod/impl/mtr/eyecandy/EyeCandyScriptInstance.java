package com.lx862.mtrscripting.mod.impl.mtr.eyecandy;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.integration.MinecraftClientWrapper;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.mod.gui.hud.SortableScriptInstance;
import net.minecraft.client.Minecraft;
import org.mtr.block.BlockEyeCandy;

import java.util.Objects;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EyeCandyScriptInstance extends ScriptInstance<EyecandyBlockEntityWrapper> implements SortableScriptInstance {
    private ScriptRenderManager renderManager;
    private ScriptSoundManager soundManager;
    private final BlockEyeCandy.EyeCandyBlockEntity be;

    public EyeCandyScriptInstance(BlockEyeCandy.EyeCandyBlockEntity blockEntity, EyeCandyScriptContext context, ParsedScript script) {
        super(context, script);
        this.be = blockEntity;
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public void updateRenderer(ScriptRenderManager renderManager) {
        /* Swap, not copy (see LiftScriptInstance). */
        this.renderManager.takeFrom(renderManager);
    }

    public void updateSound(ScriptSoundManager soundManager) {
        this.soundManager.takeFrom(soundManager);
    }

    public ScriptSoundManager getSoundManager() {
        return this.soundManager;
    }

    public ScriptRenderManager getRenderManager() {
        return this.renderManager;
    }

    public boolean shouldInvalidate() {
        boolean beRemoved = be.getLevel().getBlockEntity(be.getBlockPos()) == null;
        boolean modelChanged = getWrapperObject() != null && getWrapperObject().modelChanged(getContextObject().getName());
        boolean notInGame = Minecraft.getInstance().level == null;
        boolean shouldInvalidate = notInGame || beRemoved || modelChanged;
        return shouldInvalidate;
    }

    @Override
    public int getSortScore() {
        return (int)(MinecraftClientWrapper.localPlayer().pos().distance(new ScriptVector3f(be.getBlockPos())) * 1000);
    }
}
