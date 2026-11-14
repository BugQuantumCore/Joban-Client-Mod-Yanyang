package com.lx862.mtrscripting.core.integration;

import com.lx862.jcm.mapping.LoaderImplClient;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Minecraft;

public class EntityWrapper {
    private final Entity entity;

    @ApiInternal
    public EntityWrapper(Entity entity) {
        this.entity = entity;
    }

    public String displayName() {
        return this.entity.getDisplayName().getString();
    }

    public String name() {
        return this.entity.getName().getString();
    }

    public String uuid() {
        return this.entity.getStringUUID();
    }

    public boolean hasPermissionLevel(int permLevel) {
        return this.entity.hasPermissions(permLevel);
    }

    public ScriptVector3f pos() {
        return new ScriptVector3f(this.entity.position());
    }

    public ScriptVector3f smoothPos() {
        ScriptVector3f vec = new ScriptVector3f(this.entity.getEyePosition(Minecraft.getInstance().isPaused() ? 0 : Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
        vec.sub(new ScriptVector3f(0, entity.getEyeHeight(), 0));
        return vec;
    }

    public ScriptVector3f blockPos() {
        return new ScriptVector3f(this.entity.blockPosition());
    }

    public float yaw() {
        return entity.getViewYRot(Minecraft.getInstance().isPaused() ? 0 : Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    public float pitch() {
        return entity.getViewXRot(Minecraft.getInstance().isPaused() ? 0 : Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    public float bodyYaw() {
        LivingEntity livingEntity = (LivingEntity)entity;
        return livingEntity.yBodyRot;
    }

    public ScriptVector3f velocity() {
        return new ScriptVector3f(LoaderImplClient.getEntityVelocity(entity));
    }
}
