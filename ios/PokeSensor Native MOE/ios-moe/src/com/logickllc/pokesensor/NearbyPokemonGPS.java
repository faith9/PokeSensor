package com.logickllc.pokesensor;


import com.badlogic.gdx.math.Vector2;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;

import apple.corelocation.struct.CLLocationCoordinate2D;

public class NearbyPokemonGPS {
    private NearbyPokemon pokemon;
    private CLLocationCoordinate2D coords;
    private Vector2 cartesianCoords;

    public NearbyPokemonGPS(NearbyPokemon pokemon, CLLocationCoordinate2D coords) {
        this.pokemon = pokemon;
        this.coords = coords;
    }

    public NearbyPokemon getPokemon() {
        return pokemon;
    }

    public CLLocationCoordinate2D getCoords() {
        return coords;
    }

    public Vector2 getCartesianCoords() {
        return cartesianCoords;
    }

    public void setCartesianCoords(Vector2 cartesianCoords) {
        this.cartesianCoords = cartesianCoords;
    }
}
