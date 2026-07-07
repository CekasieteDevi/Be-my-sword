package com.ck7.bemysword.entity.goals;

import com.ck7.bemysword.entity.GuardianEntity;
import com.ck7.bemysword.gui.GuardianContainer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

// El guardián se toma pociones de combate de su inventario cuando detecta un enemigo:
// Fuerza, Velocidad y Regeneración, en ese orden de prioridad, una por vez.
public class GuardianPotionGoal extends Goal {

    private final GuardianEntity guardian;
    private int drinkTimer = 0;
    private int slotBeingDrunk = -1;

    private static final int DRINK_DURATION = 32;

    public GuardianPotionGoal(GuardianEntity guardian) {
        this.guardian = guardian;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (guardian.getTarget() == null) return false;
        return findCombatPotionSlot() >= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return slotBeingDrunk >= 0 && drinkTimer > 0;
    }

    // Sin esto, en cuanto el objetivo queda al alcance el MeleeAttackGoal (prioridad más alta)
    // le roba la mano a mitad de trago y nunca llega a tomarse la poción: queda cambiando
    // de ítem en bucle sin pegar ni tomar nada. Una vez que empieza a beber, que termine.
    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public void start() {
        guardian.setSuppressMainHandSync(true);
        drinkTimer = DRINK_DURATION;
        slotBeingDrunk = findCombatPotionSlot();
        if (slotBeingDrunk >= 0) {
            guardian.setItemInHand(InteractionHand.MAIN_HAND,
                    guardian.getGuardianContainer().getItem(slotBeingDrunk).copy());
        }
    }

    @Override
    public void tick() {
        drinkTimer--;
        if (drinkTimer % 8 == 0 && drinkTimer > 0) {
            guardian.swing(InteractionHand.MAIN_HAND);
        }
        // Mismas partículas que usa un aldeano al subir de nivel
        if (drinkTimer % 2 == 0 && drinkTimer > 0 && guardian.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    guardian.getX(), guardian.getEyeY(), guardian.getZ(),
                    4, 0.3, 0.3, 0.3, 0.0);
        }
        if (drinkTimer <= 0 && slotBeingDrunk >= 0) {
            guardian.drinkPotionFromSlot(slotBeingDrunk);
            slotBeingDrunk = -1;
        }
    }

    @Override
    public void stop() {
        guardian.setSuppressMainHandSync(false);
        ItemStack mainhand = guardian.getGuardianContainer().getItem(GuardianContainer.SLOT_MAINHAND);
        guardian.setItemInHand(InteractionHand.MAIN_HAND, mainhand);
        drinkTimer = 0;
        slotBeingDrunk = -1;
    }

    // Busca, en orden de prioridad (fuerza > velocidad > regeneración), una poción bebible
    // cuyo efecto todavía no esté activo.
    private int findCombatPotionSlot() {
        GuardianContainer container = guardian.getGuardianContainer();
        int strengthSlot = -1;
        int speedSlot = -1;
        int regenSlot = -1;

        for (int i = GuardianContainer.INVENTORY_START; i < container.getContainerSize(); i++) {
            if (strengthSlot < 0 && !guardian.hasEffect(MobEffects.DAMAGE_BOOST)
                    && guardian.slotHasDrinkablePotionWithEffect(i, MobEffects.DAMAGE_BOOST)) {
                strengthSlot = i;
            }
            if (speedSlot < 0 && !guardian.hasEffect(MobEffects.MOVEMENT_SPEED)
                    && guardian.slotHasDrinkablePotionWithEffect(i, MobEffects.MOVEMENT_SPEED)) {
                speedSlot = i;
            }
            if (regenSlot < 0 && !guardian.hasEffect(MobEffects.REGENERATION)
                    && guardian.slotHasDrinkablePotionWithEffect(i, MobEffects.REGENERATION)) {
                regenSlot = i;
            }
        }

        if (strengthSlot >= 0) return strengthSlot;
        if (speedSlot >= 0) return speedSlot;
        return regenSlot;
    }
}
