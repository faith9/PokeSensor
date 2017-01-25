package com.logickllc.pokesensor.api;


import com.badlogic.gdx.math.Vector2;

import org.robovm.apple.corelocation.CLLocation;

import java.util.ArrayList;

public class AccountScanner {
    public Account account;
    public int failedSectors = 0;
    public boolean repeat = false;
    public ArrayList<Vector2> points;
    public int pointCursor = 0;
    public Spawn startSpawn;
    public Spawn repeatSpawn;
    public int timeWaited = 0;
    public CLLocation location;
    public ArrayList<String> activeSpawns = new ArrayList<>();

    public AccountScanner(Account account, ArrayList<Vector2> scanPoints) {
        this.account = account;
        this.points = scanPoints;
    }

    public AccountScanner(Account account, Spawn mySpawn) {
        this.account = account;
        this.startSpawn = mySpawn;
    }
}
