package com.lx862.mtrscripting.core.integration;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.HashMap;
import java.util.Map;

@ApiInternal
public class MinecraftParticleData {
    private static final Map<ResourceLocation, ParticleOptions> particleTypeMap = new HashMap<>();
    public static ParticleOptions[] particleTypes = new ParticleOptions[] {
            ParticleTypes.ITEM_SLIME,
            ParticleTypes.ASH,
            ParticleTypes.FISHING,
            ParticleTypes.TOTEM_OF_UNDYING,
            ParticleTypes.FIREWORK,
            ParticleTypes.DAMAGE_INDICATOR,
            ParticleTypes.SQUID_INK,
            ParticleTypes.COMPOSTER,
            ParticleTypes.BUBBLE_POP,
            ParticleTypes.EXPLOSION_EMITTER,
            ParticleTypes.NAUTILUS,
            ParticleTypes.WITCH,
            ParticleTypes.SPLASH,
            ParticleTypes.ENCHANTED_HIT,
            ParticleTypes.PORTAL,
            ParticleTypes.SPIT,
            ParticleTypes.FALLING_OBSIDIAN_TEAR,
            ParticleTypes.SWEEP_ATTACK,
            ParticleTypes.LANDING_LAVA,
            ParticleTypes.FALLING_LAVA,
            ParticleTypes.SOUL_FIRE_FLAME,
            ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
            ParticleTypes.REVERSE_PORTAL,
            ParticleTypes.EXPLOSION,
            ParticleTypes.SNOWFLAKE,
            ParticleTypes.BUBBLE_COLUMN_UP,
            ParticleTypes.END_ROD,
            ParticleTypes.FALLING_NECTAR,
            ParticleTypes.HEART,
            ParticleTypes.POOF,
            ParticleTypes.HAPPY_VILLAGER,
            ParticleTypes.DRIPPING_HONEY,
            ParticleTypes.FLASH,
            ParticleTypes.DRIPPING_WATER,
            ParticleTypes.EFFECT,
            ParticleTypes.WARPED_SPORE,
            ParticleTypes.SOUL,
            ParticleTypes.NOTE,
            ParticleTypes.CAMPFIRE_COSY_SMOKE,
            ParticleTypes.DRIPPING_LAVA,
            ParticleTypes.CRIT,
            ParticleTypes.CLOUD,
            ParticleTypes.ENCHANT,
            ParticleTypes.LAVA,
            ParticleTypes.UNDERWATER,
            ParticleTypes.DOLPHIN,
            ParticleTypes.SMOKE,
            ParticleTypes.BUBBLE,
            ParticleTypes.CRIMSON_SPORE,
            ParticleTypes.LARGE_SMOKE,
            ParticleTypes.FLAME,
            ParticleTypes.MYCELIUM,
            ParticleTypes.INSTANT_EFFECT,
            ParticleTypes.FALLING_HONEY,
            ParticleTypes.DRAGON_BREATH,
            ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
            ParticleTypes.LANDING_HONEY,
            ParticleTypes.RAIN,
            ParticleTypes.ANGRY_VILLAGER,
            ParticleTypes.FALLING_WATER,
            ParticleTypes.LANDING_OBSIDIAN_TEAR,
            ParticleTypes.ELDER_GUARDIAN,
            ParticleTypes.CURRENT_DOWN,
            ParticleTypes.WHITE_ASH,
            ParticleTypes.SNEEZE
    };

    public static ParticleOptions getData(ResourceLocation particleId) {
        return particleTypeMap.get(particleId);
    }

    public static void init() {
        for(ParticleOptions particleType : particleTypes) {
            particleTypeMap.put(net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(particleType.getType()), particleType);
        }
    }
}
