package com.logickllc.pokesensor.api;


public class PokestopData {
    String name;
    String description;
    double lat;
    double lon;

    public PokestopData(String name, String description, double lat, double lon) {
        this.name = name;
        this.description = description;
        this.lat = lat;
        this.lon = lon;
    }
}
