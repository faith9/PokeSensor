package com.logickllc.pokesensor.api;

import android.app.Activity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.logickllc.pokemapper.AndroidMapHelper;
import com.logickllc.pokemapper.NativePreferences;
import com.logickllc.pokemapper.NearbyPokemonGPS;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import POGOProtos.Map.Pokemon.WildPokemonOuterClass;
import POGOProtos.Map.SpawnPointOuterClass;

public abstract class MapHelper {
    protected Features features;
    public static int NUM_SCAN_SECTORS = 9;
    protected double currentLat;
    protected double currentLon;
    protected boolean searched = false;
    protected boolean abortScan = false;
    protected int scanTime;
    protected int scanDistance;
    protected int scanSpeed;
    protected ArrayList<NearbyPokemonGPS> totalNearbyPokemon = new ArrayList<NearbyPokemonGPS>();
    protected HashSet<Long> totalEncounters = new HashSet<Long>();
    protected HashSet<Long> totalWildEncounters = new HashSet<Long>();
    protected final String TAG = "PokeFinder";
    protected int failedScanLogins = 0;
    public static final int LOCATION_UPDATE_INTERVAL = 5000;
    protected boolean locationOverride = false;
    public static int MAX_SCAN_RADIUS = 70;
    public static final int MAX_SCAN_DISTANCE = 2000;
    public static final int DEFAULT_SCAN_DISTANCE = 90;
    public static final int DEFAULT_SCAN_TIME = 40;
    public static final int DEFAULT_SCAN_SPEED = 20;
    protected boolean locationInitialized = false;
    protected Timer countdownTimer;
    protected ConcurrentHashMap<Long, WildPokemonTime> pokeTimes = new ConcurrentHashMap<Long, WildPokemonTime>();
    protected ConcurrentHashMap<Long, WildPokemonTime> noTimes = new ConcurrentHashMap<>();
    public static final String PREF_MAX_SCAN_DISTANCE = "MaxScanDistance";
    public static final String PREF_MIN_SCAN_TIME = "MinScanTime";
    public static double maxScanDistance, minScanTime, minTotalScanTime;
    public static int maxScanSpeed;
    public static boolean isHexMode = true;
    public ArrayList<Pokestop> visiblePokestops = new ArrayList<>();
    public ArrayList<SpawnPointOuterClass.SpawnPoint> visibleSpawnPoints = new ArrayList<>();

    public ConcurrentHashMap<String, Spawn> spawns = new ConcurrentHashMap<>();
    public int newSpawns = 0;
    public HashSet<String> searchedSpawns = new HashSet<>();
    public boolean collectSpawns = true;
    public boolean showIvs = true;
    public boolean showSpawns = true;
    final String PREF_COLLECT_SPAWNS = "CollectSpawns";
    final String PREF_SHOW_IVS = "ShowIvs";

    public boolean scanning = false;
    public boolean captchaModePopup = true;

    protected int minAttack, minDefense, minStamina, minPercent, minOverride;
    public static final String PREF_MIN_ATTACK = "MinAttack";
    public static final String PREF_MIN_DEFENSE = "MinDefense";
    public static final String PREF_MIN_STAMINA = "MinStamina";
    public static final String PREF_MIN_PERCENT = "MinPercent";
    public static final String PREF_MIN_OVERRIDE = "MinOverride";
    public static final int SPEED_CAP = 10; // Will get empty map objects over this speed. This is 10m/s or 36kph or 22mph

    public long imageSize = 2;
    public boolean showScanDetails = false;
    public boolean ivsAlwaysVisible = true;
    public long defaultMarkersMode = 0;
    public boolean overrideEnabled = false;
    public boolean clearMapOnScan = false;
    public boolean gpsModeNormal = true;
    public boolean use2Captcha = false;
    public boolean useNewApi = true;
    public boolean fallbackApi = true;
    public String captchaKey = "";
    public String newApiKey = "";
    public boolean backgroundScanning = false;
    public String backgroundInterval = "15";
    public boolean backgroundIncludeNearby = true;
    public boolean backgroundScanIvs = true;
    public boolean captchaNotifications = true;
    public boolean showMovesets = true;
    public boolean showHeightWeight = false;
    public boolean onlyScanSpawns = false;

    public synchronized boolean addSpawnInfo(CatchablePokemon pokemon) {
        if (spawns.containsKey(pokemon.getSpawnPointId())) {
            Spawn spawn = spawns.get(pokemon.getSpawnPointId());
            int num = pokemon.getPokemonId().getNumber();
            if (!spawn.history.contains(num)) spawn.history.add(num);
            return false;
        } else {
            final Spawn spawn = new Spawn(pokemon.getSpawnPointId(), new LatLng(pokemon.getLatitude(), pokemon.getLongitude()), pokemon.getPokemonId().getNumber());
            spawns.put(spawn.id, spawn);
            spawn.nickname = "Spawn " + spawns.size();

            if (showSpawns) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        showSpawnOnMap(spawn);
                    }
                };
                features.runOnMainThread(runnable);
            }

            return true;
        }
    }

    public static int getSpeed(int radius) {
        final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MapHelper.MAX_SCAN_RADIUS);
        final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
        final float ITERATIONS = MapHelper.MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

        int hexSectors = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);

        final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
        final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
        int sectors = BOXES_PER_ROW * BOXES_PER_ROW;

        int squareSectors = sectors;

        int squareSize = MINI_SQUARE_SIZE;
        int hexSize = (int) HEX_DISTANCE;
        int distancePerScan = hexSectors <= squareSectors ? hexSize : squareSize;
        String scanType = hexSectors <= squareSectors ? "Hex" : "Square";
        System.out.println("Using " + scanType + " scan for " + radius + "m");

        int speed = (int) Math.ceil(distancePerScan / minScanTime);
        maxScanSpeed = Math.min(SPEED_CAP, speed);
        return speed;
    }

    public int getScanSpeed() {
        return scanSpeed;
    }

    public void setScanSpeed(int scanSpeed) {
        this.scanSpeed = scanSpeed;
    }

    public boolean isLocationOverridden() {
        return locationOverride;
    }

    public void setLocationOverride(boolean locationOverride) {
        this.locationOverride = locationOverride;
    }

    public boolean isLocationInitialized() {
        return locationInitialized;
    }

    public void setLocationInitialized(boolean locationInitialized) {
        this.locationInitialized = locationInitialized;
    }

    public boolean isSearched() {
        return searched;
    }

    public void setSearched(boolean searched) {
        this.searched = searched;
    }

    public double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public double getCurrentLon() {
        return currentLon;
    }

    public void setCurrentLon(double currentLon) {
        this.currentLon = currentLon;
    }

    public boolean isAbortScan() {
        return abortScan;
    }

    public void setAbortScan(boolean abortScan) {
        this.abortScan = abortScan;
    }

    public int getScanTime() {
        return scanTime;
    }

    public void setScanTime(int scanTime) {
        this.scanTime = scanTime;
    }

    public int getScanDistance() {
        return scanDistance;
    }

    public void setScanDistance(int scanDistance) {
        this.scanDistance = scanDistance;
    }

    public int getFailedScanLogins() {
        return failedScanLogins;
    }

    public void setFailedScanLogins(int failedScanLogins) {
        this.failedScanLogins = failedScanLogins;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

    public int getMinAttack() {
        return minAttack;
    }

    public void setMinAttack(int minAttack) {
        this.minAttack = minAttack;
    }

    public int getMinDefense() {
        return minDefense;
    }

    public void setMinDefense(int minDefense) {
        this.minDefense = minDefense;
    }

    public int getMinStamina() {
        return minStamina;
    }

    public void setMinStamina(int minStamina) {
        this.minStamina = minStamina;
    }

    public int getMinPercent() {
        return minPercent;
    }

    public void setMinPercent(int minPercent) {
        this.minPercent = minPercent;
    }

    public int getMinOverride() {
        return minOverride;
    }

    public void setMinOverride(int minOverride) {
        this.minOverride = minOverride;
    }

    public String getTimeString(long time) {
        String timeString = (time / 60) + ":" + String.format("%02d", time % 60);
        return timeString;
    }

    public synchronized void moveMe(double lat, double lon, boolean repositionCamera, boolean reZoom) {

    }

    public synchronized void showPokemonAt(String name, Object loc, long encounterid, boolean hasTime) {

    }

    public abstract boolean updateScanSettings();
    public abstract void wideScan();
    public abstract boolean scanForPokemon(AccountScanner scanner, double lat, double lon);
    public abstract void saveSpawns();
    public abstract void loadSpawns();
    public abstract void deleteAllSpawns();
    public abstract void wideSpawnScan(boolean background);
    public abstract void refreshPrefs();
    public abstract void saveIVFilters();
    public abstract Marker showSpawnOnMap(Spawn spawn);
    public abstract boolean promptForApiKey(Activity activity);
}
