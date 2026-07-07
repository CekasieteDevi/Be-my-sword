package com.ck7.bemysword.world;

import com.ck7.bemysword.BeMySword;
import com.ck7.bemysword.entity.GuardianEntity;
import com.ck7.bemysword.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BeMySword.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuardianHouseStructure {

    private static final ResourceLocation GUARDIAN_HOUSE =
            new ResourceLocation(BeMySword.MOD_ID, "guardian_house");

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        // Iterar sobre todas las estructuras en este chunk
        chunk.getAllStarts().forEach((structure, start) -> {
            if (!start.isValid()) return;

            ResourceLocation id = serverLevel.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .getKey(structure);

            if (GUARDIAN_HOUSE.equals(id)) {
                // Solo generar guardianes una vez por casa, para siempre (no en cada
                // recarga del chunk): se marca el chunk que tiene el StructureStart
                // en datos persistentes del mundo.
                GuardianSpawnData spawnData = GuardianSpawnData.get(serverLevel);
                if (spawnData.alreadySpawned(chunk.getPos())) return;
                spawnData.markSpawned(chunk.getPos());

                BlockPos center = new BlockPos(
                        start.getBoundingBox().getCenter());
                spawnGuardiansInHouse(serverLevel, center);
            }
        });
    }

    private static void spawnGuardiansInHouse(ServerLevel level, BlockPos pos) {
        spawnGuardian(level, pos, false);
        spawnGuardian(level, pos.offset(2, 0, 0), true);
    }

    private static void spawnGuardian(ServerLevel level, BlockPos pos, boolean female) {
        GuardianEntity guardian = ModEntityTypes.GUARDIAN.get().create(level);
        if (guardian == null) return;

        guardian.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        // No usar level.getCurrentDifficultyAt(pos): el centro de la estructura puede caer
        // en un chunk distinto al que disparó ChunkEvent.Load (las estructuras jigsaw abarcan
        // varios chunks), y getCurrentDifficultyAt() intenta cargar ESE chunk vía getChunkAt().
        // Pedir un chunk ajeno desde dentro del propio callback de carga de chunk es un wait
        // reentrante que nunca se resuelve (deadlock: el servidor se "tranca", deja de generar
        // mundo, con uso de CPU casi nulo). Construimos la dificultad sin tocar ningún chunk.
        DifficultyInstance difficulty = new DifficultyInstance(
                level.getDifficulty(), level.getDayTime(), 0L, level.getMoonBrightness());
        guardian.finalizeSpawn(level, difficulty, MobSpawnType.STRUCTURE, null, null);
        guardian.setFemale(female);
        level.addFreshEntity(guardian);
    }
}