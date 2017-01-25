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

import org.robovm.apple.corelocation.CLLocation;
import org.robovm.apple.corelocation.CLLocationCoordinate2D;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.mapkit.MKMapView;
import org.robovm.apple.mapkit.MKPointAnnotation;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIControl.OnTouchUpInsideListener;
import org.robovm.apple.uikit.UIEvent;
import org.robovm.bindings.firebase.FIRAuth;
import org.robovm.bindings.firebase.FIRUser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
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
import POGOProtos.Enums.PokemonTypeOuterClass;
import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;
import POGOProtos.Settings.Master.PokemonSettingsOuterClass;

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

	public String[] types = "BULBASAUR,grass|IVYSAUR,grass|VENUSAUR,grass|CHARMANDER,fire|CHARMELEON,fire|CHARIZARD,fire|SQUIRTLE,water|WARTORTLE,water|BLASTOISE,water|CATERPIE,bug|METAPOD,bug|BUTTERFREE,bug|WEEDLE,bug|KAKUNA,bug|BEEDRILL,bug|PIDGEY,normal|PIDGEOTTO,normal|PIDGEOT,normal|RATTATA,normal|RATICATE,normal|SPEAROW,normal|FEAROW,normal|EKANS,poison|ARBOK,poison|PIKACHU,electric|RAICHU,electric|SANDSHREW,ground|SANDSLASH,ground|NIDORAN_FEMALE,poison|NIDORINA,poison|NIDOQUEEN,poison|NIDORAN_MALE,poison|NIDORINO,poison|NIDOKING,poison|CLEFAIRY,fairy|CLEFABLE,fairy|VULPIX,fire|NINETALES,fire|JIGGLYPUFF,normal|WIGGLYTUFF,normal|ZUBAT,poison|GOLBAT,poison|ODDISH,grass|GLOOM,grass|VILEPLUME,grass|PARAS,bug|PARASECT,bug|VENONAT,bug|VENOMOTH,bug|DIGLETT,ground|DUGTRIO,ground|MEOWTH,normal|PERSIAN,normal|PSYDUCK,water|GOLDUCK,water|MANKEY,fighting|PRIMEAPE,fighting|GROWLITHE,fire|ARCANINE,fire|POLIWAG,water|POLIWHIRL,water|POLIWRATH,water|ABRA,psychic|KADABRA,psychic|ALAKAZAM,psychic|MACHOP,fighting|MACHOKE,fighting|MACHAMP,fighting|BELLSPROUT,grass|WEEPINBELL,grass|VICTREEBEL,grass|TENTACOOL,water|TENTACRUEL,water|GEODUDE,rock|GRAVELER,rock|GOLEM,rock|PONYTA,fire|RAPIDASH,fire|SLOWPOKE,water|SLOWBRO,water|MAGNEMITE,electric|MAGNETON,electric|FARFETCHD,normal|DODUO,normal|DODRIO,normal|SEEL,water|DEWGONG,water|GRIMER,poison|MUK,poison|SHELLDER,water|CLOYSTER,water|GASTLY,ghost|HAUNTER,ghost|GENGAR,ghost|ONIX,rock|DROWZEE,psychic|HYPNO,psychic|KRABBY,water|KINGLER,water|VOLTORB,electric|ELECTRODE,electric|EXEGGCUTE,grass|EXEGGUTOR,grass|CUBONE,ground|MAROWAK,ground|HITMONLEE,fighting|HITMONCHAN,fighting|LICKITUNG,normal|KOFFING,poison|WEEZING,poison|RHYHORN,ground|RHYDON,ground|CHANSEY,normal|TANGELA,grass|KANGASKHAN,normal|HORSEA,water|SEADRA,water|GOLDEEN,water|SEAKING,water|STARYU,water|STARMIE,water|MR_MIME,psychic|SCYTHER,bug|JYNX,ice|ELECTABUZZ,electric|MAGMAR,fire|PINSIR,bug|TAUROS,normal|MAGIKARP,water|GYARADOS,water|LAPRAS,water|DITTO,normal|EEVEE,normal|VAPOREON,water|JOLTEON,electric|FLAREON,fire|PORYGON,normal|OMANYTE,rock|OMASTAR,rock|KABUTO,rock|KABUTOPS,rock|AERODACTYL,rock|SNORLAX,normal|ARTICUNO,ice|ZAPDOS,electric|MOLTRES,fire|DRATINI,dragon|DRAGONAIR,dragon|DRAGONITE,dragon|MEWTWO,psychic|MEW,psychic|CHIKORITA,grass|BAYLEEF,grass|MEGANIUM,grass|CYNDAQUIL,fire|QUILAVA,fire|TYPHLOSION,fire|TOTODILE,water|CROCONAW,water|FERALIGATR,water|SENTRET,normal|FURRET,normal|HOOTHOOT,normal|NOCTOWL,normal|LEDYBA,bug|LEDIAN,bug|SPINARAK,bug|ARIADOS,bug|CROBAT,poison|CHINCHOU,water|LANTURN,water|PICHU,electric|CLEFFA,fairy|IGGLYBUFF,normal|TOGEPI,fairy|TOGETIC,fairy|NATU,psychic|XATU,psychic|MAREEP,electric|FLAAFFY,electric|AMPHAROS,electric|BELLOSSOM,grass|MARILL,water|AZUMARILL,water|SUDOWOODO,rock|POLITOED,water|HOPPIP,grass|SKIPLOOM,grass|JUMPLUFF,grass|AIPOM,normal|SUNKERN,grass|SUNFLORA,grass|YANMA,bug|WOOPER,water|QUAGSIRE,water|ESPEON,psychic|UMBREON,dark|MURKROW,dark|SLOWKING,water|MISDREAVUS,ghost|UNOWN,psychic|WOBBUFFET,psychic|GIRAFARIG,normal|PINECO,bug|FORRETRESS,bug|DUNSPARCE,normal|GLIGAR,ground|STEELIX,steel|SNUBBULL,fairy|GRANBULL,fairy|QWILFISH,water|SCIZOR,bug|SHUCKLE,bug|HERACROSS,bug|SNEASEL,dark|TEDDIURSA,normal|URSARING,normal|SLUGMA,fire|MAGCARGO,fire|SWINUB,ice|PILOSWINE,ice|CORSOLA,water|REMORAID,water|OCTILLERY,water|DELIBIRD,ice|MANTINE,water|SKARMORY,steel|HOUNDOUR,dark|HOUNDOOM,dark|KINGDRA,water|PHANPY,ground|DONPHAN,ground|PORYGON2,normal|STANTLER,normal|SMEARGLE,normal|TYROGUE,fighting|HITMONTOP,fighting|SMOOCHUM,ice|ELEKID,electric|MAGBY,fire|MILTANK,normal|BLISSEY,normal|RAIKOU,electric|ENTEI,fire|SUICUNE,water|LARVITAR,rock|PUPITAR,rock|TYRANITAR,rock|LUGIA,psychic|HO_OH,fire|CELEBI,psychic".split("\\|");

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
				scanCircle = new CustomCircle(new CLLocationCoordinate2D(currentLat, currentLon), scanDistance);
				scanCircle.strokeColor = UIColor.black();
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
		CLLocation me = new CLLocation(lat, lon);
		if (myMarker != null) mMap.removeAnnotation(myMarker);
		myMarker = new MKPointAnnotation();
		myMarker.setCoordinate(me.getCoordinate());
		myMarker.setTitle("Me");
		mMap.addAnnotation(myMarker);
		if (repositionCamera) {
			mMap.getCamera().setCenterCoordinate(me.getCoordinate());
			if (reZoom) mMap.getCamera().setAltitude(DEFAULT_ZOOM); // Not sure how far up this is yet
		}
		currentLat = lat;
		currentLon = lon;
		refreshTempScanCircle();
	}

	public void wideScan() {
		final ArrayList<Account> goodAccounts = AccountManager.getGoodAccounts();
		if (goodAccounts.size() == 0) {
			if (Utilities.canScan != null && Utilities.canScan) features.longMessage("You don't have any valid accounts!");
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
								scanCircle = new CustomCircle(new CLLocationCoordinate2D(currentLat, currentLon), scanDistance);
								scanCircle.strokeColor = UIColor.blue();
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

				MapController.instance.scanView.addOnTouchUpInsideListener(new OnTouchUpInsideListener() {

					@Override
					public void onTouchUpInside(UIControl control, UIEvent event) {
						scanThread.interrupt();
						abortScan = true;
						MapController.instance.scanView.setHidden(true);

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
		for (MKPointAnnotation scanPoint : scanPoints.values()) {
			if (scanPoint != null) mMap.removeAnnotation(scanPoint);
		}
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
											scanPointCirclesDetailed.remove(scanner.account.circle);
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

						scanner.repeat = !scanForPokemon(scanner, loc.getLatitude(), loc.getLongitude());

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

						if (!scanner.repeat && scanner.pointCursor >= scanner.points.size()) {
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
				MKPointAnnotation scanPoint = scanPoints.get(account);
				CustomCircle scanPointCircle = scanPointCircles.get(account);

				if (scanPoint != null) mMap.removeAnnotation(scanPoint);
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
		MKPointAnnotation scanPoint;
		CustomCircle scanPointCircle;

		scanPoint = scanPoints.get(account);
		if (scanPoint != null) mMap.removeAnnotation(scanPoint);

		if (!showScanDetails) {
			scanPointCircle = scanPointCircles.get(account);
			if (scanPointCircle != null) mMap.removeOverlay(scanPointCircle);
		}

		scanPointCircle = new CustomCircle(loc, MAX_SCAN_RADIUS);

		if (showScanDetails) {
			scanPointCircle.strokeColor = UIColor.blue();
			scanPointCircle.fillColor = UIColor.blue().addAlpha(SCAN_DETAIL_CIRCLE_ALPHA);
			account.circle = scanPointCircle;
		}

		scanPoint = new ImageAnnotation(scanPointIcon);
		scanPoint.setCoordinate(loc);
		scanPoint.setTitle(account.getUsername());

		mMap.addAnnotation(scanPoint);
		mMap.addOverlay(scanPointCircle);

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
					CustomCircle circle = account.circle;
					if (circle != null) {
						circle.strokeColor = UIColor.red();
						circle.fillColor = UIColor.red().addAlpha(SCAN_DETAIL_CIRCLE_ALPHA);
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
						circle.strokeColor = UIColor.green();
						circle.fillColor = UIColor.green().addAlpha(SCAN_DETAIL_CIRCLE_ALPHA);
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
						circle.strokeColor = UIColor.yellow();
						circle.fillColor = UIColor.yellow().addAlpha(SCAN_DETAIL_CIRCLE_ALPHA);
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
						circle.strokeColor = UIColor.lightGray();
						circle.fillColor = UIColor.lightGray().addAlpha(SCAN_DETAIL_CIRCLE_ALPHA);
						mMap.removeOverlay(circle);
						mMap.addOverlay(circle);
					}
				}
			};

			features.runOnMainThread(runnable);
		}
	}

	public synchronized ArrayList<WildPokemonTime> getNoTimePokesInSector(double lat, double lon) {
		ArrayList<WildPokemonTime> results = new ArrayList<>();

		CLLocation here = new CLLocation(lat, lon);
		for (WildPokemonTime pokemonTime : noTimes.values()) {
			CLLocation spawnPoint = new CLLocation(pokemonTime.getPoke().getLatitude(), pokemonTime.getPoke().getLongitude());
			double meters = here.getDistanceTo(spawnPoint);
			if (meters <= MAX_SCAN_RADIUS) {
				results.add(pokemonTime);
			}
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
					}

					long timeMs = poke.getExpirationTimestampMs();
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
							showPokemonAt(poke.getPokemonId().name(), poke.getPokemonId().getNumber(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), true, myIvs);
						} else if (time < 0 || time > 3600000) {
							features.print(TAG, "No valid expiry time given");
							showPokemonAt(poke.getPokemonId().name(), poke.getPokemonId().getNumber(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), false, myIvs);
						}
					}
				};

				features.runOnMainThread(r);
			}

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

	public synchronized void showPokemonAt(String name, int pokedexNumber, CLLocationCoordinate2D loc, long encounterid, boolean hasTime, String ivs) {
		if (pokeMarkers.containsKey(encounterid)) return;

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
			localName = localName.substring(0, 1).toUpperCase() + localName.substring(1).toLowerCase();

			ImageAnnotation temp;
			if (CAN_SHOW_IMAGES) temp = new ImageAnnotation(POKEMON_FOLDER + name.toLowerCase() + IMAGE_EXTENSION);
			else {
				String filename = pokedexNumber + IMAGE_EXTENSION;
				FileHandle handle = Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER + filename);
				if (handle.exists()) {
					temp = new ImageAnnotation(Features.CUSTOM_IMAGES_FOLDER + filename);
					temp.isCustom = true;
				} else {
					if (defaultMarkersMode == 1) {
						temp = new ImageAnnotation(NUMBER_MARKER_FOLDER + pokedexNumber + IMAGE_EXTENSION);
					}
					else {
						//PokemonSettingsOuterClass.PokemonSettings settings = PokemonMeta.getPokemonSettings(PokemonIdOuterClass.PokemonId.valueOf(pokedexNumber));
						//PokemonTypeOuterClass.PokemonType pokemonType = settings.getType();
						//String type = pokemonType.name().toLowerCase();
						//type = type.substring(type.lastIndexOf("_") + 1);

						String type = types[pokedexNumber - 1].split("\\,")[1];

						print("Type of " + localName + " is " + type);
						temp = new ImageAnnotation(NUMBER_MARKER_FOLDER + BLANK_NAME_MARKER + type + IMAGE_EXTENSION);
						temp.isCustom = false;
					}
				}
			}

			temp.name = localName;
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
		final double latRadian = Math.toRadians(center.getLatitude());

		final double metersPerLatDegree = 110574.235;
		final double metersPerLonDegree = 110572.833 * Math.cos(latRadian);
		final double deltaLat = point.y / metersPerLatDegree;
		final double deltaLong = point.x / metersPerLonDegree;

		CLLocationCoordinate2D loc = new CLLocationCoordinate2D(center.getLatitude() + deltaLat, center.getLongitude() + deltaLong);
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
													image.callout.setText(image.ivs + "\n" + image.getSubtitle() + "  ");
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
													image.callout.setText(image.ivs + "\n" + image.getSubtitle() + "  ");
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
		minScanTime = NativePreferences.getFloat(PREF_MIN_SCAN_TIME, 5);

		try {
			maxScanDistance = MapController.features.getVisibleScanDistance();
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
			minScanTime = MapController.features.getMinScanRefresh();
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
		if (scanning) return;

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
				return;
			}

			for (int n = 0; n < spawnList.size(); n++) {
				Spawn spawn = spawnList.get(n);
				spawns.put(spawn.id, spawn);
			}
			//print("Loaded spawns: " + spawnList.toString());
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
		ImageAnnotation spawnPoint = new ImageAnnotation("spawn_icon.png");
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
	public void wideSpawnScan() {
		ConcurrentHashMap<String, Spawn> temp = new ConcurrentHashMap<>();
		CLLocation currentPoint = new CLLocation(currentLat, currentLon);

		for (Spawn spawn : spawns.values()) {
			print("Trying lat: " + spawn.lat + " lon: " + spawn.lon);
            /*if (left <= spawn.lon && right >= spawn.lon && bottom <= spawn.lat && top >= spawn.lat) {
                temp.put(spawn.id, spawn);
            }*/
			CLLocation spawnPoint = new CLLocation(spawn.lat, spawn.lon);
			double meters = currentPoint.getDistanceTo(spawnPoint);
			if (meters <= scanDistance) temp.put(spawn.id, spawn);
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
		minAttack = NativePreferences.getInteger(PREF_MIN_ATTACK, 0);
		minDefense = NativePreferences.getInteger(PREF_MIN_DEFENSE, 0);
		minStamina = NativePreferences.getInteger(PREF_MIN_STAMINA, 0);
		minPercent = NativePreferences.getInteger(PREF_MIN_PERCENT, 0);
		minOverride = NativePreferences.getInteger(PREF_MIN_OVERRIDE, 100);
		imageSize = NativePreferences.getLong(PREF_IMAGE_SIZE, 2);
		showScanDetails = NativePreferences.getBoolean(PREF_SHOW_SCAN_DETAILS, false);
		Features.NUM_POKEMON = NativePreferences.getInteger(PREF_NUM_POKEMON, 151);
		ivsAlwaysVisible = NativePreferences.getBoolean(PREF_IVS_ALWAYS_VISIBLE, true);
		defaultMarkersMode = NativePreferences.getLong(PREF_DEFAULT_MARKERS_MODE, 0);
		overrideEnabled = NativePreferences.getBoolean(PREF_OVERRIDE_ENABLED, minOverride != 100);
		clearMapOnScan = NativePreferences.getBoolean(PREF_CLEAR_MAP_ON_SCAN, false);
		gpsModeNormal = NativePreferences.getBoolean(PREF_GPS_MODE_NORMAL, true);
		use2Captcha = NativePreferences.getBoolean(PREF_USE_2CAPTCHA, false);
		useNewApi = NativePreferences.getBoolean(PREF_USE_NEW_API, false);
		fallbackApi = NativePreferences.getBoolean(PREF_FALLBACK_API, true);
		captchaKey = NativePreferences.getString(PREF_2CAPTCHA_KEY, "");
		newApiKey = NativePreferences.getString(PREF_NEW_API_KEY, "");

		getCacheIDThreaded();

		if (useNewApi) {
			MapController.instance.showRpmLabel();
			startRpmTimer();
		} else {
			MapController.instance.hideRpmLabel();
			MapController.instance.hideRpmCountLabel();
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
			if (Utilities.canScan != null && Utilities.canScan) features.longMessage("You don't have any valid accounts!");
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
								scanCircle = new CustomCircle(new CLLocationCoordinate2D(currentLat, currentLon), scanDistance);
								scanCircle.strokeColor = UIColor.blue();
								mMap.addOverlay(scanCircle);
							}
						};
						features.runOnMainThread(circleRunnable);

						features.print(TAG, "Scan distance: " + scanDistance);

						totalNearbyPokemon.clear();
						totalEncounters.clear();
						totalWildEncounters.clear();

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

				MapController.instance.scanView.addOnTouchUpInsideListener(new OnTouchUpInsideListener() {

					@Override
					public void onTouchUpInside(UIControl control, UIEvent event) {
						scanThread.interrupt();
						abortScan = true;
						MapController.instance.scanView.setHidden(true);

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
							if (showScanDetails && scanner.account.circle != null) {
								Runnable runnable = new Runnable() {
									@Override
									public void run() {
										if (scanner.account.circle != null) {
											mMap.removeOverlay(scanner.account.circle);
											scanPointCirclesDetailed.remove(scanner.account.circle);
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
							scanner.location = new CLLocation(scanner.startSpawn.lat, scanner.startSpawn.lon);
						} else if (scanner.repeat) {
							scanner.location = new CLLocation(scanner.repeatSpawn.lat, scanner.repeatSpawn.lon);
						} else {
							Hashtable<String, Integer> times = new Hashtable<>();

							final CLLocationCoordinate2D loc = scanner.location.getCoordinate();
							CLLocation here = new CLLocation(loc.getLatitude(), loc.getLongitude());
							ArrayList<Spawn> spawnList = new ArrayList<Spawn>(searchSpawns.values());
							for (Spawn spawn : spawnList) {
								CLLocation spawnPoint = new CLLocation(spawn.lat, spawn.lon);
								int commuteTime = 0;
								double commuteDistance = 0;
								commuteDistance = here.getDistanceTo(spawnPoint);
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
							scanner.location = new CLLocation(mySpawn.lat, mySpawn.lon);
						}

						scanner.repeat = false;
						final CLLocationCoordinate2D loc = scanner.location.getCoordinate();

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

						scanner.repeat = !scanForPokemon(scanner, loc.getLatitude(), loc.getLongitude());

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
			final CLLocationCoordinate2D loc = claimedSpawn.loc();

			features.print(TAG, "After claiming " + id + ", filtering out spawns in proximity...");
			CLLocation here = new CLLocation(loc.getLatitude(), loc.getLongitude());
			ArrayList<Spawn> spawnList = new ArrayList<Spawn>(searchSpawns.values());
			for (Spawn spawn : spawnList) {
				CLLocation spawnPoint = new CLLocation(spawn.lat, spawn.lon);
				double meters = here.getDistanceTo(spawnPoint);
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

	public void getNewCacheID() {
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
		/*if (!cacheID.equals("")) return;

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
		}*/
	}
}
