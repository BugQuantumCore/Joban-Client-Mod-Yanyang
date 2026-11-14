package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.registry.BlockEntities;
import com.lx862.jcm.mod.compat.MinecraftServerHelper;
import com.lx862.jcm.mod.compat.SoundHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class SoundLooperBlockEntity extends JCMBlockEntityBase {
    public static final SoundSource[] SOURCE_LIST = {SoundSource.MASTER, SoundSource.MUSIC, SoundSource.WEATHER, SoundSource.AMBIENT, SoundSource.PLAYERS, SoundSource.BLOCKS, SoundSource.VOICE};
    public static final int MIN_SOUND_RANGE = 64;
    public static final int MAX_SOUND_RANGE = 2000; // 2km range, should be good enough for most of the things tbh.
    private String soundID = "";
    private BlockPos corner1 = new BlockPos(0, 0, 0);
    private BlockPos corner2 = new BlockPos(0, 0, 0);
    private int repeatTick = 20;
    private float volume = 1;
    private int soundCategory = 0;
    private boolean needRedstone = false;
    private boolean limitRange = false;
    public SoundLooperBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.SOUND_LOOPER.get(), blockPos, blockState);
    }

    @Override
    public void readNbt(CompoundTag compoundTag) {
        super.readNbt(compoundTag);
        this.repeatTick = compoundTag.getInt("repeat_tick");
        this.soundID = compoundTag.getString("sound_id");
        this.soundCategory = compoundTag.getInt("sound_category");
        this.volume = compoundTag.getFloat("volume");
        this.needRedstone = compoundTag.getBoolean("need_redstone");
        this.limitRange = compoundTag.getBoolean("limit_range");
        this.corner1 = BlockPos.of(compoundTag.getLong("pos_1"));
        this.corner2 = BlockPos.of(compoundTag.getLong("pos_2"));
    }

    @Override
    public void writeNbt(CompoundTag compoundTag) {
        super.writeNbt(compoundTag);
        compoundTag.putInt("repeat_tick", repeatTick);
        compoundTag.putString("sound_id", soundID);
        compoundTag.putInt("sound_category", soundCategory);
        compoundTag.putFloat("volume", volume);
        compoundTag.putBoolean("need_redstone", needRedstone);
        compoundTag.putBoolean("limit_range", limitRange);
        compoundTag.putLong("pos_1", corner1.asLong());
        compoundTag.putLong("pos_2", corner2.asLong());
    }

    public void tick() {
        Level world = getLevel();

        if(repeatTick > 0 && !soundID.isEmpty() && world != null && !world.isClientSide() && JCMServerStats.getGameTick() % repeatTick == 0) {
            boolean bl1 = world.getDirectSignal(getBlockPos(), Direction.NORTH) > 0;
            boolean bl2 = world.getDirectSignal(getBlockPos(), Direction.EAST) > 0;
            boolean bl3 = world.getDirectSignal(getBlockPos(), Direction.SOUTH) > 0;
            boolean bl4 = world.getDirectSignal(getBlockPos(), Direction.WEST) > 0;
            boolean bl5 = world.getDirectSignal(getBlockPos(), Direction.UP) > 0;
            boolean bl6 = world.getDirectSignal(getBlockPos(), Direction.DOWN) > 0;
            boolean emittingRedstonePower = bl1 || bl2 || bl3 || bl4 || bl5 || bl6;
            if(needRedstone && !emittingRedstonePower) return;

            final SoundSource category = SOURCE_LIST[soundCategory];
            final ResourceLocation identifier;
            try {
                identifier = ResourceLocation.parse(soundID);
            } catch (Exception ignored) {
                return;
            }

            SoundEvent soundEvent = SoundHelper.createSoundEvent(identifier);

            if(!limitRange) {
                int broadcastRange = Math.min(MAX_SOUND_RANGE, Math.max(MIN_SOUND_RANGE, getLoopInterval()));
                AABB limitedRange = new AABB(getBlockPos().getX()-broadcastRange, getBlockPos().getY()-broadcastRange, getBlockPos().getZ()-broadcastRange, getBlockPos().getX()+broadcastRange, getBlockPos().getY()+broadcastRange, getBlockPos().getZ()+broadcastRange);

                MinecraftServerHelper.iteratePlayers((ServerLevel)world, (player) -> {
                    if(limitedRange.contains(player.position())) {
                        LoaderImpl.playSound(world, player, identifier, category, getBlockPos(), volume, 1);
                    }
                });
            } else {
                BlockPos corner1 = getCorner1();
                BlockPos corner2 = getCorner2();
                AABB limitedRange = new AABB(corner1.getX(), corner1.getY(), corner1.getZ(), corner2.getX(), corner2.getY(), corner2.getZ());

                MinecraftServerHelper.iteratePlayers((ServerLevel)world, (player) -> {
                    if(limitedRange.contains(player.position())) {
                        float dist = getBlockPos().distManhattan(new Vec3i((int)player.position().x(), (int)player.position().y(),(int)player.position().z()));
                        float fadeToZeroDist = 16 * volume;

                        float calculatedVolume = (1 - (Math.max(0.001f, dist / fadeToZeroDist))) * volume;
                        player.playSound(soundEvent, calculatedVolume, 1);
                    }
                });
            }
        }
    }

    public String getSoundId() {
        return soundID == null ? "" : soundID;
    }

    public int getLoopInterval() {
        return repeatTick;
    }

    public int getSoundCategory() {
        if (soundCategory > SoundLooperBlockEntity.SOURCE_LIST.length) {
            soundCategory = 0;
        }
        return soundCategory;
    }

    public float getSoundVolume() {
        return volume;
    }

    public boolean rangeLimited() {
        return limitRange;
    }

    public boolean needRedstone() {
        return needRedstone;
    }

    public BlockPos getCorner1() {
        return corner1;
    }

    public BlockPos getCorner2() { // This used to be called getBlockPos, which happens to override MC Mappings method, causing the world to be null. Ended up spending an hour debugging it, horrifying.
        return corner2;
    }

    public void setData(String soundId, int soundCategory, int interval, float volume, boolean needRedstone, boolean limitRange, BlockPos corner1, BlockPos corner2) {
        this.soundID = soundId;
        this.repeatTick = interval;
        this.soundCategory = soundCategory;
        this.volume = volume;
        this.needRedstone = needRedstone;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.limitRange = limitRange;
        this.setChanged();
    }
}