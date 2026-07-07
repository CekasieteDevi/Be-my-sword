package com.ck7.bemysword.init;

import com.ck7.bemysword.BeMySword;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BeMySword.MOD_ID);

    // Por ahora vacío — aquí agregaremos ítems custom más adelante

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}