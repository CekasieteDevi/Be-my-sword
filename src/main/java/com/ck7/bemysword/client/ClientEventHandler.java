package com.ck7.bemysword.client;

import com.ck7.bemysword.BeMySword;
import com.ck7.bemysword.client.renderer.GuardianRenderer;
import com.ck7.bemysword.client.screen.GuardianScreen;
import com.ck7.bemysword.init.ModEntityTypes;
import com.ck7.bemysword.init.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BeMySword.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.GUARDIAN.get(), GuardianRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.GUARDIAN_MENU.get(), GuardianScreen::new)
        );
    }
}