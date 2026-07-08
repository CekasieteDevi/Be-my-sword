package com.ck7.bemysword.client.screen;

import com.ck7.bemysword.gui.GuardianContainer;
import com.ck7.bemysword.gui.GuardianMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;

public class GuardianScreen extends AbstractContainerScreen<GuardianMenu> {

    private static final int COL_BG         = 0xFF1A1A2E;
    private static final int COL_PANEL      = 0xFF16213E;
    private static final int COL_PANEL2     = 0xFF0F3460;
    private static final int COL_BORDER     = 0xFF533483;
    private static final int COL_SLOT_BG    = 0xFF0D0D1A;
    private static final int COL_HEADER_TXT = 0xFFE94560;
    private static final int COL_LABEL      = 0xFF9B9BC8;
    private static final int COL_HP_BAR     = 0xFFE94560;
    private static final int COL_HP_BG      = 0xFF3A0A12;
    private static final int COL_EXP_BAR    = 0xFF44FF44;
    private static final int COL_EXP_BG     = 0xFF1A3A1A;
    private static final int COL_STATS_TXT  = 0xFFF5C542;

    // Layout — todas las constantes Y son relativas al origen de la GUI (y)
    // Panel izquierdo
    static final int LEFT_W       = 46;
    // Panel derecho
    public static final int RIGHT_X      = 47;   // LEFT_W + 1
    static final int GUI_W        = 220;  // RIGHT_X + 9*18 + 5

    // Coordenadas Y panel derecho (absolutas desde y=0 de la GUI)
    static final int HEADER_H     = 14;   // y+0  → y+14
    static final int HP_BAR_Y     = 15;   // y+15, h=5  → bottom y+20
    static final int STATS_TEXT_Y = 23;   // y+23 — ATK/DEF/SPD, sube con el nivel
    static final int EXP_TEXT_Y   = 33;   // y+33
    static final int EXP_BAR_Y    = 43;   // y+43, h=4  → bottom y+47
    static final int INV_LABEL_Y  = 51;   // y+51
    public static final int INV_START_Y  = 61;   // y+61  (INV_LABEL_Y + 10)

    private int invRows;
    private int guiH;

    // Calculados en constructor
    private int sepY;
    private int playerLabelY;
    private int playerInvY;
    private int hotbarY;

    public GuardianScreen(GuardianMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.invRows = (int) Math.ceil(
                GuardianContainer.getInventorySize(menu.guardian.getGuardianLevel()) / 9.0);

        // sep = INV_START_Y + invRows*18 + 4
        this.sepY        = INV_START_Y + invRows * 18 + 4;
        this.playerLabelY = sepY + 3;
        this.playerInvY  = sepY + 13;
        this.hotbarY     = playerInvY + 3 * 18 + 4;
        // guiH = hotbarY + 18 + 6
        this.guiH        = hotbarY + 18 + 6;

        this.imageWidth  = GUI_W;
        this.imageHeight = guiH;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX     = imageWidth + 500;
        this.inventoryLabelX = imageWidth + 500;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        int x = (this.width  - imageWidth)  / 2;
        int y = (this.height - imageHeight) / 2;

        // Fondo + borde
        g.fill(x, y, x + imageWidth, y + imageHeight, COL_BG);
        drawBorder(g, x, y, imageWidth, imageHeight, COL_BORDER);

        // Panel izquierdo
        g.fill(x + 1, y + 1, x + LEFT_W, y + imageHeight - 1, COL_PANEL);
        g.fill(x + 1, y + 1, x + LEFT_W, y + HEADER_H, COL_PANEL2);
        g.drawCenteredString(font, "EQUIP", x + LEFT_W / 2, y + 3, COL_HEADER_TXT);

        drawLabeledSlot(g, x + 5, y + 17, "H");
        drawLabeledSlot(g, x + 5, y + 35, "C");
        drawLabeledSlot(g, x + 5, y + 53, "L");
        drawLabeledSlot(g, x + 5, y + 71, "B");

        g.fill(x + 3, y + 91, x + LEFT_W - 3, y + 92, COL_BORDER);
        g.drawString(font, "HANDS", x + 4, y + 94, COL_LABEL, false);
        drawSlotBg(g, x + 5,  y + 104);
        drawSlotBg(g, x + 24, y + 104);

        // Panel derecho
        int rx = x + RIGHT_X;
        g.fill(rx, y + 1, x + imageWidth - 1, y + imageHeight - 1, COL_PANEL);
        g.fill(rx, y + 1, x + imageWidth - 1, y + HEADER_H, COL_PANEL2);

        String name = menu.guardian.getCustomName() != null
                ? menu.guardian.getCustomName().getString() : "Guardian";
        g.drawString(font, name, rx + 3, y + 3, COL_HEADER_TXT, false);

        int barW = imageWidth - RIGHT_X - 5;
        int barX = rx + 2;

        // Barra HP
        int maxHp = (int) menu.guardian.getMaxHealth();
        int curHp = (int) menu.guardian.getHealth();
        g.fill(barX, y + HP_BAR_Y, barX + barW, y + HP_BAR_Y + 5, COL_HP_BG);
        g.fill(barX, y + HP_BAR_Y, barX + (int)(curHp / (float) maxHp * barW), y + HP_BAR_Y + 5, COL_HP_BAR);
        drawBorder(g, barX, y + HP_BAR_Y, barW, 5, COL_BORDER);

        // Estadísticas de combate (suben con el nivel)
        double atk = menu.getAttackDamageDisplay();
        double def = menu.guardian.getAttributeValue(Attributes.ARMOR);
        double spd = menu.guardian.getAttributeValue(Attributes.MOVEMENT_SPEED);
        g.drawString(font,
                String.format("ATK %.1f   DEF %.1f   SPD %.2f", atk, def, spd),
                barX + 2, y + STATS_TEXT_Y, COL_STATS_TXT, false);

        // Texto + barra EXP
        int curExp  = menu.guardian.getExperience();
        int nextExp = 50 + menu.guardian.getGuardianLevel() * 25;
        g.drawString(font,
                "EXP " + curExp + "/" + nextExp + " → Lv." + (menu.guardian.getGuardianLevel() + 1),
                barX + 2, y + EXP_TEXT_Y, COL_EXP_BAR, false);
        g.fill(barX, y + EXP_BAR_Y, barX + barW, y + EXP_BAR_Y + 4, COL_EXP_BG);
        g.fill(barX, y + EXP_BAR_Y, barX + (int)(curExp / (float) nextExp * barW), y + EXP_BAR_Y + 4, COL_EXP_BAR);
        drawBorder(g, barX, y + EXP_BAR_Y, barW, 4, 0xFF226622);

        // Label + slots inventario guardián
        g.drawString(font, "Guardian inventory", rx + 3, y + INV_LABEL_Y, COL_LABEL, false);
        for (int row = 0; row < invRows; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBg(g, rx + 2 + col * 18, y + INV_START_Y + row * 18);

        // Separador + label + slots inventario jugador
        g.fill(rx + 2, y + sepY, x + imageWidth - 3, y + sepY + 1, COL_BORDER);
        g.drawString(font, "Your inventory", rx + 3, y + playerLabelY, COL_LABEL, false);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBg(g, rx + 2 + col * 18, y + playerInvY + row * 18);

        // Hotbar
        for (int col = 0; col < 9; col++)
            drawSlotBg(g, rx + 2 + col * 18, y + hotbarY);
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 16, y + 16, COL_SLOT_BG);
        drawBorder(g, x, y, 16, 16, COL_BORDER);
    }

    private void drawLabeledSlot(GuiGraphics g, int x, int y, String label) {
        drawSlotBg(g, x, y);
        g.drawString(font, label, x + 1, y + 1, COL_LABEL, false);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color);
        g.fill(x,         y + h - 1, x + w,     y + h,     color);
        g.fill(x,         y,         x + 1,     y + h,     color);
        g.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g);
        super.render(g, mx, my, partialTick);
        this.renderTooltip(g, mx, my);
    }
}