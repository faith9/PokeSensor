package com.logickllc.pokemapper;


import com.google.android.gms.maps.model.LatLng;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class NearbyPokemonGPS {
    private NearbyPokemon pokemon;
    private LatLng coords;
    private Vector2D cartesianCoords;

    public NearbyPokemonGPS(NearbyPokemon pokemon, LatLng coords) {
        this.pokemon = pokemon;
        this.coords = coords;
    }

    public NearbyPokemon getPokemon() {
        return pokemon;
    }

    public LatLng getCoords() {
        return coords;
    }

    public Vector2D getCartesianCoords() {
        return cartesianCoords;
    }

    public void setCartesianCoords(Vector2D cartesianCoords) {
        this.cartesianCoords = cartesianCoords;
    }
}
