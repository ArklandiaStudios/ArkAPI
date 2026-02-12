package com.arklandia.arkapi.providers;

import org.bukkit.Chunk;
import org.bukkit.World;

public interface ChunkProvider {
    
    enum ChunkState {
        FROZEN, COLD, WARM, BURNING, SPAWN
    }

    void setChunkState(World world, int x, int z, ChunkState state);
    ChunkState getChunkState(World world, int x, int z);
}
