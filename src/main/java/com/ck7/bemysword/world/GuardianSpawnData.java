package com.ck7.bemysword.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Recuerda para siempre qué chunks (aldeas / guardian_house) ya generaron sus guardianes,
 * para que no se repita el spawn cada vez que el chunk se vuelve a cargar (reabrir el mundo,
 * alejarse y volver, etc.).
 */
public class GuardianSpawnData extends SavedData {

    private static final String NAME = "bemysword_guardian_spawns";

    private final Set<Long> spawnedChunks = new HashSet<>();

    public static GuardianSpawnData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GuardianSpawnData::load, GuardianSpawnData::new, NAME);
    }

    public boolean alreadySpawned(ChunkPos pos) {
        return spawnedChunks.contains(pos.toLong());
    }

    public void markSpawned(ChunkPos pos) {
        spawnedChunks.add(pos.toLong());
        setDirty();
    }

    public static GuardianSpawnData load(CompoundTag tag) {
        GuardianSpawnData data = new GuardianSpawnData();
        for (long l : tag.getLongArray("SpawnedChunks")) {
            data.spawnedChunks.add(l);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("SpawnedChunks", spawnedChunks.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
}
