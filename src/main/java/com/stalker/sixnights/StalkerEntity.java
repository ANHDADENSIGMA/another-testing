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

public class StalkerEntity extends Monster {
    public static final int ROLE_WATCHER = 0, ROLE_RUNNER = 1, ROLE_MIMIC = 2, ROLE_MINION = 3, ROLE_FACE = 4;
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CHASING = SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.BOOLEAN);

    public int getRole() { return entityData.get(ROLE); }
    public void setRole(int role) { entityData.set(ROLE, Math.max(ROLE_WATCHER, Math.min(ROLE_FACE, role))); }
    public boolean isChasing() { return entityData.get(CHASING); }
    public void setChasing(boolean value) { entityData.set(CHASING, value); }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROLE, roleFromType());
        entityData.define(CHASING, false);
    }
    private int roleFromType() {
        if (getType() == StalkerRegistry.RUNNER.get()) return ROLE_RUNNER;
        if (getType() == StalkerRegistry.MIMIC.get()) return ROLE_MIMIC;
        if (getType() == StalkerRegistry.FACE.get()) return ROLE_FACE;
        return ROLE_WATCHER;
    }
    public StalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setRole(roleFromType());
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D).add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.55D);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (getType() == StalkerRegistry.FACE.get()) return;
        if (getType() == StalkerRegistry.RUNNER.get()) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, true));
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.95D));
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        } else if (getType() == StalkerRegistry.MIMIC.get()) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.88D, true));
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.62D));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        } else {
            // Watcher mostly observes; natural story appearances disable its AI entirely.
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.42D));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 30.0F));
            goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        }
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (!getPersistentData().getBoolean("SNRoleStatsApplied")) {
                var speed = getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    if (getRole() == ROLE_RUNNER) speed.setBaseValue(0.43D);
                    else if (getRole() == ROLE_MIMIC) speed.setBaseValue(0.29D);
                    else if (getRole() == ROLE_FACE) speed.setBaseValue(0.0D);
                    else speed.setBaseValue(0.24D);
                }
                if (getRole() == ROLE_FACE) setNoAi(true);
                getPersistentData().putBoolean("SNRoleStatsApplied", true);
            }
            setChasing(getTarget() != null && getTarget().isAlive() && getRole() != ROLE_WATCHER && getRole() != ROLE_FACE);
        }
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
}
