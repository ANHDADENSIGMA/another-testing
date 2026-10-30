package com.stalker.sixnights;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Random;

@Mod.EventBusSubscriber(modid = StalkerSixNights.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StalkerClientEvents {
    private static final Random RANDOM = new Random();
    private static SoundInstance chaseMusic;
    private static int heartbeatCooldown;
    private static int voiceCooldown;
    private StalkerClientEvents() {}

    @SubscribeEvent public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StalkerModel.LAYER, StalkerModel::createBodyLayer);
    }
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(StalkerRegistry.STALKER.get(), StalkerRenderer::new);
        event.registerEntityRenderer(StalkerRegistry.WATCHER.get(), StalkerRenderer::new);
        event.registerEntityRenderer(StalkerRegistry.RUNNER.get(), StalkerRenderer::new);
        event.registerEntityRenderer(StalkerRegistry.MIMIC.get(), StalkerRenderer::new);
        event.registerEntityRenderer(StalkerRegistry.FACE.get(), FaceRenderer::new);
        event.registerEntityRenderer(StalkerRegistry.CHILD.get(), StalkerChildRenderer::new);
    }

    @Mod.EventBusSubscriber(modid = StalkerSixNights.MOD_ID, value = Dist.CLIENT)
    public static final class ClientGameplay {
        @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) { stopChase(mc); return; }
            boolean chasing = false;
            double nearest = Double.MAX_VALUE;
            if (voiceCooldown > 0) voiceCooldown--;
            for (StalkerEntity entity : mc.level.getEntitiesOfClass(StalkerEntity.class, mc.player.getBoundingBox().inflate(48.0))) {
                double d = mc.player.distanceToSqr(entity);
                nearest = Math.min(nearest, d);
                if (entity.isChasing() && d < 42.0 * 42.0) chasing = true;
                if (voiceCooldown == 0 && d < 18.0 * 18.0 && RANDOM.nextInt(520) == 0) {
                    if (entity.getRole() == StalkerEntity.ROLE_MIMIC) mc.player.playSound(StalkerSounds.VOICE_MIMIC_1.get(), 0.9F, 0.96F);
                    else if (entity.getRole() == StalkerEntity.ROLE_WATCHER) mc.player.playSound(RANDOM.nextBoolean() ? StalkerSounds.VOICE_WATCHER_1.get() : StalkerSounds.VOICE_WATCHER_2.get(), 0.85F, 0.92F);
                    voiceCooldown = 20 * 35;
                }
            }
            for (StalkerChildEntity child : mc.level.getEntitiesOfClass(StalkerChildEntity.class, mc.player.getBoundingBox().inflate(32.0))) {
                double d = mc.player.distanceToSqr(child);
                nearest = Math.min(nearest, d);
                if (child.isChasing() && d < 30.0 * 30.0) chasing = true;
            }
            if (chasing) {
                if (chaseMusic == null) {
                    chaseMusic = SimpleSoundInstance.forMusic(StalkerSounds.CHASE_MUSIC.get());
                    mc.getSoundManager().play(chaseMusic);
                }
                if (heartbeatCooldown > 0) heartbeatCooldown--;
                if (heartbeatCooldown == 0 && nearest < 18.0 * 18.0) {
                    mc.player.playSound(StalkerSounds.HEARTBEAT.get(), 0.55F, 0.85F + RANDOM.nextFloat() * 0.2F);
                    heartbeatCooldown = 48;
                }
            } else stopChase(mc);
            if (!chasing && nearest < 12.0 * 12.0 && RANDOM.nextInt(260) == 0) {
                mc.player.playSound(StalkerSounds.BREATHING.get(), 0.45F, 0.78F + RANDOM.nextFloat() * 0.25F);
            }
        }
        private static void stopChase(Minecraft mc) {
            if (chaseMusic != null) { mc.getSoundManager().stop(chaseMusic); chaseMusic = null; }
            heartbeatCooldown = 0;
        }

        @SubscribeEvent public static void fogColor(ViewportEvent.ComputeFogColor event) {
            Minecraft mc = Minecraft.getInstance();
            Entity camera = event.getCamera().getEntity();
            if (mc.level == null || camera == null) return;
            double distance = nearestChasingDistance(mc, camera);
            if (distance > 34.0) return;
            float intensity = (float)Math.max(0.0, Math.min(0.96, (34.0 - distance) / 22.0));
            event.setRed(event.getRed() * (1.0F - intensity) + 0.002F * intensity);
            event.setGreen(event.getGreen() * (1.0F - intensity) + 0.002F * intensity);
            event.setBlue(event.getBlue() * (1.0F - intensity) + 0.003F * intensity);
        }
        @SubscribeEvent public static void fogDistance(ViewportEvent.RenderFog event) {
            Minecraft mc = Minecraft.getInstance();
            Entity camera = event.getCamera().getEntity();
            if (mc.level == null || camera == null) return;
            double distance = nearestChasingDistance(mc, camera);
            if (distance > 34.0) return;
            float strength = (float)Math.max(0.0, Math.min(1.0, (34.0 - distance) / 26.0));
            event.setNearPlaneDistance(Math.max(0.0F, event.getNearPlaneDistance() - strength * 1.5F));
            event.setFarPlaneDistance(Math.max(4.0F, event.getFarPlaneDistance() * (1.0F - strength * 0.86F)));
        }
        private static double nearestChasingDistance(Minecraft mc, Entity camera) {
            double nearest = Double.MAX_VALUE;
            for (StalkerEntity entity : mc.level.getEntitiesOfClass(StalkerEntity.class, camera.getBoundingBox().inflate(40.0))) {
                if (entity.isChasing()) nearest = Math.min(nearest, camera.distanceToSqr(entity));
            }
            for (StalkerChildEntity child : mc.level.getEntitiesOfClass(StalkerChildEntity.class, camera.getBoundingBox().inflate(32.0))) {
                if (child.isChasing()) nearest = Math.min(nearest, camera.distanceToSqr(child));
            }
            return Math.sqrt(nearest);
        }
    }
}
