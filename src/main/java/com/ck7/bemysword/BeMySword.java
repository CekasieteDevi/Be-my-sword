package com.ck7.bemysword;

import com.ck7.bemysword.init.ModEntityTypes;
import com.ck7.bemysword.init.ModItems;
import com.ck7.bemysword.init.ModMenuTypes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BeMySword.MOD_ID)
public class BeMySword {

    public static final String MOD_ID = "bemysword";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BeMySword() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Be My Sword initialized.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                LOGGER.info("Be My Sword common setup complete.")
        );
    }
}