package com.logickllc.pokesensor;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import com.pokegoapi.exceptions.request.CaptchaActiveException;
import com.pokegoapi.exceptions.request.HashException;
import com.pokegoapi.exceptions.request.HashLimitExceededException;
import com.pokegoapi.exceptions.request.LoginFailedException;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.util.PokeDictionary;
import com.pokegoapi.util.Signature;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import org.moe.natj.objc.SEL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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

import POGOProtos.Data.PokemonDataOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass;
import apple.corelocation.CLLocation;
import apple.corelocation.struct.CLLocationCoordinate2D;
import apple.mapkit.MKMapView;
import apple.mapkit.MKPointAnnotation;
import apple.uikit.UIApplication;
import apple.uikit.UIColor;
import apple.uikit.UILocalNotification;
import apple.uikit.c.UIKit;
import apple.uikit.enums.UIControlEvents;
import apple.uikit.enums.UIUserNotificationType;

public class IOSMapHelper extends MapHelper {
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
	public static final String PREF_BACKGROUND_SCANNING = "BackgroundScanning";
	public static final String PREF_BACKGROUND_INTERVAL = "BackgroundInterval";
	public static final String PREF_BACKGROUND_INCLUDE_NEARBY = "BackgroundIncludeNearby";
	public static final String PREF_CAPTCHA_NOTIFICATIONS = "CaptchaNotifications";
	public static final String PREF_BACKGROUND_SCAN_IVS = "BackgroundScanIvs";
	public static final String PREF_BACKGROUND_NOTIFICATION_SOUND = "BackgroundNotificationSound";
	public static final String PREF_SHOW_MOVESETS = "ShowMovesets";
	public static final String PREF_SHOW_HEIGHT_WEIGHT = "ShowHeightWeight";
	public static final String PREF_ONLY_SCAN_SPAWNS = "OnlyScanSpawns";
	public static final float DEFAULT_ZOOM = 2000f;

	private MKPointAnnotation myMarker;
	private MKMapView mMap;
	private ConcurrentHashMap<Long, MKPointAnnotation> pokeMarkers = new ConcurrentHashMap<Long, MKPointAnnotation>();
	private int paddingLeft, paddingRight, paddingTop, paddingBottom;
	private CustomCircle scanCircle;
	private String scanDialogMessage;
	private CustomCircle tempScanPointCircle;
	private int scanProgressMax;
	protected ArrayList<NearbyPokemonGPS> totalNearbyPokemon = new ArrayList<NearbyPokemonGPS>();
	public String POKEMON_FOLDER = "pokemon/";
	public final String IMAGE_EXTENSION = ".png";
	private String scanPointIcon = "scan_point_icon" + IMAGE_EXTENSION;
	public double altitude = 0;
	public final String SPAWN_FOLDER = "spawns/";
	public final String SPAWN_FILE = SPAWN_FOLDER + "spawns.ser";

	public int currentSector = 0;
	private ConcurrentHashMap<Account, MKPointAnnotation> scanPoints = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Account, CustomCircle> scanPointCircles = new ConcurrentHashMap<>();
	public boolean loadedSpawns = false;
	public ArrayList<ImageAnnotation> mapSpawns = new ArrayList<>();
	public ArrayList<CustomCircle> spawnCircles = new ArrayList<>();
	public static final String POKEMARKER = "pokemarker.png";

	public static final boolean CAN_SHOW_IMAGES = false;
	public static final String NUMBER_MARKER_FOLDER = "pokemarkers/";
	private ArrayList<CustomCircle> scanPointCirclesDetailed = new ArrayList<>();
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

	public ConcurrentHashMap<Long, String> nearbyBackgroundPokemon = new ConcurrentHashMap<>();
	public ConcurrentHashMap<Long, String> wildBackgroundPokemon = new ConcurrentHashMap<>();
	public ConcurrentHashMap<Long, String> wildBackgroundPokemonIvs = new ConcurrentHashMap<>();
	public ArrayList<Long> notifiedWilds = new ArrayList<>();
	public ArrayList<Long> notifiedNearbies = new ArrayList<>();
	public int backgroundCaptchasLost = 0;
	public boolean scanningBackground = false;
	public boolean spawnsLoaded = false;
	public boolean apiKeyPromptVisible = false;
	public static final String PAID_API_HELP_PAGE_URL = "https://www.reddit.com/r/pokesensor/comments/5yk6hu/paid_api_help/?ref=share&ref_source=link";

	public String[] types = "BULBASAUR,grass|IVYSAUR,grass|VENUSAUR,grass|CHARMANDER,fire|CHARMELEON,fire|CHARIZARD,fire|SQUIRTLE,water|WARTORTLE,water|BLASTOISE,water|CATERPIE,bug|METAPOD,bug|BUTTERFREE,bug|WEEDLE,bug|KAKUNA,bug|BEEDRILL,bug|PIDGEY,normal|PIDGEOTTO,normal|PIDGEOT,normal|RATTATA,normal|RATICATE,normal|SPEAROW,normal|FEAROW,normal|EKANS,poison|ARBOK,poison|PIKACHU,electric|RAICHU,electric|SANDSHREW,ground|SANDSLASH,ground|NIDORAN_FEMALE,poison|NIDORINA,poison|NIDOQUEEN,poison|NIDORAN_MALE,poison|NIDORINO,poison|NIDOKING,poison|CLEFAIRY,fairy|CLEFABLE,fairy|VULPIX,fire|NINETALES,fire|JIGGLYPUFF,normal|WIGGLYTUFF,normal|ZUBAT,poison|GOLBAT,poison|ODDISH,grass|GLOOM,grass|VILEPLUME,grass|PARAS,bug|PARASECT,bug|VENONAT,bug|VENOMOTH,bug|DIGLETT,ground|DUGTRIO,ground|MEOWTH,normal|PERSIAN,normal|PSYDUCK,water|GOLDUCK,water|MANKEY,fighting|PRIMEAPE,fighting|GROWLITHE,fire|ARCANINE,fire|POLIWAG,water|POLIWHIRL,water|POLIWRATH,water|ABRA,psychic|KADABRA,psychic|ALAKAZAM,psychic|MACHOP,fighting|MACHOKE,fighting|MACHAMP,fighting|BELLSPROUT,grass|WEEPINBELL,grass|VICTREEBEL,grass|TENTACOOL,water|TENTACRUEL,water|GEODUDE,rock|GRAVELER,rock|GOLEM,rock|PONYTA,fire|RAPIDASH,fire|SLOWPOKE,water|SLOWBRO,water|MAGNEMITE,electric|MAGNETON,electric|FARFETCHD,normal|DODUO,normal|DODRIO,normal|SEEL,water|DEWGONG,water|GRIMER,poison|MUK,poison|SHELLDER,water|CLOYSTER,water|GASTLY,ghost|HAUNTER,ghost|GENGAR,ghost|ONIX,rock|DROWZEE,psychic|HYPNO,psychic|KRABBY,water|KINGLER,water|VOLTORB,electric|ELECTRODE,electric|EXEGGCUTE,grass|EXEGGUTOR,grass|CUBONE,ground|MAROWAK,ground|HITMONLEE,fighting|HITMONCHAN,fighting|LICKITUNG,normal|KOFFING,poison|WEEZING,poison|RHYHORN,ground|RHYDON,ground|CHANSEY,normal|TANGELA,grass|KANGASKHAN,normal|HORSEA,water|SEADRA,water|GOLDEEN,water|SEAKING,water|STARYU,water|STARMIE,water|MR_MIME,psychic|SCYTHER,bug|JYNX,ice|ELECTABUZZ,electric|MAGMAR,fire|PINSIR,bug|TAUROS,normal|MAGIKARP,water|GYARADOS,water|LAPRAS,water|DITTO,normal|EEVEE,normal|VAPOREON,water|JOLTEON,electric|FLAREON,fire|PORYGON,normal|OMANYTE,rock|OMASTAR,rock|KABUTO,rock|KABUTOPS,rock|AERODACTYL,rock|SNORLAX,normal|ARTICUNO,ice|ZAPDOS,electric|MOLTRES,fire|DRATINI,dragon|DRAGONAIR,dragon|DRAGONITE,dragon|MEWTWO,psychic|MEW,psychic|CHIKORITA,grass|BAYLEEF,grass|MEGANIUM,grass|CYNDAQUIL,fire|QUILAVA,fire|TYPHLOSION,fire|TOTODILE,water|CROCONAW,water|FERALIGATR,water|SENTRET,normal|FURRET,normal|HOOTHOOT,normal|NOCTOWL,normal|LEDYBA,bug|LEDIAN,bug|SPINARAK,bug|ARIADOS,bug|CROBAT,poison|CHINCHOU,water|LANTURN,water|PICHU,electric|CLEFFA,fairy|IGGLYBUFF,normal|TOGEPI,fairy|TOGETIC,fairy|NATU,psychic|XATU,psychic|MAREEP,electric|FLAAFFY,electric|AMPHAROS,electric|BELLOSSOM,grass|MARILL,water|AZUMARILL,water|SUDOWOODO,rock|POLITOED,water|HOPPIP,grass|SKIPLOOM,grass|JUMPLUFF,grass|AIPOM,normal|SUNKERN,grass|SUNFLORA,grass|YANMA,bug|WOOPER,water|QUAGSIRE,water|ESPEON,psychic|UMBREON,dark|MURKROW,dark|SLOWKING,water|MISDREAVUS,ghost|UNOWN,psychic|WOBBUFFET,psychic|GIRAFARIG,normal|PINECO,bug|FORRETRESS,bug|DUNSPARCE,normal|GLIGAR,ground|STEELIX,steel|SNUBBULL,fairy|GRANBULL,fairy|QWILFISH,water|SCIZOR,bug|SHUCKLE,bug|HERACROSS,bug|SNEASEL,dark|TEDDIURSA,normal|URSARING,normal|SLUGMA,fire|MAGCARGO,fire|SWINUB,ice|PILOSWINE,ice|CORSOLA,water|REMORAID,water|OCTILLERY,water|DELIBIRD,ice|MANTINE,water|SKARMORY,steel|HOUNDOUR,dark|HOUNDOOM,dark|KINGDRA,water|PHANPY,ground|DONPHAN,ground|PORYGON2,normal|STANTLER,normal|SMEARGLE,normal|TYROGUE,fighting|HITMONTOP,fighting|SMOOCHUM,ice|ELEKID,electric|MAGBY,fire|MILTANK,normal|BLISSEY,normal|RAIKOU,electric|ENTEI,fire|SUICUNE,water|LARVITAR,rock|PUPITAR,rock|TYRANITAR,rock|LUGIA,psychic|HO_OH,fire|CELEBI,psychic".split("\\|");

	public Thread wideScanThread, wideScanBackgroundThread, spawnScanThread, spawnScanBackgroundThread;
	public static final int MAX_SCAN_ERROR_COUNT = 3;
	public ConcurrentHashMap<Runnable, Boolean> sleepingThreads = new ConcurrentHashMap<>();

	public MKPointAnnotation getMyMarker() {
		return myMarker;
	}

	public void setMyMarker(MKPointAnnotation myMarker) {
		this.myMarker = myMarker;
	}

	public ConcurrentHashMap<Long, MKPointAnnotation> getPokeMarkers() {
		return pokeMarkers;
	}

	public void setPokeMarkers(ConcurrentHashMap<Long, MKPointAnnotation> pokeMarkers) {
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

	public CustomCircle getScanCircle() {
		return scanCircle;
	}

	public void setScanCircle(CustomCircle scanCircle) {
		this.scanCircle = scanCircle;
	}

	public String getScanDialogMessage() {
		return scanDialogMessage;
	}

	public void setScanDialogMessage(String scanDialogMessage) {
		this.scanDialogMessage = scanDialogMessage;
	}

	public String getScanPointIcon() {
		return scanPointIcon;
	}

	public void setScanPointIcon(String scanPointIcon) {
		this.scanPointIcon = scanPointIcon;
	}

	public MKMapView getmMap() {
		return mMap;
	}

	public void setmMap(MKMapView mMap) {
		this.mMap = mMap;
	}

	public void refreshTempScanCircle() {
		Runnable circleRunnable = new Runnable() {
			@Override
			public void run() {
				if (scanCircle != null) mMap.removeOverlay(scanCircle);
				scanCircle = CustomCircle.alloc().initWithCenterCoordinateRadius(new CLLocationCoordinate2D(currentLat, currentLon), scanDistance);
				scanCircle.strokeColor = UIColor.blackColor();
				mMap.addOverlay(scanCircle);
			}
		};
		features.runOnMainThread(circleRunnable);
	}

	public void removeTempScanCircle() {
		Runnable circleRunnable = new Runnable() {
			@Override
			public void run() {
				if (scanCircle != null) mMap.removeOverlay(scanCircle);
			}
		};
		features.runOnMainThread(circleRunnable);
	}

	public synchronized void moveMe(double lat, double lon, double altitude, boolean repositionCamera, boolean reZoom) {
		//this.altitude = altitude;
		//print("Altitude is now " + altitude);
		// Add a marker in Sydney and move the camera
		CLLocation me = CLLocation.alloc().initWithLatitudeLongitude(lat, lon);
		if (myMarker != null) mMap.removeAnnotation(myMarker);
		myMarker = MKPointAnnotation.alloc().init();
		myMarker.setCoordinate(me.coordinate());
		myMarker.setTitle("Me");
		mMap.addAnnotation(myMarker);
		if (repositionCamera) {
			mMap.camera().setCenterCoordinate(me.coordinate());
			if (reZoom) mMap.camera().setAltitude(DEFAULT_ZOOM); // Not sure how far up this is yet
		}
		currentLat = lat;
		currentLon = lon;
		refreshTempScanCircle();
	}

	public void wideScan() {
		if (!MapController.mapHelper.promptForApiKey()) return;
		final ArrayList<Account> goodAccounts = AccountManager.getGoodAccounts();
		if (goodAccounts.size() == 0) {
            features.longMessage("You don't have any valid accounts!");
			return;
		}

		if (scanning) return;
		else scanning = true;
		searched = true;
		removeTempScanCircle();

		newSpawns = 0;
		currentSector = 0;
		features.captchaScreenVisible = false;

		updateScanSettings();
		abortScan = false;
		if (scanDistance > MAX_SCAN_DISTANCE) scanDistance = MAX_SCAN_DISTANCE;

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
								MKPointAnnotation marker = pokeMarkers.remove(id);
								if (marker != null) mMap.removeAnnotation(marker);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					noTimes.clear();

					try {
						//final ArrayList<Long> ids = new ArrayList<Long>(noTimes.keys().);
						Map<Long, WildPokemonTime> temp = pokeTimes;

						for (Long id : temp.keySet()) {
							try {
								features.print(TAG, "Removed poke marker!");
								MKPointAnnotation marker = pokeMarkers.remove(id);
								if (marker != null) mMap.removeAnnotation(marker);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					pokeTimes.clear();
				}

				removeScanPoints();
				removeScanPointCircles();

				MapController.instance.scanBar.setProgress(0);

				MapController.instance.scanView.setHidden(false);

				final Thread scanThread = new Thread() {
					public void run() {
						failedScanLogins = 0;

						Runnable circleRunnable = new Runnable() {
							@Override
							public void run() {
								if (scanCircle != null) mMap.removeOverlay(scanCircle);
								scanCircle = CustomCircle.alloc().initWithCenterCoordinateRadius(new CLLocationCoordinate2D(currentLat, currentLon), scanDistance);
								scanCircle.strokeColor = UIColor.blueColor();
								mMap.addOverlay(scanCircle);
							}
						};
						features.runOnMainThread(circleRunnable);

						features.print(TAG, "Scan distance: " + scanDistance);

						totalNearbyPokemon.clear();
						totalEncounters.clear();
						totalWildEncounters.clear();

						Vector2[] boxPoints = getSearchPoints(scanDistance);

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
						features.print(TAG,  "Min scan time: " + minScanTime);
						features.print(TAG, "Center coord is: (" + currentLat + ", " + currentLon + ")");

						SCAN_INTERVAL = Math.max(SCAN_INTERVAL, minScanTime);

						scanProgressMax = NUM_SCAN_SECTORS;

						scanPointCircles.clear();
						scanPoints.clear();

						features.resetMapObjects();

						// Start new scanning method

						int scansPerWorker = boxPoints.length / goodAccounts.size();
						int extraScans = boxPoints.length - scansPerWorker * goodAccounts.size();
						int cursor = 0;
						ArrayList<Future> scanThreads = new ArrayList<>();

						int workersPerThread = goodAccounts.size() / MAX_POOL_THREADS;
						int extraWorkers = goodAccounts.size() - workersPerThread * MAX_POOL_THREADS;
						int workerCursor = 0;

						CLLocationCoordinate2D center = new CLLocationCoordinate2D(currentLat, currentLon);

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
								AccountScanner scanner = new AccountScanner(account, new ArrayList<Vector2>());
								workers.add(scanner);
							}

							workerList.add(workers);
						}

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

						for (ArrayList<AccountScanner> scanAccounts : workerList) {
							ArrayList<AccountScanner> usableAccounts = new ArrayList<>();
							for (AccountScanner scanner : scanAccounts) {
								if (!scanner.points.isEmpty()) usableAccounts.add(scanner);
							}

							scanThreads.add(accountScan(usableAccounts, SCAN_INTERVAL, center));
						}

						/*for (int n = 0; n < MAX_POOL_THREADS; n++) {
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

								ArrayList<Vector2> scanPoints = new ArrayList<>();
								for (int y = 0; y < numScans; y++) {
									scanPoints.add(boxPoints[cursor]);
									cursor++;
								}

								Account account = goodAccounts.get(workerCursor++);
								AccountScanner scanner = new AccountScanner(account, scanPoints);
								scanAccounts.add(scanner);
							}
							scanThreads.add(accountScan(scanAccounts, SCAN_INTERVAL, center));
						}*/

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

								MapController.instance.scanView.setHidden(true);
							}
						};
						features.runOnMainThread(dismissRunnable);

						if (collectSpawns) {
							if (newSpawns > 1)
								features.shortMessage("Found " + newSpawns + " new spawn points and added them to My Spawns!");
							else if (newSpawns == 1)
								features.shortMessage("Found 1 new spawn point and added it to My Spawns!");
						}
						scanning = false;
					}
				};

				MapController.instance.scanView.addTargetActionForControlEvents(MapController.instance, new SEL("abortWideScan:"), UIControlEvents.TouchUpInside);

				wideScanThread = scanThread;

				scanThread.start();
			}
		};

		features.runOnMainThread(main);
	}

	void abortWideScan(ClickableView sender) {
		print("Entered abortWideScan:");
		wideScanThread.interrupt();
		abortScan = true;
		MapController.instance.scanView.setHidden(true);

		removeScanPoints();
		if (!showScanDetails) {
			removeScanPointCircles();
		}
	}

	public void removeScanPoints() {
		/*for (MKPointAnnotation scanPoint : scanPoints.values()) {
			if (scanPoint != null) mMap.removeAnnotation(scanPoint);
		}*/
	}

	public void removeScanPointCircles() {
		try {
			if (scanPointCirclesDetailed != null) {
				for (CustomCircle scanPointCircleDetailed : scanPointCirclesDetailed) {
					if (scanPointCircleDetailed != null) mMap.removeOverlay(scanPointCircleDetailed);
				}
			}
			if (scanPointCircles != null) {
				for (CustomCircle scanPointCircle : scanPointCircles.values()) {
					if (scanPointCircle != null) mMap.removeOverlay(scanPointCircle);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public Future accountScan(final ArrayList<AccountScanner> scanners, final long SCAN_INTERVAL, final CLLocationCoordinate2D center) {
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

					Collections.sort(scanners, new Comparator<AccountScanner>() {
						@Override
						public int compare(AccountScanner o1, AccountScanner o2) {
							return Long.compare(o1.account.lastScanTime, o2.account.lastScanTime);
						}
					});

					try {
						if (first) {
							Thread.sleep(1000);
							first = false;
						}
						else {
							for (AccountScanner scanner : scanners) {
								// Assume the first scanning account in the list is the one that will be ready fastest
								if (scanner.account.isScanning() && !readyToScan(scanner.account, SCAN_INTERVAL + 100)) {
									long waitTime = scanner.account.lastScanTime + SCAN_INTERVAL + 100 - System.currentTimeMillis();
									print(scanner.account.getUsername() + " needs to wait " + waitTime + " ms before it can scan again. Waiting...");
									Thread.sleep(waitTime);
									print(scanner.account.getUsername() + " has waited " + waitTime + " ms and is ready to scan again.");
									break;
								}
							}
						}
					} catch (InterruptedException e) {
						if (abortScan) {
							for (AccountScanner scanner : scanners) {
								scanner.account.setScanning(false);
							}
							return;
						}
					}

					for (final AccountScanner scanner : scanners) {
						if (!scanner.account.isScanning() || !readyToScan(scanner.account, SCAN_INTERVAL + 100)) continue;

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
										if (scanner.account.circle != null) {
											mMap.removeOverlay(scanner.account.circle);
											scanPointCirclesDetailed.remove(scanner.account);
										}
									}
								};
								features.runOnMainThread(runnable);
							}
							scanner.pointCursor--;
							scanner.failedSectors--;
							currentSector--;
						}
						scanner.repeat = false;

						// Had some problems here at one point
						if (scanner.pointCursor >= scanner.points.size()) {
							scanner.account.setScanning(false);
							continue;
						}
						final CLLocationCoordinate2D loc = cartesianToCoord(scanner.points.get(scanner.pointCursor), center);
						scanner.pointCursor++;

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

						scanner.repeat = !scanForPokemon(scanner, loc.latitude(), loc.longitude());

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

						if ((!scanner.repeat && scanner.pointCursor >= scanner.points.size()) || scanner.account.scanErrorCount >= MAX_SCAN_ERROR_COUNT) {
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

	public boolean readyToScan(Account account, final long SCAN_INTERVAL) {
		return account.lastScanTime + SCAN_INTERVAL < System.currentTimeMillis();
	}

	public boolean readyToSpawnScan(Account account, final long SCAN_INTERVAL) {
		return Math.max(account.lastScanTime + SCAN_INTERVAL, account.nextScanTime) < System.currentTimeMillis();
	}

	public synchronized void removeMyScanPoint(final Account account) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				//MKPointAnnotation scanPoint = scanPoints.get(account);
				CustomCircle scanPointCircle = scanPointCircles.get(account);

				//if (scanPoint != null) mMap.removeAnnotation(scanPoint);
				if (scanPointCircle != null && !showScanDetails) mMap.removeOverlay(scanPointCircle);
			}
		};

		features.runOnMainThread(runnable);
	}

	public synchronized void updateScanLayout() {
		currentSector++;
		scanDialogMessage = "Scanning sector " + currentSector + "/" + NUM_SCAN_SECTORS + "  " + R.string.tapCancel;
		MapController.instance.scanText.setText(scanDialogMessage);
		setScanProgress(currentSector);
	}

	public synchronized void updateScanPoint(CLLocationCoordinate2D loc, Account account) {
		//MKPointAnnotation scanPoint;
		CustomCircle scanPointCircle;

		//scanPoint = scanPoints.get(account);
		//if (scanPoint != null) mMap.removeAnnotation(scanPoint);

		if (!showScanDetails) {
			scanPointCircle = scanPointCircles.get(account);
			if (scanPointCircle != null) mMap.removeOverlay(scanPointCircle);
		}

		scanPointCircle = CustomCircle.alloc().initWithCenterCoordinateRadius(loc, MAX_SCAN_RADIUS);

		if (showScanDetails) {
			scanPointCircle.strokeColor = UIColor.blueColor();
			scanPointCircle.fillColor = UIColor.blueColor().colorWithAlphaComponent(SCAN_DETAIL_CIRCLE_ALPHA);
			account.circle = scanPointCircle;
		}

		/*scanPoint = ImageAnnotation.alloc().init(scanPointIcon);
		scanPoint.setCoordinate(loc);
		scanPoint.setTitle(account.getUsername());*/

		//mMap.addAnnotation(scanPoint);
		mMap.addOverlay(scanPointCircle);

		//scanPoints.put(account, scanPoint);

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
					CustomCircle circle = account.circle;
					if (circle != null) {
						circle.strokeColor = UIColor.redColor();
						circle.fillColor = UIColor.redColor().colorWithAlphaComponent(SCAN_DETAIL_CIRCLE_ALPHA);
						mMap.removeOverlay(circle);
						mMap.addOverlay(circle);
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
					CustomCircle circle = account.circle;
					if (circle != null) {
						circle.strokeColor = UIColor.greenColor();
						circle.fillColor = UIColor.greenColor().colorWithAlphaComponent(SCAN_DETAIL_CIRCLE_ALPHA);
						mMap.removeOverlay(circle);
						mMap.addOverlay(circle);
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
					CustomCircle circle = account.circle;
					if (circle != null) {
						circle.strokeColor = UIColor.yellowColor();
						circle.fillColor = UIColor.yellowColor().colorWithAlphaComponent(SCAN_DETAIL_CIRCLE_ALPHA);
						mMap.removeOverlay(circle);
						mMap.addOverlay(circle);
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
					CustomCircle circle = account.circle;
					if (circle != null) {
						circle.strokeColor = UIColor.lightGrayColor();
						circle.fillColor = UIColor.lightGrayColor().colorWithAlphaComponent(SCAN_DETAIL_CIRCLE_ALPHA);
						mMap.removeOverlay(circle);
						mMap.addOverlay(circle);
					}
				}
			};

			features.runOnMainThread(runnable);
		}
	}

	public synchronized ArrayList<WildPokemonTime> getNoTimePokesInSector(double lat, double lon) {
		Stopwatch.click("getNoTimePokesInSector");
		ArrayList<WildPokemonTime> results = new ArrayList<>();

		CLLocation here = CLLocation.alloc().initWithLatitudeLongitude(lat, lon);
		for (WildPokemonTime pokemonTime : noTimes.values()) {
			CLLocation spawnPoint = CLLocation.alloc().initWithLatitudeLongitude(pokemonTime.getPoke().getLatitude(), pokemonTime.getPoke().getLongitude());
			double meters = here.distanceFromLocation(spawnPoint);
			if (meters <= MAX_SCAN_RADIUS) {
				results.add(pokemonTime);
			}
		}

		Stopwatch.click("getNoTimePokesInSector");

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

			go.setLocation(lat, lon, 0);
			Thread.sleep(200);

			scanner.account.lastScanTime = System.currentTimeMillis();

			try {
				features.refreshMapObjects(account);
			} catch (Exception c) {
				if (c instanceof CaptchaActiveException) showCaptchaCircle(account);
				account.checkExceptionForCaptcha(c);
			}
			Thread.sleep(200);

			scanner.account.lastScanTime = System.currentTimeMillis();

			scanner.activeSpawns.clear();

			if (use2Captcha) {
				if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED || account.getStatus() == Account.AccountStatus.SOLVING_CAPTCHA) {
					showCaptchaCircle(account);
					((IOSFeatures) features).shortMessage("Solving captcha for " + account.getUsername() + "...");
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
						((IOSFeatures) features).superLongMessage("Captcha required for " + account.getUsername() + ". You can do this from the Accounts screen or set the Captcha Mode to Pop-up from the Preferences screen.");
						return false;
					}
				}
			}

			DateFormat df = null;
			if (collectSpawns) df = new SimpleDateFormat("mm:ss");

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

				try {
					if (collectSpawns) {
						Stopwatch.click("First despawn learning");
						Spawn mySpawn = spawns.get(poke.getSpawnPointId());
						if (mySpawn != null) {
							long time = poke.getExpirationTimestampMs() - System.currentTimeMillis();
							print("Spawn point " + mySpawn.id + " despawns at minute " + mySpawn.despawnMinute + " and second " + mySpawn.despawnSecond);
							if (time > 0 && time <= 3600000) {
								if (mySpawn.despawnMinute < 0 || mySpawn.despawnSecond < 0) {
									long expTime = poke.getExpirationTimestampMs();
									Date date = new Date(expTime);
									String[] timeStrings = df.format(date).split(":");
									mySpawn.despawnMinute = Integer.parseInt(timeStrings[0]);
									mySpawn.despawnSecond = Integer.parseInt(timeStrings[1]);
									print("We found the despawn time for spawn " + poke.getSpawnPointId() + ". It despawns at the " + timeStrings[0] + " minute and " + timeStrings[1] + " second mark!");
								}
							}
						}
						Stopwatch.click("First despawn learning");
					}
				} catch (Exception e) {
					e.printStackTrace();
					ErrorReporter.logExceptionThreaded(e);
				}

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
						ErrorReporter.logExceptionThreaded(e);
					}

					long timeMs = poke.getExpirationTimestampMs() - System.currentTimeMillis();

					try {
						if (collectSpawns) {
							Stopwatch.click("Second despawn learning");
							Spawn mySpawn = spawns.get(poke.getSpawnPointId());
							if (mySpawn != null) {
								if (mySpawn.despawnMinute >= 0 && mySpawn.despawnSecond >= 0) {
									Date date = new Date(System.currentTimeMillis());
									String[] timeStrings = df.format(date).split(":");
									int currentMinute = Integer.parseInt(timeStrings[0]);
									int currentSecond = Integer.parseInt(timeStrings[1]);
									int currentTotalSeconds = currentMinute * 60 + currentSecond;
									int despawnTotalSeconds = mySpawn.despawnMinute * 60 + mySpawn.despawnSecond;
									if (despawnTotalSeconds < currentTotalSeconds)
										despawnTotalSeconds += 3600;

									int diffSeconds = despawnTotalSeconds - currentTotalSeconds;

									timeMs = diffSeconds * 1000;

									print("Calculated despawn time for " + mySpawn.id + " is " + timeMs);
								}
							}
							Stopwatch.click("Second despawn learning");
						}
					} catch (Exception e) {
						e.printStackTrace();
						ErrorReporter.logExceptionThreaded(e);
					}

					if (timeMs > 0 && timeMs <= 3600000) {
						long despawnTime = System.currentTimeMillis() + timeMs;
						pokeTimes.put(poke.getEncounterId(), new WildPokemonTime(poke, despawnTime));
						features.print(TAG, poke.getPokemonId() + " will despawn at " + despawnTime);
					} else {
						noTimes.put(poke.getEncounterId(), new WildPokemonTime(poke, System.currentTimeMillis(), poke.getSpawnPointId()));
					}
				}

			}

			final List<CatchablePokemon> pokes = features.getCatchablePokemon(account, 15);
			final List<NearbyPokemon> nearbyPokes = features.getNearbyPokemon(account, 15);

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

			if (wildPokes.isEmpty() && pokes.isEmpty() && nearbyPokes.isEmpty()) {
				showEmptyResultsCircle(account);
			} else {
				showGoodCircle(account);
			}

			for (NearbyPokemon poke : nearbyPokes) {
				int pokedexNumber = poke.getPokemonId().getNumber();

				if (pokedexNumber > Features.NUM_POKEMON) activateGen2();

				if (!features.filter.get(pokedexNumber) && (!showIvs || !overrideEnabled)) continue;
				totalNearbyPokemon.add(new NearbyPokemonGPS(poke, new CLLocationCoordinate2D(lat, lon)));
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
				ArrayList<String> moveHolder = new ArrayList<>();
				ArrayList<String> heightWeightHolder = new ArrayList<>();
				ArrayList<String> genderHolder = new ArrayList<>();
				boolean hide = false;
				if (showIvs) {
					for (CatchablePokemon pokemon : pokes) {
						if (poke.getPokemonId().getNumber() > Features.NUM_POKEMON) activateGen2();
						if (poke.getSpawnPointId().equals(pokemon.getSpawnPointId())) {
							try {
								hide = encounterPokemon(poke, pokemon, ivHolder, moveHolder, heightWeightHolder, genderHolder, pokedexNumber);
							} catch (Throwable e) {
								if (!features.filter.get(pokedexNumber)) hide = true;

								account.checkExceptionForCaptcha(e);

								Throwable t = e;
								while (t != null) {
									if (t instanceof HashException) {
										if (t instanceof HashLimitExceededException) {
											waitForHashLimit();
											try {
												hide = encounterPokemon(poke, pokemon, ivHolder, moveHolder, heightWeightHolder, genderHolder, pokedexNumber);
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

				final Spawn finalSpawn = spawns.get(poke.getSpawnPointId());
				final String myIvs = ivHolder.size() > 0 ? ivHolder.get(0) : "";
				final String myMoves = moveHolder.size() > 0 ? moveHolder.get(0) : "";
				final String myHeightWeight = heightWeightHolder.size() > 0 ? heightWeightHolder.get(0) : "";
				final String gender = genderHolder.size() > 0 ? genderHolder.get(0) : "";

				long tempTime = poke.getExpirationTimestampMs() - System.currentTimeMillis();

				if (collectSpawns && finalSpawn != null && finalSpawn.despawnMinute >= 0 && finalSpawn.despawnSecond >= 0) {
					Stopwatch.click("Third despawn learning");
					Date date = new Date(System.currentTimeMillis() * 1000);
					String[] timeStrings = df.format(date).split(":");
					int currentMinute = Integer.parseInt(timeStrings[0]);
					int currentSecond = Integer.parseInt(timeStrings[1]);
					int currentTotalSeconds = currentMinute * 60 + currentSecond;
					int despawnTotalSeconds = finalSpawn.despawnMinute * 60 + finalSpawn.despawnSecond;
					if (despawnTotalSeconds < currentTotalSeconds) despawnTotalSeconds += 3600;

					int diffSeconds = despawnTotalSeconds - currentTotalSeconds;

					tempTime = System.currentTimeMillis() + diffSeconds * 1000;
					Stopwatch.click("Third despawn learning");
				}

				final long time = tempTime;

				Runnable r = new Runnable() {
					@Override
					public void run() {
						Stopwatch.click("Show pokemon runnable");
						if (pokeTimes.containsKey(poke.getEncounterId()) || noTimes.containsKey(poke.getEncounterId())) {
							if (time > 0 && time <= 3600000) {
								String ms = String.format("%06d", time);
								int sec = Integer.parseInt(ms.substring(0, 3));
								//features.print(TAG, "Time string: " + time);
								//features.print(TAG, "Time shifted: " + (Long.parseLong(time) >> 16));
								features.print(TAG, "Time till hidden seconds: " + sec + "s");
								//features.print(TAG, "Data for " + poke.getPokemonId() + ":\n" + poke);
								showPokemonAt(poke.getPokemonId().name(), poke.getPokemonId().getNumber(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), true, myIvs, myMoves, myHeightWeight, gender);
							} else {
								features.print(TAG, "No valid expiry time given");
								showPokemonAt(poke.getPokemonId().name(), poke.getPokemonId().getNumber(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), false, myIvs, myMoves, myHeightWeight, gender);
							}
						} else {
							features.print(TAG, "Neither pokeTimes nor noTimes contains " + poke.getPokemonId() + " at spawn " + poke.getSpawnPointId() + " but it's trying to show on the map");
						}
						Stopwatch.click("Show pokemon runnable");
					}
				};

				features.runOnMainThread(r);
			}

			saveSpawns();

			// Now remove everything that needs to be removed
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Stopwatch.click("Removing removables");
					try {
						for (Long id : removables) {
							if (!noTimes.containsKey(id)) {
								MKPointAnnotation marker = pokeMarkers.remove(id);
								if (marker != null) mMap.removeAnnotation(marker);
							}
						}
						removables.clear();
					} catch (Throwable e) {
						e.printStackTrace();
						ErrorReporter.logException(e);
					}
					Stopwatch.click("Removing removables");
				}
			};
			features.runOnMainThread(runnable);

			account.scanErrorCount = 0;

			return true;
		} catch (Throwable e) {
			if (account.checkExceptionForCaptcha(e)) {
				showCaptchaCircle(account);
			} else {
				account.scanErrorCount++;
				showErrorCircle(account);
				ErrorReporter.logExceptionThreaded(e);
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

			// Now remove everything that needs to be removed
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Stopwatch.click("Removing removables in catch");
					try {
						for (Long id : removables) {
							if (!noTimes.containsKey(id)) {
								MKPointAnnotation marker = pokeMarkers.remove(id);
								if (marker != null) mMap.removeAnnotation(marker);
							}
						}
						removables.clear();
					} catch (Throwable e) {
						e.printStackTrace();
						ErrorReporter.logException(e);
					}
					Stopwatch.click("Removing removables in catch");
				}
			};
			features.runOnMainThread(runnable);

			return false;
		}
	}

	public boolean encounterPokemon(CatchablePokemon poke, CatchablePokemon pokemon, ArrayList<String> ivHolder, ArrayList<String> moveHolder, ArrayList<String> heightWeightHolder, ArrayList<String> genderHolder, int pokedexNumber) throws RequestFailedException {
		String ivs = "";
		ivHolder.add(ivs);

		String moves = "";
		moveHolder.add(moves);

		String heightWeight = "";
		heightWeightHolder.add(heightWeight);

		String gender = "";
		genderHolder.add(gender);

		PokemonDataOuterClass.PokemonData result = pokemon.encounter().getEncounteredPokemon();

		int attack = result.getIndividualAttack();
		int defense = result.getIndividualDefense();
		int stamina = result.getIndividualStamina();
		int percent = (int) ((attack + defense + stamina) / 45f * 100);

		ivs = attack + " ATK  " + defense + " DEF  " + stamina + " STAM\n" + percent + "%";

		ivHolder.clear();
		ivHolder.add(ivs);

		features.print(TAG, "IVs: " + ivs);

		String move1 = readableEnum(result.getMove1().name());
		String move2 = readableEnum(result.getMove2().name());

		moves = "Move 1: " + move1 + "\nMove 2: " + move2;

		moveHolder.clear();
		moveHolder.add(moves);

		String height = (Math.round(result.getHeightM() * 100) / 100.0) + " m";
		String weight = (Math.round(result.getWeightKg() * 100) / 100.0) + " kg";

		heightWeight = "Height: " + height + "\nWeight: " + weight;

		heightWeightHolder.clear();
		heightWeightHolder.add(heightWeight);

		try {
			gender = getGender(result.getPokemonDisplay().getGender().getNumber());
			genderHolder.clear();
			genderHolder.add(gender);
			print("Gender of " + pokemon.getPokemonId().name() + " is " + gender);
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
		}

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

	public String getGender(int genderInt) {
		switch (genderInt) {
			case 1:
				return "♂";

			case 2:
				return "♀";

			case 3:
				return "";

			default:
				return "";
		}
	}

	public String readableEnum(String value) {
		try {
			String temp = value.replaceAll("_", " ").toLowerCase();
			String result = "";
			for (String word : temp.split(" ")) {
				result += word.substring(0, 1).toUpperCase() + (word.length() > 1 ? word.substring(1) : "") + " ";
			}

			return result.substring(0, result.length() - 1);
		} catch (Exception e) {
			return value;
		}
	}

	public synchronized void showPokemonAt(String name, int pokedexNumber, CLLocationCoordinate2D loc, long encounterid, boolean hasTime, String ivs, String moves, String heightWeight, String gender) {
		if (pokeMarkers.containsKey(encounterid)) return;

		Stopwatch.click("showPokemonAt");

		String localName;
		try {
			localName = getLocalName(pokedexNumber);
		} catch (Exception e) {
			localName = PokemonIdOuterClass.PokemonId.valueOf(pokedexNumber).name();
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
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
			localName = localName.substring(0, 1).toUpperCase() + localName.substring(1).toLowerCase() + " " + gender;

			ImageAnnotation temp;
			if (CAN_SHOW_IMAGES) temp = ImageAnnotation.alloc().init(POKEMON_FOLDER + name.toLowerCase() + IMAGE_EXTENSION);
			else {
				String filename = pokedexNumber + IMAGE_EXTENSION;
				FileHandle handle = Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER + filename);
				if (handle.exists()) {
					temp = ImageAnnotation.alloc().init(Features.CUSTOM_IMAGES_FOLDER + filename);
					temp.isCustom = true;
				} else {
					if (defaultMarkersMode == 1) {
						temp = ImageAnnotation.alloc().init(NUMBER_MARKER_FOLDER + pokedexNumber + IMAGE_EXTENSION);
					}
					else {
						//PokemonSettingsOuterClass.PokemonSettings settings = PokemonMeta.getPokemonSettings(PokemonIdOuterClass.PokemonId.valueOf(pokedexNumber));
						//PokemonTypeOuterClass.PokemonType pokemonType = settings.getType();
						//String type = pokemonType.name().toLowerCase();
						//type = type.substring(type.lastIndexOf("_") + 1);

						String type = types[pokedexNumber - 1].split("\\,")[1];

						print("Type of " + localName + " is " + type);
						temp = ImageAnnotation.alloc().init(NUMBER_MARKER_FOLDER + BLANK_NAME_MARKER + type + IMAGE_EXTENSION);
						temp.isCustom = false;
					}
				}
			}

			temp.name = localName;

			if (showMovesets) temp.moves = moves;
			else temp.moves = "";

			if (showHeightWeight) temp.heightWeight = heightWeight;
			else temp.heightWeight = "";

			temp.pokedexNumber = pokedexNumber;

			if (hasTime) {
				//ImageAnnotation temp = new ImageAnnotation(POKEMARKER);
				//System.out.println("Path for " + name + ": " + temp.imagePath);
				temp.setCoordinate(loc);
				temp.setTitle(localName);
				temp.ivs = ivs;
				mMap.addAnnotation(temp);
				pokeMarkers.put(encounterid, temp);
			}
			else {
				//ImageAnnotation temp = new ImageAnnotation(POKEMARKER);
				//System.out.println("Path for " + name + ": " + temp.imagePath);
				temp.setCoordinate(loc);
				temp.setTitle(localName);
				temp.ivs = ivs;
				temp.setSubtitle(R.string.timeNotGiven);
				mMap.addAnnotation(temp);
				pokeMarkers.put(encounterid, temp);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			print("All-green?");
			ConcurrentHashMap<String, String> extras = new ConcurrentHashMap<>();
			extras.put("Pokedex Number", pokedexNumber + "");

			try {
				extras.put("Pokemon", PokemonIdOuterClass.PokemonId.valueOf(pokedexNumber).name());
			} catch (Throwable t) {
				t.printStackTrace();
				ErrorReporter.logExceptionThreaded(t);
			}
			ErrorReporter.logExceptionThreaded(e, extras);

			/*features.longMessage("Cannot find image for \"" + name + "\". Please alert the developer.");
			name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
			localName = localName.substring(0, 1).toUpperCase() + localName.substring(1).toLowerCase();
			if (hasTime) {
				MKPointAnnotation temp = new MKPointAnnotation();
				temp.setCoordinate(loc);
				temp.setTitle(localName);
				mMap.addAnnotation(temp);
				pokeMarkers.put(encounterid, temp); }
			else {
				MKPointAnnotation temp = new MKPointAnnotation();
				temp.setCoordinate(loc);
				temp.setTitle(localName);
				temp.setSubtitle(R.string.timeNotGiven);
				mMap.addAnnotation(temp);
				pokeMarkers.put(encounterid, temp);
			}*/
		}

		Stopwatch.click("showPokemonAt");
	}

	/*private CLLocationCoordinate2D[] getBoundingBox(final double lat, final double lon, final int distanceInMeters) {

		CLLocationCoordinate2D[] points = new CLLocationCoordinate2D[MapHelper.NUM_SCAN_SECTORS];

		final double latRadian = Math.toRadians(lat);

		final double metersPerLatDegree = 110574.235;
		final double metersPerLonDegree = 110572.833 * Math.cos(latRadian);
		final double deltaLat = distanceInMeters / metersPerLatDegree;
		final double deltaLong = distanceInMeters / metersPerLonDegree;

		final double minLat = lat - deltaLat;
		final double minLong = lon - deltaLong;
		final double maxLat = lat + deltaLat;
		final double maxLong = lon + deltaLong;

		final double deltaLatDiagonal = Math.sin(Math.toRadians(45)) * deltaLat;
		final double deltaLongDiagonal = Math.cos(Math.toRadians(45)) * deltaLong;

		final double minDiagonalLat = lat - deltaLatDiagonal;
		final double minDiagonalLong = lon - deltaLongDiagonal;
		final double maxDiagonalLat = lat + deltaLatDiagonal;
		final double maxDiagonalLong = lon + deltaLongDiagonal;

		points[0] = new CLLocationCoordinate2D(lat, lon);
		points[1] = new CLLocationCoordinate2D(minDiagonalLat, minDiagonalLong);
		points[2] = new CLLocationCoordinate2D(lat, minLong);
		points[3] = new CLLocationCoordinate2D(maxDiagonalLat, minDiagonalLong);
		points[4] = new CLLocationCoordinate2D(maxLat, lon);
		points[5] = new CLLocationCoordinate2D(maxDiagonalLat, maxDiagonalLong);
		points[6] = new CLLocationCoordinate2D(lat, maxLong);
		points[7] = new CLLocationCoordinate2D(minDiagonalLat, maxDiagonalLong);
		points[8] = new CLLocationCoordinate2D(minLat, lon);

		return points;
	}*/

	private void print(String message) {
		features.print("PokeFinder", message);
	}

	private Vector2[] getSearchPoints(int radius) {
		final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)* MapHelper.MAX_SCAN_RADIUS);
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

	private Vector2[] getHexSearchPoints(int radius) {
		isHexMode = true;

		final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MAX_SCAN_RADIUS);
		final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
		final float ITERATIONS = MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

		NUM_SCAN_SECTORS = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);

		Vector2 startPoint;
		startPoint = Vector2.Zero;

		int direction = 0; // 0 = upright, 1 = downright, 2 = down, 3 = downleft, 4 = upleft, 5 = up
		ArrayList<Vector2> points = new ArrayList<Vector2>();
		points.add(startPoint);
		int numMoves = 0;

		Vector2 currentPoint = new Vector2(startPoint);

		print("Distance between hexes = " + HEX_DISTANCE);
		print("Num scan sectors = " + NUM_SCAN_SECTORS);
		print("Start point = " + startPoint.toString());

		if (ITERATIONS == 1) {
			Vector2[] pointsArray = new Vector2[points.size()];
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

		Vector2[] pointsArray = new Vector2[points.size()];
		points.toArray(pointsArray);
		return pointsArray;
	}

	public Vector2 move(int times, int direction, Vector2 currentPoint, ArrayList<Vector2> points) {
		final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MAX_SCAN_RADIUS);
		final float Y_OFFSET = (float) (Math.sqrt(3)*MAX_SCAN_RADIUS*Math.sin(Math.toRadians(30)));
		final float X_OFFSET = (float) (Math.sqrt(3)*MAX_SCAN_RADIUS*Math.cos(Math.toRadians(30)));

		final float Y_NEG = (-1)*Y_OFFSET;
		final float X_NEG = (-1)*X_OFFSET;

		for (int n = 0; n < times; n++) {
			currentPoint = new Vector2(currentPoint);

			switch(direction) {

				case 0:
					print("Upright");
					currentPoint.x += X_OFFSET;
					currentPoint.y += Y_OFFSET;
					break;

				case 1:
					print("Downright");
					currentPoint.x += X_OFFSET;
					currentPoint.y -= Y_OFFSET;
					break;

				case 2:
					print("Down");
					currentPoint.y -= HEX_DISTANCE;
					break;


				case 3:
					print("Downleft");
					currentPoint.x -= X_OFFSET;
					currentPoint.y -= Y_OFFSET;
					break;

				case 4:
					print("Upleft");
					currentPoint.x -= X_OFFSET;
					currentPoint.y += Y_OFFSET;
					break;

				case 5:
					print("Up");
					currentPoint.y += HEX_DISTANCE;
					break;
			}

			print("Current point = " + currentPoint.toString() + "\n");
			points.add(currentPoint);
		}

		return currentPoint;
	}

	private Vector2[] getSquareSearchPoints(int radius) {
		isHexMode = false;

		final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MAX_SCAN_RADIUS * 2, 2) / 2);
		final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
		NUM_SCAN_SECTORS = BOXES_PER_ROW * BOXES_PER_ROW;

		boolean isOdd = BOXES_PER_ROW / 2 * 2 == BOXES_PER_ROW ? false : true;

		Vector2 startPoint;
		if (isOdd) startPoint = Vector2.Zero;
		else {
			float offset = MAX_SCAN_RADIUS * (float) Math.sin(Math.toRadians(45));
			startPoint = new Vector2((-1) * offset, offset);
		}

		int direction = 0; // 0 = right, 1 = down, 2 = left, 3 = up
		ArrayList<Vector2> points = new ArrayList<Vector2>();
		points.add(startPoint);
		int numMoves = 0;

		Vector2 currentPoint = new Vector2(startPoint);

		print("Mini square radius = " + MINI_SQUARE_SIZE);
		print("Num scan sectors = " + NUM_SCAN_SECTORS);
		print("Start point = " + startPoint.toString());

		for (int n = 1; n < NUM_SCAN_SECTORS; n++) {
			currentPoint = new Vector2(currentPoint);
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
					currentPoint.x += MINI_SQUARE_SIZE;
					break;
				case 1:
					print("Down " + numMoves);
					currentPoint.y -= MINI_SQUARE_SIZE;
					break;
				case 2:
					print("Left " + numMoves);
					currentPoint.x -= MINI_SQUARE_SIZE;
					break;
				case 3:
					print("Top " + numMoves);
					currentPoint.y += MINI_SQUARE_SIZE;
					break;
			}

			print("Current point = " + currentPoint.toString() + "\n");
			points.add(currentPoint);
		}

		Vector2[] pointsArray = new Vector2[points.size()];
		points.toArray(pointsArray);
		return pointsArray;
	}

	private CLLocationCoordinate2D cartesianToCoord(Vector2 point, CLLocationCoordinate2D center) {
		final double latRadian = Math.toRadians(center.latitude());

		final double metersPerLatDegree = 110574.235;
		final double metersPerLonDegree = 110572.833 * Math.cos(latRadian);
		final double deltaLat = point.y / metersPerLatDegree;
		final double deltaLong = point.x / metersPerLonDegree;

		CLLocationCoordinate2D loc = new CLLocationCoordinate2D(center.latitude() + deltaLat, center.longitude() + deltaLong);
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
						try {
							ArrayList<Long> removables = new ArrayList<Long>();
							for (WildPokemonTime poke : pokeTimes.values()) {
								long timeLeftMs = poke.getDespawnTimeMs() - System.currentTimeMillis();
								if (timeLeftMs < 0) {
									mMap.removeAnnotation(pokeMarkers.remove(poke.getPoke().getEncounterId()));
									removables.add(poke.getPoke().getEncounterId());
								} else {
									MKPointAnnotation marker = pokeMarkers.get(poke.getPoke().getEncounterId());
									if (marker != null) {
										marker.setSubtitle("Leaves in " + getTimeString(timeLeftMs / 1000 + 1));
										if (marker instanceof ImageAnnotation) {
											try {
												ImageAnnotation image = (ImageAnnotation) marker;
												if (image.callout != null) {
													// TODO Add fields for moves and heightWeight
													image.callout.setText(image.ivs + "\n" + (image.moves.equals("") ? "" : image.moves + "\n") + (image.heightWeight.equals("") ? "" : image.heightWeight + "\n") + image.subtitle() + "  ");
													image.callout.setNumberOfLines(0);
													image.callout.setNeedsLayout();
												}
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
									}
								}
							}
							for (Long id : removables) {
								pokeTimes.remove(id);
							}

							// Now do the ones that don't have times. Count upwards instead
							removables.clear();
							for (WildPokemonTime poke : noTimes.values()) {
								long timeElapsed = System.currentTimeMillis() - poke.getDespawnTimeMs();
								if (timeElapsed > 1800000) {
									mMap.removeAnnotation(pokeMarkers.remove(poke.getPoke().getEncounterId()));
									removables.add(poke.getPoke().getEncounterId());
								} else {
									MKPointAnnotation marker = pokeMarkers.get(poke.getPoke().getEncounterId());
									if (marker != null) {
										marker.setSubtitle("Seen " + getTimeString(timeElapsed / 1000) + " ago");
										if (marker instanceof ImageAnnotation) {
											try {
												ImageAnnotation image = (ImageAnnotation) marker;
												if (image.callout != null) {
													image.callout.setText(image.ivs + "\n" + (image.moves.equals("") ? "" : image.moves + "\n") + (image.heightWeight.equals("") ? "" : image.heightWeight + "\n") + image.subtitle() + "  ");
													image.callout.setNumberOfLines(0);
													image.callout.setNeedsLayout();
												}
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
									}
								}
							}
							for (Long id : removables) {
								noTimes.remove(id);
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
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

	public void setScanProgress(int progress) {
		MapController.instance.scanBar.setProgress(((float) progress) / scanProgressMax);
	}

	public boolean updateScanSettings() {
		boolean distanceFailed = false, timeFailed = false;
		NativePreferences.lock("update scan settings");
		maxScanDistance = NativePreferences.getFloat(PREF_MAX_SCAN_DISTANCE, 70);
		minScanTime = NativePreferences.getFloat(PREF_MIN_SCAN_TIME, 10);

		if (!MapController.mapHelper.promptForApiKey()) return false;

		try {
			maxScanDistance = MapController.features.getVisibleScanDistance();
			if (maxScanDistance <= 0) throw new RequestFailedException("Unable to get scan distance from server");
			NativePreferences.putFloat(PREF_MAX_SCAN_DISTANCE, (float) maxScanDistance);
			features.print("PokeFinder", "Server says max visible scan distance is " + maxScanDistance);
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof RequestFailedException) distanceFailed = true;
			maxScanDistance = NativePreferences.getFloat(PREF_MAX_SCAN_DISTANCE, 70);
			if (maxScanDistance <= 0) maxScanDistance = 70;
		}

		MAX_SCAN_RADIUS = (int) maxScanDistance;

		try {
			minScanTime = MapController.features.getMinScanRefresh();
			if (minScanTime <= 0) throw new RequestFailedException("Unable to get scan delay from server");
			NativePreferences.putFloat(PREF_MIN_SCAN_TIME, (float) minScanTime);
			features.print("PokeFinder", "Server says min scan refresh is " + minScanTime);
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof RequestFailedException) timeFailed = true;
			minScanTime = NativePreferences.getFloat(PREF_MIN_SCAN_TIME, 5);
			if (minScanTime <= 0) minScanTime = 5;
		}

		NativePreferences.unlock();

		int squareDist = (int) Math.sqrt(Math.pow(MAX_SCAN_RADIUS * 2, 2) / 2);
		int hexDist = (int) (Math.sqrt(3)* MapHelper.MAX_SCAN_RADIUS);
		int distancePerScan = Math.max(squareDist, hexDist);

		int speed = (int) Math.ceil(distancePerScan / minScanTime);
		maxScanSpeed = Math.min(SPEED_CAP, speed);

		return !distanceFailed && !timeFailed;
	}

	@Override
	public void saveSpawns() {
		if (spawns.isEmpty() || !spawnsLoaded) return;
		/*for (Spawn spawn : spawns.values()) {
			print("Spawn ID: " + spawn.id);
			print("Location: " + spawn.loc());
			print("History:");
			for (Integer num : spawn.history) {
				print(getLocalName(num));
			}
		}*/

		ArrayList<Spawn> spawnList = new ArrayList<Spawn>(MapController.mapHelper.spawns.values());
		//print("SpawnList: " + spawnList.toString());

		NativePreferences.lock("save spawns");
		try {
			Type type = new TypeToken<List<Spawn>>(){}.getType();
			Gson gson = new Gson();
			String text = gson.toJson(spawnList, type);

			NativePreferences.putString("SpawnStorage", text);
			//print("Saved spawns: " + spawns.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		NativePreferences.unlock();
	}

	public void testSpawnStorage() {
		NativePreferences.lock("test spawn storage");
		try {

			Type type = new TypeToken<List<Spawn>>(){}.getType();
			ArrayList<Spawn> spawnList = new ArrayList<>(spawns.values());
			spawns.clear();
			Gson gson = new Gson();
			String text = gson.toJson(spawnList, type);

			NativePreferences.putString("SpawnStorage", text);

			String newText = NativePreferences.getString("SpawnStorage", "");
			spawnList.clear();
			spawnList = gson.fromJson(newText, type);

			//print("Spawn storage saved and loaded successfully:\n\n" + newText);
			for (Spawn spawn : spawnList) {
				/*print("Spawn ID: " + spawn.id);
				print("Location: " + spawn.loc());
				print("History:");
				for (Integer num : spawn.history) {
					print(getLocalName(num));
				}*/
				spawns.put(spawn.id, spawn);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		NativePreferences.unlock();
	}

	@Override
	public void loadSpawns() {
		if (scanning || spawnsLoaded) return;

		ArrayList<Spawn> spawnList;
		NativePreferences.lock("load spawns");
		try {

			Type type = new TypeToken<List<Spawn>>(){}.getType();
			spawns.clear();
			Gson gson = new Gson();
			String newText = NativePreferences.getString("SpawnStorage", "");
			spawnList = gson.fromJson(newText, type);
			if (spawnList == null) {
				NativePreferences.unlock();
				spawnsLoaded = true;
				return;
			}

			for (int n = 0; n < spawnList.size(); n++) {
				Spawn spawn = spawnList.get(n);
				spawns.put(spawn.id, spawn);
			}

			spawnsLoaded = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		NativePreferences.unlock();

		try {
			print("Loaded " + spawns.size() + " spawns!");
		} catch (Exception e) {
			e.printStackTrace();
		}

		showSpawnsOnMap();
	}

	public void showSpawnsOnMap() {
		if (loadedSpawns || !showSpawns) return;

		loadedSpawns = true;
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					mapSpawns.clear();
					for (Spawn spawn : spawns.values()) {
						showSpawnOnMap(spawn);
					}
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
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
				try {
					int size = mapSpawns.size();
					for (int n = 0; n < size; n++) {
						ImageAnnotation spawn = mapSpawns.get(n);
						mMap.removeAnnotation(spawn);
					}
					mapSpawns.clear();
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public ImageAnnotation showSpawnOnMap(Spawn spawn) {
		ImageAnnotation spawnPoint = ImageAnnotation.alloc().init("spawn_icon.png");
		spawnPoint.setCoordinate(new CLLocationCoordinate2D(spawn.lat, spawn.lon));
		spawnPoint.setTitle(spawn.nickname);
		mMap.addAnnotation(spawnPoint);
		mapSpawns.add(spawnPoint);
		return spawnPoint;
	}

	public static void writeStringToFile(String string, String filepath) {
		BufferedWriter writer;
		try {
			// Delete the file if it exists
			FileHandle file = Gdx.files.local(filepath);
			file.delete();

			// Create a new file and write the String straight to it
			file.file().createNewFile();
			writer = new BufferedWriter(new FileWriter(file.file().getAbsolutePath()));
			writer.write(string);

			// Clean up
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getFileAsString(String filepath) {
		try {
			FileHandle file = Gdx.files.local(filepath);
			String value = "";
			if (file.exists()) {
				BufferedReader reader = new BufferedReader(file.reader());

				// Read the file line by line and add each line to the string
				String line;
				while((line = reader.readLine()) != null) {
					value += line + "\n";
				}

				// Get rid of the last newline
				value = value.substring(0, value.length() - 1);
				reader.close();
			}
			return value;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public void deleteAllSpawns() {
		NativePreferences.lock("delete all spawns");
		NativePreferences.remove("SpawnStorage");
		NativePreferences.unlock();
		spawns.clear();
		hideSpawnsOnMap();
	}

	@Override
	public void wideSpawnScan(boolean background) {
		if (!MapController.mapHelper.promptForApiKey()) return;
		if (scanning) return;

		ConcurrentHashMap<String, Spawn> temp = new ConcurrentHashMap<>();
		CLLocation currentPoint = CLLocation.alloc().initWithLatitudeLongitude(currentLat, currentLon);

		for (Spawn spawn : spawns.values()) {
			print("Trying lat: " + spawn.lat + " lon: " + spawn.lon);
            /*if (left <= spawn.lon && right >= spawn.lon && bottom <= spawn.lat && top >= spawn.lat) {
                temp.put(spawn.id, spawn);
            }*/
			CLLocation spawnPoint = CLLocation.alloc().initWithLatitudeLongitude(spawn.lat, spawn.lon);
			double meters = currentPoint.distanceFromLocation(spawnPoint);
			if (meters <= scanDistance) temp.put(spawn.id, spawn);
		}

		if (!temp.isEmpty()) {
			spawnScan(temp, background);
		}
		else {
			if (!background) features.shortMessage("You have to find spawn points in this area with regular scanning before you can do a spawn scan.");
		}
	}

	@Override
	public void refreshPrefs() {
		NativePreferences.lock("refresh prefs");

		collectSpawns = NativePreferences.getBoolean(PREF_COLLECT_SPAWNS, true);
		showIvs = NativePreferences.getBoolean(PREF_SHOW_IVS, true);
		showSpawns = NativePreferences.getBoolean(PREF_SHOW_SPAWNS, true);
		captchaModePopup = NativePreferences.getBoolean(PREF_CAPTCHA_MODE_POPUP, false);
		minAttack = NativePreferences.getInteger(PREF_MIN_ATTACK, 0);
		minDefense = NativePreferences.getInteger(PREF_MIN_DEFENSE, 0);
		minStamina = NativePreferences.getInteger(PREF_MIN_STAMINA, 0);
		minPercent = NativePreferences.getInteger(PREF_MIN_PERCENT, 0);
		minOverride = NativePreferences.getInteger(PREF_MIN_OVERRIDE, 100);
		imageSize = NativePreferences.getLong(PREF_IMAGE_SIZE, 2);
		showScanDetails = NativePreferences.getBoolean(PREF_SHOW_SCAN_DETAILS, false);
		Features.NUM_POKEMON = NativePreferences.getInteger(PREF_NUM_POKEMON, 251);
		ivsAlwaysVisible = NativePreferences.getBoolean(PREF_IVS_ALWAYS_VISIBLE, true);
		defaultMarkersMode = NativePreferences.getLong(PREF_DEFAULT_MARKERS_MODE, 0);
		overrideEnabled = NativePreferences.getBoolean(PREF_OVERRIDE_ENABLED, minOverride != 100);
		clearMapOnScan = NativePreferences.getBoolean(PREF_CLEAR_MAP_ON_SCAN, false);
		gpsModeNormal = NativePreferences.getBoolean(PREF_GPS_MODE_NORMAL, true);
		use2Captcha = NativePreferences.getBoolean(PREF_USE_2CAPTCHA, false);
		//useNewApi = NativePreferences.getBoolean(PREF_USE_NEW_API, false);
		fallbackApi = NativePreferences.getBoolean(PREF_FALLBACK_API, true);
		captchaKey = NativePreferences.getString(PREF_2CAPTCHA_KEY, "");
		newApiKey = NativePreferences.getString(PREF_NEW_API_KEY, "");

		backgroundScanning = NativePreferences.getBoolean(PREF_BACKGROUND_SCANNING, false);
		backgroundInterval = NativePreferences.getString(PREF_BACKGROUND_INTERVAL, "15");
		backgroundIncludeNearby = NativePreferences.getBoolean(PREF_BACKGROUND_INCLUDE_NEARBY, true);
		captchaNotifications = NativePreferences.getBoolean(PREF_CAPTCHA_NOTIFICATIONS, true);
		backgroundScanIvs = NativePreferences.getBoolean(PREF_BACKGROUND_SCAN_IVS, true);
		backgroundNotificationSound = NativePreferences.getBoolean(PREF_BACKGROUND_NOTIFICATION_SOUND, true);
		showMovesets = NativePreferences.getBoolean(PREF_SHOW_MOVESETS, true);
		showHeightWeight = NativePreferences.getBoolean(PREF_SHOW_HEIGHT_WEIGHT, false);
		onlyScanSpawns = NativePreferences.getBoolean(PREF_ONLY_SCAN_SPAWNS, false);

		//getCacheIDThreaded();
		useNewApi = true;

		refreshNewApiLabels();

		NativePreferences.unlock();

		//Signature.fallbackApi = fallbackApi;
		Signature.fallbackApi = false;
	}

	public void refreshNewApiLabels() {
		if (useNewApi) {
			MapController.instance.showRpmLabel();
			startRpmTimer();
		}
	}

	public void spawnScan(final ConcurrentHashMap<String, Spawn> searchSpawns, final boolean background) {
		if (!promptForApiKey()) return;
		if (searchSpawns.isEmpty()) return;

		final ArrayList<Account> goodAccounts = AccountManager.getGoodAccounts();
		if (goodAccounts.size() == 0) {
				if (!background) features.longMessage("You don't have any valid accounts!");
				else {
					sendNotification("PokeSensor doesn't have any valid scanning accounts!", "You may have to fill out some captchas or handle some other errors.");
					MapController.instance.lastScanTime = System.currentTimeMillis();
				}
			return;
		}

		if (scanning) return;
		else scanning = true;
		searched = true;
		removeTempScanCircle();

		newSpawns = 0;
		currentSector = 0;

		final ArrayList<Spawn> spawnList = new ArrayList<>(searchSpawns.values());

		searchedSpawns.clear();
		features.captchaScreenVisible = false;

		updateScanSettings();
		abortScan = false;
		if (scanDistance > MAX_SCAN_DISTANCE) scanDistance = MAX_SCAN_DISTANCE;

		NUM_SCAN_SECTORS = searchSpawns.size();

		Runnable main = new Runnable() {
			@Override
			public void run() {
				if (clearMapOnScan && !background) {
					try {
						//final ArrayList<Long> ids = new ArrayList<Long>(noTimes.keys().);
						Map<Long, WildPokemonTime> temp = noTimes;

						for (Long id : temp.keySet()) {
							try {
								features.print(TAG, "Removed poke marker!");
								MKPointAnnotation marker = pokeMarkers.remove(id);
								if (marker != null) mMap.removeAnnotation(marker);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					noTimes.clear();

					try {
						//final ArrayList<Long> ids = new ArrayList<Long>(noTimes.keys().);
						Map<Long, WildPokemonTime> temp = pokeTimes;

						for (Long id : temp.keySet()) {
							try {
								features.print(TAG, "Removed poke marker!");
								MKPointAnnotation marker = pokeMarkers.remove(id);
								if (marker != null) mMap.removeAnnotation(marker);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					pokeTimes.clear();
				}

				removeScanPoints();
				removeScanPointCircles();

				MapController.instance.scanBar.setProgress(0);

				MapController.instance.scanView.setHidden(false);

				final Thread scanThread = new Thread() {
					public void run() {
						Runnable circleRunnable = new Runnable() {
							@Override
							public void run() {
								if (scanCircle != null) mMap.removeOverlay(scanCircle);
								scanCircle = CustomCircle.alloc().initWithCenterCoordinateRadius(new CLLocationCoordinate2D(currentLat, currentLon), scanDistance);
								scanCircle.strokeColor = UIColor.blueColor();
								mMap.addOverlay(scanCircle);
							}
						};
						features.runOnMainThread(circleRunnable);

						features.print(TAG, "Scan distance: " + scanDistance);

						totalNearbyPokemon.clear();
						totalEncounters.clear();
						totalWildEncounters.clear();

						nearbyBackgroundPokemon.clear();
						wildBackgroundPokemon.clear();

						long SCAN_INTERVAL = (long) MapHelper.minScanTime * 1000;

						features.print(TAG, "Scan interval: " + SCAN_INTERVAL);

						scanProgressMax = searchSpawns.size();

						scanPointCircles.clear();
						scanPoints.clear();

						features.resetMapObjects();

						int scansPerWorker = searchSpawns.size() / goodAccounts.size();
						int extraScans = searchSpawns.size() - scansPerWorker * goodAccounts.size();
						int cursor = 0;

						int workersPerThread = goodAccounts.size() / MAX_POOL_THREADS;
						int extraWorkers = goodAccounts.size() - workersPerThread * MAX_POOL_THREADS;
						int workerCursor = 0;

						ArrayList<Future> scanThreads = new ArrayList<>();

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
								AccountScanner scanner = new AccountScanner(account);
								workers.add(scanner);
							}

							workerList.add(workers);
						}

						//while (cursor < boxPoints.length) {
						for (int n = 0; n < workersPerThread + 1; n++) {
							for (ArrayList<AccountScanner> scanAccounts : workerList) {
								if (n >= scanAccounts.size()) continue;
								AccountScanner scanner = scanAccounts.get(n);

								if (searchSpawns.isEmpty()) break;
								Spawn mySpawn = new ArrayList<Spawn>(searchSpawns.values()).get(0);
								claimSpawn(mySpawn.id, searchSpawns);
								scanner.startSpawn = mySpawn;
							}
						}
						//}

						// Pass a list of CLLocations for spawns so we don't keep allocating and releasing them in the main scan loop
						ConcurrentHashMap<String, CLLocation> searchSpawnLocations = new ConcurrentHashMap<>();
						for (Spawn spawn : searchSpawns.values()) {
							searchSpawnLocations.put(spawn.id, CLLocation.alloc().initWithLatitudeLongitude(spawn.lat, spawn.lon));
						}

						sleepingThreads.clear();

						for (ArrayList<AccountScanner> scanAccounts : workerList) {
							boolean valid = false;
							for (AccountScanner scanner : scanAccounts) {
								if (scanner.startSpawn != null) {
									valid = true;
									break;
								}
							}
							if (valid) scanThreads.add(accountScanSpawn(scanAccounts, SCAN_INTERVAL, searchSpawns, background, searchSpawnLocations));
						}

						/*for (int n = 0; n < MAX_POOL_THREADS; n++) {
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
						}*/

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
								if (!background) features.longMessage(R.string.abortScan);
								scanning = false;

								break;
							}
							if (searchSpawns.isEmpty()) {
								boolean done = true;
								for (boolean sleeping : sleepingThreads.values()) {
									if (!sleeping) {
										print("Spawns are all claimed but someone is still scanning...");
										done = false;
										break;
									}
								}

								for (ArrayList<AccountScanner> scanAccounts : workerList) {
									for (AccountScanner scanner : scanAccounts) {
										if (scanner.startSpawn != null) {
											done = false;
											break;
										}
									}
									if (!done) break;
								}

								if (done) {
									print("Spawns are all claimed and everyone is either done or sleeping. Finishing scan now.");
									for (Future thread : scanThreads) {
										thread.cancel(true);
									}
									break;
								}
							}
						}

						Runnable dismissRunnable = new Runnable() {
							@Override
							public void run() {
								removeScanPoints();
								if (!showScanDetails) {
									removeScanPointCircles();
								}

								MapController.instance.scanView.setHidden(true);
							}
						};
						features.runOnMainThread(dismissRunnable);

						if (collectSpawns && !background) {
							if (newSpawns > 1)
								features.shortMessage("Found " + newSpawns + " new spawn points and added them to My Spawns!");
							else if (newSpawns == 1)
								features.shortMessage("Found 1 new spawn point and added it to My Spawns!");
						}
						scanning = false;

						if (background) {
							backgroundCaptchasLost = 0;
							for (int n = 0; n < goodAccounts.size(); n++) {
								if (goodAccounts.get(n).getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
									backgroundCaptchasLost++;
								}
							}

							if (backgroundCaptchasLost > 0) {
								if (captchaNotifications)
									sendNotification(backgroundCaptchasLost + " accounts got captcha'd in the last scan", "You now have " + AccountManager.getGoodAccounts().size() + " good accounts.");
							}

							if (backgroundIncludeNearby) {
								// Get rid of the duplicates that are already on the map
								Map<Long, String> temp = wildBackgroundPokemon;
								for (Long encounterId : temp.keySet()) {
									nearbyBackgroundPokemon.remove(encounterId);
								}

								ArrayList<String> nearbyPokes = new ArrayList<>();

								temp = nearbyBackgroundPokemon;
								for (Long encounterId : temp.keySet()) {
									String name = nearbyBackgroundPokemon.get(encounterId);

									if (!notifiedNearbies.contains(encounterId)) {
										notifiedNearbies.add(encounterId);
										nearbyPokes.add(name);
									}
								}

								if (!nearbyPokes.isEmpty()) sendNearbyNotification(nearbyPokes);
								else
									print("No nearby background pokes that weren't already included in wilds");
							}

							// Report on the Pokemon we found
							ArrayList<String> wildPokes = new ArrayList<>();
							ArrayList<String> wildPokeIvs = new ArrayList<>();

							Map<Long, String> temp = wildBackgroundPokemon;
							for (Long encounterId : temp.keySet()) {
								String name = wildBackgroundPokemon.get(encounterId);
								String ivs = wildBackgroundPokemonIvs.get(encounterId);

								if (!notifiedWilds.contains(encounterId)) {
									notifiedWilds.add(encounterId);
									wildPokes.add(name);
									wildPokeIvs.add(ivs);
								}
							}

							if (!wildBackgroundPokemon.isEmpty())
								sendCatchableNotification(wildPokes, wildPokeIvs);

							MapController.instance.lastScanTime = System.currentTimeMillis();
						}
					}
				};

				MapController.instance.scanView.addTargetActionForControlEvents(MapController.instance, new SEL("abortSpawnScan:"), UIControlEvents.TouchUpInside);

				spawnScanThread = scanThread;

				scanThread.start();
			}
		};

		features.runOnMainThread(main);
	}

	void abortSpawnScan(ClickableView sender) {
		print("Entered abortSpawnScan:");
		spawnScanThread.interrupt();
		abortScan = true;
		MapController.instance.scanView.setHidden(true);

		removeScanPoints();
		if (!showScanDetails) {
			removeScanPointCircles();
		}
	}

	public Future accountScanSpawn(final ArrayList<AccountScanner> scanners, final long SCAN_INTERVAL, final ConcurrentHashMap<String, Spawn> searchSpawns, final boolean background, final ConcurrentHashMap<String, CLLocation> searchSpawnLocations) {
		for (AccountScanner scanner : scanners) {
			if (scanner.startSpawn != null) scanner.account.setScanning(true);
			else scanner.account.setScanning(false);
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

					Collections.sort(scanners, new Comparator<AccountScanner>() {
						@Override
						public int compare(AccountScanner o1, AccountScanner o2) {
							return Long.compare(o1.account.nextScanTime, o2.account.nextScanTime);
						}
					});

					try {
						sleepingThreads.put(this, true);
						if (first) {
							Thread.sleep(1000);
						}
						else {
							print("Taking a 1 second break to make sure UI doesn't lag");
							Thread.sleep(1000);
							for (AccountScanner scanner : scanners) {
								if (readyToSpawnScan(scanner.account, SCAN_INTERVAL + 100)) break;
								// Assume the first scanning account in the list is the one that will be ready fastest
								if (scanner.account.isScanning() && !readyToSpawnScan(scanner.account, SCAN_INTERVAL + 100)) {
									long waitTime = Math.max(scanner.account.lastScanTime + SCAN_INTERVAL + 100, scanner.account.nextScanTime) - System.currentTimeMillis();
									print(scanner.account.getUsername() + " needs to wait " + waitTime + " ms before it can scan again. Waiting...");
									if (waitTime > 0) Thread.sleep(waitTime);
									print(scanner.account.getUsername() + " has waited " + waitTime + " ms and is ready to scan again.");
									break;
								}
							}
						}
						sleepingThreads.put(this, false);
					} catch (InterruptedException e) {
						if (abortScan) {
							for (AccountScanner scanner : scanners) {
								scanner.account.setScanning(false);
							}
							return;
						}
					}

					for (final AccountScanner scanner : scanners) {
						if (!scanner.account.isScanning() || !readyToSpawnScan(scanner.account, SCAN_INTERVAL + 100)) continue;

						//String id = scanner.startSpawn.id;
						if (scanner.repeat) {
							if (showScanDetails && scanner.account.circle != null) {
								Runnable runnable = new Runnable() {
									@Override
									public void run() {
										if (scanner.account.circle != null) {
											mMap.removeOverlay(scanner.account.circle);
											scanPointCirclesDetailed.remove(scanner.account);
										}
									}
								};
								features.runOnMainThread(runnable);
							}
							scanner.failedSectors--;
							currentSector--;
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
							scanner.location = CLLocation.alloc().initWithLatitudeLongitude(scanner.startSpawn.lat, scanner.startSpawn.lon);
						} else if (scanner.repeat) {
							scanner.location = CLLocation.alloc().initWithLatitudeLongitude(scanner.repeatSpawn.lat, scanner.repeatSpawn.lon);
						} else {
							Hashtable<String, Integer> times = new Hashtable<>();

							final CLLocationCoordinate2D loc = scanner.location.coordinate();
							CLLocation here = CLLocation.alloc().initWithLatitudeLongitude(loc.latitude(), loc.longitude());
							ArrayList<Spawn> spawnList = new ArrayList<Spawn>(searchSpawns.values());
							CLLocation spawnPoint;
							for (Spawn spawn : spawnList) {
								Stopwatch.click("Measuring time to spawns");
								spawnPoint = searchSpawnLocations.get(spawn.id);
								int commuteTime = 0;
								double commuteDistance = 0;
								commuteDistance = here.distanceFromLocation(spawnPoint);
								commuteTime = (int) Math.ceil(commuteDistance / scanSpeed);
								features.print(TAG, scanner.account.getUsername() + " will take " + commuteTime + "s to get to " + spawn.id);
								times.put(spawn.id, Math.max(commuteTime, (int) SCAN_INTERVAL / 1000));
								Stopwatch.click("Measuring time to spawns");
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

							//scanner.timeWaited++;
							//minTime -= scanner.timeWaited;
							scanner.account.nextScanTime = scanner.account.lastScanTime + minTime*1000 + 100;
							features.print(TAG, scanner.account.getUsername() + " wants to claim " + myId + " and has to wait for " + minTime + "s");

							if (scanner.account.nextScanTime > System.currentTimeMillis()) continue;

							if (searchSpawns.containsKey(myId)) {
								mySpawn = claimSpawn(myId, searchSpawns);
								features.print(TAG, scanner.account.getUsername() + " is trying now to claim " + myId);
							}

							if (mySpawn == null) continue;

							scanner.timeWaited = 0;

							features.print(TAG, scanner.account.getUsername() + " claimed " + mySpawn.id);

							scanner.repeatSpawn = mySpawn;
							scanner.location = CLLocation.alloc().initWithLatitudeLongitude(mySpawn.lat, mySpawn.lon);
						}

						scanner.repeat = false;
						final CLLocationCoordinate2D loc = scanner.location.coordinate();

						try {
							Runnable progressRunnable = new Runnable() {
								@Override
								public void run() {
									Stopwatch.click("Progress runnable");
									updateScanLayout();
									updateScanPoint(loc, scanner.account);
									Stopwatch.click("Progress runnable");
								}
							};
							features.runOnMainThread(progressRunnable);
						} catch (Exception e) {
							e.printStackTrace();
						}

						if (!background) scanner.repeat = !scanForPokemon(scanner, loc.latitude(), loc.longitude());
						else scanner.repeat = !scanForPokemonBackground(scanner, loc.latitude(), loc.longitude());

						if (!scanner.repeat && first) {
							scanner.startSpawn = null;
						}

						if (!background) {
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
						} else {
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
						}

						if (scanner.account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED || scanner.account.scanErrorCount >= MAX_SCAN_ERROR_COUNT) {
							print(scanner.account.getUsername() + " is aborting.");
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

				sleepingThreads.put(this, true);
			}
		};

		sleepingThreads.put(scanThread, false);

		return run(scanThread);
	}

	public synchronized Spawn claimSpawn(String id, final ConcurrentHashMap<String, Spawn> searchSpawns) {
		Stopwatch.click("claimSpawn");
		if (searchSpawns.containsKey(id)) {
			Spawn claimedSpawn = searchSpawns.remove(id);
			// Claim it and remove redundant spawns from the scan
			final CLLocationCoordinate2D loc = claimedSpawn.loc();

			features.print(TAG, "After claiming " + id + ", filtering out spawns in proximity...");
			CLLocation here = CLLocation.alloc().initWithLatitudeLongitude(loc.latitude(), loc.longitude());
			ArrayList<Spawn> spawnList = new ArrayList<Spawn>(searchSpawns.values());
			for (Spawn spawn : spawnList) {
				CLLocation spawnPoint = CLLocation.alloc().initWithLatitudeLongitude(spawn.lat, spawn.lon);
				double meters = here.distanceFromLocation(spawnPoint);
				if (meters <= MAX_SCAN_RADIUS) {
					features.print(TAG, "Filtered out " + spawn.id);
					searchSpawns.remove(spawn.id);
					markSpawnAsSearched(spawn.id);
					currentSector++;
				}
			}
			Stopwatch.click("claimSpawn");
			return claimedSpawn;
		}
		else {
			Stopwatch.click("claimSpawn");
			return null;
		}
	}

	public synchronized void markSpawnAsSearched(String id) {
		if (!searchedSpawns.contains(id)) searchedSpawns.add(id);
	}

	public String getLocalName(int pokedexNumber) {
		return PokeDictionary.getDisplayName(pokedexNumber, Locale.getDefault());
	}

	public void provokeCaptcha() {
		/*final ConcurrentHashMap<String, Spawn> mySpawnList = new ConcurrentHashMap<>();
		final ConcurrentHashMap<String, Spawn> mySpawnList2 = new ConcurrentHashMap<>();
		final Spawn spawn = new ArrayList<>(spawns.values()).get(0);
		final Spawn spawn2 = new ArrayList<>(spawns.values()).get(1);
		mySpawnList.put(spawn.id, spawn);
		mySpawnList2.put(spawn2.id, spawn2);

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

								if (features.checkForCaptcha(AccountManager.accounts.get(0))) {
									AudioServices.playSystemSound(AudioServices.SystemSoundVibrate);
									throw new RuntimeException("Got a captcha!");
								}
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
		}*/
	}

	public void saveIVFilters() {
		NativePreferences.lock("save IV filters");

		NativePreferences.putInteger(PREF_MIN_ATTACK, minAttack);
		NativePreferences.putInteger(PREF_MIN_DEFENSE, minDefense);
		NativePreferences.putInteger(PREF_MIN_STAMINA, minStamina);
		NativePreferences.putInteger(PREF_MIN_PERCENT, minPercent);
		NativePreferences.putInteger(PREF_MIN_OVERRIDE, minOverride);
		NativePreferences.putBoolean(PREF_OVERRIDE_ENABLED, overrideEnabled);

		NativePreferences.unlock();
	}

	public boolean imageExists(int pokedexNumber) {
		String filename = pokedexNumber + MapController.mapHelper.IMAGE_EXTENSION;
		FileHandle handle = Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER + filename);
		return handle.exists();
	}

	public synchronized void activateGen2() {
		// Need to extend the filter and the custom images
		if (Features.NUM_POKEMON > 151) return;

		NativePreferences.lock("activate gen 2");
		Features.NUM_POKEMON = 251;
		NativePreferences.putInteger(PREF_NUM_POKEMON, Features.NUM_POKEMON);
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
			MapController.features.print("PokeFinder", "Initializing a new thread pool");
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
		MapController.instance.updateRpmLabelText();
	}

	public synchronized void startRpmTimer() {
		if (rpmTimer != null) rpmTimer.cancel();
		rpmTimer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if (PokeHashProvider.rpmTimeLeft != -1 && !resetTimerRunning) {
					print("RateLimit is " + PokeHashProvider.rateLimit);
					startResetRpmTimer();
				} else if (resetTimerRunning) {
					MapController.instance.showRpmCountLabel();
					MapController.instance.updateRpmCountLabelText();
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
				MapController.instance.hideRpmCountLabel();
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

	// TODO Background scanning + notifications

	public void wideScanBackground() {
		final ArrayList<Account> goodAccounts = AccountManager.getGoodAccounts();
		if (goodAccounts.size() == 0) {
			sendNotification("PokeSensor doesn't have any valid scanning accounts!", "You may have to fill out some captchas or handle some other errors.");
			MapController.instance.lastScanTime = System.currentTimeMillis();
			return;
		}

		if (scanning) return;
		else scanning = true;
		searched = true;
		scanningBackground = true;

		removeTempScanCircle();

		newSpawns = 0;
		currentSector = 0;
		features.captchaScreenVisible = false;

		updateScanSettings();
		abortScan = false;
		if (scanDistance > MAX_SCAN_DISTANCE) scanDistance = MAX_SCAN_DISTANCE;

		Runnable main = new Runnable() {
			@Override
			public void run() {
				removeScanPoints();
				removeScanPointCircles();

				final Thread scanThread = new Thread() {
					public void run() {
						failedScanLogins = 0;

						Runnable circleRunnable = new Runnable() {
							@Override
							public void run() {
								if (scanCircle != null) mMap.removeOverlay(scanCircle);
								scanCircle = CustomCircle.alloc().initWithCenterCoordinateRadius(new CLLocationCoordinate2D(currentLat, currentLon), scanDistance);
								scanCircle.strokeColor = UIColor.blueColor();
								mMap.addOverlay(scanCircle);
							}
						};
						features.runOnMainThread(circleRunnable);

						features.print(TAG, "Scan distance: " + scanDistance);

						totalNearbyPokemon.clear();
						totalEncounters.clear();
						totalWildEncounters.clear();

						nearbyBackgroundPokemon.clear();
						wildBackgroundPokemon.clear();

						//Vector2[] boxPoints = getBackgroundSearchPoints(goodAccounts.size());
						Vector2[] boxPoints = getSearchPoints(scanDistance);

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

						CLLocationCoordinate2D center = new CLLocationCoordinate2D(currentLat, currentLon);

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
								AccountScanner scanner = new AccountScanner(account, new ArrayList<Vector2>());
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

						Runnable dismissRunnable = new Runnable() {
							@Override
							public void run() {
								removeScanPoints();
								if (!showScanDetails) {
									removeScanPointCircles();
								}
							}
						};
						features.runOnMainThread(dismissRunnable);

						scanning = false;

						backgroundCaptchasLost = 0;
						for (int n = 0; n < goodAccounts.size(); n++) {
							if (goodAccounts.get(n).getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
								backgroundCaptchasLost++;
							}
						}

						if (backgroundCaptchasLost > 0) {
							if (captchaNotifications) sendNotification(backgroundCaptchasLost + " accounts got captcha'd in the last scan", "You now have " + AccountManager.getGoodAccounts().size() + " good accounts.");
						}

						if (backgroundIncludeNearby) {
							// Get rid of the duplicates that are already on the map
							Map<Long, String> temp = wildBackgroundPokemon;
							for (Long encounterId : temp.keySet()) {
								nearbyBackgroundPokemon.remove(encounterId);
							}

							ArrayList<String> nearbyPokes = new ArrayList<>();

							temp = nearbyBackgroundPokemon;
							for (Long encounterId : temp.keySet()) {
								String name = nearbyBackgroundPokemon.get(encounterId);

								if (!notifiedNearbies.contains(encounterId)) {
									notifiedNearbies.add(encounterId);
									nearbyPokes.add(name);
								}
							}

							if (!nearbyPokes.isEmpty()) sendNearbyNotification(nearbyPokes);
							else print("No nearby background pokes that weren't already included in wilds");
						}

						// Report on the Pokemon we found
						ArrayList<String> wildPokes = new ArrayList<>();
						ArrayList<String> wildPokeIvs = new ArrayList<>();

						Map<Long, String> temp = wildBackgroundPokemon;
						for (Long encounterId : temp.keySet()) {
							String name = wildBackgroundPokemon.get(encounterId);
							String ivs = wildBackgroundPokemonIvs.get(encounterId);

							if (!notifiedWilds.contains(encounterId)) {
								notifiedWilds.add(encounterId);
								wildPokes.add(name);
								wildPokeIvs.add(ivs);
							}
						}

						if (!wildBackgroundPokemon.isEmpty()) sendCatchableNotification(wildPokes, wildPokeIvs);

						MapController.instance.lastScanTime = System.currentTimeMillis();
					}
				};

				scanThread.start();
			}
		};

		features.runOnMainThread(main);
	}

	public void sendCatchableNotification(ArrayList<String> pokemon, ArrayList<String> ivs) {
		String message = "";
		String title = "";

		if (pokemon.isEmpty()) return;

		if (pokemon.size() > 1) {
			for (int n = 0; n < pokemon.size(); n++) {
				String poke = pokemon.get(n);

				String iv = ivs.get(n).equals("") ? "" : ivs.get(n) + "\uFF05";

				if (n < pokemon.size() - 1) {
					message += poke + " " + iv + ", ";
				} else {
					message += "and " + poke + " " + iv;
				}
			}
		} else {
			message = pokemon.get(0) + " " + ivs.get(0) + "\uFF05";
		}

		message = "Found " + message;

		message = message.replaceAll(" \\%", "");

		title = "Found " + pokemon.size() + " Pokemon on the map!";

		sendNotification(title, message);
	}

	public void sendNearbyNotification(ArrayList<String> pokemon) {
		String message = "";
		String title = "";

		if (pokemon.isEmpty()) return;

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

		message = "Nearby Pokemon: " + message;

		title = pokemon.size() + " nearby Pokemon were not found!";

		sendNotification(title, message);
	}

	public void sendNotification(String title, String message) {
		if ((UIApplication.sharedApplication().currentUserNotificationSettings().types() & UIUserNotificationType.Alert) == 0) {
			print("User doesn't allow notification alerts");
			return;
		}
		UILocalNotification notification = UILocalNotification.alloc().init();
		//notification.setAlertTitle("PokeSensor");
		notification.setAlertBody(message);
		if (backgroundNotificationSound) notification.setSoundName(UIKit.UILocalNotificationDefaultSoundName());
		UIApplication.sharedApplication().presentLocalNotificationNow(notification);
		/*NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(con)
						.setSmallIcon(R.drawable.ic_notification)
						.setContentTitle(title)
						.setContentText(message)
						.setDefaults(Notification.DEFAULT_ALL);

		if (Build.VERSION.SDK_INT >= 16) {
			mBuilder.setPriority(Notification.PRIORITY_HIGH);
		}

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(con, MapController.class);
		resultIntent.setAction(Intent.ACTION_MAIN);
		resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(act, 0, resultIntent, 0);
		mBuilder.setContentIntent(contentIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) con.getSystemService(Context.NOTIFICATION_SERVICE);

		// mId allows you to update the notification later on.
		Notification notification = mBuilder.build();
		//notification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotificationManager.notify((int) System.currentTimeMillis(), notification);*/
	}

	public Future accountScanBackground(final ArrayList<AccountScanner> scanners, final long SCAN_INTERVAL, final CLLocationCoordinate2D center) {
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
										if (scanner.account.circle != null) {
											mMap.removeOverlay(scanner.account.circle);
											scanPointCirclesDetailed.remove(scanner.account);
										}
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

						final CLLocationCoordinate2D loc = cartesianToCoord(scanner.points.get(scanner.pointCursor), center);
						scanner.pointCursor++;

						try {
							Runnable progressRunnable = new Runnable() {
								@Override
								public void run() {
									updateScanPoint(loc, scanner.account);
								}
							};
							features.runOnMainThread(progressRunnable);
						} catch (Exception e) {
							e.printStackTrace();
						}

						scanner.repeat = !scanForPokemonBackground(scanner, loc.latitude(), loc.longitude());

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
							removeMyScanPoint(scanner.account);
							continue;
						}

						if (!scanner.repeat && scanner.pointCursor == scanner.points.size()) {
							print(scanner.account.getUsername() + " is finished scanning.");
							scanner.account.setScanning(false);
							removeMyScanPoint(scanner.account);
						}
					}
				}

				for (AccountScanner scanner : scanners) {
					scanner.account.setScanning(false);
					removeMyScanPoint(scanner.account);
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
				showErrorCircle(account);
				return true;
			}
			features.print(TAG, "Scanning (" + lat + "," + lon + ")...");

			go.setLocation(lat, lon, 0);
			Thread.sleep(200);
			try {
				features.refreshMapObjects(account);
			} catch (Exception c) {
				if (c instanceof CaptchaActiveException) showCaptchaCircle(account);
				account.checkExceptionForCaptcha(c);
			}
			Thread.sleep(200);

			scanner.activeSpawns.clear();

			if (use2Captcha) {
				if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED || account.getStatus() == Account.AccountStatus.SOLVING_CAPTCHA) {
					showCaptchaCircle(account);
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
						return false;
					}
				}
			}

			DateFormat df = null;
			if (collectSpawns) df = new SimpleDateFormat("mm:ss");

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

				try {
					if (collectSpawns) {
						Spawn mySpawn = spawns.get(poke.getSpawnPointId());
						if (mySpawn != null) {
							long time = poke.getExpirationTimestampMs() - System.currentTimeMillis();
							print("Spawn point " + mySpawn.id + " despawns at minute " + mySpawn.despawnMinute + " and second " + mySpawn.despawnSecond);
							if (time > 0 && time <= 3600000) {
								if (mySpawn.despawnMinute < 0 || mySpawn.despawnSecond < 0) {
									long expTime = poke.getExpirationTimestampMs();
									Date date = new Date(expTime);
									String[] timeStrings = df.format(date).split(":");
									mySpawn.despawnMinute = Integer.parseInt(timeStrings[0]);
									mySpawn.despawnSecond = Integer.parseInt(timeStrings[1]);
									print("We found the despawn time for spawn " + poke.getSpawnPointId() + ". It despawns at the " + timeStrings[0] + " minute and " + timeStrings[1] + " second mark!");
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					ErrorReporter.logExceptionThreaded(e);
				}

				if (!features.filter.get(pokedexNumber) && (!backgroundScanIvs || !overrideEnabled)) continue;

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
					}

					long timeMs = poke.getExpirationTimestampMs() - System.currentTimeMillis();

					try {
						if (collectSpawns) {
							Spawn mySpawn = spawns.get(poke.getSpawnPointId());
							if (mySpawn != null) {
								if (mySpawn.despawnMinute >= 0 && mySpawn.despawnSecond >= 0) {
									Date date = new Date(System.currentTimeMillis());
									String[] timeStrings = df.format(date).split(":");
									int currentMinute = Integer.parseInt(timeStrings[0]);
									int currentSecond = Integer.parseInt(timeStrings[1]);
									int currentTotalSeconds = currentMinute * 60 + currentSecond;
									int despawnTotalSeconds = mySpawn.despawnMinute * 60 + mySpawn.despawnSecond;
									if (despawnTotalSeconds < currentTotalSeconds)
										despawnTotalSeconds += 3600;

									int diffSeconds = despawnTotalSeconds - currentTotalSeconds;

									timeMs = diffSeconds * 1000;

									print("Calculated despawn time for " + mySpawn.id + " is " + timeMs);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						ErrorReporter.logExceptionThreaded(e);
					}

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

			if (wildPokes.isEmpty() && pokes.isEmpty() && nearbyPokes.isEmpty()) {
				showEmptyResultsCircle(account);
			} else {
				showGoodCircle(account);
			}

			for (NearbyPokemon poke : nearbyPokes) {
				int pokedexNumber = poke.getPokemonId().getNumber();

				if (pokedexNumber > Features.NUM_POKEMON) activateGen2();

				if (!features.filter.get(pokedexNumber)) continue;
				totalNearbyPokemon.add(new NearbyPokemonGPS(poke, new CLLocationCoordinate2D(lat, lon)));
				totalEncounters.add(poke.getEncounterId());
				if (backgroundIncludeNearby && features.notificationFilter.get(pokedexNumber)) nearbyBackgroundPokemon.put(poke.getEncounterId(), getLocalName(poke.getPokemonId().getNumber()));
			}

			if (nearbyPokes.isEmpty()) features.print("PokeFinder", "No nearby pokes :(");
			for (NearbyPokemon poke : nearbyPokes) {
				features.print("PokeFinder", "Found NearbyPokemon: " + poke.getPokemonId().name());
				features.print(TAG, "Distance in meters: " + poke.getDistanceInMeters());
				//mMap.addCircle(new CircleOptions().center(new CLLocationCoordinate2D(go.getLatitude(), go.getLongitude())).radius(poke.getDistanceInMeters()));
			}

			if (wildPokes.isEmpty()) features.print("PokeFinder", "No wild pokes :(");
			for (final CatchablePokemon poke : wildPokes) {
				features.print("PokeFinder", "Found WildPokemon: " + poke.toString());

				int pokedexNumber = poke.getPokemonId().getNumber();

				if (!features.filter.get(pokedexNumber) && (!backgroundScanIvs || !overrideEnabled)) continue;

				//String ivs = "";
				ArrayList<String> ivHolder = new ArrayList<>();
				ArrayList<String> moveHolder = new ArrayList<>();
				ArrayList<String> heightWeightHolder = new ArrayList<>();
				ArrayList<String> genderHolder = new ArrayList<>();
				boolean hide = false;
				if (backgroundScanIvs) {
					for (CatchablePokemon pokemon : pokes) {
						if (poke.getPokemonId().getNumber() > Features.NUM_POKEMON) activateGen2();
						if (poke.getSpawnPointId().equals(pokemon.getSpawnPointId())) {
							try {
								hide = encounterPokemon(poke, pokemon, ivHolder, moveHolder, heightWeightHolder, genderHolder, pokedexNumber);
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

				final Spawn finalSpawn = spawns.get(poke.getSpawnPointId());
				final String myIvs = ivHolder.size() > 0 ? ivHolder.get(0) : "";
				final String myMoves = moveHolder.size() > 0 ? moveHolder.get(0) : "";
				final String myHeightWeight = heightWeightHolder.size() > 0 ? heightWeightHolder.get(0) : "";
				final String gender = genderHolder.size() > 0 ? genderHolder.get(0) : "";

				long tempTime = poke.getExpirationTimestampMs() - System.currentTimeMillis();

				if (collectSpawns && finalSpawn != null && finalSpawn.despawnMinute >= 0 && finalSpawn.despawnSecond >= 0) {
					Date date = new Date(System.currentTimeMillis() * 1000);
					String[] timeStrings = df.format(date).split(":");
					int currentMinute = Integer.parseInt(timeStrings[0]);
					int currentSecond = Integer.parseInt(timeStrings[1]);
					int currentTotalSeconds = currentMinute * 60 + currentSecond;
					int despawnTotalSeconds = finalSpawn.despawnMinute * 60 + finalSpawn.despawnSecond;
					if (despawnTotalSeconds < currentTotalSeconds) despawnTotalSeconds += 3600;

					int diffSeconds = despawnTotalSeconds - currentTotalSeconds;

					tempTime = System.currentTimeMillis() + diffSeconds * 1000;
				}

				final long time = tempTime;

				Runnable r = new Runnable() {
					@Override
					public void run() {
						if (pokeTimes.containsKey(poke.getEncounterId()) || noTimes.containsKey(poke.getEncounterId())) {
							if (time > 0 && time <= 3600000) {
								String ms = String.format("%06d", time);
								int sec = Integer.parseInt(ms.substring(0, 3));
								//features.print(TAG, "Time string: " + time);
								//features.print(TAG, "Time shifted: " + (Long.parseLong(time) >> 16));
								features.print(TAG, "Time till hidden seconds: " + sec + "s");
								//features.print(TAG, "Data for " + poke.getPokemonId() + ":\n" + poke);
								showPokemonAt(poke.getPokemonId().name(), poke.getPokemonId().getNumber(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), true, myIvs, myMoves, myHeightWeight, gender);
							} else {
								features.print(TAG, "No valid expiry time given");
								showPokemonAt(poke.getPokemonId().name(), poke.getPokemonId().getNumber(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), false, myIvs, myMoves, myHeightWeight, gender);
							}
						} else {
							features.print(TAG, "Neither pokeTimes nor noTimes contains " + poke.getPokemonId() + " at spawn " + poke.getSpawnPointId() + " but it's trying to show on the map");
						}
					}
				};

				features.runOnMainThread(r);

				String percent = myIvs;
				int index = percent.indexOf("%");
				if (index >= 0) {
					percent = percent.substring(index - 3, index + 1).trim();
					if (percent.substring(0,1).equals("M")) percent = percent.substring(1).trim();
					percent = percent.substring(0, percent.length() - 1);
				}

				if (features.notificationFilter.get(pokedexNumber)) {
					wildBackgroundPokemon.put(poke.getEncounterId(), getLocalName(pokedexNumber) + (gender.equals("") ? "" : " " + gender));
					wildBackgroundPokemonIvs.put(poke.getEncounterId(), percent);
				}
			}

			saveSpawns();

			// Now remove everything that needs to be removed
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						for (Long id : removables) {
							if (!noTimes.containsKey(id)) {
								MKPointAnnotation marker = pokeMarkers.remove(id);
								if (marker != null) mMap.removeAnnotation(marker);
							}
						}
						removables.clear();
					} catch (Throwable e) {
						e.printStackTrace();
						ErrorReporter.logException(e);
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

			e.printStackTrace();

			// Now remove everything that needs to be removed
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						for (Long id : removables) {
							if (!noTimes.containsKey(id)) {
								MKPointAnnotation marker = pokeMarkers.remove(id);
								if (marker != null) mMap.removeAnnotation(marker);
							}
						}
						removables.clear();
					} catch (Throwable e) {
						e.printStackTrace();
						ErrorReporter.logException(e);
					}
				}
			};
			features.runOnMainThread(runnable);

			// Don't want endless errors in the background. not good
			return true;
		}
	}

	private Vector2[] getBackgroundSearchPoints(int numPoints) {
		isHexMode = true;
		int radius = 100000; // impossible number

		final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MAX_SCAN_RADIUS);
		final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
		final float ITERATIONS = MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

		NUM_SCAN_SECTORS = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);

		Vector2 startPoint;
		startPoint = Vector2.Zero;

		int direction = 0; // 0 = upright, 1 = downright, 2 = down, 3 = downleft, 4 = upleft, 5 = up
		ArrayList<Vector2> points = new ArrayList<Vector2>();
		points.add(startPoint);
		int numMoves = 0;

		Vector2 currentPoint = new Vector2(startPoint);

		print("Distance between hexes = " + HEX_DISTANCE);
		print("Num scan sectors = " + NUM_SCAN_SECTORS);
		print("Start point = " + startPoint.toString());

		if (numPoints == 1) {
			Vector2[] pointsArray = new Vector2[points.size()];
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

			if (points.size() >= numPoints) break;
		}

		Vector2[] pointsArray = new Vector2[numPoints];
		for (int n = 0; n < numPoints; n++) {
			pointsArray[n] = points.get(n);
		}

		return pointsArray;
	}

	// TODO End Background scanning + notification code

	@Override
	public boolean promptForApiKey() {
		// Prompt user for an API key if they don't have one. Return whether or not to proceed with the action
		// This should be done in a thread so we can wait for user input without blocking UI thread
		if (apiKeyPromptVisible) return false;

		if (!newApiKey.equals("") && PokeHashProvider.isKeyExpired()) {
			return true;
		} else {
			if (MapController.instance.inBackground) return false;

			apiKeyPromptVisible = true;
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Runnable ok = new Runnable() {
						@Override
						public void run() {
							Runnable main = new Runnable() {
								@Override
								public void run() {
									Runnable positive = new Runnable() {
										@Override
										public void run() {
											apiKeyPromptVisible = false;
											Gdx.net.openURI(PAID_API_HELP_PAGE_URL);
										}
									};

									Runnable negative = new Runnable() {
										@Override
										public void run() {
											askForApiKey();
										}
									};

									Runnable maybe = new Runnable() {
										@Override
										public void run() {
											// Do nothing
											apiKeyPromptVisible = false;
										}
									};
									DialogHelper.yesNoMaybeBox("Need API Key", "You need a paid API key in order to login and scan. This isn't how I want it to be, but it's the only way to scan right now unfortunately, and it's out of my control.", "How do I get a key?", positive, "I have a key", negative, "No thanks", maybe).build().show();
								}
							};

							features.runOnMainThread(main);
						}
					};

					if (PokeHashProvider.isKeyExpired()) DialogHelper.messageBox("API Key Expired", "Your API key has expired. You will need a new one to keep scanning. They last 31 days from first use.", "Ok", ok);
				}
			};

			features.runOnMainThread(runnable);
			return false;
		}
	}

	public void askForApiKey() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Lambda positive = new Lambda() {
					@Override
					public void execute() {
						String text = (String) params.get("Text");

						if (text == null || text.equals("")) {
							apiKeyPromptVisible = false;
							return;
						}

						newApiKey = text;
						Signature.validApiKey = null;
						NativePreferences.lock();
						NativePreferences.putString(IOSMapHelper.PREF_NEW_API_KEY, MapController.mapHelper.newApiKey);
						NativePreferences.unlock();

						apiKeyPromptVisible = false;

						AccountManager.tryTalkingToServer();
					}
				};

				Lambda negative = new Lambda() {
					@Override
					public void execute() {
						apiKeyPromptVisible = false;
					}
				};
				DialogHelper.textPrompt("Enter API Key", "Enter your PokeHash API key. This will let you use the latest reversed API provided by the PokeFarmer devs for a fee. Note this is not my system. It can be buggy/unavailable at times and I can't do anything about it.", MapController.mapHelper.newApiKey, "Confirm", positive, negative).build().show();
			}
		};

		MapController.features.runOnMainThread(runnable);
	}

	/*public void getNewCacheID() {
		try {
			FIRAuth.getAuth().signInAnonymouslyWithCompletion(new FIRAuth.FIRAuthResultCallback() {

				@Override
				public void invoke(FIRUser user, NSError error) {
					try {
						if (error == null) {
							cacheID = user.getUid();
							print("Got cache ID: " + cacheID);
							NativePreferences.lock();
							NativePreferences.putString(PREF_CACHE_ID, cacheID);
							NativePreferences.unlock();

							user.getTokenWithCompletion(new FIRUser.FIRAuthTokenCallback() {
								@Override
								public void invoke(String token, NSError nsError) {
									if (nsError == null) {
										print("Got token: " + token);
										retrievedCacheID = true;
									} else {
										print(nsError.toString());
									}
								}
							});
						} else {
							features.longMessage("Failed to get cache ID.");
							print("Cache ID fetch failed with error: " + error.toString());
						}
					} catch (Throwable t) {
						t.printStackTrace();
						ErrorReporter.logExceptionThreaded(t);
					}
					retrievedCacheID = true;
				}

			});
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logExceptionThreaded(t);
		}
	}

	public void getCacheIDThreaded() {
		retrievedCacheID = false;
		new Thread() {
			public void run() {
				getCacheID();
			}
		}.start();
	}

	public synchronized void getCacheID() {
		FIRUser user = FIRAuth.getAuth().getCurrentUser();
		if (user == null) {
			getNewCacheID();
		} else {
			user.getTokenWithCompletion(new FIRUser.FIRAuthTokenCallback() {
				@Override
				public void invoke(String token, NSError nsError) {
					if (nsError == null) {
						print("Got token: " + token);
						retrievedCacheID = true;
					} else {
						print(nsError.toString());
					}
				}
			});
		}
		*//*if (!cacheID.equals("")) return;

		NativePreferences.lock();
		cacheID = NativePreferences.getString(PREF_CACHE_ID, "");
		NativePreferences.unlock();

		if (cacheID.equals("")) {
			retrievedCacheID = false;
			getNewCacheID();
		} else {
			print("Loaded cache ID from prefs: " + cacheID);
			retrievedCacheID = true;
		}

		while (!retrievedCacheID) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}*//*
	}*/
}
