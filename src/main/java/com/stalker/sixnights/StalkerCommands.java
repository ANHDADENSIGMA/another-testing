package com.stalker.sixnights;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StalkerSixNights.MOD_ID)
public final class StalkerCommands {
    private StalkerCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("stalker").then(Commands.literal("spawn").requires(s -> s.hasPermission(2))
                .then(Commands.literal("watcher").executes(c -> spawn(c.getSource().getPlayerOrException(), StalkerRegistry.WATCHER.get())))
                .then(Commands.literal("runner").executes(c -> spawn(c.getSource().getPlayerOrException(), StalkerRegistry.RUNNER.get())))
                .then(Commands.literal("mimic").executes(c -> spawn(c.getSource().getPlayerOrException(), StalkerRegistry.MIMIC.get())))
                .then(Commands.literal("face").executes(c -> spawn(c.getSource().getPlayerOrException(), StalkerRegistry.FACE.get())))
                .then(Commands.literal("child").executes(c -> spawn(c.getSource().getPlayerOrException(), StalkerRegistry.CHILD.get())))
                .executes(c -> spawn(c.getSource().getPlayerOrException(), StalkerRegistry.WATCHER.get())))
                .then(Commands.literal("remove").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    var mains = player.serverLevel().getEntitiesOfClass(StalkerEntity.class, player.getBoundingBox().inflate(64.0));
                    var children = player.serverLevel().getEntitiesOfClass(StalkerChildEntity.class, player.getBoundingBox().inflate(64.0));
                    mains.forEach(Entity::discard); children.forEach(Entity::discard);
                    c.getSource().sendSuccess(() -> Component.literal("Removed " + (mains.size()+children.size()) + " Stalker entities."), false);
                    return mains.size()+children.size();
                }));
        event.getDispatcher().register(root);
    }
    private static int spawn(ServerPlayer player, EntityType<? extends Mob> type) {
        Vec3 look = player.getLookAngle().normalize();
        Mob mob = type.create(player.serverLevel());
        if (mob == null) return 0;
        mob.moveTo(player.getX()+look.x*3.0, player.getY(), player.getZ()+look.z*3.0, player.getYRot()+180.0F, 0.0F);
        mob.setPersistenceRequired();
        if (player.serverLevel().addFreshEntity(mob)) {
            player.displayClientMessage(Component.literal("Spawned: " + type.getDescription().getString()), false);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
}
