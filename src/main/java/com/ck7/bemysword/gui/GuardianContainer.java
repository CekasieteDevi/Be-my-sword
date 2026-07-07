package com.ck7.bemysword.gui;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class GuardianContainer extends SimpleContainer {

    // Slots fijos: 4 armadura + 2 manos = 6
    public static final int ARMOR_SLOTS = 4;
    public static final int HAND_SLOTS = 2;
    public static final int FIXED_SLOTS = ARMOR_SLOTS + HAND_SLOTS;

    // Slots de inventario incremental según nivel
    public static int getInventorySize(int level) {
        // Nivel 1: 9 slots, cada nivel agrega 3, máximo 54
        return Math.min(9 + (level - 1) * 3, 54);
    }

    public static int getTotalSize(int level) {
        return FIXED_SLOTS + getInventorySize(level);
    }

    public GuardianContainer(int level) {
        super(getTotalSize(level));
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