package com.stalker.sixnights;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;

/** Small secondary stalkers rendered as the supplied transparent PNG cutouts, not jumpscare overlays. */
public class StalkerChildEntity extends Monster {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StalkerChildEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CHASING = SynchedEntityData.defineId(StalkerChildEntity.class, EntityDataSerializers.BOOLEAN);
    public StalkerChildEntity(EntityType<? extends Monster> type, Level level) { super(type, level); this.xpReward = 0; entityData.set(VARIANT, random.nextInt(3)); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(VARIANT, 0); entityData.define(CHASING, false); }
    public int getVariant() { return entityData.get(VARIANT); }
    public boolean isChasing() { return entityData.get(CHASING); }
    @Override public void tick() { super.tick(); if (!level().isClientSide) entityData.set(CHASING, getTarget() != null && getTarget().isAlive()); }
    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData data, net.minecraft.nbt.CompoundTag tag) {
        net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        entityData.set(VARIANT, random.nextInt(3));
        return result;
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D).add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.9D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.65D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 22.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
}
