package com.logickllc.pokesensor;

import org.robovm.apple.corelocation.CLLocationCoordinate2D;

import com.badlogic.gdx.math.Vector2;

import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;

public class NearbyPokemonGPS {
    private NearbyPokemonOuterClass.NearbyPokemon pokemon;
    private CLLocationCoordinate2D coords;
    private Vector2 cartesianCoords;

    public NearbyPokemonGPS(NearbyPokemonOuterClass.NearbyPokemon pokemon, CLLocationCoordinate2D coords) {
        this.pokemon = pokemon;
        this.coords = coords;
    }

    public NearbyPokemonOuterClass.NearbyPokemon getPokemon() {
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
