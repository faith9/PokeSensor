package com.logickllc.pokemapper;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.AccountScanner;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.MapHelper;
import com.logickllc.pokesensor.api.WildPokemonTime;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class BackgroundHandler {
    /*public void wideScanBackground() {
        final ArrayList<Account> goodAccounts = AccountManager.getGoodAccounts();
        if (goodAccounts.size() == 0) {
            // TODO Notification here instead
            features.longMessage("You don't have any valid accounts!");
            return;
        }

        if (scanning) return;
        else scanning = true;
        if (mMap == null) {
            scanning = false;
            return;
        }
        searched = true;

        newSpawns = 0;
        currentSector = 0;
        features.captchaScreenVisible = false;

        updateScanSettings();
        abortScan = false;
        if (scanDistance > MAX_SCAN_DISTANCE) scanDistance = MAX_SCAN_DISTANCE;

        final Context con = act;

        Runnable main = new Runnable() {
            @Override
            public void run() {
                final Thread scanThread = new Thread() {
                    public void run() {
                        failedScanLogins = 0;

                        features.print(TAG, "Scan distance: " + scanDistance);

                        totalNearbyPokemon.clear();
                        totalEncounters.clear();
                        totalWildEncounters.clear();

                        Vector2D[] boxPoints = getSearchPoints(scanDistance);

                        long SCAN_INTERVAL;

                        if (isHexMode) {
                            final float HEX_DISTANCE = (float) Math.sqrt(3)*MAX_SCAN_RADIUS;
                            SCAN_INTERVAL = Math.round(HEX_DISTANCE / scanSpeed * 1000);
                        } else {
                            final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
                            SCAN_INTERVAL = Math.round(MINI_SQUARE_SIZE / scanSpeed * 1000);
                        }

                        long minScanTime = (long) MapHelper.minScanTime * 1000;

                        features.print(TAG, "Scan interval: " + SCAN_INTERVAL);
                        features.print(TAG,  "Min scan time: " + minScanTime * 1000);
                        features.print(TAG, "Center coord is: (" + currentLat + ", " + currentLon + ")");

                        SCAN_INTERVAL = Math.max(SCAN_INTERVAL, minScanTime);

                        features.resetMapObjects();

                        // Start the new scanning method

                        int scansPerWorker = boxPoints.length / goodAccounts.size();
                        int extraScans = boxPoints.length - scansPerWorker * goodAccounts.size();
                        int cursor = 0;
                        ArrayList<Future> scanThreads = new ArrayList<>();

                        int workersPerThread = goodAccounts.size() / MAX_POOL_THREADS;
                        int extraWorkers = goodAccounts.size() - workersPerThread * MAX_POOL_THREADS;
                        int workerCursor = 0;

                        LatLng center = new LatLng(currentLat, currentLon);

                        ArrayList<ArrayList<AccountScanner>> workerList = new ArrayList<>();

                        for (int n = 0; n < MAX_POOL_THREADS; n++) {
                            int numWorkers = workersPerThread;
                            if (extraWorkers > 0) {
                                extraWorkers--;
                                numWorkers++;
                            }

                            ArrayList<AccountScanner> workers = new ArrayList<>();

                            for (int x = 0; x < numWorkers; x++) {
                                Account account = goodAccounts.get(workerCursor++);
                                AccountScanner scanner = new AccountScanner(account, new ArrayList<Vector2D>());
                                workers.add(scanner);
                            }

                            workerList.add(workers);
                        }

                        //while (cursor < boxPoints.length) {
                        for (int n = 0; n < workersPerThread + 1; n++) {
                            for (ArrayList<AccountScanner> scanAccounts : workerList) {
                                if (n >= scanAccounts.size()) continue;
                                AccountScanner scanner = scanAccounts.get(n);

                                int numScans = scansPerWorker;
                                if (extraScans > 0) {
                                    extraScans--;
                                    numScans++;
                                }

                                if (numScans == 0) continue;

                                for (int y = 0; y < numScans; y++) {
                                    scanner.points.add(boxPoints[cursor]);
                                    cursor++;
                                }
                            }
                        }
                        //}

                        for (ArrayList<AccountScanner> scanAccounts : workerList) {
                            ArrayList<AccountScanner> usableAccounts = new ArrayList<>();
                            for (AccountScanner scanner : scanAccounts) {
                                if (!scanner.points.isEmpty()) usableAccounts.add(scanner);
                            }

                            scanThreads.add(accountScanBackground(usableAccounts, SCAN_INTERVAL, center));
                        }

                        // Insert individual scans here

                        while (AccountManager.isScanning()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                // do nothing here. do it below
                            }
                            if (abortScan) {
                                for (Future thread : scanThreads) {
                                    thread.cancel(true);
                                }
                                scanning = false;

                                break;
                            }
                        }

                        scanning = false;
                    }
                };

                scanThread.start();
            }
        };

        features.runOnMainThread(main);
    }

    public void sendCatchableNotification(ArrayList<String> pokemon, ArrayList<Integer> ivs, Context con) {
        String message = "";
        String title = "";

        if (pokemon.size() > 1) {
            for (int n = 0; n < pokemon.size(); n++) {
                String poke = pokemon.get(n);

                if (n < pokemon.size() - 1) {
                    message += poke + " " + ivs.get(n) + "%, ";
                } else {
                    message += "and " + poke + ivs.get(n) + "%";
                }
            }
        } else {
            message = pokemon.get(0) + " " + ivs.get(0) + "%";
        }

        message = "Found " + message;

        title = "PokeSensor found " + pokemon.size() + " Pokemon on the map!";

        sendNotification(title, message, con);
    }

    public void sendNearbyNotification(ArrayList<String> pokemon, ArrayList<Integer> ivs, Context con) {
        String message = "";
        String title = "";

        if (pokemon.size() > 1) {
            for (int n = 0; n < pokemon.size(); n++) {
                String poke = pokemon.get(n);

                if (n < pokemon.size() - 1) {
                    message += poke + ", ";
                } else {
                    message += "and " + poke;
                }
            }
        } else {
            message = pokemon.get(0);
        }

        message = "Found " + message;

        title = "PokeSensor says " + pokemon.size() + " Pokemon are nearby but outside the scan radius!";
    }

    private void sendNotification(String title, String message, Context con) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(con)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(con, PokeFinderActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(con);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(PokeFinderActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
    }

    public Future accountScanBackground(final ArrayList<AccountScanner> scanners, final long SCAN_INTERVAL, final LatLng center) {
        for (AccountScanner scanner : scanners) {
            scanner.account.setScanning(true);
        }

        Runnable scanThread = new Runnable() {
            public void run() {
                boolean stillScanning = true;
                boolean first = true;
                while (stillScanning) {
                    stillScanning = false;

                    for (AccountScanner scanner : scanners) {
                        if (scanner.account.isScanning()) {
                            stillScanning = true;
                            break;
                        }
                    }

                    if (abortScan) {
                        for (AccountScanner scanner : scanners) {
                            scanner.account.setScanning(false);
                        }
                        return;
                    }

                    try {
                        if (first) {
                            Thread.sleep(1000);
                            first = false;
                        }
                        else Thread.sleep(SCAN_INTERVAL);
                    } catch (InterruptedException e) {
                        if (abortScan) {
                            for (AccountScanner scanner : scanners) {
                                scanner.account.setScanning(false);
                            }
                            return;
                        }
                    }

                    for (final AccountScanner scanner : scanners) {
                        if (!scanner.account.isScanning()) continue;

                        if (abortScan) {
                            for (int n = 0; n < scanners.size(); n++) {
                                scanners.get(n).account.setScanning(false);
                            }
                            return;
                        }

                        if (scanner.repeat) {
                            scanner.pointCursor--;
                            scanner.failedSectors--;
                            currentSector--;
                        }
                        scanner.repeat = false;

                        if (scanner.pointCursor >= scanner.points.size()) {
                            scanner.account.setScanning(false);
                            continue;
                        }

                        final LatLng loc = cartesianToCoord(scanner.points.get(scanner.pointCursor), center);
                        scanner.pointCursor++;

                        scanner.repeat = !scanForPokemonBackground(scanner, loc.latitude, loc.longitude);

                        while ((use2Captcha && scanner.account.isSolvingCaptcha()) && !abortScan) {
                            try {
                                scanner.repeat = true;
                                Thread.sleep(1000);
                                if (abortScan) {
                                    for (int n = 0; n < scanners.size(); n++) {
                                        scanners.get(n).account.setScanning(false);
                                    }
                                    return;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                if (abortScan) {
                                    for (int n = 0; n < scanners.size(); n++) {
                                        scanners.get(n).account.setScanning(false);
                                    }
                                    return;
                                }
                            }
                        }

                        if (scanner.account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
                            print(scanner.account.getUsername() + " is aborting from a captcha.");
                            scanner.account.setScanning(false);
                            continue;
                        }

                        if (!scanner.repeat && scanner.pointCursor == scanner.points.size()) {
                            print(scanner.account.getUsername() + " is finished scanning.");
                            scanner.account.setScanning(false);
                        }
                    }
                }

                for (AccountScanner scanner : scanners) {
                    scanner.account.setScanning(false);
                }
            }
        };

        return run(scanThread);
    }

    public boolean scanForPokemonBackground(AccountScanner scanner, double lat, double lon) {
        Account account = scanner.account;
        PokemonGo go = account.go;
        final ArrayList<Long> removables = new ArrayList<>();
        try {
            if (useNewApi && PokeHashProvider.exceededRpm && !fallbackApi) {
                return true;
            }
            features.print(TAG, "Scanning (" + lat + "," + lon + ")...");

            go.setLocation(lat, lon, 0);
            Thread.sleep(200);
            try {
                features.refreshMapObjects(account);
            } catch (CaptchaActiveException c) {
                account.checkExceptionForCaptcha(c);
            }
            Thread.sleep(200);

            scanner.activeSpawns.clear();

            if (use2Captcha) {
                if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED || account.getStatus() == Account.AccountStatus.SOLVING_CAPTCHA) {
                    return false;
                }
            } else {
                if (captchaModePopup) {
                    if (features.checkForCaptcha(account)) {
                        return false;
                    }
                } else {
                    if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
                        return false;
                    }
                }
            }

            // Figure out which pokemon in noTimes would show up in this search
            // All these Pokemon should show up in this search. Otherwise they must've despawned
            ArrayList<WildPokemonTime> currentPokes = getNoTimePokesInSector(lat, lon);

            final List<CatchablePokemon> wildPokes = features.getCatchablePokemon(account, 15);

            for (CatchablePokemon poke : wildPokes) {
                if (poke.getPokemonId().getNumber() > Features.NUM_POKEMON) activateGen2();
                if (collectSpawns && addSpawnInfo(poke)) {
                    newSpawns++;

                    print("Found new spawn: " + poke.getSpawnPointId());
                }
                searchedSpawns.add(poke.getSpawnPointId());

                int pokedexNumber = poke.getPokemonId().getNumber();

                if (!features.filter.get(pokedexNumber) && (!showIvs || !overrideEnabled)) continue;

                totalWildEncounters.add(poke.getEncounterId());

                if (!scanner.activeSpawns.contains(poke.getSpawnPointId())) scanner.activeSpawns.add(poke.getSpawnPointId());

                if ((!pokeTimes.containsKey(poke.getEncounterId()) && !noTimes.containsKey(poke.getEncounterId())) || ((poke.getExpirationTimestampMs() > 0 && poke.getExpirationTimestampMs() - System.currentTimeMillis() > 0 && poke.getExpirationTimestampMs() - System.currentTimeMillis() <= 3600000) && noTimes.containsKey(poke.getEncounterId()))) {
                    try {
                        //removables.add(poke.getEncounterId()); // If it's in noTimes and makes it here, that means we need to update the timer
                        for (WildPokemonTime temp : noTimes.values()) {
                            if (temp.getSpawnID().equals(poke.getSpawnPointId())) {
                                removables.add(temp.getPoke().getEncounterId());
                            }
                        }

                        for (Long temp : removables) {
                            noTimes.remove(temp);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }

                    long timeMs = poke.getExpirationTimestampMs() - System.currentTimeMillis();
                    if (timeMs > 0 && timeMs <= 3600000) {
                        long despawnTime = System.currentTimeMillis() + timeMs;
                        pokeTimes.put(poke.getEncounterId(), new WildPokemonTime(poke, despawnTime));
                        features.print(TAG, poke.getPokemonId() + " will despawn at " + despawnTime);
                    } else if (timeMs < 0 || timeMs > 3600000) {
                        noTimes.put(poke.getEncounterId(), new WildPokemonTime(poke, System.currentTimeMillis(), poke.getSpawnPointId()));
                    }
                }

            }

            final List<CatchablePokemon> pokes = features.getCatchablePokemon(account, 15);
            final List<NearbyPokemon> nearbyPokes = features.getNearbyPokemon(account, 15);

            if (wildPokes.isEmpty() && pokes.isEmpty() && nearbyPokes.isEmpty()) {
                return true;
            } else {
                // If a current Pokemon is not found on rescanning this sector, it must be gone
                for (WildPokemonTime currentPoke : currentPokes) {
                    boolean contains = false;
                    for (CatchablePokemon poke : wildPokes) {
                        if (poke.getEncounterId() == currentPoke.getEncounterID()) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        features.print(TAG, "Looks like " + currentPoke.getPoke().getPokemonId().name() + " with encounter ID " + currentPoke.getEncounterID() + " is no longer at spawn point " + currentPoke.getSpawnID() + ". Gonna remove it now.");
                        removables.add(currentPoke.getEncounterID());
                        noTimes.remove(currentPoke.getEncounterID());
                        pokeTimes.remove(currentPoke.getEncounterID());
                    }
                }
            }

            for (NearbyPokemon poke : nearbyPokes) {
                int pokedexNumber = poke.getPokemonId().getNumber();

                if (pokedexNumber > Features.NUM_POKEMON) activateGen2();

                if (!features.filter.get(pokedexNumber) && (!showIvs || !overrideEnabled)) continue;
                totalNearbyPokemon.add(new NearbyPokemonGPS(poke, new LatLng(lat, lon)));
                totalEncounters.add(poke.getEncounterId());
            }

            if (nearbyPokes.isEmpty()) features.print("PokeFinder", "No nearby pokes :(");
            for (NearbyPokemon poke : nearbyPokes) {
                features.print("PokeFinder", "Found NearbyPokemon: " + poke.toString());
                features.print(TAG, "Distance in meters: " + poke.getDistanceInMeters());
                //mMap.addCircle(new CircleOptions().center(new CLLocationCoordinate2D(go.getLatitude(), go.getLongitude())).radius(poke.getDistanceInMeters()));
            }

            if (wildPokes.isEmpty()) features.print("PokeFinder", "No wild pokes :(");
            for (final CatchablePokemon poke : wildPokes) {
                features.print("PokeFinder", "Found WildPokemon: " + poke.toString());

                int pokedexNumber = poke.getPokemonId().getNumber();

                if (!features.filter.get(pokedexNumber) && (!showIvs || !overrideEnabled)) continue;

                //String ivs = "";
                ArrayList<String> ivHolder = new ArrayList<>();
                boolean hide = false;
                if (showIvs) {
                    for (CatchablePokemon pokemon : pokes) {
                        if (poke.getPokemonId().getNumber() > Features.NUM_POKEMON) activateGen2();
                        if (poke.getSpawnPointId().equals(pokemon.getSpawnPointId())) {
                            try {
                                hide = encounterPokemon(poke, pokemon, ivHolder, pokedexNumber);
                            } catch (Throwable e) {
                                if (!features.filter.get(pokedexNumber)) hide = true;

                                account.checkExceptionForCaptcha(e);

                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                } else if (!features.filter.get(pokedexNumber)) {
                    features.print(TAG, "Filtered out " + poke.getPokemonId().name() + " for being a " + poke.getPokemonId().name());
                    hide = true;
                }

                if (hide) {
                    //features.print(TAG, "IV filtered out " + poke.getPokemonId().name() + " for having " + ivs);
                    noTimes.remove(poke.getEncounterId());
                    pokeTimes.remove(poke.getEncounterId());
                    continue;
                }

                final String myIvs = ivHolder.size() > 0 ? ivHolder.get(0) : "";
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (Long id : removables) {
                                if (!noTimes.containsKey(id)) {
                                    Marker marker = pokeMarkers.remove(id);
                                    if (marker != null) marker.remove();
                                }
                            }
                            removables.clear();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Crashlytics.logException(e);
                        }

                        long time = poke.getExpirationTimestampMs() - System.currentTimeMillis();
                        if (time > 0 && time <= 3600000) {
                            String ms = String.format("%06d", time);
                            int sec = Integer.parseInt(ms.substring(0, 3));
                            //features.print(TAG, "Time string: " + time);
                            //features.print(TAG, "Time shifted: " + (Long.parseLong(time) >> 16));
                            features.print(TAG, "Time till hidden seconds: " + sec + "s");
                            //features.print(TAG, "Data for " + poke.getPokemonId() + ":\n" + poke);
                            showPokemonAt(poke.getPokemonId().name(), poke.getPokemonId().getNumber(), new LatLng(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), true, myIvs);
                        } else {
                            features.print(TAG, "No valid expiry time given");
                            showPokemonAt(poke.getPokemonId().name(), poke.getPokemonId().getNumber(), new LatLng(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), false, myIvs);
                        }
                    }
                };

                features.runOnMainThread(r);
            }

            return true;
        } catch (Throwable e) {
            if (account.checkExceptionForCaptcha(e)) {

            } else {

            }

            e.printStackTrace();

            return false;
        }
    }*/
}
