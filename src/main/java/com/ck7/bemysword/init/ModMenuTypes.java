package com.ck7.bemysword.init;

import com.ck7.bemysword.BeMySword;
import com.ck7.bemysword.gui.GuardianMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BeMySword.MOD_ID);

    public static final RegistryObject<MenuType<GuardianMenu>> GUARDIAN_MENU =
            MENU_TYPES.register("guardian_menu",
                    () -> IForgeMenuType.create(GuardianMenu::new));

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }
}