package com.example.regenmod.client;
import com.example.regenmod.RegenMod;
import com.example.regenmod.event.GameplayEvents;
import com.example.regenmod.network.AssemblePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RegenMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    public static KeyMapping assembleKey;

    @Mod.EventBusSubscriber(modid = RegenMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
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
        var player = event.getEntity();
        if (GameplayEvents.limbRegenTimers.containsKey(player.getUUID())) {
            event.getRenderer().getModel().rightArm.visible = false;
            event.getRenderer().getModel().rightSleeve.visible = false;
        } else {
            event.getRenderer().getModel().rightArm.visible = true;
            event.getRenderer().getModel().rightSleeve.visible = true;
        }
    }
}