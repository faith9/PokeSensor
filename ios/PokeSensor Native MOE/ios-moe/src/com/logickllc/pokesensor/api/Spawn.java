package com.logickllc.pokesensor.api;


import java.io.Serializable;
import java.util.ArrayList;

import apple.corelocation.struct.CLLocationCoordinate2D;

public class Spawn implements Serializable {
    public String id;
    public double lat;
    public double lon;
    public String nickname = "";
    public String location = "Unknown";
    public long timeFound;
    public ArrayList<Integer> history = new ArrayList<Integer>();
    public int despawnMinute = -1;
    public int despawnSecond = -1;

    public Spawn() {};

    public Spawn(String id, CLLocationCoordinate2D loc) {
        this.id = id;
        this.lat = loc.latitude();
        this.lon = loc.longitude();
        timeFound = System.currentTimeMillis();
    }

    public Spawn(String id, CLLocationCoordinate2D loc, Integer pokedexNumber) {
        this.id = id;
        this.lat = loc.latitude();
        this.lon = loc.longitude();
        history.add(pokedexNumber);
        timeFound = System.currentTimeMillis();
    }

    public CLLocationCoordinate2D loc() { return new CLLocationCoordinate2D(lat, lon); }
}
