package com.logickllc.pokesensor.api;


import org.robovm.apple.corelocation.CLLocationCoordinate2D;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Spawn implements Serializable {
    public String id;
    public double lat;
    public double lon;
    public String nickname = "";
    public String location = "Unknown";
    public long timeFound;
    public ArrayList<Integer> history = new ArrayList<Integer>();

    public Spawn() {};

    public Spawn(String id, CLLocationCoordinate2D loc) {
        this.id = id;
        this.lat = loc.getLatitude();
        this.lon = loc.getLongitude();
        timeFound = System.currentTimeMillis();
    }

    public Spawn(String id, CLLocationCoordinate2D loc, Integer pokedexNumber) {
        this.id = id;
        this.lat = loc.getLatitude();
        this.lon = loc.getLongitude();
        history.add(pokedexNumber);
        timeFound = System.currentTimeMillis();
    }

    public CLLocationCoordinate2D loc() { return new CLLocationCoordinate2D(lat, lon); }
}
