package com.ck7.bemysword.entity.goals;

import com.ck7.bemysword.entity.GuardianEntity;
import com.ck7.bemysword.gui.GuardianContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Comparator;
import java.util.EnumSet;

// Si un guardián aliado (mismo dueño) está herido y este guardián lleva una poción
// arrojadiza de Regeneración, se la tira encima.
public class GuardianThrowRegenGoal extends Goal {

    private final GuardianEntity guardian;
    private GuardianEntity allyToHeal;
    private int cooldown = 0;

    private static final double RANGE = 10.0;
    private static final float LOW_HEALTH_FRACTION = 0.5f;
    private static final int THROW_COOLDOWN_TICKS = 100; // 5 segundos entre tiros

    public GuardianThrowRegenGoal(GuardianEntity guardian) {
        this.guardian = guardian;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!guardian.isTame()) return false;
        if (findSplashRegenSlot() < 0) return false;
        allyToHeal = findWoundedAlly();
        return allyToHeal != null;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // acción instantánea, un solo tiro
    }

    @Override
    public void start() {
        int slot = findSplashRegenSlot();
        if (slot >= 0 && allyToHeal != null) {
            guardian.throwSplashPotionAt(slot, allyToHeal);
        }
        allyToHeal = null;
        cooldown = THROW_COOLDOWN_TICKS;
    }

    private int findSplashRegenSlot() {
        GuardianContainer container = guardian.getGuardianContainer();
        for (int i = GuardianContainer.INVENTORY_START; i < container.getContainerSize(); i++) {
            if (guardian.slotHasSplashPotionWithEffect(i, MobEffects.REGENERATION)) {
                return i;
            }
        }
        return -1;
    }

    private GuardianEntity findWoundedAlly() {
        return guardian.level().getEntitiesOfClass(GuardianEntity.class, guardian.getBoundingBox().inflate(RANGE),
                        g -> g != guardian && g.isAlive() && g.isTame()
                                && g.getOwnerUUID() != null && g.getOwnerUUID().equals(guardian.getOwnerUUID())
                                && g.getHealth() < g.getMaxHealth() * LOW_HEALTH_FRACTION)
                .stream()
                .min(Comparator.comparingDouble(guardian::distanceToSqr))
                .orElse(null);
    }
}
