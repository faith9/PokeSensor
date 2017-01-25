package com.logickllc.pokesensor.api;


import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.HashSet;

public class Spawn implements Serializable {
    public String id;
    public double lat;
    public double lon;
    public String nickname;
    public String location;
    public long timeFound;
    public HashSet<Integer> history = new HashSet<Integer>();

    public Spawn(String id, LatLng loc) {
        this.id = id;
        this.lat = loc.latitude;
        this.lon = loc.longitude;
        timeFound = System.currentTimeMillis();
    }

    public Spawn(String id, LatLng loc, Integer pokedexNumber) {
        this.id = id;
        this.lat = loc.latitude;
        this.lon = loc.longitude;
        history.add(pokedexNumber);
        timeFound = System.currentTimeMillis();
    }

    public LatLng loc() { return new LatLng(lat, lon); }
}
