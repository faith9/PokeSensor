package com.logickllc.pokemapper;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.AccountScanner;
import com.logickllc.pokesensor.api.ExceptionCatchingRunnable;
import com.logickllc.pokesensor.api.ExceptionCatchingThreadFactory;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.MapHelper;
import com.logickllc.pokesensor.api.Spawn;
import com.logickllc.pokesensor.api.WildPokemonTime;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;
import com.pokegoapi.api.map.pokemon.encounter.EncounterResult;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.exceptions.hash.HashException;
import com.pokegoapi.exceptions.hash.HashLimitExceededException;
import com.pokegoapi.main.PokemonMeta;
import com.pokegoapi.util.PokeDictionary;
import com.pokegoapi.util.Signature;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import POGOProtos.Enums.PokemonIdOuterClass;

public class AndroidMapHelper extends MapHelper {
    public static final String PREF_SCAN_DISTANCE = "ScanDistance";
    public static final String PREF_SCAN_TIME = "ScanTime";
    public static final String PREF_SCAN_SPEED = "ScanSpeed";
    public static final String PREF_COLLECT_SPAWNS = "CollectSpawns";
    public static final String PREF_SHOW_IVS = "ShowIvs";
    public static final String PREF_SHOW_SPAWNS = "ShowSpawns";
    public static final String PREF_CAPTCHA_MODE_POPUP = "CaptchaModePopup";
    public static final String PREF_IMAGE_SIZE = "ImageSize";
    public static final String PREF_NUM_POKEMON = "NumPokemon";
    public static final String PREF_SHOW_SCAN_DETAILS = "ShowScanDetails";
    public static final String PREF_IVS_ALWAYS_VISIBLE = "IvsAlwaysVisible";
    public static final String PREF_DEFAULT_MARKERS_MODE = "DefaultMarkersMode";
    public static final String PREF_OVERRIDE_ENABLED = "OverrideEnabled";
    public static final String PREF_CLEAR_MAP_ON_SCAN = "ClearMapOnScan";
    public static final String PREF_GPS_MODE_NORMAL = "GPSModeNormal";
    public static final String PREF_USE_2CAPTCHA = "Use2Captcha";
    public static final String PREF_USE_NEW_API = "UseNewApi";
    public static final String PREF_FALLBACK_API = "FallbackApi";
    public static final String PREF_2CAPTCHA_KEY = "CaptchaKey";
    public static final String PREF_NEW_API_KEY = "NewApiKey";
    public static final float DEFAULT_ZOOM = 16f;

    private Activity act;
    private Marker myMarker;
    private GoogleMap mMap;
    private ConcurrentHashMap<Long, Marker> pokeMarkers = new ConcurrentHashMap<Long, Marker>();
    private int paddingLeft = 5, paddingRight = 5, paddingTop = 5, paddingBottom = 5;
    private Circle scanCircle;
    private String scanDialogMessage;
    private Circle tempScanPointCircle;
    private BitmapDescriptor scanPointIcon;
    private BitmapDescriptor pokestopIcon;
    public final String IMAGE_EXTENSION = ".png";

    private Hashtable<Account, Marker> scanPoints = new Hashtable<>();
    private Hashtable<Account, Circle> scanPointCircles = new Hashtable<>();
    private int currentSector = 0;
    private TextView scanText;
    private ProgressBar scanBar;
    private boolean loadedSpawns = false;
    private ArrayList<Marker> mapSpawns = new ArrayList<>();
    private BitmapDescriptor spawnIcon;

    public static final boolean CAN_SHOW_IMAGES = false;
    public static final String NUMBER_MARKER_FOLDER = "pokemarkers/";
    private ArrayList<Circle> scanPointCirclesDetailed = new ArrayList<>();
    public static double SCAN_DETAIL_CIRCLE_ALPHA = 0.2;
    public static final String BLANK_NAME_MARKER = "blank_name_marker_small_";
    public static ThreadPoolExecutor pool;
    public static final int MAX_POOL_THREADS = 10;

    private boolean hashLimitWarning = false;
    private boolean hashWarning = false;
    private Timer rpmTimer;
    private final int RPM_REFRESH_RATE = 500;
    private Timer resetRpmTimer;
    private final int RESET_RPM_REFRESH_RATE = 60000;
    private boolean resetTimerRunning = false;
    private FrameLayout markerLayout;
    private FrameLayout customImageLayout;
    private final int GOOD_ACCOUNTS_IMAGE_DP = 32;
    private Bitmap tempBitmap = null, tempCustomBitmap = null;
    private float INITIAL_NAME_MARKER_TEXT_SIZE = Float.NEGATIVE_INFINITY;

    public String[] types = "BULBASAUR,grass|IVYSAUR,grass|VENUSAUR,grass|CHARMANDER,fire|CHARMELEON,fire|CHARIZARD,fire|SQUIRTLE,water|WARTORTLE,water|BLASTOISE,water|CATERPIE,bug|METAPOD,bug|BUTTERFREE,bug|WEEDLE,bug|KAKUNA,bug|BEEDRILL,bug|PIDGEY,normal|PIDGEOTTO,normal|PIDGEOT,normal|RATTATA,normal|RATICATE,normal|SPEAROW,normal|FEAROW,normal|EKANS,poison|ARBOK,poison|PIKACHU,electric|RAICHU,electric|SANDSHREW,ground|SANDSLASH,ground|NIDORAN_FEMALE,poison|NIDORINA,poison|NIDOQUEEN,poison|NIDORAN_MALE,poison|NIDORINO,poison|NIDOKING,poison|CLEFAIRY,fairy|CLEFABLE,fairy|VULPIX,fire|NINETALES,fire|JIGGLYPUFF,normal|WIGGLYTUFF,normal|ZUBAT,poison|GOLBAT,poison|ODDISH,grass|GLOOM,grass|VILEPLUME,grass|PARAS,bug|PARASECT,bug|VENONAT,bug|VENOMOTH,bug|DIGLETT,ground|DUGTRIO,ground|MEOWTH,normal|PERSIAN,normal|PSYDUCK,water|GOLDUCK,water|MANKEY,fighting|PRIMEAPE,fighting|GROWLITHE,fire|ARCANINE,fire|POLIWAG,water|POLIWHIRL,water|POLIWRATH,water|ABRA,psychic|KADABRA,psychic|ALAKAZAM,psychic|MACHOP,fighting|MACHOKE,fighting|MACHAMP,fighting|BELLSPROUT,grass|WEEPINBELL,grass|VICTREEBEL,grass|TENTACOOL,water|TENTACRUEL,water|GEODUDE,rock|GRAVELER,rock|GOLEM,rock|PONYTA,fire|RAPIDASH,fire|SLOWPOKE,water|SLOWBRO,water|MAGNEMITE,electric|MAGNETON,electric|FARFETCHD,normal|DODUO,normal|DODRIO,normal|SEEL,water|DEWGONG,water|GRIMER,poison|MUK,poison|SHELLDER,water|CLOYSTER,water|GASTLY,ghost|HAUNTER,ghost|GENGAR,ghost|ONIX,rock|DROWZEE,psychic|HYPNO,psychic|KRABBY,water|KINGLER,water|VOLTORB,electric|ELECTRODE,electric|EXEGGCUTE,grass|EXEGGUTOR,grass|CUBONE,ground|MAROWAK,ground|HITMONLEE,fighting|HITMONCHAN,fighting|LICKITUNG,normal|KOFFING,poison|WEEZING,poison|RHYHORN,ground|RHYDON,ground|CHANSEY,normal|TANGELA,grass|KANGASKHAN,normal|HORSEA,water|SEADRA,water|GOLDEEN,water|SEAKING,water|STARYU,water|STARMIE,water|MR_MIME,psychic|SCYTHER,bug|JYNX,ice|ELECTABUZZ,electric|MAGMAR,fire|PINSIR,bug|TAUROS,normal|MAGIKARP,water|GYARADOS,water|LAPRAS,water|DITTO,normal|EEVEE,normal|VAPOREON,water|JOLTEON,electric|FLAREON,fire|PORYGON,normal|OMANYTE,rock|OMASTAR,rock|KABUTO,rock|KABUTOPS,rock|AERODACTYL,rock|SNORLAX,normal|ARTICUNO,ice|ZAPDOS,electric|MOLTRES,fire|DRATINI,dragon|DRAGONAIR,dragon|DRAGONITE,dragon|MEWTWO,psychic|MEW,psychic|CHIKORITA,grass|BAYLEEF,grass|MEGANIUM,grass|CYNDAQUIL,fire|QUILAVA,fire|TYPHLOSION,fire|TOTODILE,water|CROCONAW,water|FERALIGATR,water|SENTRET,normal|FURRET,normal|HOOTHOOT,normal|NOCTOWL,normal|LEDYBA,bug|LEDIAN,bug|SPINARAK,bug|ARIADOS,bug|CROBAT,poison|CHINCHOU,water|LANTURN,water|PICHU,electric|CLEFFA,fairy|IGGLYBUFF,normal|TOGEPI,fairy|TOGETIC,fairy|NATU,psychic|XATU,psychic|MAREEP,electric|FLAAFFY,electric|AMPHAROS,electric|BELLOSSOM,grass|MARILL,water|AZUMARILL,water|SUDOWOODO,rock|POLITOED,water|HOPPIP,grass|SKIPLOOM,grass|JUMPLUFF,grass|AIPOM,normal|SUNKERN,grass|SUNFLORA,grass|YANMA,bug|WOOPER,water|QUAGSIRE,water|ESPEON,psychic|UMBREON,dark|MURKROW,dark|SLOWKING,water|MISDREAVUS,ghost|UNOWN,psychic|WOBBUFFET,psychic|GIRAFARIG,normal|PINECO,bug|FORRETRESS,bug|DUNSPARCE,normal|GLIGAR,ground|STEELIX,steel|SNUBBULL,fairy|GRANBULL,fairy|QWILFISH,water|SCIZOR,bug|SHUCKLE,bug|HERACROSS,bug|SNEASEL,dark|TEDDIURSA,normal|URSARING,normal|SLUGMA,fire|MAGCARGO,fire|SWINUB,ice|PILOSWINE,ice|CORSOLA,water|REMORAID,water|OCTILLERY,water|DELIBIRD,ice|MANTINE,water|SKARMORY,steel|HOUNDOUR,dark|HOUNDOOM,dark|KINGDRA,water|PHANPY,ground|DONPHAN,ground|PORYGON2,normal|STANTLER,normal|SMEARGLE,normal|TYROGUE,fighting|HITMONTOP,fighting|SMOOCHUM,ice|ELEKID,electric|MAGBY,fire|MILTANK,normal|BLISSEY,normal|RAIKOU,electric|ENTEI,fire|SUICUNE,water|LARVITAR,rock|PUPITAR,rock|TYRANITAR,rock|LUGIA,psychic|HO_OH,fire|CELEBI,psychic".split("\\|");

    public AndroidMapHelper(Activity act) {
        this.act = act;
    }

    public Marker getMyMarker() {
        return myMarker;
    }

    public void setMyMarker(Marker myMarker) {
        this.myMarker = myMarker;
    }

    public ConcurrentHashMap<Long, Marker> getPokeMarkers() {
        return pokeMarkers;
    }

    public void setPokeMarkers(ConcurrentHashMap<Long, Marker> pokeMarkers) {
        this.pokeMarkers = pokeMarkers;
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    public Circle getScanCircle() {
        return scanCircle;
    }

    public void setScanCircle(Circle scanCircle) {
        this.scanCircle = scanCircle;
    }

    public String getScanDialogMessage() {
        return scanDialogMessage;
    }

    public void setScanDialogMessage(String scanDialogMessage) {
        this.scanDialogMessage = scanDialogMessage;
    }

    public BitmapDescriptor getScanPointIcon() {
        return scanPointIcon;
    }

    public void setScanPointIcon(BitmapDescriptor scanPointIcon) {
        this.scanPointIcon = scanPointIcon;
    }

    public GoogleMap getmMap() {
        return mMap;
    }

    public void setmMap(GoogleMap mMap) {
        this.mMap = mMap;
    }

    public void refreshTempScanCircle() {
        Runnable circleRunnable = new Runnable() {
            @Override
            public void run() {
                if (tempScanPointCircle != null) tempScanPointCircle.remove();
                tempScanPointCircle = mMap.addCircle(new CircleOptions().center(new LatLng(currentLat, currentLon)).strokeWidth(1).radius(scanDistance).strokeColor(Color.argb(128, 0, 0, 0)));
            }
        };
        features.runOnMainThread(circleRunnable);
    }

    public void removeTempScanCircle() {
        Runnable circleRunnable = new Runnable() {
            @Override
            public void run() {
                if (tempScanPointCircle != null) tempScanPointCircle.remove();
            }
        };
        features.runOnMainThread(circleRunnable);
    }

    public synchronized void moveMe(double lat, double lon, boolean repositionCamera, boolean reZoom) {
        // Add a marker in Sydney and move the camera
        LatLng me = new LatLng(lat, lon);
        if (myMarker != null) myMarker.remove();
        if (mMap == null) return;
        myMarker = mMap.addMarker(new MarkerOptions().position(me).title("Me"));
        if (repositionCamera) {
            if (reZoom) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, DEFAULT_ZOOM));
            else mMap.animateCamera(CameraUpdateFactory.newLatLng(me));
        }
        currentLat = lat;
        currentLon = lon;
        refreshTempScanCircle();
    }

    public void saveScanEvent(int numPokemon, float area) {
        try {
            Answers.getInstance().logCustom(new CustomEvent("Scan")
                    .putCustomAttribute("Pokemon Found", numPokemon)
                    .putCustomAttribute("Scan Area", area));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void wideScan() {
        final ArrayList<Account> goodAccounts = AccountManager.getGoodAccounts();
        if (goodAccounts.size() == 0) {
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
        removeTempScanCircle();

        newSpawns = 0;
        currentSector = 0;
        features.captchaScreenVisible = false;

        updateScanSettings();
        abortScan = false;
        if (scanDistance > MAX_SCAN_DISTANCE) scanDistance = MAX_SCAN_DISTANCE;

        final Context con = act;
        final LinearLayout scanLayout = (LinearLayout) act.findViewById(R.id.scanLayout);
        scanBar = (ProgressBar) act.findViewById(R.id.scanBar);
        scanText = (TextView) act.findViewById(R.id.scanText);

        Runnable main = new Runnable() {
            @Override
            public void run() {
                if (clearMapOnScan) {
                    try {
                        //final ArrayList<Long> ids = new ArrayList<Long>(noTimes.keys().);
                        Map<Long, WildPokemonTime> temp = noTimes;

                        for (Long id : temp.keySet()) {
                            try {
                                features.print(TAG, "Removed poke marker!");
                                Marker marker = pokeMarkers.remove(id);
                                if (marker != null) marker.remove();
                            } catch (Exception e) {
                                // don't worry about it. Just in case the marker is removed while iterating
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    noTimes.clear();
                }

                removeScanPoints();
                removeScanPointCircles();

                scanBar.setProgress(0);
                //scanBar.setMax(NUM_SCAN_SECTORS);
                scanText.setText("");

                scanLayout.setVisibility(View.VISIBLE);
                scanBar.setVisibility(View.VISIBLE);
                scanLayout.requestLayout();
                scanLayout.bringToFront();
                scanLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

                DisplayMetrics metrics = act.getResources().getDisplayMetrics();
                paddingTop = Math.round(scanLayout.getMeasuredHeight() * metrics.density) + 2;
                int goodAccountsPadding = Math.round(GOOD_ACCOUNTS_IMAGE_DP * metrics.density) + 5;
                paddingTop += goodAccountsPadding;

                features.print(TAG, "Padding top: " + paddingTop);
                mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

                final Thread scanThread = new Thread() {
                    public void run() {
                        failedScanLogins = 0;

                        Runnable circleRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (scanCircle != null) scanCircle.remove();
                                scanCircle = mMap.addCircle(new CircleOptions().center(new LatLng(currentLat, currentLon)).strokeWidth(1).radius(scanDistance).strokeColor(Color.argb(128, 0, 0, 255)));
                            }
                        };
                        features.runOnMainThread(circleRunnable);

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

                        scanBar.setMax(NUM_SCAN_SECTORS);

                        scanPointCircles.clear();
                        scanPoints.clear();

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

                        for (int n = 0; n < MAX_POOL_THREADS; n++) {
                            int numWorkers = workersPerThread;
                            if (extraWorkers > 0) {
                                extraWorkers--;
                                numWorkers++;
                            }

                            ArrayList<AccountScanner> scanAccounts = new ArrayList<>();
                            for (int x = 0; x < numWorkers; x++) {
                                int numScans = scansPerWorker;
                                if (extraScans > 0) {
                                    extraScans--;
                                    numScans++;
                                }

                                if (numScans == 0) break;

                                ArrayList<Vector2D> scanPoints = new ArrayList<>();
                                for (int y = 0; y < numScans; y++) {
                                    scanPoints.add(boxPoints[cursor]);
                                    cursor++;
                                }

                                Account account = goodAccounts.get(workerCursor++);
                                AccountScanner scanner = new AccountScanner(account, scanPoints);
                                scanAccounts.add(scanner);
                            }
                            scanThreads.add(accountScan(scanAccounts, SCAN_INTERVAL, center));
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
                                features.longMessage(R.string.abortScan);
                                scanning = false;

                                break;
                            }
                        }

                        Runnable dismissRunnable = new Runnable() {
                            @Override
                            public void run() {
                                removeScanPoints();
                                if (!showScanDetails) {
                                    removeScanPointCircles();
                                }

                                scanLayout.setVisibility(View.GONE);

                                DisplayMetrics metrics = act.getResources().getDisplayMetrics();
                                int paddingTop = Math.round(GOOD_ACCOUNTS_IMAGE_DP * metrics.density) + 5;

                                mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                            }
                        };
                        features.runOnMainThread(dismissRunnable);


                        if (collectSpawns) {
                            if (newSpawns > 1)
                                features.shortMessage("Found " + newSpawns + " new spawn points and added them to My Spawns!");
                            else if (newSpawns == 1)
                                features.shortMessage("Found 1 new spawn point and added it to My Spawns!");
                        }

                        saveScanEvent(totalWildEncounters.size(), (float) (Math.PI * Math.pow(scanDistance, 2)));

                        scanning = false;
                    }
                };

                scanLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scanThread.interrupt();
                        abortScan = true;
                        scanLayout.setVisibility(View.GONE);

                        DisplayMetrics metrics = act.getResources().getDisplayMetrics();
                        int paddingTop = Math.round(GOOD_ACCOUNTS_IMAGE_DP * metrics.density) + 5;

                        mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

                        removeScanPoints();
                        if (!showScanDetails) {
                            removeScanPointCircles();
                        }
                    }
                });

                scanThread.start();
            }
        };

        features.runOnMainThread(main);
    }

    public void removeScanPoints() {
        for (Marker scanPoint : scanPoints.values()) {
            if (scanPoint != null) scanPoint.remove();
        }
    }

    public void removeScanPointCircles() {
        try {
            if (scanPointCirclesDetailed != null) {
                for (Circle circle : scanPointCirclesDetailed) {
                    if (circle != null) circle.remove();
                }
            }
            if (scanPointCircles != null) {
                for (Circle scanPointCircle : scanPointCircles.values()) {
                    if (scanPointCircle != null) scanPointCircle.remove();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Future accountScan(final ArrayList<AccountScanner> scanners, final long SCAN_INTERVAL, final LatLng center) {
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
                            if (showScanDetails && scanner.account.circle != null) {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        scanner.account.circle.remove();
                                        scanPointCirclesDetailed.remove(scanner.account.circle);
                                    }
                                };
                                features.runOnMainThread(runnable);
                            }
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

                        try {
                            Runnable progressRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (scanPointIcon == null) setScanPointIcon(BitmapDescriptorFactory.fromResource(R.drawable.scan_point_icon));
                                    updateScanLayout();
                                    updateScanPoint(loc, scanner.account);
                                }
                            };
                            features.runOnMainThread(progressRunnable);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        scanner.repeat = !scanForPokemon(scanner, loc.latitude, loc.longitude);

                        while (((captchaModePopup && scanner.account.captchaScreenVisible) || (use2Captcha && scanner.account.isSolvingCaptcha())) && !abortScan) {
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
                            removeMyScanPoint(scanner.account);
                            continue;
                        }

                        if (scanner.repeat && !abortScan) {
                            //features.longMessage("Resuming scan...");
                        }

                        if (!scanner.repeat && scanner.pointCursor == scanner.points.size()) {
                            print(scanner.account.getUsername() + " is finished scanning.");
                            scanner.account.setScanning(false);
                            removeMyScanPoint(scanner.account);
                        }
                    }
                }

                try {
					/*if (scanner.failedSectors > 0) {
						if (failedScanLogins == NUM_SCAN_SECTORS) account.login();
						else {
							// TODO Make a new way to mark failed sectors
						}
					}*/
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (AccountScanner scanner : scanners) {
                    scanner.account.setScanning(false);
                    removeMyScanPoint(scanner.account);
                }
            }
        };

        return run(scanThread);
    }

    public synchronized void removeMyScanPoint(final Account account) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Marker scanPoint = scanPoints.get(account);
                Circle scanPointCircle = scanPointCircles.get(account);

                if (scanPoint != null) scanPoint.remove();
                if (scanPointCircle != null && !showScanDetails) scanPointCircle.remove();
            }
        };

        features.runOnMainThread(runnable);
    }

    public synchronized void updateScanLayout() {
        currentSector++;
        scanDialogMessage = "Scanning sector " + currentSector + "/" + NUM_SCAN_SECTORS + "   " + act.getResources().getString(R.string.tapCancel);
        scanText.setText(scanDialogMessage);
        scanBar.setProgress(currentSector);
    }

    public synchronized void updateScanPoint(LatLng loc, Account account) {
        Marker scanPoint;
        Circle scanPointCircle;

        scanPoint = scanPoints.get(account);
        if (scanPoint != null) scanPoint.remove();

        if (!showScanDetails) {
            scanPointCircle = scanPointCircles.get(account);
            if (scanPointCircle != null) scanPointCircle.remove();
        }

        scanPointCircle = mMap.addCircle(new CircleOptions().radius(MAX_SCAN_RADIUS).strokeWidth(2).center(loc).zIndex(-1.5f));

        if (showScanDetails) {
            scanPointCircle.setStrokeColor(Color.BLUE);
            scanPointCircle.setFillColor(Color.argb((int) (SCAN_DETAIL_CIRCLE_ALPHA*255), 0, 0, 255));
            account.circle = scanPointCircle;
        }

        float alpha = 1f;
        if (showScanDetails) alpha = 0.5f;
        scanPoint = mMap.addMarker(new MarkerOptions().position(loc).title(account.getUsername()).icon(scanPointIcon).anchor(0.32f, 0.32f).zIndex(-1.0f).alpha(alpha));

        scanPoints.put(account, scanPoint);

        if (!showScanDetails) {
            scanPointCircles.put(account, scanPointCircle);
        } else {
            scanPointCirclesDetailed.add(scanPointCircle);
        }
    }

    public void showErrorCircle(final Account account) {
        if (showScanDetails) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Circle circle = account.circle;
                    if (circle != null) {
                        circle.setStrokeColor(Color.RED);
                        circle.setFillColor(Color.argb((int) (SCAN_DETAIL_CIRCLE_ALPHA*255), 255, 0, 0));
                        // TODO Discrepancy with ios version on whether to add/remove circle after this
                    }
                }
            };

            features.runOnMainThread(runnable);
        }
    }

    public void showGoodCircle(final Account account) {
        if (showScanDetails) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Circle circle = account.circle;
                    if (circle != null) {
                        circle.setStrokeColor(Color.GREEN);
                        circle.setFillColor(Color.argb((int) (SCAN_DETAIL_CIRCLE_ALPHA*255), 0, 255, 0));
                        // TODO Discrepancy with ios version on whether to add/remove circle after this
                    }
                }
            };

            features.runOnMainThread(runnable);
        }
    }

    public void showCaptchaCircle(final Account account) {
        if (showScanDetails) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Circle circle = account.circle;
                    if (circle != null) {
                        circle.setStrokeColor(Color.YELLOW);
                        circle.setFillColor(Color.argb((int) (SCAN_DETAIL_CIRCLE_ALPHA*255), 255, 255, 0));
                        // TODO Discrepancy with ios version on whether to add/remove circle after this
                    }
                }
            };

            features.runOnMainThread(runnable);
        }
    }

    public void showEmptyResultsCircle(final Account account) {
        if (showScanDetails) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Circle circle = account.circle;
                    if (circle != null) {
                        circle.setStrokeColor(Color.LTGRAY);
                        circle.setFillColor(Color.argb((int) (SCAN_DETAIL_CIRCLE_ALPHA*255), 211, 211, 211));
                        // TODO Discrepancy with ios version on whether to add/remove circle after this
                    }
                }
            };

            features.runOnMainThread(runnable);
        }
    }

    public synchronized ArrayList<WildPokemonTime> getNoTimePokesInSector(double lat, double lon) {
        ArrayList<WildPokemonTime> results = new ArrayList<>();

        for (WildPokemonTime pokemonTime : noTimes.values()) {
            LatLng spawn = new LatLng(pokemonTime.getPoke().getLatitude(), pokemonTime.getPoke().getLongitude());
            float[] distance = new float[3];
            Location.distanceBetween(lat, lon, spawn.latitude, spawn.longitude, distance);
            if (distance[0] <= MAX_SCAN_RADIUS) results.add(pokemonTime);
        }

        return results;
    }

    public boolean scanForPokemon(AccountScanner scanner, double lat, double lon) {
        Account account = scanner.account;
        PokemonGo go = account.go;
        final ArrayList<Long> removables = new ArrayList<>();
        try {
            if (useNewApi && PokeHashProvider.exceededRpm && !fallbackApi) {
                waitForHashLimit();
            }
            features.print(TAG, "Scanning (" + lat + "," + lon + ")...");

            if (scanPointIcon == null)
                setScanPointIcon(BitmapDescriptorFactory.fromResource(R.drawable.scan_point_icon));

            go.setLocation(lat, lon, 0);
            Thread.sleep(200);
            try {
                features.refreshMapObjects(account);
            } catch (CaptchaActiveException c) {
                showCaptchaCircle(account);
                account.checkExceptionForCaptcha(c);
            }
            Thread.sleep(200);

            scanner.activeSpawns.clear();

            if (use2Captcha) {
                if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED || account.getStatus() == Account.AccountStatus.SOLVING_CAPTCHA) {
                    showCaptchaCircle(account);
                    features.shortMessage("Solving captcha for " + account.getUsername() + "...");
                    return false;
                }
            } else {
                if (captchaModePopup) {
                    if (features.checkForCaptcha(account)) {
                        showCaptchaCircle(account);
                        return false;
                    }
                } else {
                    if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
                        showCaptchaCircle(account);
                        features.superLongMessage("Captcha required for " + account.getUsername() + ". You can do this from the Accounts screen or set the Captcha Mode to Pop-up from the Preferences screen.");
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

                if ((!pokeTimes.containsKey(poke.getEncounterId()) && !noTimes.containsKey(poke.getEncounterId())) || ((poke.getExpirationTimestampMs() > 0 && poke.getExpirationTimestampMs() <= 3600000) && noTimes.containsKey(poke.getEncounterId()))) {
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

                    long timeMs = poke.getExpirationTimestampMs();
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
                showEmptyResultsCircle(account);
                return true;
            } else {
                showGoodCircle(account);

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

                                Throwable t = e;
                                while (t != null) {
                                    if (t instanceof HashException) {
                                        if (t instanceof HashLimitExceededException) {
                                            waitForHashLimit();
                                            try {
                                                hide = encounterPokemon(poke, pokemon, ivHolder, pokedexNumber);
                                            } catch (Throwable e2) {
                                                if (!features.filter.get(pokedexNumber)) hide = true;

                                                account.checkExceptionForCaptcha(e);

                                                Throwable t2 = e2;
                                                while (t2 != null) {
                                                    if (t2 instanceof HashException) {
                                                        if (t2 instanceof HashLimitExceededException) {
                                                            waitForHashLimit();
                                                        } else if (t2.getMessage() != null && t2.getMessage().toLowerCase().contains("unauthorized")) {
                                                            tryGenericHashWarning("Something is wrong with your API key. Please make sure it is valid.");
                                                        } else {
                                                            tryGenericHashWarning(t2.getMessage());
                                                        }
                                                        break;
                                                    } else {
                                                        t2 = t2.getCause();
                                                    }
                                                }

                                                e2.printStackTrace();
                                            }
                                        } else if (t.getMessage() != null && t.getMessage().toLowerCase().contains("unauthorized")) {
                                            tryGenericHashWarning("Something is wrong with your API key. Please make sure it is valid.");
                                        } else {
                                            tryGenericHashWarning(t.getMessage());
                                        }
                                        break;
                                    } else {
                                        t = t.getCause();
                                    }
                                }

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
                        long time = poke.getExpirationTimestampMs();
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

            // Now remove everything that needs to be removed
            Runnable runnable = new Runnable() {
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
                }
            };
            features.runOnMainThread(runnable);

            return true;
        } catch (Throwable e) {
            if (account.checkExceptionForCaptcha(e)) {
                showCaptchaCircle(account);
            } else {
                showErrorCircle(account);
            }

            Throwable t = e;
            while (t != null) {
                if (t instanceof HashException) {
                    if (t instanceof HashLimitExceededException) {
                        waitForHashLimit();
                    } else if (t.getMessage() != null && t.getMessage().toLowerCase().contains("unauthorized")) {
                        tryGenericHashWarning("Something is wrong with your API key. Please make sure it is valid.");
                    } else {
                        tryGenericHashWarning(t.getMessage());
                    }
                    break;
                } else {
                    t = t.getCause();
                }
            }

            e.printStackTrace();
            if (e instanceof LoginFailedException) failedScanLogins++;

            return false;
        }
    }

    public boolean encounterPokemon(CatchablePokemon poke, CatchablePokemon pokemon, ArrayList<String> ivHolder, int pokedexNumber) throws CaptchaActiveException, RemoteServerException, LoginFailedException {
        String ivs = "";
        ivHolder.add(ivs);

        EncounterResult result = (EncounterResult) pokemon.encounterPokemon();

        int attack = result.getPokemonData().getIndividualAttack();
        int defense = result.getPokemonData().getIndividualDefense();
        int stamina = result.getPokemonData().getIndividualStamina();
        int percent = (int) ((attack + defense + stamina) / 45f * 100);

        ivs = attack + " ATK  " + defense + " DEF  " + stamina + " STAM\n" + percent + "%";

        ivHolder.clear();
        ivHolder.add(ivs);

        features.print(TAG, "IVs: " + ivs);

        if (attack < minAttack || defense < minDefense || stamina < minStamina || percent < minPercent) {
            if (features.filterOverrides.get(pokedexNumber)) return false;
            features.print(TAG, "Filtered out " + poke.getPokemonId().name() + " for low IVs");
            return true;
        }

        if (!features.filter.get(pokedexNumber) && percent < minOverride) {
            features.print(TAG, "Filtered out " + poke.getPokemonId().name() + " for being a " + poke.getPokemonId().name());
            return true;
        }

        return false;
    }

    public synchronized void showPokemonAt(String name, int pokedexNumber, LatLng loc, long encounterid, boolean hasTime, String ivs) {
        if (pokeMarkers.containsKey(encounterid)) return;

        String localName;
        try {
            localName = getLocalName(pokedexNumber);
        } catch (Exception e) {
            localName = PokemonIdOuterClass.PokemonId.valueOf(pokedexNumber).name();
            e.printStackTrace();
            Crashlytics.logException(e);
        }

        name = name.replaceAll("\\-", "");
        name = name.replaceAll("\\'", "");
        name = name.replaceAll("\\.", "");
        name = name.replaceAll(" ", "_");
        if (name.equals("CHARMENDER")) name = "CHARMANDER";
        if (name.equals("ALAKHAZAM")) name = "ALAKAZAM";
        if (name.equals("CLEFARY")) name = "CLEFAIRY";
        if (name.equals("GEODUGE")) name = "GEODUDE";
        if (name.equals("SANDLASH")) name = "SANDSLASH";
        try {
            int resourceID = act.getResources().getIdentifier(name.toLowerCase(), "drawable", act.getPackageName());
            localName = localName.substring(0, 1).toUpperCase() + localName.substring(1).toLowerCase();

            float anchorY = 0.5f;
            BitmapDescriptor icon;
            if (CAN_SHOW_IMAGES) icon = BitmapDescriptorFactory.fromResource(resourceID);
            else {
                String filename = pokedexNumber + IMAGE_EXTENSION;
                String baseFolder = PokeFinderActivity.instance.getFilesDir().getAbsolutePath();
                String customImagesFolder = baseFolder + Features.CUSTOM_IMAGES_FOLDER;
                File file = new File(customImagesFolder + filename);
                if (file.exists()) {
                    anchorY = 0.5f;
                    icon = getCustomImageMarker(file, localName, ivs);
                } else {
                    if (defaultMarkersMode == 1) {
                        int markerResourceID = act.getResources().getIdentifier(Features.NUMBER_MARKER_PREFIX + pokedexNumber + "", "drawable", act.getPackageName());
                        icon = BitmapDescriptorFactory.fromResource(markerResourceID);
                    }
                    else {
                        anchorY = 1.0f;
                        //String type = PokemonMeta.getPokemonSettings(PokemonIdOuterClass.PokemonId.valueOf(pokedexNumber)).getType().name().toLowerCase();
                        //type = type.substring(type.lastIndexOf("_") + 1);
                        String type = types[pokedexNumber - 1].split("\\,")[1];
                        int markerResourceID = act.getResources().getIdentifier(BLANK_NAME_MARKER + type + "", "drawable", act.getPackageName());
                        icon = getNameMarker(markerResourceID, localName, ivs);
                    }
                }
            }

            float zIndex = 0;
            if (showIvs) {
                String percent = ivs;
                int index = percent.indexOf("%");
                if (index >= 0) {
                    percent = percent.substring(index - 3, index + 1).trim();
                    if (percent.substring(0,1).equals("M")) percent = percent.substring(1).trim();
                    percent = percent.substring(0, percent.length() - 1);
                    zIndex = Integer.parseInt(percent) / 100f;
                }
            }

            localName += "\n" + ivs;
            if (hasTime) {
                pokeMarkers.put(encounterid, mMap.addMarker(new MarkerOptions().position(loc).title(localName).icon(icon).anchor(0.5f, anchorY).zIndex(zIndex)));
            } else {
                pokeMarkers.put(encounterid, mMap.addMarker(new MarkerOptions().position(loc).title(localName).icon(icon).anchor(0.5f, anchorY).zIndex(zIndex)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            print("All-green error?");
            Crashlytics.logException(e);
            /*features.longMessage("Cannot find image for \"" + name + "\". Please alert the developer.");
            localName = localName.substring(0, 1).toUpperCase() + localName.substring(1).toLowerCase();
            localName += ivs;
            if (hasTime) {
                pokeMarkers.put(encounterid, mMap.addMarker(new MarkerOptions().position(loc).title(localName).zIndex(1)));
            } else {
                pokeMarkers.put(encounterid, mMap.addMarker(new MarkerOptions().position(loc).title(localName).zIndex(1)));
            }*/
        }
    }

    public synchronized BitmapDescriptor getNameMarker(int resID, String name, String ivs) {
        if (markerLayout == null) {
            markerLayout = (FrameLayout) act.getLayoutInflater().inflate(R.layout.name_marker, null);
        }

        // set the text string into the view before we turn it into an image
        TextView textView = (TextView) markerLayout.findViewById(R.id.markerName);
        if (INITIAL_NAME_MARKER_TEXT_SIZE == Float.NEGATIVE_INFINITY) {
            INITIAL_NAME_MARKER_TEXT_SIZE = textView.getTextSize();
            print("Initial name marker text size = " + INITIAL_NAME_MARKER_TEXT_SIZE);
        }

        textView.setText(name);

        if (showIvs && ivsAlwaysVisible) {
            String percent = ivs;
            int index = percent.indexOf("%");
            if (index >= 0) {
                percent = percent.substring(index - 3, index + 1).trim();
                if (percent.substring(0,1).equals("M")) percent = percent.substring(1).trim();
                textView.setText(textView.getText().toString() + " " + percent);
            }
        }

        //textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        // Shrink text if needed to fit text bounds
        DisplayMetrics metrics = act.getResources().getDisplayMetrics();
        int width = (int) (82 * metrics.density);
        int height = (int) (19 * metrics.density);
        Rect bounds = new Rect();
        Paint textPaint = textView.getPaint();
        textPaint.setTextSize(INITIAL_NAME_MARKER_TEXT_SIZE);
        String text = textView.getText().toString();

        boolean success = false;

        while (!success) {
            textPaint.getTextBounds(text, 0, text.length(), bounds);
            print("TextView.width = " + width);
            print("text bounds width = " + bounds.width());
            print("Text size = " + textView.getTextSize());
            print("Paint text size = " + textView.getTextSize());
            if (bounds.width() > width) {
                //textView.setTextSize(textView.getTextSize() - 1);
                textPaint.setTextSize(textPaint.getTextSize() - 1);
                print("Shrinking text width to " + textPaint.getTextSize());
            } else {
                success = true;
                break;
            }
        }

        ImageView image = (ImageView) markerLayout.findViewById(R.id.markerImage);
        image.setImageResource(resID);

        int paddingTop = textView.getPaddingTop();
        int paddingLeft = textView.getPaddingLeft();
        int paddingRight = textView.getPaddingRight();
        int paddingBottom = textView.getPaddingBottom();

        textView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

        markerLayout.setDrawingCacheEnabled(true);

        markerLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        markerLayout.layout(0, 0, markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight());

        markerLayout.buildDrawingCache(true);

        if (markerLayout.getDrawingCache() != null) {
            if (tempBitmap != null) tempBitmap.recycle();
            tempBitmap = Bitmap.createBitmap(markerLayout.getDrawingCache());

            markerLayout.setDrawingCacheEnabled(false);

            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(tempBitmap);
            return descriptor;
        }

        // This shouldn't happen
        return null;
    }

    public synchronized BitmapDescriptor getCustomImageMarker(File file, String name, String ivs) {
        if (customImageLayout == null) {
            customImageLayout = (FrameLayout) act.getLayoutInflater().inflate(R.layout.custom_image_marker, null);
        }

        ImageView customImage = (ImageView) customImageLayout.findViewById(R.id.customImage);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) customImage.getLayoutParams();
        int imageSize = PokeFinderActivity.instance.getImageSize();
        params.width = imageSize;
        params.height = imageSize;
        customImage.setLayoutParams(params);

        customImage.setImageDrawable(BitmapDrawable.createFromPath(file.getAbsolutePath()));

        // set the text string into the view before we turn it into an image
        TextView textView = (TextView) customImageLayout.findViewById(R.id.ivLabel);

        if (showIvs && ivsAlwaysVisible) {
            textView.setVisibility(View.VISIBLE);
            String percent = ivs;
            int index = percent.indexOf("%");
            if (index >= 0) {
                percent = percent.substring(index - 3, index + 1).trim();
                if (percent.substring(0,1).equals("M")) percent = percent.substring(1).trim();
                textView.setText(percent);
            }
        } else {
            textView.setVisibility(View.GONE);
        }

        int paddingTop = textView.getPaddingTop();
        int paddingLeft = textView.getPaddingLeft();
        int paddingRight = textView.getPaddingRight();
        int paddingBottom = textView.getPaddingBottom();

        textView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

        customImageLayout.setDrawingCacheEnabled(true);

        customImageLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        customImageLayout.layout(0, 0, customImageLayout.getMeasuredWidth(), customImageLayout.getMeasuredHeight());

        customImageLayout.buildDrawingCache(true);

        if (customImageLayout.getDrawingCache() != null) {
            if (tempCustomBitmap != null) tempCustomBitmap.recycle();
            tempCustomBitmap = Bitmap.createBitmap(customImageLayout.getDrawingCache());

            customImageLayout.setDrawingCacheEnabled(false);
            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(tempCustomBitmap);
            return descriptor;
        }

        // This shouldn't happen
        return null;
    }

    public void print(String message) {
        features.print(TAG, message);
    }

    private Vector2D[] getSearchPoints(int radius) {
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

        double squareSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(MINI_SQUARE_SIZE / MapHelper.minScanTime, (double) scanSpeed));
        double hexSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(HEX_DISTANCE / MapHelper.minScanTime, (double) scanSpeed));

        if (hexSectors * hexSpeed <= squareSectors * squareSpeed) return getHexSearchPoints(radius);
        else return getSquareSearchPoints(radius);
    }

    private Vector2D[] getHexSearchPoints(int radius) {
        isHexMode = true;

        final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MAX_SCAN_RADIUS);
        final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
        final float ITERATIONS = MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

        NUM_SCAN_SECTORS = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);

        Vector2D startPoint;
        startPoint = Vector2D.ZERO;

        int direction = 0; // 0 = upright, 1 = downright, 2 = down, 3 = downleft, 4 = upleft, 5 = up
        ArrayList<Vector2D> points = new ArrayList<Vector2D>();
        points.add(startPoint);
        int numMoves = 0;

        Vector2D currentPoint = new Vector2D(startPoint.toArray());

        print("Distance between hexes = " + HEX_DISTANCE);
        print("Num scan sectors = " + NUM_SCAN_SECTORS);
        print("Start point = " + startPoint.toString());

        if (ITERATIONS == 1) {
            Vector2D[] pointsArray = new Vector2D[points.size()];
            points.toArray(pointsArray);
            return pointsArray;
        }

        for (int n = 1; n < ITERATIONS; n++) {
            currentPoint = move(1, 0, currentPoint, points); // 1 upright move

            currentPoint = move(n-1, 1, currentPoint, points); // n-1 downright moves

            currentPoint = move(n, 2, currentPoint, points); // n down moves

            currentPoint = move(n, 3, currentPoint, points); // n downleft moves

            currentPoint = move(n, 4, currentPoint, points); // n upleft moves

            currentPoint = move(n, 5, currentPoint, points); // n up moves

            currentPoint = move(n, 0, currentPoint, points); // n upright moves
        }

        Vector2D[] pointsArray = new Vector2D[points.size()];
        points.toArray(pointsArray);
        return pointsArray;
    }

    public Vector2D move(int times, int direction, Vector2D currentPoint, ArrayList<Vector2D> points) {
        final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MAX_SCAN_RADIUS);
        final float Y_OFFSET = (float) (Math.sqrt(3)*MAX_SCAN_RADIUS*Math.sin(Math.toRadians(30)));
        final float X_OFFSET = (float) (Math.sqrt(3)*MAX_SCAN_RADIUS*Math.cos(Math.toRadians(30)));

        final float Y_NEG = (-1)*Y_OFFSET;
        final float X_NEG = (-1)*X_OFFSET;

        for (int n = 0; n < times; n++) {
            currentPoint = new Vector2D(currentPoint.toArray());

            switch(direction) {

                case 0:
                    print("Upright");
                    currentPoint = new Vector2D(currentPoint.getX() + X_OFFSET, currentPoint.getY() + Y_OFFSET);
                    break;

                case 1:
                    print("Downright");
                    currentPoint = new Vector2D(currentPoint.getX() + X_OFFSET, currentPoint.getY() - Y_OFFSET);
                    break;

                case 2:
                    print("Down");
                    currentPoint = new Vector2D(currentPoint.getX(), currentPoint.getY() - HEX_DISTANCE);
                    break;


                case 3:
                    print("Downleft");
                    currentPoint = new Vector2D(currentPoint.getX() - X_OFFSET, currentPoint.getY() - Y_OFFSET);
                    break;

                case 4:
                    print("Upleft");
                    currentPoint = new Vector2D(currentPoint.getX() - X_OFFSET, currentPoint.getY() + Y_OFFSET);
                    break;

                case 5:
                    print("Up");
                    currentPoint = new Vector2D(currentPoint.getX(), currentPoint.getY() + HEX_DISTANCE);
                    break;
            }

            print("Current point = " + currentPoint.toString() + "\n");
            points.add(currentPoint);
        }

        return currentPoint;
    }

    private Vector2D[] getSquareSearchPoints(int radius) {
        isHexMode = false;

        final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MAX_SCAN_RADIUS * 2, 2) / 2);
        final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
        NUM_SCAN_SECTORS = BOXES_PER_ROW * BOXES_PER_ROW;

        boolean isOdd = BOXES_PER_ROW / 2 * 2 == BOXES_PER_ROW ? false : true;

        Vector2D startPoint;
        if (isOdd) startPoint = Vector2D.ZERO;
        else {
            float offset = MAX_SCAN_RADIUS * (float) Math.sin(Math.toRadians(45));
            startPoint = new Vector2D((-1) * offset, offset);
        }

        int direction = 0; // 0 = right, 1 = down, 2 = left, 3 = up
        ArrayList<Vector2D> points = new ArrayList<Vector2D>();
        points.add(startPoint);
        int numMoves = 0;

        Vector2D currentPoint = new Vector2D(startPoint.toArray());

        print("Mini square radius = " + MINI_SQUARE_SIZE);
        print("Num scan sectors = " + NUM_SCAN_SECTORS);
        print("Start point = " + startPoint.toString());

        for (int n = 1; n < NUM_SCAN_SECTORS; n++) {
            currentPoint = new Vector2D(currentPoint.toArray());
            int maxMoves = (int) Math.sqrt(n);

            print("Num moves = " + numMoves);
            print("Max moves = " + maxMoves);

            if (numMoves == maxMoves) {
                numMoves = 0;
                direction = (direction + 1) % 4;
            }

            numMoves++;
            switch (direction) {
                case 0:
                    print("Right " + numMoves);
                    currentPoint = new Vector2D(currentPoint.getX() + MINI_SQUARE_SIZE, currentPoint.getY());
                    break;
                case 1:
                    print("Down " + numMoves);
                    currentPoint = new Vector2D(currentPoint.getX(), currentPoint.getY() - MINI_SQUARE_SIZE);
                    break;
                case 2:
                    print("Left " + numMoves);
                    currentPoint = new Vector2D(currentPoint.getX() - MINI_SQUARE_SIZE, currentPoint.getY());
                    break;
                case 3:
                    print("Top " + numMoves);
                    currentPoint = new Vector2D(currentPoint.getX(), currentPoint.getY() + MINI_SQUARE_SIZE);
                    break;
            }

            print("Current point = " + currentPoint.toString() + "\n");
            points.add(currentPoint);
        }

        Vector2D[] pointsArray = new Vector2D[points.size()];
        points.toArray(pointsArray);
        return pointsArray;
    }

    private LatLng cartesianToCoord(Vector2D point, LatLng center) {
        final double latRadian = Math.toRadians(center.latitude);

        final double metersPerLatDegree = 110574.235;
        final double metersPerLonDegree = 110572.833 * Math.cos(latRadian);
        final double deltaLat = point.getY() / metersPerLatDegree;
        final double deltaLong = point.getX() / metersPerLonDegree;

        LatLng loc = new LatLng(center.latitude + deltaLat, center.longitude + deltaLong);
        return loc;
    }

    public void startCountdownTimer() {
        if (countdownTimer != null) countdownTimer.cancel();
        countdownTimer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<Long> removables = new ArrayList<Long>();
                        for (WildPokemonTime poke : pokeTimes.values()) {
                            long timeLeftMs = poke.getDespawnTimeMs() - System.currentTimeMillis();
                            if (timeLeftMs < 0) {
                                try {
                                    pokeMarkers.remove(poke.getPoke().getEncounterId()).remove();
                                    removables.add(poke.getPoke().getEncounterId());
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Marker marker = pokeMarkers.get(poke.getPoke().getEncounterId());
                                if (marker != null) {
                                    marker.setSnippet("Leaves in " + getTimeString(timeLeftMs / 1000 + 1));
                                    if (marker.isInfoWindowShown()) marker.showInfoWindow();
                                }
                            }
                        }
                        for (Long id : removables) {
                            pokeTimes.remove(id);
                        }

                        // Do the same for noTimes
                        removables.clear();
                        for (WildPokemonTime poke : noTimes.values()) {
                            long timeElapsed = System.currentTimeMillis() - poke.getDespawnTimeMs();
                            if (timeElapsed > 1800000) {
                                try {
                                    pokeMarkers.remove(poke.getPoke().getEncounterId()).remove();
                                    removables.add(poke.getPoke().getEncounterId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Marker marker = pokeMarkers.get(poke.getPoke().getEncounterId());
                                if (marker != null) {
                                    marker.setSnippet("Seen " + getTimeString(timeElapsed / 1000) + " ago");
                                    if (marker.isInfoWindowShown()) marker.showInfoWindow();
                                }
                            }
                        }
                        for (Long id : removables) {
                            noTimes.remove(id);
                        }
                    }
                };
                features.runOnMainThread(r);
            }
        };

        countdownTimer.schedule(task, 0, 1000);
    }

    public void stopCountdownTimer() {
        if (countdownTimer != null) countdownTimer.cancel();
    }

    public boolean updateScanSettings() {
        boolean distanceFailed = false, timeFailed = false;
        NativePreferences.lock("update scan settings");
        maxScanDistance = NativePreferences.getFloat(PREF_MAX_SCAN_DISTANCE, 70);
        minScanTime = NativePreferences.getFloat(PREF_MIN_SCAN_TIME, 5);

        try {
            maxScanDistance = features.getVisibleScanDistance();
            if (maxScanDistance <= 0) throw new RemoteServerException("Unable to get scan distance from server");
            NativePreferences.putFloat(PREF_MAX_SCAN_DISTANCE, (float) maxScanDistance);
            features.print("PokeFinder", "Server says max visible scan distance is " + maxScanDistance);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RemoteServerException) distanceFailed = true;
            maxScanDistance = NativePreferences.getFloat(PREF_MAX_SCAN_DISTANCE, 70);
            if (maxScanDistance <= 0) maxScanDistance = 70;
        }

        MAX_SCAN_RADIUS = (int) maxScanDistance;

        try {
            minScanTime = features.getMinScanRefresh();
            if (minScanTime <= 0) throw new RemoteServerException("Unable to get scan delay from server");
            NativePreferences.putFloat(PREF_MIN_SCAN_TIME, (float) minScanTime);
            features.print("PokeFinder", "Server says min scan refresh is " + minScanTime);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RemoteServerException) timeFailed = true;
            minScanTime = NativePreferences.getFloat(PREF_MIN_SCAN_TIME, 5);
            if (minScanTime <= 0) minScanTime = 5;
        }

        NativePreferences.unlock();

        int squareDist = (int) Math.sqrt(Math.pow(MAX_SCAN_RADIUS * 2, 2) / 2);
        int hexDist = (int) (Math.sqrt(3)*MapHelper.MAX_SCAN_RADIUS);
        int distancePerScan = Math.max(squareDist, hexDist);

        int speed = (int) Math.ceil(distancePerScan / minScanTime);
        maxScanSpeed = Math.min(SPEED_CAP, speed);

        return !distanceFailed && !timeFailed;
    }

    @Override
    public void saveSpawns() {
        if (spawns.isEmpty() || scanning) return;

        for (Spawn spawn : spawns.values()) {
            print("Spawn ID: " + spawn.id);
            print("Location: " + spawn.loc());
            print("History:");

            HashSet<Integer> history = new HashSet<>(spawn.history);
            for (Integer num : history) {
                print(getLocalName(num));
            }
        }

        String baseFolder = act.getFilesDir().getAbsolutePath();
        File fileName = new File(baseFolder + "spawn.ser");

        OutputStream file = null;
        OutputStream buffer = null;
        ObjectOutput output = null;
        try {
            file = new FileOutputStream(fileName);
            buffer = new BufferedOutputStream(file);
            output = new ObjectOutputStream(buffer);

            output.writeObject(spawns);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //if (file != null) file.close();
            //if (buffer != null) buffer.close();
            if (output != null) output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadSpawns() {
        if (scanning) return;

        String baseFolder = act.getFilesDir().getAbsolutePath();
        File fileName = new File(baseFolder + "spawn.ser");

        InputStream file = null;
        InputStream buffer = null;
        ObjectInput input = null;
        try{
            file = new FileInputStream(fileName);
            buffer = new BufferedInputStream(file);
            input = new ObjectInputStream(buffer);
            try{
                spawns = (ConcurrentHashMap<String, Spawn>)input.readObject();
            } catch(Exception f) {
                f.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //if (file != null) file.close();
            //if (buffer != null) buffer.close();
            if (input != null) input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSpawnsOnMap() {
        if (loadedSpawns || !showSpawns) return;

        loadedSpawns = true;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mapSpawns.clear();
                for (Spawn spawn : spawns.values()) {
                    showSpawnOnMap(spawn);
                }
            }
        };
        features.runOnMainThread(runnable);
    }

    public void hideSpawnsOnMap() {
        loadedSpawns = false;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (Marker spawn : mapSpawns) {
                    if (spawn != null) spawn.remove();
                }
                mapSpawns.clear();
            }
        };
        features.runOnMainThread(runnable);
    }

    public Marker showSpawnOnMap(Spawn spawn) {
        if (spawnIcon == null) spawnIcon = BitmapDescriptorFactory.fromResource(R.drawable.spawn_icon);
        Marker spawnPoint = mMap.addMarker(new MarkerOptions().position(spawn.loc()).title(spawn.nickname).icon(spawnIcon).anchor(0.5f, 0.5f).alpha(0.5f));
        spawnPoint.setZIndex(-2);
        mapSpawns.add(spawnPoint);
        return spawnPoint;
    }

    public void deleteAllSpawns() {
        String baseFolder = act.getFilesDir().getAbsolutePath();
        File fileName = new File(baseFolder + "spawn.ser");
        fileName.delete();
        spawns.clear();
        hideSpawnsOnMap();
    }

    @Override
    public void wideSpawnScan() {
        ConcurrentHashMap<String, Spawn> temp = new ConcurrentHashMap<>();

        // Figure out which coords are within the radius (of the square)
        LatLng bottomLeft = cartesianToCoord(new Vector2D((-1)*scanDistance, (-1)*scanDistance), new LatLng(currentLat, currentLon));
        LatLng topRight = cartesianToCoord(new Vector2D(scanDistance, scanDistance), new LatLng(currentLat, currentLon));

        print("Bottom left: " + bottomLeft.toString() + ", top right: " + topRight.toString());

        double left = bottomLeft.longitude;
        double bottom = bottomLeft.latitude;
        double top = topRight.latitude;
        double right = topRight.longitude;

        for (Spawn spawn : spawns.values()) {
            print("Trying lat: " + spawn.lat + " lon: " + spawn.lon);
            /*if (left <= spawn.lon && right >= spawn.lon && bottom <= spawn.lat && top >= spawn.lat) {
                temp.put(spawn.id, spawn);
            }*/
            float[] distance = new float[3];
            Location.distanceBetween(currentLat, currentLon, spawn.lat, spawn.lon, distance);
            if (distance[0] <= scanDistance) temp.put(spawn.id, spawn);
        }

        if (!temp.isEmpty()) spawnScan(temp);
        else features.shortMessage("You have to find spawn points in this area with regular scanning before you can do a spawn scan.");
    }

    @Override
    public void refreshPrefs() {
        NativePreferences.lock("refresh prefs");

        collectSpawns = NativePreferences.getBoolean(PREF_COLLECT_SPAWNS, true);
        showIvs = NativePreferences.getBoolean(PREF_SHOW_IVS, true);
        showSpawns = NativePreferences.getBoolean(PREF_SHOW_SPAWNS, true);
        captchaModePopup = NativePreferences.getBoolean(PREF_CAPTCHA_MODE_POPUP, false);
        minAttack = NativePreferences.getInt(PREF_MIN_ATTACK, 0);
        minDefense = NativePreferences.getInt(PREF_MIN_DEFENSE, 0);
        minStamina = NativePreferences.getInt(PREF_MIN_STAMINA, 0);
        minPercent = NativePreferences.getInt(PREF_MIN_PERCENT, 0);
        minOverride = NativePreferences.getInt(PREF_MIN_OVERRIDE, 100);
        imageSize = NativePreferences.getLong(PREF_IMAGE_SIZE, 2);
        showScanDetails = NativePreferences.getBoolean(PREF_SHOW_SCAN_DETAILS, false);
        Features.NUM_POKEMON = NativePreferences.getInt(PREF_NUM_POKEMON, 151);
        ivsAlwaysVisible = NativePreferences.getBoolean(PREF_IVS_ALWAYS_VISIBLE, true);
        defaultMarkersMode = NativePreferences.getLong(PREF_DEFAULT_MARKERS_MODE, 0);
        overrideEnabled = NativePreferences.getBoolean(PREF_OVERRIDE_ENABLED, minOverride != 100);
        clearMapOnScan = NativePreferences.getBoolean(PREF_CLEAR_MAP_ON_SCAN, false);
        gpsModeNormal = NativePreferences.getBoolean(PREF_GPS_MODE_NORMAL, true);
        use2Captcha = NativePreferences.getBoolean(PREF_USE_2CAPTCHA, false);
        useNewApi = NativePreferences.getBoolean(PREF_USE_NEW_API, false);
        //fallbackApi = NativePreferences.getBoolean(PREF_FALLBACK_API, true);
        fallbackApi = false;
        NativePreferences.putBoolean(PREF_FALLBACK_API, false);
        captchaKey = NativePreferences.getString(PREF_2CAPTCHA_KEY, "");
        newApiKey = NativePreferences.getString(PREF_NEW_API_KEY, "");

        if (useNewApi) {
            PokeFinderActivity.instance.showRpmLabel();
            startRpmTimer();
        } else {
            PokeFinderActivity.instance.hideRpmLabel();
            PokeFinderActivity.instance.hideRpmCountLabel();
            stopRpmTimer();
        }

        NativePreferences.unlock();

        //Signature.fallbackApi = fallbackApi;
        Signature.fallbackApi = false;
    }

    public void spawnScan(final ConcurrentHashMap<String, Spawn> searchSpawns) {
        if (searchSpawns.isEmpty()) return;

        final ArrayList<Account> goodAccounts = AccountManager.getGoodAccounts();
        if (goodAccounts.size() == 0) {
            features.longMessage("You don't have any valid accounts!");
            return;
        }

        if (scanning) return;
        else scanning = true;
        searched = true;

        if (mMap == null) return;
        removeTempScanCircle();

        newSpawns = 0;
        currentSector = 0;

        final ArrayList<Spawn> spawnList = new ArrayList<>(searchSpawns.values());

        searchedSpawns.clear();
        features.captchaScreenVisible = false;

        updateScanSettings();
        abortScan = false;
        if (scanDistance > MAX_SCAN_DISTANCE) scanDistance = MAX_SCAN_DISTANCE;

        final Context con = act;
        final LinearLayout scanLayout = (LinearLayout) act.findViewById(R.id.scanLayout);
        scanBar = (ProgressBar) act.findViewById(R.id.scanBar);
        scanText = (TextView) act.findViewById(R.id.scanText);

        NUM_SCAN_SECTORS = searchSpawns.size();

        Runnable main = new Runnable() {
            @Override
            public void run() {
                if (clearMapOnScan) {
                    try {
                        //final ArrayList<Long> ids = new ArrayList<Long>(noTimes.keys().);
                        Map<Long, WildPokemonTime> temp = noTimes;

                        for (Long id : temp.keySet()) {
                            try {
                                features.print(TAG, "Removed poke marker!");
                                Marker marker = pokeMarkers.remove(id);
                                if (marker != null) marker.remove();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    noTimes.clear();
                }

                removeScanPoints();
                removeScanPointCircles();

                scanBar.setProgress(0);
                scanBar.setMax(searchSpawns.size());
                scanText.setText("");

                scanLayout.setVisibility(View.VISIBLE);
                scanBar.setVisibility(View.VISIBLE);
                scanLayout.requestLayout();
                scanLayout.bringToFront();
                scanLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);


                DisplayMetrics metrics = act.getResources().getDisplayMetrics();
                paddingTop = Math.round(scanLayout.getMeasuredHeight() * metrics.density) + 2;

                int goodAccountsPadding = Math.round(GOOD_ACCOUNTS_IMAGE_DP * metrics.density) + 5;
                paddingTop += goodAccountsPadding;

                features.print(TAG, "Padding top: " + paddingTop);
                mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

                final Thread scanThread = new Thread() {
                    public void run() {
                        Runnable circleRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (scanCircle != null) scanCircle.remove();
                                scanCircle = mMap.addCircle(new CircleOptions().center(new LatLng(currentLat, currentLon)).strokeWidth(1).radius(scanDistance).strokeColor(Color.argb(128, 0, 0, 255)));
                            }
                        };
                        features.runOnMainThread(circleRunnable);

                        features.print(TAG, "Scan distance: " + scanDistance);

                        totalNearbyPokemon.clear();
                        totalEncounters.clear();
                        totalWildEncounters.clear();

                        long SCAN_INTERVAL = (long) MapHelper.minScanTime * 1000;

                        features.print(TAG, "Scan interval: " + SCAN_INTERVAL);

                        scanBar.setMax(searchSpawns.size());

                        scanPointCircles.clear();
                        scanPoints.clear();

                        features.resetMapObjects();

                        int scansPerWorker = searchSpawns.size() / goodAccounts.size();
                        int extraScans = searchSpawns.size() - scansPerWorker * goodAccounts.size();
                        int cursor = 0;

                        int workersPerThread = goodAccounts.size() / AccountManager.MAX_POOL_THREADS;
                        int extraWorkers = goodAccounts.size() - workersPerThread * AccountManager.MAX_POOL_THREADS;
                        int workerCursor = 0;

                        ArrayList<Future> scanThreads = new ArrayList<>();

                        for (int n = 0; n < MAX_POOL_THREADS; n++) {
                            int numWorkers = workersPerThread;
                            if (extraWorkers > 0) {
                                extraWorkers--;
                                numWorkers++;
                            }

                            if (numWorkers == 0) break;

                            ArrayList<AccountScanner> scanAccounts = new ArrayList<>();
                            for (int x = 0; x < numWorkers; x++) {
                                if (searchSpawns.isEmpty()) break;
                                Spawn mySpawn = new ArrayList<Spawn>(searchSpawns.values()).get(0);
                                claimSpawn(mySpawn.id, searchSpawns);

                                Account account = goodAccounts.get(workerCursor++);
                                AccountScanner scanner = new AccountScanner(account, mySpawn);
                                scanAccounts.add(scanner);
                            }
                            scanThreads.add(accountScanSpawn(scanAccounts, SCAN_INTERVAL, searchSpawns));
                            if (searchSpawns.isEmpty()) break;
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
                                features.longMessage(R.string.abortScan);
                                scanning = false;

                                break;
                            }
                        }

                        Runnable dismissRunnable = new Runnable() {
                            @Override
                            public void run() {
                                removeScanPoints();
                                if (!showScanDetails) {
                                    removeScanPointCircles();
                                }

                                scanLayout.setVisibility(View.GONE);

                                DisplayMetrics metrics = act.getResources().getDisplayMetrics();
                                int paddingTop = Math.round(GOOD_ACCOUNTS_IMAGE_DP * metrics.density) + 5;

                                mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                            }
                        };
                        features.runOnMainThread(dismissRunnable);


                        if (collectSpawns) {
                            if (newSpawns > 1)
                                features.shortMessage("Found " + newSpawns + " new spawn points and added them to My Spawns!");
                            else if (newSpawns == 1)
                                features.shortMessage("Found 1 new spawn point and added it to My Spawns!");
                        }

                        saveScanEvent(totalWildEncounters.size(), (float) (Math.PI * Math.pow(scanDistance, 2)));

                        scanning = false;
                    }
                };

                scanLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scanThread.interrupt();
                        abortScan = true;
                        scanLayout.setVisibility(View.GONE);

                        DisplayMetrics metrics = act.getResources().getDisplayMetrics();
                        int paddingTop = Math.round(GOOD_ACCOUNTS_IMAGE_DP * metrics.density) + 5;

                        mMap.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

                        removeScanPoints();
                        if (!showScanDetails) {
                            removeScanPointCircles();
                        }
                    }
                });

                scanThread.start();
            }
        };

        features.runOnMainThread(main);
    }

    /*public Thread accountScanSpawn(final Account account, final Spawn startSpawn, final long SCAN_INTERVAL, final ConcurrentHashMap<String, Spawn> searchSpawns) {
        account.setScanning(true);

        Thread scanThread = new Thread() {
            public void run() {
                int failedSectors = 0;
                LatLng location = null;

                boolean first = true;
                boolean repeat = false;
                Spawn repeatSpawn = null;
                String id = startSpawn.id;
                while (first || !searchSpawns.isEmpty() || repeat) {
                    if (repeat) {
                        failedSectors--;
                    }
                    // TODO Any changes to this should be reflected in the below identical abort block
                    if (abortScan) {
                        account.setScanning(false);
                        return;
                    }

                    if (first) {
                        repeatSpawn = startSpawn;
                        first = false;
                        location = new LatLng(startSpawn.lat, startSpawn.lon);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            if (abortScan) {
                                account.setScanning(false);
                                return;
                            }
                        }
                    } else if (repeat) {
                        location = new LatLng(repeatSpawn.lat, repeatSpawn.lon);
                        try {
                            Thread.sleep(SCAN_INTERVAL);
                        } catch (InterruptedException e) {
                            if (abortScan) {
                                account.setScanning(false);
                                return;
                            }
                        }
                    } else {
                        Hashtable<String, Integer> times = new Hashtable<>();

                        final LatLng loc = new LatLng(location.latitude, location.longitude);
                        LatLng here = new LatLng(loc.latitude, loc.longitude);
                        ArrayList<Spawn> spawnList = new ArrayList<Spawn>(searchSpawns.values());
                        for (Spawn spawn : spawnList) {
                            int commuteTime = 0;
                            double commuteDistance = 0;

                            float[] distance = new float[3];
                            Location.distanceBetween(here.latitude, here.longitude, spawn.lat, spawn.lon, distance);
                            commuteDistance = distance[0];

                            commuteTime = (int) Math.ceil(commuteDistance / scanSpeed);
                            features.print(TAG, account.getUsername() + " will take " + commuteTime + "s to get to " + spawn.id);
                            times.put(spawn.id, Math.max(commuteTime, (int) SCAN_INTERVAL / 1000));
                        }

                        int timeWaited = 0;
                        String myId = null;
                        Spawn mySpawn = null;
                        while (mySpawn == null) {
                            // Find the shortest wait time and try to claim it
                            int minTime = Integer.MAX_VALUE;
                            for (String temp : times.keySet()) {
                                if (searchSpawns.containsKey(temp)) {
                                    if (times.get(temp) < minTime) {
                                        minTime = times.get(temp);
                                        myId = temp;
                                    }
                                }
                            }

                            if (searchSpawns.isEmpty() || myId == null) {
                                removeMyScanPoint(account);
                                account.setScanning(false);
                                return; // must be done
                            }

                            minTime -= timeWaited;
                            features.print(TAG, account.getUsername() + " wants to claim " + myId + " and has to wait for " + minTime + "s");
                            for (int n = 0; n < minTime; n++) {
                                if (searchSpawns.containsKey(myId)) {
                                    try {
                                        Thread.sleep(1000);
                                        timeWaited += 1;
                                    } catch (InterruptedException e) {
                                        if (abortScan) {
                                            account.setScanning(false);
                                            return;
                                        }
                                    }
                                } else {
                                    myId = null;
                                    break;
                                }
                            }

                            if (myId != null) {
                                mySpawn = claimSpawn(myId, searchSpawns);
                                features.print(TAG, account.getUsername() + " is trying now to claim " + myId);
                            }
                        }

                        features.print(TAG, account.getUsername() + " claimed " + mySpawn.id);

                        repeatSpawn = mySpawn;
                        location = new LatLng(mySpawn.lat, mySpawn.lon);
                    }

                    repeat = false;
                    final LatLng loc = new LatLng(location.latitude, location.longitude);

                    LatLng here = new LatLng(loc.latitude, loc.latitude);
                    ArrayList<Spawn> spawnList = new ArrayList<Spawn>(searchSpawns.values());
                    for (Spawn spawn : spawnList) {
                        LatLng spawnPoint = new LatLng(spawn.lat, spawn.lon);

                        float[] distance = new float[3];
                        Location.distanceBetween(currentLat, currentLon, spawn.lat, spawn.lon, distance);

                        double meters = distance[0];
                        if (meters <= MAX_SCAN_RADIUS) {
                            claimSpawn(spawn.id, searchSpawns);
                            markSpawnAsSearched(spawn.id);
                        }
                    }

                    Runnable progressRunnable = new Runnable() {
                        @Override
                        public void run() {
                            updateScanLayout();
                            updateScanPoint(loc, account);
                        }
                    };
                    features.runOnMainThread(progressRunnable);

                    if (!scanForPokemon(account, loc.latitude, loc.longitude)) failedSectors++;

                    while(captchaModePopup && account.captchaScreenVisible && !abortScan) {
                        try {
                            repeat = true;
                            Thread.sleep(1000);
                            if (abortScan) {
                                account.setScanning(false);
                                return;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            if (abortScan) {
                                account.setScanning(false);
                                return;
                            }
                        }
                    }

                    if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
                        print(account.getUsername() + " is aborting due to a captcha");
                        account.setScanning(false);
                        removeMyScanPoint(account);
                        return;
                    }

                    if (repeat && !abortScan) {
                        //features.longMessage("Resuming scan...");
                    }
                }

                try {
                    if (failedSectors > 0) {
                        if (failedScanLogins == NUM_SCAN_SECTORS) account.login();
                        else {
                            // TODO Make a new way to mark failed sectors
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                removeMyScanPoint(account);
                account.setScanning(false);
            }
        };
        scanThread.start();

        return scanThread;
    }*/

    public Future accountScanSpawn(final ArrayList<AccountScanner> scanners, final long SCAN_INTERVAL, final ConcurrentHashMap<String, Spawn> searchSpawns) {
        for (AccountScanner scanner : scanners) {
            scanner.account.setScanning(true);
        }

        Runnable scanThread = new Runnable() {
            public void run() {
                boolean stillScanning = true;
                boolean first = true;
                while (first || (stillScanning && !searchSpawns.isEmpty())) {
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
                        Thread.sleep(1000);
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

                        String id = scanner.startSpawn.id;
                        if (scanner.repeat) {
                            scanner.failedSectors--;
                            currentSector--;
                            if (showScanDetails) {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        scanner.account.circle.remove();
                                        scanPointCirclesDetailed.remove(scanner.account.circle);
                                    }
                                };
                                features.runOnMainThread(runnable);
                            }
                        }
                        // TODO Any changes to this should be reflected in the below identical abort block
                        if (abortScan) {
                            for (int n = 0; n < scanners.size(); n++) {
                                scanners.get(n).account.setScanning(false);
                            }
                            return;
                        }

                        if (first) {
                            scanner.repeatSpawn = scanner.startSpawn;
                            scanner.location = new LatLng(scanner.startSpawn.lat, scanner.startSpawn.lon);
                        } else if (scanner.repeat) {
                            scanner.location = new LatLng(scanner.repeatSpawn.lat, scanner.repeatSpawn.lon);
                        } else {
                            Hashtable<String, Integer> times = new Hashtable<>();

                            final LatLng loc = new LatLng(scanner.location.latitude, scanner.location.longitude);
                            LatLng here = new LatLng(loc.latitude, loc.longitude);
                            ArrayList<Spawn> spawnList = new ArrayList<Spawn>(searchSpawns.values());
                            for (Spawn spawn : spawnList) {
                                int commuteTime = 0;
                                double commuteDistance = 0;

                                float[] distance = new float[3];
                                Location.distanceBetween(here.latitude, here.longitude, spawn.lat, spawn.lon, distance);
                                commuteDistance = distance[0];

                                commuteTime = (int) Math.ceil(commuteDistance / scanSpeed);
                                features.print(TAG, scanner.account.getUsername() + " will take " + commuteTime + "s to get to " + spawn.id);
                                times.put(spawn.id, Math.max(commuteTime, (int) SCAN_INTERVAL / 1000));
                            }

                            String myId = null;
                            Spawn mySpawn = null;
                            // Find the shortest wait time and try to claim it
                            int minTime = Integer.MAX_VALUE;
                            for (String temp : times.keySet()) {
                                if (searchSpawns.containsKey(temp)) {
                                    if (times.get(temp) < minTime) {
                                        minTime = times.get(temp);
                                        myId = temp;
                                    }
                                }
                            }

                            if (searchSpawns.isEmpty() || myId == null) {
                                removeMyScanPoint(scanner.account);
                                scanner.account.setScanning(false);
                                continue; // must be done
                            }

                            scanner.timeWaited++;
                            minTime -= scanner.timeWaited;
                            features.print(TAG, scanner.account.getUsername() + " wants to claim " + myId + " and has to wait for " + minTime + "s");

                            if (minTime > 0) continue;

                            if (searchSpawns.containsKey(myId)) {
                                mySpawn = claimSpawn(myId, searchSpawns);
                                features.print(TAG, scanner.account.getUsername() + " is trying now to claim " + myId);
                            }

                            if (mySpawn == null) continue;

                            scanner.timeWaited = 0;

                            features.print(TAG, scanner.account.getUsername() + " claimed " + mySpawn.id);

                            scanner.repeatSpawn = mySpawn;
                            scanner.location = new LatLng(mySpawn.lat, mySpawn.lon);
                        }

                        scanner.repeat = false;
                        final LatLng loc = new LatLng(scanner.location.latitude, scanner.location.longitude);

                        try {
                            Runnable progressRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    updateScanLayout();
                                    updateScanPoint(loc, scanner.account);
                                }
                            };
                            features.runOnMainThread(progressRunnable);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        scanner.repeat = !scanForPokemon(scanner, loc.latitude, loc.longitude);

                        while (((captchaModePopup && scanner.account.captchaScreenVisible) || (use2Captcha && scanner.account.isSolvingCaptcha())) && !abortScan) {
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
                            removeMyScanPoint(scanner.account);
                            continue;
                        }
                    }

                    if (first) first = false;
                }

                try {
					/*if (scanner.failedSectors > 0) {
						if (failedScanLogins == NUM_SCAN_SECTORS) account.login();
						else {
							// TODO Make a new way to mark failed sectors
						}
					}*/
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (AccountScanner scanner : scanners) {
                    scanner.account.setScanning(false);
                    removeMyScanPoint(scanner.account);
                }
            }
        };

        return run(scanThread);
    }

    public synchronized Spawn claimSpawn(String id, final ConcurrentHashMap<String, Spawn> searchSpawns) {
        if (searchSpawns.containsKey(id)) {
            Spawn claimedSpawn = searchSpawns.remove(id);
            // Claim it and remove redundant spawns from the scan
            final LatLng loc = claimedSpawn.loc();

            features.print(TAG, "After claiming " + id + ", filtering out spawns in proximity...");
            LatLng here = new LatLng(loc.latitude, loc.longitude);
            ArrayList<Spawn> spawnList = new ArrayList<Spawn>(searchSpawns.values());
            for (Spawn spawn : spawnList) {
                LatLng spawnPoint = new LatLng(spawn.lat, spawn.lon);

                float[] distance = new float[3];
                Location.distanceBetween(here.latitude, here.longitude, spawn.lat, spawn.lon, distance);

                double meters = distance[0];
                if (meters <= MAX_SCAN_RADIUS) {
                    features.print(TAG, "Filtered out " + spawn.id);
                    searchSpawns.remove(spawn.id);
                    markSpawnAsSearched(spawn.id);
                    currentSector++;
                }
            }
            return claimedSpawn;
        }
        else return null;
    }

    public synchronized void markSpawnAsSearched(String id) {
        if (!searchedSpawns.contains(id)) searchedSpawns.add(id);
    }

    public String getLocalName(int pokedexNumber) {
        return PokeDictionary.getDisplayName(pokedexNumber, Locale.getDefault());
    }

    /*public void provokeCaptcha() {
        final ConcurrentHashMap<String, Spawn> mySpawnList = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Spawn> mySpawnList2 = new ConcurrentHashMap<>();

        features.provoking = true;

        if (spawns.size() >= 2) {
            final Spawn spawn = new ArrayList<>(spawns.values()).get(0);
            final Spawn spawn2 = new ArrayList<>(spawns.values()).get(1);
            mySpawnList.put(spawn.id, spawn);
            mySpawnList2.put(spawn2.id, spawn2);
        } else {
            mySpawnList.put("1", new Spawn("1", new LatLng(currentLat, currentLon), 25));
            mySpawnList2.put("2", new Spawn("2", new LatLng(currentLat + 0.01, currentLon + 0.01), 26));
        }

        for (int x = 0; x < 10; x++) {
            if (RequestHandler.captchaRequired) break;
            Thread thread = new Thread() {
                public void run() {
                    final Random random = new Random();
                    for (int n = 0; n < 100; n++) {
                        if (RequestHandler.captchaRequired) break;
                        final String name = n + "";
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                if (RequestHandler.captchaRequired) return;
                                scanning = false;
                                spawnScan(mySpawnList);
                                if (RequestHandler.captchaRequired) return;
                                scanning = false;
                                spawnScan(mySpawnList2);
                                if (RequestHandler.captchaRequired) return;
                            }
                        };
                        r.run();
                        //features.runOnMainThread(r);
					*//*
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}*//*
                    }
                }
            };
            thread.start();
        }
    }*/

    public void saveIVFilters() {
        NativePreferences.lock("save IV filters");

        NativePreferences.putInt(PREF_MIN_ATTACK, minAttack);
        NativePreferences.putInt(PREF_MIN_DEFENSE, minDefense);
        NativePreferences.putInt(PREF_MIN_STAMINA, minStamina);
        NativePreferences.putInt(PREF_MIN_PERCENT, minPercent);
        NativePreferences.putInt(PREF_MIN_OVERRIDE, minOverride);
        NativePreferences.putBoolean(PREF_OVERRIDE_ENABLED, overrideEnabled);

        NativePreferences.unlock();
    }

    public synchronized void activateGen2() {
        // Need to extend the filter and the custom images
        if (Features.NUM_POKEMON > 151) return;

        NativePreferences.lock("activate gen 2");
        Features.NUM_POKEMON = 251;
        NativePreferences.putInt(PREF_NUM_POKEMON, Features.NUM_POKEMON);
        NativePreferences.unlock();

        for (int n = features.customImages.size(); n < Features.NUM_POKEMON; n++) {
            features.customImages.add(" ");
            features.filter.put(n+1, true);
            features.filterOverrides.put(n+1, false);
        }

        features.saveCustomImagesUrls();
        features.saveFilter();
        features.saveFilterOverrides();
    }

    public synchronized static Future run(Runnable runnable) {
        if (pool == null) {
            PokeFinderActivity.features.print("PokeFinder", "Initializing a new thread pool");
            pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
        }
        Future future = pool.submit(new ExceptionCatchingRunnable(runnable));
        return future;
    }

    public void waitForHashLimit() {
        tryHashLimitWarning();
        try {
            if (PokeHashProvider.rpmTimeLeft != -1) {
                long timeLeft = PokeHashProvider.rpmTimeLeft - Calendar.getInstance().getTime().getTime() / 1000 + 1;
                if (timeLeft <= 0) return;
                Thread.sleep(timeLeft * 1000);
            } else {
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        hashLimitWarning = false;
    }

    public synchronized void tryHashLimitWarning() {
        if (hashLimitWarning) return;

        hashLimitWarning = true;
        features.longMessage("Exceeded API usage limit. Waiting a few seconds before trying again...");
    }

    public synchronized void tryGenericHashWarning(String warning) {
        if (hashWarning || warning == null) return;
        if (warning.contains("DOCTYPE")) return; // This is just HTML garbage

        hashWarning = true;
        features.longMessage(warning);

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                hashWarning = false;
            }
        };
        timer.schedule(task, 10000);
    }

    public void updateRpmLabel() {
        PokeFinderActivity.instance.updateRpmLabelText();
    }

    public void startRpmTimer() {
        if (rpmTimer != null) rpmTimer.cancel();
        rpmTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (PokeHashProvider.rpmTimeLeft != -1 && !resetTimerRunning) {
                    print("RateLimit is " + PokeHashProvider.rateLimit);
                    startResetRpmTimer();
                } else if (resetTimerRunning) {
                    PokeFinderActivity.instance.showRpmCountLabel();
                    PokeFinderActivity.instance.updateRpmCountLabelText();
                }
                updateRpmLabel();
            }
        };
        rpmTimer.schedule(task, RPM_REFRESH_RATE, RPM_REFRESH_RATE);
    }

    public void stopRpmTimer() {
        if (rpmTimer != null) rpmTimer.cancel();
    }

    public void startResetRpmTimer() {
        if (resetRpmTimer != null) resetRpmTimer.cancel();
        resetTimerRunning = true;
        resetRpmTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                PokeHashProvider.rpmTimeLeft = -1;
                PokeHashProvider.requestsRemaining = PokeHashProvider.rpm;
                PokeHashProvider.exceededRpm = false;
                updateRpmLabel();
                PokeFinderActivity.instance.hideRpmCountLabel();
                resetTimerRunning = false;
            }
        };
        long timeLeft = PokeHashProvider.rpmTimeLeft - Calendar.getInstance().getTime().getTime() / 1000 + 1;
        if (timeLeft < 0) {
            return;
            //print("Time left for rpm is " + timeLeft + " so we're gonna call it 0");
            //timeLeft = 0;
        }
        if (timeLeft > -1) resetRpmTimer.schedule(task, timeLeft * 1000);
        print("Starting rpm countdown timer for " + timeLeft + "s");
    }

    public void stopResetRpmTimer() {
        if (resetRpmTimer != null) resetRpmTimer.cancel();
    }
}
