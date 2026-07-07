package com.ck7.bemysword.gui;

import com.ck7.bemysword.client.screen.GuardianScreen;
import com.ck7.bemysword.entity.GuardianEntity;
import com.ck7.bemysword.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;

public class GuardianMenu extends AbstractContainerMenu {

    public final GuardianEntity guardian;
    private final GuardianContainer guardianContainer;
    private final int inventorySize;
    private final int invRows;

    public GuardianMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory,
                (GuardianEntity) playerInventory.player.level().getEntity(buf.readInt()));
    }

    public GuardianMenu(int windowId, Inventory playerInventory, GuardianEntity guardian) {
        super(ModMenuTypes.GUARDIAN_MENU.get(), windowId);
        this.guardian          = guardian;
        this.inventorySize     = GuardianContainer.getInventorySize(guardian.getGuardianLevel());
        this.invRows           = (int) Math.ceil(inventorySize / 9.0);
        this.guardianContainer = guardian.getGuardianContainer();

        // Panel izquierdo — armadura (mismas Y que GuardianScreen)
        this.addSlot(new Slot(guardianContainer, GuardianContainer.SLOT_HELMET, 6, 18) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof ArmorItem a &&
                        a.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.HEAD;
            }
        });
        this.addSlot(new Slot(guardianContainer, GuardianContainer.SLOT_CHESTPLATE, 6, 36) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof ArmorItem a &&
                        a.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.CHEST;
            }
        });
        this.addSlot(new Slot(guardianContainer, GuardianContainer.SLOT_LEGGINGS, 6, 54) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof ArmorItem a &&
                        a.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.LEGS;
            }
        });
        this.addSlot(new Slot(guardianContainer, GuardianContainer.SLOT_BOOTS, 6, 72) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof ArmorItem a &&
                        a.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.FEET;
            }
        });

        // Manos
        this.addSlot(new Slot(guardianContainer, GuardianContainer.SLOT_MAINHAND, 6,  105));
        this.addSlot(new Slot(guardianContainer, GuardianContainer.SLOT_OFFHAND,  25, 105));

        // Inventario guardián — X e Y usando las constantes de GuardianScreen
        // rx = RIGHT_X + 2 = 49, invStartY = INV_START_Y = 51
        for (int row = 0; row < invRows; row++) {
            for (int col = 0; col < 9; col++) {
                int index = GuardianContainer.INVENTORY_START + row * 9 + col;
                if (index >= GuardianContainer.INVENTORY_START + inventorySize) break;
                this.addSlot(new Slot(guardianContainer, index,
                        GuardianScreen.RIGHT_X + 2 + col * 18,
                        GuardianScreen.INV_START_Y + row * 18));
            }
        }

        // Inventario jugador
        // playerInvY = INV_START_Y + invRows*18 + 4 + 13 = INV_START_Y + invRows*18 + 17
        int playerInvY = GuardianScreen.INV_START_Y + invRows * 18 + 17;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                        col + row * 9 + 9,
                        GuardianScreen.RIGHT_X + 2 + col * 18,
                        playerInvY + row * 18));
            }
        }

        // Hotbar
        int hotbarY = playerInvY + 3 * 18 + 4;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    GuardianScreen.RIGHT_X + 2 + col * 18,
                    hotbarY));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return guardian.isAlive() && player.distanceTo(guardian) < 8.0f;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            returnStack = slotStack.copy();
            int guardianSlots = GuardianContainer.FIXED_SLOTS + inventorySize;

            if (index < guardianSlots) {
                if (!this.moveItemStackTo(slotStack, guardianSlots, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (slotStack.getItem() instanceof ArmorItem armor) {
                    int target = switch (armor.getEquipmentSlot()) {
                        case HEAD  -> GuardianContainer.SLOT_HELMET;
                        case CHEST -> GuardianContainer.SLOT_CHESTPLATE;
                        case LEGS  -> GuardianContainer.SLOT_LEGGINGS;
                        case FEET  -> GuardianContainer.SLOT_BOOTS;
                        default    -> -1;
                    };
                    if (target >= 0 && !this.moveItemStackTo(slotStack, target, target + 1, false))
                        return ItemStack.EMPTY;
                } else if (slots.get(GuardianContainer.SLOT_MAINHAND).getItem().isEmpty()) {
                    if (!this.moveItemStackTo(slotStack,
                            GuardianContainer.SLOT_MAINHAND,
                            GuardianContainer.SLOT_MAINHAND + 1, false))
                        return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(slotStack,
                            GuardianContainer.INVENTORY_START,
                            GuardianContainer.INVENTORY_START + inventorySize, false))
                        return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return returnStack;
    }
}