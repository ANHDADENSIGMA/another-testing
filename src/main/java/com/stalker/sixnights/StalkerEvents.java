package com.stalker.sixnights;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;
import java.util.UUID;

/** Server-side director for the six-night horror story. */
@Mod.EventBusSubscriber(modid = StalkerSixNights.MOD_ID)
public final class StalkerEvents {
    private static final String DATA_KEY = "StalkerSixNights";
    private static final String NEXT_EVENT = "NextEventTick";
    private static final String LAST_LETTER_DAY = "LastLetterDay";
    private static final String STALKER_ID = "StalkerEntity";
    private static final String LAST_ENCOUNTER_DAY = "LastEncounterDay";
    private static final Random RANDOM = new Random();

    private StalkerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) return;
        if (player.isCreative() && player.isInvulnerable()) return;

        CompoundTag data = getData(player);
        long gameTime = level.getGameTime();
        long timeOfDay = level.getDayTime() % 24000L;
        int day = (int) (level.getDayTime() / 24000L) + 1;
        boolean night = timeOfDay >= 13000L && timeOfDay < 23000L;

        tickWatcher(player, level, data);
        tickMiniGame(player, level, data);
        for (StalkerEntity special : level.getEntitiesOfClass(StalkerEntity.class, player.getBoundingBox().inflate(64.0), e -> e.getPersistentData().getLong("SNExpiry") > 0L)) {
            if (level.getGameTime() >= special.getPersistentData().getLong("SNExpiry")) special.discard();
        }
        if ((gameTime % 20L) == 0L) tickMinions(player, level);

        if (day >= 6) {
            if (night && !data.getBoolean("SNFinalFaceSeen") && gameTime >= data.getLong(NEXT_EVENT)) {
                spawnFace(player, level);
                data.putBoolean("SNFinalFaceSeen", true);
                data.putLong(NEXT_EVENT, gameTime + 20L * 45L);
            }
            if (data.getInt(LAST_LETTER_DAY) < 6 && timeOfDay < 12000L) {
                giveFinalLetter(player, data);
                data.putInt(LAST_LETTER_DAY, 6);
                removeWatcher(player, level, data);
            }
            return;
        }

        if (!night || gameTime < data.getLong(NEXT_EVENT)) return;
        runNightEvent(player, level, day, data);
        maybeSpawnMinions(player, level, day, data);

        // Each night becomes more active, but the random pauses are intentional.
        int minDelay = Math.max(100, 520 - day * 70);
        int maxDelay = Math.max(minDelay + 80, 900 - day * 95);
        data.putLong(NEXT_EVENT, gameTime + minDelay + RANDOM.nextInt(maxDelay - minDelay + 1));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag oldData = event.getOriginal().getPersistentData();
        if (oldData.contains(DATA_KEY)) {
            event.getEntity().getPersistentData().put(DATA_KEY, oldData.getCompound(DATA_KEY).copy());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            CompoundTag data = getData(player);
            removeWatcher(player, level, data);
        }
    }

    private static CompoundTag getData(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(DATA_KEY)) root.put(DATA_KEY, new CompoundTag());
        return root.getCompound(DATA_KEY);
    }

    private static void runNightEvent(ServerPlayer player, ServerLevel level, int day, CompoundTag data) {
        int roll = RANDOM.nextInt(100);
        boolean hasShelter = findNearbyDoor(player, level, 12) != null;
        boolean hasWindow = findNearbyWindow(player, level, 10) != null;

        if (day == 3 && roll < 19 && !data.getBoolean("SNMiniGameActive")) {
            startMiniGame(player, level, data, 1);
            return;
        }
        if (day == 4 && roll < 18 && !data.getBoolean("SNMiniGameActive")) {
            startMiniGame(player, level, data, 2);
            return;
        }

        if (day == 3 && roll >= 94) { spawnMimic(player, level); return; }
        if (day == 4 && roll >= 91) { spawnMimic(player, level); return; }
        if (day == 5 && roll >= 94) { spawnFace(player, level); return; }
        if (day == 5 && roll >= 88) { spawnMimic(player, level); return; }

        if (day == 1) {
            if (roll < 42) whisper(player, level, false);
            else if (roll < 76) footsteps(player, level, false);
            else if (roll < 91) fakeDoorSound(player, level);
            else spawnWatcher(player, level, data, false, false);
            return;
        }
        if (day == 2) {
            if (roll < 26) footsteps(player, level, false);
            else if (roll < 48) whisper(player, level, true);
            else if (roll < 68) knockNearby(player, level, hasShelter);
            else if (roll < 86) spawnWatcher(player, level, data, false, hasWindow);
            else fakeDoorSound(player, level);
            return;
        }
        if (day == 3) {
            if (roll < 23) knockNearby(player, level, hasShelter);
            else if (roll < 43) spawnWatcher(player, level, data, true, hasWindow);
            else if (roll < 61) footsteps(player, level, true);
            else if (roll < 78) whisper(player, level, true);
            else if (roll < 90) leaveOmen(player, level);
            else fakeDoorSound(player, level);
            return;
        }
        if (day == 4) {
            if (roll < 27) spawnWatcher(player, level, data, true, hasWindow);
            else if (roll < 47) knockNearby(player, level, hasShelter);
            else if (roll < 63) footsteps(player, level, true);
            else if (roll < 78) leaveOmen(player, level);
            else if (roll < 91) whisper(player, level, true);
            else fakeDoorSound(player, level);
            return;
        }

        // Night five: the Stalker gets bolder, but no unavoidable damage or forced death.
        if (roll < 34) spawnWatcher(player, level, data, true, hasWindow);
        else if (roll < 53) knockNearby(player, level, hasShelter);
        else if (roll < 69) leaveOmen(player, level);
        else if (roll < 83) whisper(player, level, true);
        else if (roll < 94) footsteps(player, level, true);
        else fakeDoorSound(player, level);
    }

    private static void whisper(ServerPlayer player, ServerLevel level, boolean intense) {
        BlockPos soundPos = player.blockPosition();
        level.playSound(null, soundPos, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                intense ? 0.95F : 0.48F, intense ? 0.55F : 0.82F);
        if (intense) {
            int line = RANDOM.nextInt(4);
            String[] messages = {
                    "§8You hear breathing just behind you.",
                    "§8Something whispers your name.",
                    "§8The silence feels occupied.",
                    "§8You are not alone in this room."
            };
            player.displayClientMessage(Component.literal(messages[line]), true);
        }
    }

    private static void footsteps(ServerPlayer player, ServerLevel level, boolean close) {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        if (side.lengthSqr() < 0.01) side = new Vec3(1, 0, 0);
        double sideOffset = RANDOM.nextBoolean() ? 1.0 : -1.0;
        double distance = close ? 2.5 + RANDOM.nextDouble() * 2.0 : 5.0 + RANDOM.nextDouble() * 4.0;
        Vec3 at = player.position().subtract(forward.scale(distance)).add(side.scale(sideOffset * 2.0));
        BlockPos pos = findGround(level, BlockPos.containing(at.x, at.y, at.z));
        if (pos == null) pos = player.blockPosition().offset((int) sideOffset * 3, 0, -4);
        level.playSound(null, pos, StalkerSounds.FOOTSTEPS.get(), SoundSource.AMBIENT, close ? 0.9F : 0.65F, 0.72F);
        if (close && RANDOM.nextBoolean()) {
            level.playSound(null, pos, SoundEvents.WOOD_STEP, SoundSource.AMBIENT, 0.7F, 0.72F);
        }
    }

    private static void knockNearby(ServerPlayer player, ServerLevel level, boolean hasShelter) {
        BlockPos door = findNearbyDoor(player, level, 12);
        if (door != null) {
            level.playSound(null, door, StalkerSounds.DOOR_KNOCK.get(), SoundSource.BLOCKS, 1.0F, 0.92F);
            level.playSound(null, door, StalkerSounds.DOOR_KNOCK.get(), SoundSource.BLOCKS, 0.85F, 1.08F);
            if (RANDOM.nextBoolean()) level.playSound(null, door, StalkerSounds.DOOR_KNOCK.get(), SoundSource.BLOCKS, 0.7F, 0.78F);
            player.displayClientMessage(Component.literal("§8* knock... knock... *"), true);
        } else {
            BlockPos pos = player.blockPosition().offset(RANDOM.nextInt(13) - 6, 0, RANDOM.nextInt(13) - 6);
            level.playSound(null, pos, StalkerSounds.DOOR_KNOCK.get(), SoundSource.AMBIENT, hasShelter ? 0.65F : 0.9F, 0.82F);
            player.displayClientMessage(Component.literal("§8A knock comes from the dark."), true);
        }
    }

    private static BlockPos findNearbyDoor(ServerPlayer player, ServerLevel level, int radius) {
        BlockPos origin = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -3, -radius), origin.offset(radius, 3, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock) {
                double distance = pos.distToCenterSqr(player.position());
                if (distance < bestDistance) { bestDistance = distance; best = pos.immutable(); }
            }
        }
        return best;
    }

    private static BlockPos findNearbyWindow(ServerPlayer player, ServerLevel level, int radius) {
        BlockPos origin = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 3, radius))) {
            if (level.getBlockState(pos).getBlock() instanceof AbstractGlassBlock) {
                double distance = pos.distToCenterSqr(player.position());
                if (distance < bestDistance) { bestDistance = distance; best = pos.immutable(); }
            }
        }
        return best;
    }

    private static void fakeDoorSound(ServerPlayer player, ServerLevel level) {
        BlockPos door = findNearbyDoor(player, level, 14);
        BlockPos pos = door != null ? door : player.blockPosition().offset(RANDOM.nextInt(15) - 7, 0, RANDOM.nextInt(15) - 7);
        level.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.AMBIENT, 0.9F, 0.72F);
        if (RANDOM.nextBoolean()) level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 0.85F, 0.82F);
    }

    private static void leaveOmen(ServerPlayer player, ServerLevel level) {
        BlockPos origin = player.blockPosition();
        BlockPos ground = findGround(level, origin.offset(RANDOM.nextInt(11) - 5, 0, RANDOM.nextInt(11) - 5));
        if (ground != null) {
            level.playSound(null, ground, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.85F, 0.45F);
            // A harmless, non-block-changing clue: a renamed paper is left nearby.
            if (RANDOM.nextInt(3) == 0) {
                ItemStack note = new ItemStack(Items.PAPER);
                note.setHoverName(Component.literal("§0A note from outside"));
                if (level.getBlockState(ground).isAir()) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                        level, ground.getX() + 0.5, ground.getY() + 0.15, ground.getZ() + 0.5, note));
            }
        }
        player.displayClientMessage(Component.literal("§8Something has left a sign nearby."), true);
    }

    private static void spawnWatcher(ServerPlayer player, ServerLevel level, CompoundTag data, boolean close, boolean preferWindow) {
        if (data.hasUUID(STALKER_ID)) {
            Entity old = level.getEntity(data.getUUID(STALKER_ID));
            if (old != null && old.isAlive()) return;
            data.remove(STALKER_ID);
        }

        BlockPos window = preferWindow ? findNearbyWindow(player, level, 10) : null;
        BlockPos spawnPos = null;
        if (window != null) {
            // Try to put the watcher just outside one side of a window.
            BlockPos[] candidates = {window.north(), window.south(), window.east(), window.west()};
            for (BlockPos candidate : candidates) {
                BlockPos ground = findGround(level, candidate);
                if (ground != null && ground.distToCenterSqr(player.position()) >= 9.0) {
                    spawnPos = ground;
                    break;
                }
            }
        }
        if (spawnPos == null) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = close ? 5.0 + RANDOM.nextDouble() * 4.0 : 12.0 + RANDOM.nextDouble() * 10.0;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            spawnPos = findGround(level, new BlockPos(x, player.getBlockY(), z));
        }
        if (spawnPos == null) return;

        StalkerEntity watcher = StalkerRegistry.WATCHER.get().create(level);
        if (watcher == null) return;
        watcher.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
        watcher.setRole(StalkerEntity.ROLE_WATCHER);
        watcher.setNoAi(true);
        watcher.setNoGravity(true);
        watcher.setSilent(true);
        watcher.setInvulnerable(true);
        watcher.setPersistenceRequired();
        watcher.setCustomName(Component.literal("§0The Stalker"));
        watcher.setCustomNameVisible(false);
        watcher.setTarget(null);
        watcher.setYRot((float) (Math.atan2(player.getZ() - watcher.getZ(), player.getX() - watcher.getX()) * 180.0 / Math.PI) - 90.0F);
        if (level.addFreshEntity(watcher)) {
            data.putUUID(STALKER_ID, watcher.getUUID());
            data.putInt(LAST_ENCOUNTER_DAY, (int) (level.getDayTime() / 24000L) + 1);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_AMBIENT, SoundSource.AMBIENT, close ? 0.75F : 0.32F, 0.52F);
            if (close) player.displayClientMessage(Component.literal("§8You feel something watching you."), true);
        }
    }

    private static void spawnMimic(ServerPlayer player, ServerLevel level) {
        BlockPos door = findNearbyDoor(player, level, 14);
        BlockPos around = door != null ? door : player.blockPosition().offset(RANDOM.nextInt(13)-6, 0, RANDOM.nextInt(13)-6);
        BlockPos ground = findGround(level, around);
        if (ground == null) return;
        StalkerEntity mimic = StalkerRegistry.MIMIC.get().create(level);
        if (mimic == null) return;
        mimic.moveTo(ground.getX()+0.5, ground.getY(), ground.getZ()+0.5,
                (float)(Math.atan2(player.getZ()-ground.getZ(), player.getX()-ground.getX())*180.0/Math.PI)-90.0F, 0.0F);
        mimic.setPersistenceRequired();
        mimic.getPersistentData().putLong("SNExpiry", level.getGameTime()+20L*32L);
        if (RANDOM.nextBoolean()) mimic.setTarget(player);
        if (level.addFreshEntity(mimic)) {
            level.playSound(null, ground, StalkerSounds.VOICE_MIMIC_1.get(), SoundSource.AMBIENT, 0.9F, 0.9F);
            player.displayClientMessage(Component.literal("§8A familiar voice comes from somewhere outside."), true);
        }
    }

    private static void spawnFace(ServerPlayer player, ServerLevel level) {
        double angle = RANDOM.nextDouble()*Math.PI*2.0;
        double distance = 5.0 + RANDOM.nextDouble()*3.0;
        BlockPos ground = findGround(level, BlockPos.containing(player.getX()+Math.cos(angle)*distance, player.getY(), player.getZ()+Math.sin(angle)*distance));
        if (ground == null) return;
        StalkerEntity face = StalkerRegistry.FACE.get().create(level);
        if (face == null) return;
        face.moveTo(ground.getX()+0.5, ground.getY(), ground.getZ()+0.5,
                (float)(Math.atan2(player.getZ()-ground.getZ(), player.getX()-ground.getX())*180.0/Math.PI)-90.0F, 0.0F);
        face.setNoAi(true); face.setNoGravity(true); face.setInvulnerable(true); face.setPersistenceRequired();
        face.getPersistentData().putLong("SNExpiry", level.getGameTime()+20L*8L);
        if (level.addFreshEntity(face)) {
            level.playSound(null, ground, StalkerSounds.IMPACT.get(), SoundSource.HOSTILE, 1.0F, 0.72F);
            player.displayClientMessage(Component.literal("§4DON'T MOVE."), true);
        }
    }

    private static void startMiniGame(ServerPlayer player, ServerLevel level, CompoundTag data, int type) {
        Vec3 away = player.getLookAngle().normalize().scale(-1.0);
        double distance = 11.0 + RANDOM.nextDouble() * 5.0;
        BlockPos around = BlockPos.containing(player.getX() + away.x * distance, player.getY(),
                player.getZ() + away.z * distance);
        BlockPos spawnPos = findGround(level, around);
        if (spawnPos == null) {
            player.displayClientMessage(Component.literal("§8You hear something running in the dark."), true);
            level.playSound(null, player.blockPosition(), SoundEvents.SKELETON_STEP, SoundSource.HOSTILE, 1.0F, 0.55F);
            data.putLong("SNMiniGameEnd", level.getGameTime() + 20L * 18L);
            data.putInt("SNMiniGameType", type);
            data.putBoolean("SNMiniGameActive", true);
            return;
        }

        net.minecraft.world.entity.Mob hunter = (type == 1 ? StalkerRegistry.CHILD.get().create(level) : StalkerRegistry.RUNNER.get().create(level));
        if (hunter == null) return;
        if (type == 1) {
            // Hide-and-seek hunter is a small PNG stalker child.
        }
        hunter.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                (float) (Math.atan2(player.getZ() - spawnPos.getZ(), player.getX() - spawnPos.getX()) * 180.0 / Math.PI) - 90.0F, 0.0F);
        hunter.setCustomName(Component.literal(type == 1 ? "§8Hide and Seek" : "§4Don't Look Back"));
        hunter.setCustomNameVisible(false);
        hunter.setPersistenceRequired();
        hunter.setTarget(player);
        if (type == 1) {
            hunter.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.24D);
            hunter.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0D);
        } else {
            hunter.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.39D);
            hunter.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0D);
        }
        if (!level.addFreshEntity(hunter)) return;
        data.putUUID("SNMiniGameHunter", hunter.getUUID());
        data.putLong("SNMiniGameEnd", level.getGameTime() + 20L * (type == 1 ? 24 : 20));
        data.putInt("SNMiniGameType", type);
        data.putBoolean("SNMiniGameActive", true);
        if (type == 1) {
            player.displayClientMessage(Component.literal("§8HIDE. It is looking for you. Survive until the footsteps stop."), false);
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.75F, 0.7F);
        } else {
            player.displayClientMessage(Component.literal("§4DON'T LOOK BACK. Keep moving until the sound stops."), false);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 0.85F, 0.48F);
        }
    }

    private static void tickMiniGame(ServerPlayer player, ServerLevel level, CompoundTag data) {
        if (!data.getBoolean("SNMiniGameActive")) return;
        if (level.getGameTime() < data.getLong("SNMiniGameEnd")) return;
        if (data.hasUUID("SNMiniGameHunter")) {
            Entity hunter = level.getEntity(data.getUUID("SNMiniGameHunter"));
            if (hunter != null) hunter.discard();
            data.remove("SNMiniGameHunter");
        }
        int type = data.getInt("SNMiniGameType");
        data.putBoolean("SNMiniGameActive", false);
        ItemStack clue = new ItemStack(Items.PAPER);
        clue.setHoverName(Component.literal("§0A clue from the dark"));
        if (!player.getInventory().add(clue)) player.drop(clue, false);
        if (type == 1) {
            player.displayClientMessage(Component.literal("§7The footsteps stop. Something scratches a message into the silence."), false);
        } else {
            player.displayClientMessage(Component.literal("§7You made it. But you can hear breathing behind you."), false);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.8F, 0.45F);
    }

    private static void maybeSpawnMinions(ServerPlayer player, ServerLevel level, int day, CompoundTag data) {
        long now = level.getGameTime();
        String nextKey = "NextMinionTick";
        if (day >= 6 || now < data.getLong(nextKey)) return;

        int delay = 220 + RANDOM.nextInt(360);
        data.putLong(nextKey, now + delay);
        int nearbyCount = level.getEntitiesOfClass(StalkerChildEntity.class, player.getBoundingBox().inflate(28.0)).size();
        int maxMinions = day <= 2 ? 1 : (day <= 4 ? 2 : 3);
        if (nearbyCount >= maxMinions || RANDOM.nextInt(100) > 48 + day * 7) return;

        int count = day >= 4 && RANDOM.nextInt(100) < 32 ? 2 : 1;
        for (int i = 0; i < count && nearbyCount < maxMinions; i++) {
            BlockPos anchor = null;
            boolean useWindow = RANDOM.nextBoolean();
            if (useWindow) anchor = findNearbyWindow(player, level, 12);
            if (anchor == null) anchor = findNearbyDoor(player, level, 14);

            BlockPos spawnPos = null;
            if (anchor != null) {
                BlockPos[] sides = {anchor.north(), anchor.south(), anchor.east(), anchor.west()};
                for (int attempt = 0; attempt < sides.length; attempt++) {
                    BlockPos candidate = sides[RANDOM.nextInt(sides.length)];
                    BlockPos ground = findGround(level, candidate);
                    if (ground != null && ground.distToCenterSqr(player.position()) > 6.0
                            && ground.distToCenterSqr(player.position()) < 18.0 * 18.0) {
                        spawnPos = ground;
                        break;
                    }
                }
            }
            if (spawnPos == null) {
                double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                double distance = 7.0 + RANDOM.nextDouble() * 8.0;
                spawnPos = findGround(level, BlockPos.containing(
                        player.getX() + Math.cos(angle) * distance, player.getY(),
                        player.getZ() + Math.sin(angle) * distance));
            }
            if (spawnPos == null) continue;

            StalkerChildEntity minion = StalkerRegistry.CHILD.get().create(level);
            if (minion == null) continue;
            minion.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    0.0F, 0.0F);
            minion.setInvulnerable(true);
            if (RANDOM.nextInt(4) == 0) minion.setTarget(player);
            minion.setPersistenceRequired();
            minion.setCustomName(Component.literal("§8The Little One"));
            minion.setCustomNameVisible(false);
            minion.getPersistentData().putLong("SNMinionExpiry", now + 20L * (24 + RANDOM.nextInt(28)));
            minion.setYRot((float) (Math.atan2(player.getZ() - minion.getZ(), player.getX() - minion.getX()) * 180.0 / Math.PI) - 90.0F);
            if (level.addFreshEntity(minion)) {
                nearbyCount++;
                if (useWindow && anchor != null) {
                    level.playSound(null, anchor, StalkerSounds.WINDOW_SCRATCH.get(), SoundSource.AMBIENT,
                            0.65F + day * 0.05F, 0.82F + RANDOM.nextFloat() * 0.22F);
                } else {
                    level.playSound(null, anchor != null ? anchor : spawnPos, StalkerSounds.DOOR_KNOCK.get(),
                            SoundSource.AMBIENT, 0.55F + day * 0.06F, 0.75F + RANDOM.nextFloat() * 0.3F);
                }
                if (RANDOM.nextBoolean()) {
                    String[] lines = {"§8Let me in.", "§8I know you're there.", "§8Don't look away."};
                    player.displayClientMessage(Component.literal(lines[RANDOM.nextInt(lines.length)]), true);
                }
            }
        }
    }

    private static void tickMinions(ServerPlayer player, ServerLevel level) {
        for (StalkerChildEntity minion : level.getEntitiesOfClass(StalkerChildEntity.class,
                player.getBoundingBox().inflate(52.0))) {
            if (minion.getPersistentData().getLong("SNMinionExpiry") > 0L
                    && level.getGameTime() >= minion.getPersistentData().getLong("SNMinionExpiry")) {
                minion.discard();
                continue;
            }
            double dx = player.getX() - minion.getX();
            double dz = player.getZ() - minion.getZ();
            minion.setYRot((float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F);
        }
    }

    private static BlockPos findGround(ServerLevel level, BlockPos around) {
        int startY = Math.min(level.getMaxBuildHeight() - 3, Math.max(level.getMinBuildHeight() + 2, around.getY()));
        for (int dy = 4; dy >= -8; dy--) {
            BlockPos pos = new BlockPos(around.getX(), startY + dy, around.getZ());
            if (!level.isEmptyBlock(pos) && level.getBlockState(pos).isSolidRender(level, pos)
                    && level.isEmptyBlock(pos.above()) && level.isEmptyBlock(pos.above(2))) {
                return pos.above();
            }
        }
        return null;
    }

    private static void tickWatcher(ServerPlayer player, ServerLevel level, CompoundTag data) {
        if (!data.hasUUID(STALKER_ID)) return;
        Entity entity = level.getEntity(data.getUUID(STALKER_ID));
        if (!(entity instanceof StalkerEntity watcher) || !watcher.isAlive()) {
            data.remove(STALKER_ID);
            return;
        }
        if (watcher.distanceToSqr(player) > 46.0 * 46.0 || watcher.tickCount > 20 * 78) {
            watcher.discard();
            data.remove(STALKER_ID);
            return;
        }

        // Keep the model facing the player, even while it stands perfectly still.
        double dx = player.getX() - watcher.getX();
        double dz = player.getZ() - watcher.getZ();
        watcher.setYRot((float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F);
        Vec3 toWatcher = watcher.position().add(0, watcher.getBbHeight() * 0.55, 0).subtract(player.getEyePosition()).normalize();
        double dot = player.getLookAngle().normalize().dot(toWatcher);
        boolean visible = canSeeStalker(player, level, watcher);
        double distanceSqr = watcher.distanceToSqr(player);

        // The figure creeps closer only while it is not being watched. Its slow glide is intentional.
        if (visible && dot > 0.72) {
            watcher.setDeltaMovement(Vec3.ZERO);
        } else if (distanceSqr > 4.5 * 4.5) {
            Vec3 towardPlayer = player.position().subtract(watcher.position());
            towardPlayer = new Vec3(towardPlayer.x, 0.0, towardPlayer.z);
            if (towardPlayer.lengthSqr() > 0.001) watcher.setDeltaMovement(towardPlayer.normalize().scale(0.055));
        } else {
            watcher.setDeltaMovement(Vec3.ZERO);
        }

        int watchedTicks = watcher.getPersistentData().getInt("SNWatchedTicks");
        if (dot > 0.965 && visible && distanceSqr < 30.0 * 30.0) {
            watchedTicks++;
            watcher.getPersistentData().putInt("SNWatchedTicks", watchedTicks);
        } else {
            watchedTicks = Math.max(0, watchedTicks - 2);
            watcher.getPersistentData().putInt("SNWatchedTicks", watchedTicks);
        }
        // Let the player notice the figure before it reacts. It only vanishes after a sustained stare.
        if (watchedTicks > 64) {
            level.playSound(null, watcher.blockPosition(), StalkerSounds.TELEPORT.get(), SoundSource.HOSTILE, 0.9F, 0.8F);
            level.playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.9F, 0.38F);
            watcher.discard();
            data.remove(STALKER_ID);
            player.displayClientMessage(Component.literal("§8It was there for a long time. Then it wasn't."), true);
            data.putLong(NEXT_EVENT, Math.max(data.getLong(NEXT_EVENT), level.getGameTime() + 80));
        }
    }

    /**
     * A short sampled sight ray that treats glass as transparent. Vanilla line-of-sight
     * can stop at glass collision, which would break the signature window encounter.
     */
    private static boolean canSeeStalker(ServerPlayer player, ServerLevel level, Entity watcher) {
        Vec3 start = player.getEyePosition();
        Vec3 end = watcher.position().add(0.0, watcher.getBbHeight() * 0.55, 0.0);
        Vec3 delta = end.subtract(start);
        int samples = Math.max(8, (int) (delta.length() * 4.0));
        for (int i = 1; i < samples; i++) {
            Vec3 point = start.add(delta.scale((double) i / samples));
            BlockPos pos = BlockPos.containing(point);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getBlock() instanceof AbstractGlassBlock || state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static void removeWatcher(ServerPlayer player, ServerLevel level, CompoundTag data) {
        if (!data.hasUUID(STALKER_ID)) return;
        Entity entity = level.getEntity(data.getUUID(STALKER_ID));
        if (entity != null) entity.discard();
        data.remove(STALKER_ID);
    }

    private static void giveFinalLetter(ServerPlayer player, CompoundTag data) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag bookTag = book.getOrCreateTag();
        bookTag.putString("title", "A Letter From Outside");
        bookTag.putString("author", "Someone");
        bookTag.putInt("generation", 0);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("To the one who kept looking back.\n\nYou heard me on the first night. You saw me on the second. By the fifth, you were listening for me."))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("I did not choose your house. You chose the place where I could find you.\n\nDo not follow the footprints into the trees."))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("This is goodbye.\n\nFor now.\n\n— The one outside"))));
        bookTag.put("pages", pages);
        if (!player.getInventory().add(book)) player.drop(book, false);
        player.displayClientMessage(Component.literal("§8There is a letter waiting for you."), false);
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.7F);
    }
}
