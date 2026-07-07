package com.ck7.bemysword.events;

import com.ck7.bemysword.BeMySword;
import com.ck7.bemysword.entity.GuardianEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.ck7.bemysword.entity.GuardianEntity;
import com.ck7.bemysword.init.ModEntityTypes;
import com.ck7.bemysword.world.GuardianSpawnData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

@Mod.EventBusSubscriber(modid = BeMySword.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldEventHandler {

    private static final TagKey<Structure> VILLAGE_TAG =
            TagKey.create(Registries.STRUCTURE, new ResourceLocation("minecraft", "village"));

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();

        // Solo nos interesa cuando muere un mob hostil
        if (!(killed instanceof Monster)) return;
        if (killed.level().isClientSide) return;

        // Calcular exp según dificultad del mob
        int expReward = getExpReward(killed);

        // Buscar al guardián que hizo el kill o asistió
        if (event.getSource().getEntity() instanceof GuardianEntity guardian) {
            guardian.addExperience(expReward);
            guardian.pauseAfterKill();
            notifyLevelUp(guardian);
        }
        // Si el killer fue el jugador, buscar guardianes cercanos adiestrados que asistieron
        else if (event.getSource().getEntity() instanceof Player player) {
            player.level().getEntitiesOfClass(GuardianEntity.class,
                    killed.getBoundingBox().inflate(16),
                    g -> g.isTame() && g.isOwnedBy(player)
            ).forEach(g -> {
                g.addExperience(expReward / 2); // 50% exp por asistencia
                notifyLevelUp(g);
            });
        }
    }

    private static int getExpReward(LivingEntity entity) {
        // Exp base según la vida máxima del mob
        int baseExp = (int)(entity.getMaxHealth() * 2);
        return Math.max(5, Math.min(baseExp, 100));
    }

    private static void notifyLevelUp(GuardianEntity guardian) {
        // Partículas de nivel al subir (se maneja en addExperience de GuardianEntity)
        // El nombre se actualiza automáticamente en onLevelUp()
    }

    @SubscribeEvent
    public static void onVillageChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        // Detectar directamente la estructura de aldea (igual que GuardianHouseStructure),
        // en vez de esperar a que un Villager haga spawn natural: así los guardianes
        // aparecen apenas se genera/carga el chunk de la aldea, no recién al reabrir el mundo.
        chunk.getAllStarts().forEach((structure, start) -> {
            if (!start.isValid()) return;

            Holder<Structure> holder = serverLevel.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .wrapAsHolder(structure);
            if (!holder.is(VILLAGE_TAG)) return;

            // Solo generar guardianes una vez por aldea, para siempre (no en cada
            // recarga del chunk): se marca el chunk que tiene el StructureStart en
            // datos persistentes del mundo, en vez de confiar en "no hay guardianes
            // cerca ahora mismo" (que falla si se alejaron o murieron).
            GuardianSpawnData spawnData = GuardianSpawnData.get(serverLevel);
            if (spawnData.alreadySpawned(chunk.getPos())) return;

            spawnData.markSpawned(chunk.getPos());
            spawnGuardiansInVillage(serverLevel, chunk);
        });
    }

    private static void spawnGuardiansInVillage(ServerLevel serverLevel, LevelChunk chunk) {
        // Usamos SOLO el chunk que ya sabemos cargado (el que disparó ChunkEvent.Load) para
        // la posición y la altura. Nunca serverLevel.getHeight()/getCurrentDifficultyAt() con
        // una posición de OTRO chunk (p. ej. el centro de la estructura, que en una aldea
        // grande puede caer varios chunks más allá): eso dispara Level.getChunkAt() intentando
        // cargar ese chunk ajeno desde dentro del propio callback de carga de chunk, lo cual es
        // un wait reentrante que nunca se resuelve (deadlock: el servidor se "tranca", deja de
        // generar mundo, con uso de CPU casi nulo).
        int chunkOriginX = chunk.getPos().getMinBlockX();
        int chunkOriginZ = chunk.getPos().getMinBlockZ();

        DifficultyInstance difficulty = new DifficultyInstance(
                serverLevel.getDifficulty(), serverLevel.getDayTime(), 0L, serverLevel.getMoonBrightness());

        // Spawnear entre 2 y 4 guardianes
        int count = 2 + serverLevel.getRandom().nextInt(3);
        for (int i = 0; i < count; i++) {
            GuardianEntity guardian = ModEntityTypes.GUARDIAN.get().create(serverLevel);
            if (guardian == null) continue;

            int spawnX = chunkOriginX + serverLevel.getRandom().nextInt(16);
            int spawnZ = chunkOriginZ + serverLevel.getRandom().nextInt(16);
            int spawnY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);

            // Seguridad: si la altura calculada es inválida (por debajo del mundo),
            // usar el nivel del mar como respaldo en vez de spawnear al vacío.
            if (spawnY <= serverLevel.getMinBuildHeight()) {
                spawnY = serverLevel.getSeaLevel();
            }

            guardian.moveTo(spawnX + 0.5, spawnY, spawnZ + 0.5, 0, 0);
            guardian.finalizeSpawn(serverLevel, difficulty, MobSpawnType.STRUCTURE, null, null);
            serverLevel.addFreshEntity(guardian);
        }
    }
}