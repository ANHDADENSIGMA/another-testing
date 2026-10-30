package com.stalker.sixnights;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class StalkerRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StalkerSixNights.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StalkerSixNights.MOD_ID);

    private static EntityType<StalkerEntity> stalkerType(String id) {
        return EntityType.Builder.of(StalkerEntity::new, MobCategory.MONSTER)
                .sized(0.68F, 2.85F).clientTrackingRange(16).updateInterval(2)
                .build("stalkersixnights:" + id);
    }

    public static final RegistryObject<EntityType<StalkerEntity>> WATCHER = ENTITY_TYPES.register("watcher", () -> stalkerType("watcher"));
    public static final RegistryObject<EntityType<StalkerEntity>> RUNNER = ENTITY_TYPES.register("runner", () -> stalkerType("runner"));
    public static final RegistryObject<EntityType<StalkerEntity>> MIMIC = ENTITY_TYPES.register("mimic", () -> stalkerType("mimic"));
    public static final RegistryObject<EntityType<StalkerEntity>> FACE = ENTITY_TYPES.register("face", () -> stalkerType("face"));
    // Backward-compatible registry name for older commands and worlds.
    public static final RegistryObject<EntityType<StalkerEntity>> STALKER = ENTITY_TYPES.register("stalker", () -> stalkerType("stalker"));
    public static final RegistryObject<EntityType<StalkerChildEntity>> CHILD = ENTITY_TYPES.register("stalker_child", () ->
            EntityType.Builder.of(StalkerChildEntity::new, MobCategory.MONSTER)
                    .sized(0.48F, 1.55F).clientTrackingRange(16).updateInterval(2)
                    .build("stalkersixnights:stalker_child"));

    public static final RegistryObject<Item> WATCHER_EGG = egg("watcher_spawn_egg", WATCHER, 0x050505, 0x6A1018);
    public static final RegistryObject<Item> RUNNER_EGG = egg("runner_spawn_egg", RUNNER, 0x181818, 0xC51B24);
    public static final RegistryObject<Item> MIMIC_EGG = egg("mimic_spawn_egg", MIMIC, 0x34302B, 0x8C8172);
    public static final RegistryObject<Item> FACE_EGG = egg("face_spawn_egg", FACE, 0xD0C6B5, 0x130D0D);
    public static final RegistryObject<Item> CHILD_EGG = egg("stalker_child_spawn_egg", CHILD, 0x090909, 0x777777);
    public static final RegistryObject<Item> STALKER_SPAWN_EGG = egg("stalker_spawn_egg", STALKER, 0x080808, 0x5A1018);

    private static <T extends Mob> RegistryObject<Item> egg(String id, RegistryObject<EntityType<T>> type, int primary, int secondary) {
        return ITEMS.register(id, () -> new ForgeSpawnEggItem(type, primary, secondary, new Item.Properties()));
    }
    private StalkerRegistry() {}
}
