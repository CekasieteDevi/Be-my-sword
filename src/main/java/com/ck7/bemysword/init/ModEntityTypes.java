package com.ck7.bemysword.init;

import com.ck7.bemysword.BeMySword;
import com.ck7.bemysword.entity.GuardianEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BeMySword.MOD_ID);

    public static final RegistryObject<EntityType<GuardianEntity>> GUARDIAN =
            ENTITY_TYPES.register("guardian",
                    () -> EntityType.Builder.<GuardianEntity>of(GuardianEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)  // mismo tamaño que Steve
                            .build("guardian"));

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}