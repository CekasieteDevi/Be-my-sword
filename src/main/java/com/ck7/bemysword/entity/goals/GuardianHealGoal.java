package com.ck7.bemysword.entity.goals;

import com.ck7.bemysword.entity.GuardianEntity;
import com.ck7.bemysword.gui.GuardianContainer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class GuardianHealGoal extends Goal {

    private final GuardianEntity guardian;
    private int eatTimer = 0;
    private int fullCooldown = 0;

    private static final int EAT_DURATION  = 32;
    private static final int FULL_COOLDOWN = 200;

    public GuardianHealGoal(GuardianEntity guardian) {
        this.guardian = guardian;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (fullCooldown > 0) { fullCooldown--; return false; }
        if (guardian.getTarget() != null) return false;
        if (guardian.getHealth() >= guardian.getMaxHealth()) return false;
        return findFoodSlot() >= 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (guardian.getTarget() != null) return false;
        if (guardian.getHealth() >= guardian.getMaxHealth()) return false;
        return findFoodSlot() >= 0;
    }

    @Override
    public void start() {
        beginEating();
    }

    @Override
    public void tick() {
        eatTimer--;

        int slot = findFoodSlot();
        if (slot < 0) return;
        ItemStack food = guardian.getGuardianContainer().getItem(slot);

        // Animación brazo
        if (eatTimer % 8 == 0 && eatTimer > 0) {
            guardian.swing(InteractionHand.MAIN_HAND);
        }

        // Partículas
        if (eatTimer % 2 == 0 && eatTimer > 0 && !food.isEmpty()) {
            if (guardian.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, food),
                        guardian.getX(),
                        guardian.getEyeY() - 0.1,
                        guardian.getZ(),
                        8, 0.25, 0.25, 0.25, 0.2
                );
            }
        }

        // Comer
        if (eatTimer <= 0) {
            doEat(slot);
            // Si sigue necesitando curación, reiniciar
            if (guardian.getHealth() < guardian.getMaxHealth() && findFoodSlot() >= 0) {
                beginEating();
            }
        }
    }

    @Override
    public void stop() {
        // Restaurar ítem de mano principal
        ItemStack mainhand = guardian.getGuardianContainer()
                .getItem(GuardianContainer.SLOT_MAINHAND);
        guardian.setItemInHand(InteractionHand.MAIN_HAND, mainhand);
        fullCooldown = FULL_COOLDOWN;
        eatTimer = 0;
    }

    private void beginEating() {
        eatTimer = EAT_DURATION;
        int slot = findFoodSlot();
        if (slot >= 0) {
            ItemStack food = guardian.getGuardianContainer().getItem(slot);
            guardian.setItemInHand(InteractionHand.MAIN_HAND, food.copy());
        }
    }

    private int findFoodSlot() {
        GuardianContainer container = guardian.getGuardianContainer();
        for (int i = GuardianContainer.INVENTORY_START; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.isEdible()) return i;
        }
        return -1;
    }

    private void doEat(int slot) {
        GuardianContainer container = guardian.getGuardianContainer();
        ItemStack food = container.getItem(slot);
        if (food.isEmpty() || !food.isEdible()) return;

        FoodProperties props = food.getFoodProperties(guardian);
        if (props != null) {
            float heal = props.getNutrition() * 0.5f + props.getSaturationModifier();
            guardian.heal(heal);
        }

        food.shrink(1);
        if (food.isEmpty()) container.setItem(slot, ItemStack.EMPTY);

        // Restaurar mano principal después de comer
        ItemStack mainhand = container.getItem(GuardianContainer.SLOT_MAINHAND);
        guardian.setItemInHand(InteractionHand.MAIN_HAND, mainhand);
    }
}