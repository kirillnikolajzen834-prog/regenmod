package com.example.regenmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record AssemblePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AssemblePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RegenMod.MODID, "assemble"));
    public static final StreamCodec<FriendlyByteBuf, AssemblePayload> STREAM_CODEC = StreamCodec.unit(new AssemblePayload());
    
    @Override 
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { 
        return TYPE; 
    }
    
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
