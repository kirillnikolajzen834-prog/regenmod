package com.example.regenmod.event;
import com.example.regenmod.RegenMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = RegenMod.MODID)
public class GameplayEvents {
    public static final HashSet<UUID> hasRegenAbility = new HashSet<>();
    public static final ConcurrentHashMap<UUID, Integer> limbRegenTimers = new ConcurrentHashMap<>();
    public static final HashSet<UUID> shatteredPlayers = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            if (event.getNewDamage() >= 6.0F) {
                if (!limbRegenTimers.containsKey(uuid)) {
                    limbRegenTimers.put(uuid, 200);
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            if (limbRegenTimers.containsKey(uuid)) {
                int left = limbRegenTimers.get(uuid) - 1;
                if (left <= 0) limbRegenTimers.remove(uuid);
                else limbRegenTimers.put(uuid, left);
            }
            if (shatteredPlayers.contains(uuid) && player.tickCount % 40 == 0) {
                player.sendSystemMessage(Component.literal("Нажмите [X], чтобы собраться воедино."), true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            if (!shatteredPlayers.contains(uuid)) {
                event.setCanceled(true);
                shatteredPlayers.add(uuid);
                player.setHealth(1.0F);
                player.setInvisible(true);
                var level = player.serverLevel();
                for (int i = 0; i < 50; i++) {
                    level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1, player.getZ(), 1, 0.6, 0.6, 0.6, 0.15);
                }
            }
        }
    }
}