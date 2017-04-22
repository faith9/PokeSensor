package com.logickllc.pokesensor.api;


import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;

public class AccountScanner {
    public Account account;
    public int failedSectors = 0;
    public boolean repeat = false;
    public ArrayList<Vector2D> points;
    public int pointCursor = 0;
    public Spawn startSpawn;
    public Spawn repeatSpawn;
    public int timeWaited = 0;
    public LatLng location;
    public ArrayList<String> activeSpawns = new ArrayList<>();

    public AccountScanner(Account account, ArrayList<Vector2D> scanPoints) {
        this.account = account;
        this.points = scanPoints;
    }

    public AccountScanner(Account account, Spawn mySpawn) {
        this.account = account;
        this.startSpawn = mySpawn;
    }

    public AccountScanner(Account account) {
        this.account = account;
    }
}
