import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod("regenmod")
public class RegenMod {
    public static final String MODID = "regenmod";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final DeferredItem<Item> REGENERATION_SYRINGE = ITEMS.register("regeneration_syringe", () -> new Item(new Item.Properties().stacksTo(1)) {
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide()) {
                GameplayEvents.hasRegenAbility.add(player.getUUID());
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    });

    public RegenMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::registerNetwork);
    }

    private void registerNetwork(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0");
        registrar.playToServer(AssemblePayload.TYPE, AssemblePayload.STREAM_CODEC, AssemblePayload::handle);
    }

    public record AssemblePayload() implements CustomPacketPayload {
        public static final Type<AssemblePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "assemble"));
        public static final StreamCodec<FriendlyByteBuf, AssemblePayload> STREAM_CODEC = StreamCodec.unit(new AssemblePayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        public static void handle(AssemblePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    UUID uuid = player.getUUID();
                    if (GameplayEvents.shatteredPlayers.contains(uuid)) {
                        GameplayEvents.shatteredPlayers.remove(uuid);
                        player.setInvisible(false);
                        player.setHealth(player.getMaxHealth());
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                }
            });
        }
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class GameplayEvents {
        public static final HashSet<UUID> hasRegenAbility = new HashSet<>();
        public static final ConcurrentHashMap<UUID, Integer> limbRegenTimers = new ConcurrentHashMap<>();
        public static final HashSet<UUID> shatteredPlayers = new HashSet<>();

        @SubscribeEvent
        public static void onPlayerDamage(LivingDamageEvent.Post event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                UUID uuid = player.getUUID();
                if (hasRegenAbility.contains(uuid) && event.getNewDamage() >= 6.0F) {
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
            }
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                UUID uuid = player.getUUID();
                if (hasRegenAbility.contains(uuid) && !shatteredPlayers.contains(uuid)) {
                    event.setCanceled(true);
                    shatteredPlayers.add(uuid);
                    player.setHealth(1.0F);
                    player.setInvisible(true);
                    var level = player.serverLevel();
                    for (int i = 0; i < 40; i++) {
                        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1, player.getZ(), 1, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        public static KeyMapping assembleKey;

        @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
        public static class ModBusEvents {
            @SubscribeEvent
            public static void registerKeys(RegisterKeyMappingsEvent event) {
                assembleKey = new KeyMapping("key.regenmod.assemble", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.misc");
                event.register(assembleKey);
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            var mc = Minecraft.getInstance();
            if (mc.player != null && assembleKey != null) {
                while (assembleKey.consumeClick()) {
                    mc.getConnection().send(new AssemblePayload());
                }
            }
        }

        @SubscribeEvent
        public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
            if (GameplayEvents.limbRegenTimers.containsKey(event.getEntity().getUUID())) {
                event.getRenderer().getModel().rightArm.visible = false;
                event.getRenderer().getModel().rightSleeve.visible = false;
            } else {
                event.getRenderer().getModel().rightArm.visible = true;
                event.getRenderer().getModel().rightSleeve.visible = true;
            }
        }
    }
}
