package com.ck7.bemysword.gui;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class GuardianContainer extends SimpleContainer {

    // Slots fijos: 4 armadura + 2 manos = 6
    public static final int ARMOR_SLOTS = 4;
    public static final int HAND_SLOTS = 2;
    public static final int FIXED_SLOTS = ARMOR_SLOTS + HAND_SLOTS;
    public static final int MAX_LEVEL = 10;

    // Slots de inventario incremental según nivel
    public static int getInventorySize(int level) {
        // Nivel 1: 9 slots, cada nivel agrega 3, máximo 54
        return Math.min(9 + (level - 1) * 3, 54);
    }

    public static int getTotalSize(int level) {
        return FIXED_SLOTS + getInventorySize(level);
    }

    // El contenedor real siempre se crea con el tamaño del nivel máximo, sin importar el
    // nivel actual del guardián. Antes se creaba del tamaño del nivel actual y se
    // "agrandaba" (swapeando a un SimpleContainer nuevo) al subir de nivel, pero eso
    // desincroniza cualquier GuardianMenu que ya esté abierto en ese momento en el cliente
    // (su lista de slots quedó armada para el tamaño viejo) -> ArrayIndexOutOfBoundsException
    // en bucle al sincronizar el inventario. GuardianMenu sigue mostrando solo los slots
    // desbloqueados según el nivel actual (getInventorySize); el resto del array simplemente
    // no se muestra todavía.
    public GuardianContainer() {
        super(getTotalSize(MAX_LEVEL));
    }

    // Índices de slots fijos
    public static final int SLOT_HELMET     = 0;
    public static final int SLOT_CHESTPLATE = 1;
    public static final int SLOT_LEGGINGS   = 2;
    public static final int SLOT_BOOTS      = 3;
    public static final int SLOT_MAINHAND   = 4;
    public static final int SLOT_OFFHAND    = 5;
    // Slots de inventario empiezan en índice 6
    public static final int INVENTORY_START = 6;
}