package com.example.regenmod;
import com.example.regenmod.network.AssemblePayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(RegenMod.MODID)
public class RegenMod {
    public static final String MODID = "regenmod";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final net.neoforged.neoforge.registries.DeferredItem<Item> REGENERATION_SYRINGE = 
            ITEMS.register("regeneration_syringe", () -> new Item(new Item.Properties().stacksTo(1)));

    public RegenMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::registerNetwork);
    }
    private void registerNetwork(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0");
        registrar.playToServer(AssemblePayload.TYPE, AssemblePayload.STREAM_CODEC, AssemblePayload::handle);
    }
}