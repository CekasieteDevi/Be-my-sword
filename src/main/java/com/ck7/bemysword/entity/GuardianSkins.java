package com.ck7.bemysword.entity;

import com.ck7.bemysword.BeMySword;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Skins reales de Minecraft que pueden tocarle a un guardián, separadas por género.
 * Cada skin indica si usa el modelo de brazos "slim" (fino, tipo Alex) o "classic" (ancho, tipo Steve).
 */
public class GuardianSkins {

    public record Skin(ResourceLocation texture, boolean slim) {}

    public static final List<Skin> MALE = List.of(
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/male/thatboyday.png"), true),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/male/annoyingbrook.png"), false),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/male/pluewars.png"), true),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/male/skotosey.png"), false),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/male/_azimussuper_.png"), false)
    );

    public static final List<Skin> FEMALE = List.of(
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/female/holaholah.png"), true),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/female/kishi_cao.png"), false),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/female/xjmo.png"), false),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/female/notjudelow.png"), true),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/female/collllcollol.png"), true),
            new Skin(new ResourceLocation(BeMySword.MOD_ID, "textures/entity/guardian/female/chillycoolballs.png"), false)
    );

    public static Skin get(boolean female, int index) {
        List<Skin> pool = female ? FEMALE : MALE;
        if (index < 0 || index >= pool.size()) return null;
        return pool.get(index);
    }
}
