package com.logickllc.pokesensor.api;

import com.pokegoapi.api.map.pokemon.CatchablePokemon;

public class WildPokemonTime {
    private CatchablePokemon poke;
    private long despawnTimeMs;
    private long encounterID;
    private String spawnID;

    public WildPokemonTime(CatchablePokemon poke, long despawnTimeMs) {
        this.poke = poke;
        this.despawnTimeMs = despawnTimeMs;
    }

    public WildPokemonTime(long encounterID, long despawnTimeMs) {
        this.encounterID = encounterID;
        this.despawnTimeMs = despawnTimeMs;
    }

    public WildPokemonTime(CatchablePokemon poke, long despawnTimeMs, String spawnID) {
        this.poke = poke;
        this.despawnTimeMs = despawnTimeMs;
        this.spawnID = spawnID;
    }

    public CatchablePokemon getPoke() {
        return poke;
    }

    public long getDespawnTimeMs() {
        return despawnTimeMs;
    }

    public long getEncounterID() {
        if (poke != null) return poke.getEncounterId();
        else return encounterID;
    }

    public String getSpawnID() {
        return spawnID;
    }

    public void setSpawnID(String spawnID) {
        this.spawnID = spawnID;
    }
}
