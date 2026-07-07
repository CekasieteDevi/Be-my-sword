package com.ck7.bemysword.entity;

import com.ck7.bemysword.gui.GuardianContainer;
import com.ck7.bemysword.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.sounds.SoundEvents;
import com.ck7.bemysword.init.ModEntityTypes;

import java.util.List;
import java.util.Random;

public class GuardianEntity extends net.minecraft.world.entity.TamableAnimal implements RangedAttackMob {


    // --- Datos sincronizados cliente/servidor ---
    private static final EntityDataAccessor<Integer> LEVEL =
            SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EXPERIENCE =
            SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FEMALE =
            SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SKIN_INDEX =
            SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.INT);

    private int loveTicks = 0;
    private java.util.UUID lovePlayer = null;
    private int breedCooldown = 0;

    // Saludo de guardianes salvajes al hacerles click derecho (sin manzana dorada)
    private int greetCooldown = 0;
    private static final int GREET_COOLDOWN_TICKS = 24 * 60 * 60 * 20; // 1 día real (20 ticks/seg)

    // Se queda quieto un momento (usado cuando le están tirando una poción arrojadiza,
    // para que no se mueva justo cuando está en el aire y termine esquivándola sin querer)
    private int holdStillTicks = 0;

    // --- Armas: melee o arco, según lo que tenga equipado en la mano principal ---
    // Se instancian dentro de registerGoals() (no como inicializadores de campo): Mob.<init>
    // llama a registerGoals() antes de que corran los inicializadores de campo de esta subclase,
    // así que un inicializador acá quedaría null en ese momento y rompería addGoal() con NPE.
    private MeleeAttackGoal meleeGoal;
    private RangedBowAttackGoal<GuardianEntity> bowGoal;
    private RangedAttackGoal tridentGoal;
    private enum WeaponMode { MELEE, BOW, TRIDENT }
    private WeaponMode weaponMode = WeaponMode.MELEE;

    // Nivel mínimo para poder tirar tridentes (igual que hace un Drowned)
    private static final int TRIDENT_UNLOCK_LEVEL = 5;

    // A partir de este nivel, cada golpe cuerpo a cuerpo tiene chance de ser un salto+crítico
    private static final int MELEE_CRIT_LEVEL = 10;
    private static final float MELEE_CRIT_CHANCE = 0.1f;
    private static final float MELEE_CRIT_DAMAGE_MULTIPLIER = 1.5f;
    private static final float MELEE_CRIT_KNOCKBACK = 1.5f;

    // Nivel máximo: al llegar acá deja de ganar experiencia y subir de nivel
    private static final int MAX_LEVEL = 10;

    // Chance de bloquear un golpe con el escudo (si lo tiene equipado en el offhand),
    // escala linealmente entre nivel 1 y MAX_LEVEL, igual que el resto de las stats.
    private static final float SHIELD_BLOCK_CHANCE_AT_LVL1 = 0.05f;
    private static final float SHIELD_BLOCK_CHANCE_AT_MAX_LVL = 0.25f;

    // Nombres random para guardianes masculinos y femeninos
    private static final List<String> MALE_NAMES = List.of(
            "Aldric", "Bran", "Cedric", "Dorian", "Pecorin",
            "Fenrir", "Gareth", "Hadrian", "Ivan", "Jorin", "El nefasto", "Velga", "Evilblack", "Kuro", "Laz", "Justt", "Fox", "Chaos"
    );
    private static final List<String> FEMALE_NAMES = List.of(
            "Sofi", "Brynn", "Cera", "Dara", "Elara",
            "Fira", "Gwen", "Hana", "Isla", "Jora", "Milanesa", "Luzt", "Alex"
    );

    public GuardianEntity(EntityType<? extends net.minecraft.world.entity.TamableAnimal> type, Level level) {
        super(type, level);
    }

    // --- Atributos base ---
    public static AttributeSupplier.Builder createAttributes() {
        // Stats de nivel 1 (deben coincidir con la fórmula de onLevelUp() en lvl=1)
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LEVEL, 1);
        this.entityData.define(EXPERIENCE, 0);
        this.entityData.define(IS_FEMALE, false);
        this.entityData.define(SKIN_INDEX, -1);
    }

    @Override
    protected void registerGoals() {
        this.meleeGoal = new MeleeAttackGoal(this, 1.2, false);
        this.bowGoal = new RangedBowAttackGoal<>(this, 1.0, 20, 15.0f);
        // Mismos parámetros que usa el Drowned real: intervalo de 40 ticks, radio 10 bloques
        this.tridentGoal = new RangedAttackGoal(this, 1.0, 40, 10.0f);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, meleeGoal);
        this.goalSelector.addGoal(4, new com.ck7.bemysword.entity.goals.GuardianHealGoal(this));
        this.goalSelector.addGoal(4, new com.ck7.bemysword.entity.goals.GuardianPotionGoal(this));
        this.goalSelector.addGoal(4, new com.ck7.bemysword.entity.goals.GuardianThrowRegenGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 10f, 2f, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers());
        // Ataca proactivamente a cualquier monstruo cercano, sin esperar a que golpeen primero.
        // Solo para guardianes ya domesticados: uno salvaje (nivel 1, sin dueño que lo ayude)
        // que le entra de prepo a un Enderman o similar se muere solo antes de que lo encuentres.
        // Excepción: a los Creepers solo los busca si tiene escudo (para bloquear la explosión)
        // o arco (para atacarlos a distancia); si no tiene ninguno, los evita.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                entity -> {
                    if (!isTame()) return false;
                    if (entity instanceof Creeper) {
                        return hasShieldEquipped() || hasBowEquipped() || hasTridentEquipped();
                    }
                    return true;
                }));
    }

    private boolean hasShieldEquipped() {
        return getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).is(Items.SHIELD);
    }

    // Chance de bloquear cualquier golpe si tiene escudo equipado: 5% a nivel 1, hasta 25% a nivel MAX_LEVEL.
    private float getShieldBlockChance() {
        double t = (Math.min(getGuardianLevel(), MAX_LEVEL) - 1) / (double) (MAX_LEVEL - 1);
        return (float) (SHIELD_BLOCK_CHANCE_AT_LVL1 + (SHIELD_BLOCK_CHANCE_AT_MAX_LVL - SHIELD_BLOCK_CHANCE_AT_LVL1) * t);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide
                && hasShieldEquipped()
                && source.getEntity() != null
                && random.nextFloat() < getShieldBlockChance()) {
            playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.9f + random.nextFloat() * 0.2f);
            return false;
        }
        return super.hurt(source, amount);
    }

    private boolean hasBowEquipped() {
        return getMainHandItem().getItem() instanceof BowItem;
    }

    private boolean hasTridentEquipped() {
        return getMainHandItem().getItem() instanceof net.minecraft.world.item.TridentItem;
    }

    // Alterna entre cuerpo a cuerpo, arco y tridente según lo que tenga en la mano principal
    // (el tridente recién se habilita desde TRIDENT_UNLOCK_LEVEL, igual que el Drowned lo tiene siempre).
    private void reassessWeaponGoal() {
        if (level().isClientSide) return;

        WeaponMode desired;
        if (getGuardianLevel() >= TRIDENT_UNLOCK_LEVEL && hasTridentEquipped()) {
            desired = WeaponMode.TRIDENT;
        } else if (hasBowEquipped()) {
            desired = WeaponMode.BOW;
        } else {
            desired = WeaponMode.MELEE;
        }
        if (desired == weaponMode) return;

        this.goalSelector.removeGoal(meleeGoal);
        this.goalSelector.removeGoal(bowGoal);
        this.goalSelector.removeGoal(tridentGoal);
        switch (desired) {
            case BOW -> this.goalSelector.addGoal(3, bowGoal);
            case TRIDENT -> this.goalSelector.addGoal(3, tridentGoal);
            default -> this.goalSelector.addGoal(3, meleeGoal);
        }
        weaponMode = desired;
    }

    // Réplica de Mob.doHurtTarget con un extra: a partir de MELEE_CRIT_LEVEL, chance de saltar
    // y pegar un crítico (x1.5 daño + 1.5 de knockback), solo si el arma equipada es cuerpo a cuerpo.
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean rollCrit = weaponMode == WeaponMode.MELEE
                && getGuardianLevel() >= MELEE_CRIT_LEVEL
                && onGround()
                && random.nextFloat() < MELEE_CRIT_CHANCE;

        if (rollCrit) {
            jumpFromGround();
        }

        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        float knockback = (float) getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        if (target instanceof LivingEntity livingTarget) {
            damage += EnchantmentHelper.getDamageBonus(getMainHandItem(), livingTarget.getMobType());
            knockback += EnchantmentHelper.getKnockbackBonus(this);
        }

        if (rollCrit) {
            damage *= MELEE_CRIT_DAMAGE_MULTIPLIER;
            knockback = Math.max(knockback, MELEE_CRIT_KNOCKBACK);
        }

        int fireAspect = EnchantmentHelper.getFireAspect(this);
        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }

        boolean success = target.hurt(damageSources().mobAttack(this), damage);
        if (success) {
            if (knockback > 0.0f && target instanceof LivingEntity livingTarget) {
                livingTarget.knockback(knockback * 0.5f,
                        Mth.sin(getYRot() * ((float) Math.PI / 180f)),
                        -Mth.cos(getYRot() * ((float) Math.PI / 180f)));
                setDeltaMovement(getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }
            doEnchantDamageEffects(this, target);
            setLastHurtMob(target);
            if (rollCrit) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_CRIT, getSoundSource(), 1.0f, 1.0f);
            }
        }

        return success;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (hasTridentEquipped()) {
            throwTrident(target);
        } else {
            shootArrow(target);
        }
    }

    private void shootArrow(LivingEntity target) {
        ItemStack bow = getMainHandItem();
        AbstractArrow arrow = new Arrow(level(), this);

        int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
        int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
        if (power > 0) {
            arrow.setBaseDamage(arrow.getBaseDamage() + power * 0.5 + 0.5);
        }
        if (punch > 0) {
            arrow.setKnockback(punch);
        }
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) {
            arrow.setSecondsOnFire(100);
        }

        double dx = target.getX() - getX();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontalDist * 0.2, dz, 1.6f, (float) (14 - level().getDifficulty().getId() * 4));
        playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (random.nextFloat() * 0.4f + 0.8f));
        level().addFreshEntity(arrow);
    }

    // Misma fórmula de tiro y sonido que usa el Drowned real al lanzar su tridente.
    private void throwTrident(net.minecraft.world.entity.LivingEntity target) {
        net.minecraft.world.entity.projectile.ThrownTrident trident =
                new net.minecraft.world.entity.projectile.ThrownTrident(level(), this, getMainHandItem().copy());
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333333333333333) - trident.getY();
        double dz = target.getZ() - getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        trident.shoot(dx, dy + horizontalDist * 0.2, dz, 1.6f, (float) (14 - level().getDifficulty().getId() * 4));
        playSound(SoundEvents.DROWNED_SHOOT, 1.0f, 1.0f / (random.nextFloat() * 0.4f + 0.8f));
        level().addFreshEntity(trident);
    }

    // Se queda quieto (no se mueve, no lo controla ningún goal de movimiento) por los
    // ticks indicados. Usado para que un aliado no se corra justo cuando le están
    // tirando una poción arrojadiza encima.
    public void holdStillFor(int ticks) {
        if (level().isClientSide) return;
        if (holdStillTicks <= 0) {
            goalSelector.disableControlFlag(Goal.Flag.MOVE);
        }
        holdStillTicks = Math.max(holdStillTicks, ticks);
        getNavigation().stop();
    }

    // --- Pociones: usadas desde GuardianPotionGoal / GuardianThrowRegenGoal ---

    // ¿El ítem del slot es una poción bebible (no arrojadiza) que da el efecto indicado?
    public boolean slotHasDrinkablePotionWithEffect(int slot, net.minecraft.world.effect.MobEffect effect) {
        ItemStack stack = getGuardianContainer().getItem(slot);
        if (stack.isEmpty() || !stack.is(Items.POTION)) return false;
        for (MobEffectInstance instance : PotionUtils.getMobEffects(stack)) {
            if (instance.getEffect() == effect) return true;
        }
        return false;
    }

    // ¿El ítem del slot es una poción arrojadiza que da el efecto indicado?
    public boolean slotHasSplashPotionWithEffect(int slot, net.minecraft.world.effect.MobEffect effect) {
        ItemStack stack = getGuardianContainer().getItem(slot);
        if (stack.isEmpty() || !stack.is(Items.SPLASH_POTION)) return false;
        for (MobEffectInstance instance : PotionUtils.getMobEffects(stack)) {
            if (instance.getEffect() == effect) return true;
        }
        return false;
    }

    // Se toma la poción bebible del slot indicado: aplica sus efectos y consume el ítem
    // (dejando el frasco de cristal vacío atrás, igual que hace un jugador al beber).
    public void drinkPotionFromSlot(int slot) {
        if (level().isClientSide) return;
        GuardianContainer container = getGuardianContainer();
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty() || !stack.is(Items.POTION)) return;

        for (MobEffectInstance instance : PotionUtils.getMobEffects(stack)) {
            addEffect(new MobEffectInstance(instance));
        }
        playSound(SoundEvents.GENERIC_DRINK, 1.0f, 1.0f);

        stack.shrink(1);
        container.setItem(slot, stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack);

        ItemStack mainhand = container.getItem(GuardianContainer.SLOT_MAINHAND);
        setItemInHand(InteractionHand.MAIN_HAND, mainhand);
    }

    // Le tira la poción arrojadiza del slot indicado a un aliado herido (misma fórmula que usa el Witch real).
    public void throwSplashPotionAt(int slot, net.minecraft.world.entity.LivingEntity target) {
        if (level().isClientSide) return;
        GuardianContainer container = getGuardianContainer();
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty() || !stack.is(Items.SPLASH_POTION)) return;

        double dx = target.getX() - getX();
        double dy = target.getEyeY() - 1.1 - getY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        ThrownPotion thrown = new ThrownPotion(level(), this);
        thrown.setItem(stack.copy());
        thrown.setXRot(thrown.getXRot() - -20.0f);
        thrown.shoot(dx, dy + dist * 0.2, dz, 0.75f, 8.0f);
        playSound(SoundEvents.WITCH_THROW, 1.0f, 0.8f + random.nextFloat() * 0.4f);
        level().addFreshEntity(thrown);

        stack.shrink(1);
        container.setItem(slot, stack);
    }

    // Levanta el escudo cuando hay un Creeper cerca para amortiguar la explosión.
    private void updateShieldBlocking() {
        if (level().isClientSide) return;
        boolean shouldBlock = hasShieldEquipped()
                && getTarget() instanceof Creeper creeper
                && creeper.isAlive()
                && distanceTo(creeper) <= 4.0f;

        if (shouldBlock && !isUsingItem()) {
            startUsingItem(getOffhandItem().is(Items.SHIELD) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        } else if (!shouldBlock && isUsingItem() && getUseItem().is(Items.SHIELD)) {
            stopUsingItem();
        }
    }

    // --- Sistema de diálogos: el guardián comenta ciertas acciones en el chat del dueño ---
    private void say(String message) {
        if (level().isClientSide) return;
        if (getOwner() instanceof Player owner) {
            String name = getCustomName() != null ? getCustomName().getString() : "Guardian";
            owner.sendSystemMessage(Component.literal("§b" + name + "§f: " + message));
        }
    }

    // --- Interacción click derecho ---
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);

        // Adiestramiento
        if (!isTame() && itemInHand.is(Items.GOLDEN_APPLE)) {
            if (!level().isClientSide) {
                if (random.nextInt(3) == 0) { // 33% de probabilidad
                    tame(player);
                    setOrderedToSit(false);
                    level().broadcastEntityEvent(this, (byte) 7); // partículas de corazón
                } else {
                    level().broadcastEntityEvent(this, (byte) 6); // partículas de humo
                }
                if (!player.getAbilities().instabuild) {
                    itemInHand.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Saludo casual: si es salvaje y no le dieron manzana dorada, saluda por nombre (con cooldown)
        if (!isTame() && !level().isClientSide && greetCooldown <= 0) {
            greetCooldown = GREET_COOLDOWN_TICKS;
            player.sendSystemMessage(Component.literal("§b" + (getCustomName() != null ? getCustomName().getString() : "Guardian")
                    + "§f: hi " + player.getName().getString() + "!"));
        }

        // Reproducción con melón brillante
        if (isTame() && isOwnedBy(player) && itemInHand.is(Items.GLISTERING_MELON_SLICE)) {
            if (!level().isClientSide && loveTicks <= 0 && breedCooldown <= 0) {
                loveTicks = 600; // 30 segundos en modo amor
                lovePlayer = player.getUUID();
                level().broadcastEntityEvent(this, (byte) 7); // corazones
                if (!player.getAbilities().instabuild) itemInHand.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Abrir GUI (shift+click para sentarse/pararse)
        if (isTame() && isOwnedBy(player)) {
            if (player.isShiftKeyDown()) {
                boolean sitting = !isOrderedToSit();
                setOrderedToSit(sitting);
                say(sitting ? "Okay I'm staying right here!" : "Okay I will follow you!");
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!level().isClientSide) {
                net.minecraftforge.network.NetworkHooks.openScreen(
                        (net.minecraft.server.level.ServerPlayer) player,
                        new MenuProvider() {
                            @Override
                            public Component getDisplayName() {
                                return Component.literal("Guardian");
                            }

                            @Override
                            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                                    int id, Inventory inv, Player p) {
                                return new com.ck7.bemysword.gui.GuardianMenu(id, inv, GuardianEntity.this);
                            }
                        },
                        buf -> buf.writeInt(GuardianEntity.this.getId())
                );
            }
            // Sin este return, el código caía a super.mobInteract() y el juego interpretaba
            // el click derecho como "usar ítem" en paralelo (por eso empezaba a comer si
            // tenías comida en la mano al abrir el inventario del guardián).
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    // --- Nivel y experiencia ---
    public int getGuardianLevel() {
        return this.entityData.get(LEVEL);
    }

    public void setGuardianLevel(int level) {
        this.entityData.set(LEVEL, level);
    }

    public int getExperience() {
        return this.entityData.get(EXPERIENCE);
    }

    public void addExperience(int amount) {
        if (level().isClientSide) return;
        if (getGuardianLevel() >= MAX_LEVEL) return; // nivel máximo: no gana más experiencia
        int newExp = getExperience() + amount;
        int expNeeded = getExpForNextLevel();
        if (newExp >= expNeeded) {
            setGuardianLevel(getGuardianLevel() + 1);
            this.entityData.set(EXPERIENCE, newExp - expNeeded);
            onLevelUp();
        } else {
            this.entityData.set(EXPERIENCE, newExp);
        }
    }

    private int getExpForNextLevel() {
        return 50 + (getGuardianLevel() * 25);
    }

    private void onLevelUp() {
        int lvl = getGuardianLevel();

        // Actualizar atributos: escalan linealmente entre nivel 1 y nivel 10, y se topan ahí.
        // Nivel 1  -> vida 10, ataque 1, armadura 1, velocidad 0.3
        // Nivel 10 -> vida 30, ataque 5, armadura 10, velocidad 0.5
        int cappedLvl = Math.min(lvl, 10);
        double t = (cappedLvl - 1) / 9.0; // 0.0 en nivel 1, 1.0 en nivel 10
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(10 + 20 * t);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1 + 4 * t);
        getAttribute(Attributes.ARMOR).setBaseValue(1 + 9 * t);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3 + 0.2 * t);
        heal(getMaxHealth()); // curar al subir de nivel

        growContainerIfNeeded();

        // Partículas de level up
        level().broadcastEntityEvent(this, (byte) 7);

        // Notificar al dueño
        if (getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            String name = getCustomName() != null ? getCustomName().getString() : "Guardian";
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§a" + name + " §fsubió al §anivel " + lvl + "§f!"));
        }

        updateCustomName();
    }

    // El inventario del guardián crece con el nivel, pero el GuardianContainer (SimpleContainer)
    // tiene tamaño fijo desde que se crea. Sin esto, GuardianMenu/GuardianScreen calculan el
    // tamaño nuevo según el nivel actual y terminan accediendo a slots que no existen en el
    // array interno del container viejo -> ArrayIndexOutOfBoundsException en bucle.
    private void growContainerIfNeeded() {
        if (guardianContainer == null) return;
        int neededSize = GuardianContainer.getTotalSize(getGuardianLevel());
        if (guardianContainer.getContainerSize() >= neededSize) return;

        // Si alguien tiene el inventario abierto justo ahora, su GuardianMenu ya armó
        // la lista de slots para el tamaño viejo. Swapear el contenedor por uno más
        // grande debajo de un menú abierto desincroniza el inventario y tira
        // ArrayIndexOutOfBoundsException en bucle en el cliente al sincronizar. Se lo
        // cerramos para forzar que lo reabra (ya con el tamaño nuevo).
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (net.minecraft.server.level.ServerPlayer p : serverLevel.players()) {
                if (p.containerMenu instanceof com.ck7.bemysword.gui.GuardianMenu gm && gm.guardian == this) {
                    p.closeContainer();
                }
            }
        }

        GuardianContainer bigger = new GuardianContainer(getGuardianLevel());
        for (int i = 0; i < guardianContainer.getContainerSize(); i++) {
            bigger.setItem(i, guardianContainer.getItem(i));
        }
        guardianContainer = bigger;
    }

    // --- Género ---
    public boolean isFemale() {
        return this.entityData.get(IS_FEMALE);
    }

    public void setFemale(boolean female) {
        this.entityData.set(IS_FEMALE, female);
    }

    // --- Skin (textura de jugador real) ---
    public int getSkinIndex() {
        return this.entityData.get(SKIN_INDEX);
    }

    public void setSkinIndex(int index) {
        this.entityData.set(SKIN_INDEX, index);
    }

    // null si no le tocó ninguna skin custom (usa el fallback steve/alex del renderer)
    public GuardianSkins.Skin getSkin() {
        return GuardianSkins.get(isFemale(), getSkinIndex());
    }

    private void assignRandomSkin() {
        List<GuardianSkins.Skin> pool = isFemale() ? GuardianSkins.FEMALE : GuardianSkins.MALE;
        setSkinIndex(pool.isEmpty() ? -1 : random.nextInt(pool.size()));
    }

    // --- Nombre con nivel y vida---
    private void updateCustomName() {
        String prefix = isFemale() ? "♀ " : "♂ ";
        String currentName = getCustomName() != null ? getCustomName().getString() : "";
        String baseName = currentName
                .replaceAll("♀ ", "")
                .replaceAll("♂ ", "")
                .replaceAll(" §c♥.*?§f", "")
                .replaceAll(" \\[Lv\\.\\d+\\]", "")
                .trim();

        int currentHp = (int) getHealth();
        int maxHp = (int) getMaxHealth();
        String healthText = " §c♥" + currentHp + "/" + maxHp + "§f";

        setCustomName(net.minecraft.network.chat.Component.literal(
                prefix + baseName + healthText + " [Lv." + getGuardianLevel() + "]"));
        setCustomNameVisible(true);
    }

    // --- Spawn inicial ---
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType spawnType,
            net.minecraft.world.entity.SpawnGroupData spawnGroupData,
            net.minecraft.nbt.CompoundTag dataTag) {

        boolean female = random.nextBoolean();
        setFemale(female);
        List<String> names = female ? FEMALE_NAMES : MALE_NAMES;
        String name = names.get(random.nextInt(names.size()));
        setCustomName(net.minecraft.network.chat.Component.literal(name));
        updateCustomName();
        assignRandomSkin();

        setGuardianLevel(1);
        updateCustomName();
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);
    }

    // --- NBT: guardar y cargar datos ---
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GuardianLevel", getGuardianLevel());
        tag.putInt("GuardianExp", getExperience());
        tag.putBoolean("IsFemale", isFemale());
        tag.putInt("SkinIndex", getSkinIndex());
        // Guardar inventario
        if (guardianContainer != null) {
            net.minecraft.nbt.ListTag inventoryTag = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < guardianContainer.getContainerSize(); i++) {
                ItemStack stack = guardianContainer.getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag slotTag = new CompoundTag();
                    slotTag.putByte("Slot", (byte) i);
                    stack.save(slotTag);
                    inventoryTag.add(slotTag);
                }
            }
            tag.put("GuardianInventory", inventoryTag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGuardianLevel(tag.getInt("GuardianLevel"));
        this.entityData.set(EXPERIENCE, tag.getInt("GuardianExp"));
        setFemale(tag.getBoolean("IsFemale"));
        // -1 (sin skin custom, usa el fallback steve/alex) si es un guardián guardado
        // antes de que existiera este campo.
        setSkinIndex(tag.contains("SkinIndex") ? tag.getInt("SkinIndex") : -1);
        // Cargar inventario
        if (tag.contains("GuardianInventory")) {
            guardianContainer = new com.ck7.bemysword.gui.GuardianContainer(getGuardianLevel());
            net.minecraft.nbt.ListTag inventoryTag = tag.getList("GuardianInventory", 10);
            for (int i = 0; i < inventoryTag.size(); i++) {
                CompoundTag slotTag = inventoryTag.getCompound(i);
                int slot = slotTag.getByte("Slot") & 255;
                if (slot < guardianContainer.getContainerSize()) {
                    guardianContainer.setItem(slot, ItemStack.of(slotTag));
                }
            }
        }
        updateCustomName();
    }

    // El guardián no envejece (es TamableAnimal que extiende Animal)
    @Override
    public AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level, AgeableMob mate) {
        return null; // lo implementamos cuando hagamos la reproducción
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false; // no come comida normal
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 10 == 0) {
            updateCustomName();
            if (breedCooldown > 0) breedCooldown--;
            if (greetCooldown > 0) greetCooldown -= 10;
            if (holdStillTicks > 0) {
                holdStillTicks -= 10;
                getNavigation().stop();
                if (holdStillTicks <= 0) {
                    holdStillTicks = 0;
                    goalSelector.enableControlFlag(Goal.Flag.MOVE);
                }
            }

            // Sistema de amor
            if (loveTicks > 0) {
                loveTicks--;
                // Emitir corazones cada 10 ticks
                level().broadcastEntityEvent(this, (byte) 7);

                // Buscar pareja cercana también en modo amor
                GuardianEntity partner = level().getEntitiesOfClass(GuardianEntity.class,
                                getBoundingBox().inflate(8),
                                e -> e != this
                                        && e.isTame()
                                        && e.loveTicks > 0
                                        && e.isFemale() != this.isFemale()
                                        && e.lovePlayer != null
                                        && e.lovePlayer.equals(lovePlayer))
                        .stream().findFirst().orElse(null);

                if (partner != null) {
                    spawnChild(partner);
                    loveTicks = 0;
                    partner.loveTicks = 0;
                }
            }
            updateCustomName();

            // Sincronizar equipamiento desde el container al modelo
            if (guardianContainer != null) {
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                        guardianContainer.getItem(GuardianContainer.SLOT_HELMET));
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                        guardianContainer.getItem(GuardianContainer.SLOT_CHESTPLATE));
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                        guardianContainer.getItem(GuardianContainer.SLOT_LEGGINGS));
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                        guardianContainer.getItem(GuardianContainer.SLOT_BOOTS));
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                        guardianContainer.getItem(GuardianContainer.SLOT_MAINHAND));
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                        guardianContainer.getItem(GuardianContainer.SLOT_OFFHAND));
            }

            reassessWeaponGoal();
            updateShieldBlocking();
        }
    }

    private GuardianContainer guardianContainer;

    public GuardianContainer getGuardianContainer() {
        if (this.guardianContainer == null) {
            this.guardianContainer = new GuardianContainer(getGuardianLevel());
        }
        return this.guardianContainer;
    }

    private void spawnChild(GuardianEntity partner) {
        GuardianEntity baby = ModEntityTypes.GUARDIAN.get().create(level());
        if (baby == null) return;

        // Posición entre los dos padres
        double cx = (getX() + partner.getX()) / 2;
        double cy = getY();
        double cz = (getZ() + partner.getZ()) / 2;
        baby.moveTo(cx, cy, cz, getYRot(), 0);

        // Aplicar finalizeSpawn para nombre, género, nivel
        baby.finalizeSpawn(
                (net.minecraft.world.level.ServerLevelAccessor) level(),
                level().getCurrentDifficultyAt(blockPosition()),
                net.minecraft.world.entity.MobSpawnType.BREEDING,
                null, null
        );

        // Adiestrar automáticamente al dueño
        if (getOwner() instanceof net.minecraft.world.entity.player.Player owner) {
            baby.tame(owner);
        }

        level().addFreshEntity(baby);

        // Corazones y mensaje
        if (getOwner() instanceof net.minecraft.world.entity.player.Player owner) {
            owner.sendSystemMessage(Component.literal(
                    "§a" + baby.getCustomName().getString() + " §fha nacido!"));
        }

        // Cooldown de reproducción (6000 ticks = 5 minutos)
        this.resetLove();
        partner.resetLove();


    }

    public void resetLove() {
        loveTicks = 0;
        lovePlayer = null;
        // Cooldown de 5 minutos antes de poder reproducirse de nuevo
        // Lo guardamos en un campo separado
        breedCooldown = 6000;
    }
}