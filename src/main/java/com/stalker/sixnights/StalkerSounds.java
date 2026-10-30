package com.stalker.sixnights;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class StalkerSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StalkerSixNights.MOD_ID);
    private static RegistryObject<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StalkerSixNights.MOD_ID, id)));
    }
    public static final RegistryObject<SoundEvent> CHASE_MUSIC = sound("chase_music");
    public static final RegistryObject<SoundEvent> HEARTBEAT = sound("heartbeat");
    public static final RegistryObject<SoundEvent> FOOTSTEPS = sound("footsteps");
    public static final RegistryObject<SoundEvent> DOOR_KNOCK = sound("door_knock");
    public static final RegistryObject<SoundEvent> WINDOW_SCRATCH = sound("window_scratch");
    public static final RegistryObject<SoundEvent> BREATHING = sound("breathing");
    public static final RegistryObject<SoundEvent> TELEPORT = sound("teleport");
    public static final RegistryObject<SoundEvent> IMPACT = sound("impact");
    public static final RegistryObject<SoundEvent> VOICE_WATCHER_1 = sound("voice_watcher_1");
    public static final RegistryObject<SoundEvent> VOICE_WATCHER_2 = sound("voice_watcher_2");
    public static final RegistryObject<SoundEvent> VOICE_MIMIC_1 = sound("voice_mimic_1");
    private StalkerSounds() {}
}
