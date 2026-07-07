package com.ck7.bemysword.events;

import com.ck7.bemysword.BeMySword;
import com.ck7.bemysword.entity.GuardianEntity;
import com.ck7.bemysword.init.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BeMySword.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventHandler {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.GUARDIAN.get(), GuardianEntity.createAttributes().build());
    }
}