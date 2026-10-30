package com.stalker.sixnights;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StalkerSixNights.MOD_ID)
public final class StalkerSixNights {
    public static final String MOD_ID = "stalkersixnights";
    public StalkerSixNights() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        StalkerRegistry.ENTITY_TYPES.register(bus);
        StalkerRegistry.ITEMS.register(bus);
        StalkerSounds.SOUNDS.register(bus);
        bus.addListener(this::createEntityAttributes);
        bus.addListener(this::addCreativeItems);
    }
    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(StalkerRegistry.WATCHER_EGG.get());
            event.accept(StalkerRegistry.RUNNER_EGG.get());
            event.accept(StalkerRegistry.MIMIC_EGG.get());
            event.accept(StalkerRegistry.FACE_EGG.get());
            event.accept(StalkerRegistry.CHILD_EGG.get());
            event.accept(StalkerRegistry.STALKER_SPAWN_EGG.get());
        }
    }
    private void createEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(StalkerRegistry.STALKER.get(), StalkerEntity.createAttributes().build());
        event.put(StalkerRegistry.WATCHER.get(), StalkerEntity.createAttributes().build());
        event.put(StalkerRegistry.RUNNER.get(), StalkerEntity.createAttributes().build());
        event.put(StalkerRegistry.MIMIC.get(), StalkerEntity.createAttributes().build());
        event.put(StalkerRegistry.FACE.get(), StalkerEntity.createAttributes().build());
        event.put(StalkerRegistry.CHILD.get(), StalkerChildEntity.createAttributes().build());
    }
}
