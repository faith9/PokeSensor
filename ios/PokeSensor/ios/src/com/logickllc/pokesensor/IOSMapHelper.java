package com.logickllc.pokesensor;


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.robovm.apple.corelocation.CLLocation;
import org.robovm.apple.corelocation.CLLocationCoordinate2D;
import org.robovm.apple.mapkit.MKMapView;
import org.robovm.apple.mapkit.MKPointAnnotation;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIControl.OnTouchUpInsideListener;
import org.robovm.apple.uikit.UIEvent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector2;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.logickllc.pokesensor.api.MapHelper;
import com.logickllc.pokesensor.api.WildPokemonTime;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;

public class IOSMapHelper extends MapHelper {
	public static final String PREF_SCAN_DISTANCE = "ScanDistance";
	public static final String PREF_SCAN_TIME = "ScanTime";
	public static final String PREF_SCAN_SPEED = "ScanSpeed";
	public static final float DEFAULT_ZOOM = 2000f;

	private MKPointAnnotation myMarker;
	private MKMapView mMap;
	private ConcurrentHashMap<Long, MKPointAnnotation> pokeMarkers = new ConcurrentHashMap<Long, MKPointAnnotation>();
	private int paddingLeft, paddingRight, paddingTop, paddingBottom;
	private CustomCircle scanCircle;
	private String scanDialogMessage;
	private MKPointAnnotation scanPoint;
	private CustomCircle scanPointCircle;
	private int scanProgressMax;
	protected ArrayList<NearbyPokemonGPS> totalNearbyPokemon = new ArrayList<NearbyPokemonGPS>();
	private final String POKEMON_FOLDER = "pokemon/";
	private final String IMAGE_EXTENSION = ".png";
	private String scanPointIcon = "scan_point_icon" + IMAGE_EXTENSION;
	private boolean scanning = false;

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

	public MKPointAnnotation getScanPoint() {
		return scanPoint;
	}

	public void setScanPoint(MKPointAnnotation scanPoint) {
		this.scanPoint = scanPoint;
	}

	public CustomCircle getScanPointCircle() {
		return scanPointCircle;
	}

	public void setScanPointCircle(CustomCircle scanPointCircle) {
		this.scanPointCircle = scanPointCircle;
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



	public synchronized void moveMe(double lat, double lon, boolean repositionCamera, boolean reZoom) {
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
	}

	public void wideScan() {
		if (!features.loggedIn()) return;
		if (scanning) return;
		else scanning = true;
		searched = true;
		updateScanSettings();
		abortScan = false;
		if (scanDistance > MAX_SCAN_DISTANCE) scanDistance = MAX_SCAN_DISTANCE;

		Runnable main = new Runnable() {
			@Override
			public void run() {
				final ArrayList<Long> ids = new ArrayList<Long>(noTimes);

				for (Long id : ids) {
					features.print(TAG, "Removed poke marker!");
					MKPointAnnotation marker = pokeMarkers.remove(id);
					mMap.removeAnnotation(marker);
				}

				MapController.instance.scanBar.setProgress(0);

				MapController.instance.scanView.setHidden(false);

				noTimes.clear();

				final Thread scanThread = new Thread() {
					public void run() {
						double lat = currentLat;
						double lon = currentLon;
						int offsetMeters = scanDistance;
						final long METERS_PER_SECOND = 50;
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
						//pokeMarkers.clear();

						//scanForPokemon(lat, lon);

						// Calculate bounding box of this point at certain intervals and poll them
						// all for a complete mapping. Pause a few seconds between polling to not agitate the servers

						/*int negOffsetMeters = -1 * offsetMeters;
						float offsetDiagonal = (float) Math.sin(Math.toRadians(45));
						float negOffsetDiagonal = -1 * offsetDiagonal;
						CLLocationCoordinate2D[] boundingBox = getBoundingBox(lat, lon, offsetMeters);
						ArrayList<CLLocationCoordinate2D> boxList = new ArrayList<CLLocationCoordinate2D>(Arrays.asList(boundingBox));
						Vector2[] boxPoints = new Vector2[]{Vector2.Zero,
								new Vector2(negOffsetDiagonal, negOffsetDiagonal),
								new Vector2(negOffsetMeters, 0),
								new Vector2(negOffsetDiagonal, offsetDiagonal),
								new Vector2(0, offsetMeters),
								new Vector2(offsetDiagonal, offsetDiagonal),
								new Vector2(offsetMeters, 0),
								new Vector2(offsetDiagonal, negOffsetDiagonal),
								new Vector2(0, negOffsetMeters)};*/

						int failedSectors = 0;
						Vector2[] boxPoints = getSearchPoints(scanDistance);
						ArrayList<CLLocationCoordinate2D> boxList = new ArrayList<CLLocationCoordinate2D>();


						final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
						final long SCAN_INTERVAL = Math.round(MINI_SQUARE_SIZE / scanSpeed * 1000);
						features.print(TAG, "Scan interval: " + SCAN_INTERVAL);
						features.print(TAG,  "Min scan time: " + minScanTime * 1000);

						scanProgressMax = NUM_SCAN_SECTORS;

						boolean first = true;
						for (int n = 0; n < boxPoints.length; n++) {
							if (abortScan) {
								features.longMessage(R.string.abortScan);
								scanning = false;
								return;
							}

							final CLLocationCoordinate2D loc = cartesianToCoord(boxPoints[n], new CLLocationCoordinate2D(lat, lon));
							boxList.add(loc);
							try {
								if (!first) Thread.sleep(Math.max(SCAN_INTERVAL, (int) minScanTime * 1000));
								//if (!first) Thread.sleep(5000);
								else first = false;

								if (abortScan) {
									features.longMessage(R.string.abortScan);
									scanning = false;
									return;
								}

								final int sector = n + 1;
								Runnable progressRunnable = new Runnable() {
									@Override
									public void run() {
										scanDialogMessage = "Scanning sector " + sector + "/" + NUM_SCAN_SECTORS + "  " + R.string.tapCancel;
										//dialog.setMessage(scanDialogMessage);
										//dialog.setProgress(sector);
										MapController.instance.scanText.setText(scanDialogMessage);
										setScanProgress(sector);
										if (false) {
											scanPointCircle = new CustomCircle(loc, MAX_SCAN_RADIUS);
											scanPointCircle.fillColor = UIColor.green();
											mMap.addOverlay(scanPointCircle);
											//scanPointCircle = mMap.addCircle(new CircleOptions().radius(MAX_SCAN_RADIUS).strokeWidth(2).fillColor(Color.GREEN).center(loc).zIndex(-1));
										} else {
											if (scanPointCircle != null) mMap.removeOverlay(scanPointCircle);
											scanPointCircle = new CustomCircle(loc, MAX_SCAN_RADIUS);
											mMap.addOverlay(scanPointCircle);
										}

										if (scanPoint != null) mMap.removeAnnotation(scanPoint);
										scanPoint = new ImageAnnotation(scanPointIcon);
										scanPoint.setCoordinate(loc);
										scanPoint.setTitle("Sector " + sector);
										mMap.addAnnotation(scanPoint);
									}
								};
								features.runOnMainThread(progressRunnable);
							} catch (Exception e) {
								e.printStackTrace();
								if (abortScan) {
									features.longMessage(R.string.abortScan);
									scanning = false;
									return;
								}
							}
							if (!scanForPokemon(loc.getLatitude(), loc.getLongitude())) failedSectors++;
						}

						try {
							// Trilaterate everything we don't have on the map yet
							for (long encounter : totalEncounters) {
								if (totalWildEncounters.contains(encounter)) continue;
								String name = "Unknown";
								ArrayList<NearbyPokemonGPS> triPoints = new ArrayList<NearbyPokemonGPS>();
								float minDistance = Float.POSITIVE_INFINITY;
								for (NearbyPokemonGPS poke : totalNearbyPokemon) {
									if (poke.getPokemon().getEncounterId() == encounter) {
										minDistance = Math.min(minDistance, poke.getPokemon().getDistanceInMeters());
										name = poke.getPokemon().getPokemonId().name();
										if (poke.getPokemon().getDistanceInMeters() == 200
                                                || poke.getPokemon().getDistanceInMeters() <= 0.0)
											continue;
										int index = boxList.indexOf(poke.getCoords());
										if (index == -1) continue;
										poke.setCartesianCoords(boxPoints[index]);
										triPoints.add(poke);
									}
								}
								if (triPoints.size() >= 3) {
									// Center location is (0,0)
									int size = triPoints.size();
									double[][] positions = new double[size][2];
									double[] distances = new double[size];

									for (int n = 0; n < size; n++) {
										positions[n][0] = triPoints.get(n).getCartesianCoords().x;
										positions[n][1] = triPoints.get(n).getCartesianCoords().y;
										distances[n] = triPoints.get(n).getPokemon().getDistanceInMeters();
									}

									NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
									LeastSquaresOptimizer.Optimum optimum = solver.solve();

									double[] centroid = optimum.getPoint().toArray();
									double offsetX = centroid[1];
									double offsetY = centroid[0];

									final double latRadian = Math.toRadians(lat);

									final double metersPerLatDegree = 110574.235;
									final double metersPerLonDegree = 110572.833 * Math.cos(latRadian);
									final CLLocationCoordinate2D target = new CLLocationCoordinate2D(offsetY / metersPerLatDegree + lat, offsetX / metersPerLonDegree + lon);
									final String finalName = name;

									Runnable markerRunnable = new Runnable() {
										@Override
										public void run() {
											features.print(TAG, "Adding marker for " + finalName + " at " + target.toString());
											showPokemonAt(finalName, target, System.currentTimeMillis(), false);
										}
									};
									features.runOnMainThread(markerRunnable);
								} else {
									final String finalName = name;
									final float finalMinDistance = minDistance;
									Runnable r = new Runnable() {
										@Override
										public void run() {
											System.out.println(finalName + " is " + finalMinDistance + "m away but can't be pinpointed");
										}
									};
									//features.runOnMainThread(r);
								}
							}

							Runnable dismissRunnable = new Runnable() {
								@Override
								public void run() {
									//diafeatures.printismiss();
									if (scanPoint != null) mMap.removeAnnotation(scanPoint);
									if (scanPointCircle != null) mMap.removeOverlay(scanPointCircle);

									MapController.instance.scanView.setHidden(true);
								}
							};
							features.runOnMainThread(dismissRunnable);

							if (failedSectors > 0) {
								if (failedScanLogins == NUM_SCAN_SECTORS) features.login();
								else
									features.shortMessage(failedSectors + " out of " + NUM_SCAN_SECTORS + " sectors failed to scan");
							}
						} catch (Exception e) {
							e.printStackTrace();
							features.longMessage("Trilateration error. Please inform the developer.");
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

						if (scanPoint != null) mMap.removeAnnotation(scanPoint);
						if (scanPointCircle != null) mMap.removeOverlay(scanPointCircle);
					}

				});						

				scanThread.start();
			}
		};

		features.runOnMainThread(main);
	}

	public boolean scanForPokemon(double lat, double lon) {
		try {
			features.print(TAG, "Scanning (" + lat + "," + lon + ")...");
			features.go.setLocation(lat, lon, 0);
			final List<CatchablePokemon> pokes = features.getCatchablePokemon(features.go, 9);
			final List<NearbyPokemonOuterClass.NearbyPokemon> nearbyPokes = features.getNearbyPokemon(features.go, 9);
			for (NearbyPokemonOuterClass.NearbyPokemon poke : nearbyPokes) {
				totalNearbyPokemon.add(new NearbyPokemonGPS(poke, new CLLocationCoordinate2D(lat, lon)));
				totalEncounters.add(poke.getEncounterId());
			}
			final List<WildPokemonOuterClass.WildPokemon> wildPokes = features.getWildPokemon(features.go, 9);
			for (WildPokemonOuterClass.WildPokemon poke : wildPokes) {
				totalWildEncounters.add(poke.getEncounterId());
				if (!pokeTimes.containsKey(poke.getEncounterId()) && !noTimes.contains(poke.getEncounterId())) {
					long timeMs = poke.getTimeTillHiddenMs();
					if (timeMs > 0) {
						long despawnTime = System.currentTimeMillis() + timeMs;
						pokeTimes.put(poke.getEncounterId(), new WildPokemonTime(poke, despawnTime));
						features.print(TAG, poke.getPokemonData().getPokemonId() + " will despawn at " + despawnTime);
					} else if (timeMs < 0) {
						noTimes.add(poke.getEncounterId());
					}
				}

			}

			Runnable r = new Runnable() {
				@Override
				public void run() {
					/*if (pokes.isEmpty()) features.print("PokeFinder", "No catchable pokes :(");
                    for (CatchablePokemon poke : pokes) {
                        features.print("PokeFinder", "Found CatchablePokemon: " + poke.toString());
                        //showPokemonAt(poke.getPokemonId().name(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), true);
                    }*/

					if (nearbyPokes.isEmpty()) features.print("PokeFinder", "No nearby pokes :(");
					for (NearbyPokemonOuterClass.NearbyPokemon poke : nearbyPokes) {
						features.print("PokeFinder", "Found NearbyPokemon: " + poke.toString());
						features.print(TAG, "Distance in meters: " + poke.getDistanceInMeters());
						//mMap.addCircle(new CircleOptions().center(new CLLocationCoordinate2D(features.go.getLatitude(), features.go.getLongitude())).radius(poke.getDistanceInMeters()));
					}

					if (wildPokes.isEmpty()) features.print("PokeFinder", "No wild pokes :(");
					for (WildPokemonOuterClass.WildPokemon poke : wildPokes) {
						features.print("PokeFinder", "Found WildPokemon: " + poke.toString());
						//features.print(TAG, "Most recent way of finding time till hidden: " +  (poke.getTimeTillHiddenMs() & 0xffffffffL));
						//features.print(TAG, "BigDecimal: " + asString(poke.getTimeTillHiddenMs()));
						//features.print(TAG, "Integer shift: " + Integer.toString(poke.getTimeTillHiddenMs() >> 16));
						//features.print(TAG, "Long shift: " + Long.toString(poke.getTimeTillHiddenMs() >> 16));
						/*String time = asString(poke.getTimeTillHiddenMs());

                        if (time.length() < 6) {
                            time = String.format("%06d", Long.parseLong(time));
                        }

                        String ms = time.substring(time.length() - 6);
                        int sec = Integer.parseInt(ms.substring(0, 3));
                        features.print(TAG, "Time til hidden ms: " + asString(poke.getTimeTillHiddenMs()));
                        if (poke.getTimeTillHiddenMs() < 0) features.print(TAG, "Time approximation ms: " + (Math.abs(Integer.MIN_VALUE) - Math.abs(poke.getTimeTillHiddenMs())));*/
						long time = poke.getTimeTillHiddenMs();
						if (time > 0) {
							String ms = String.format("%06d", time);
							int sec = Integer.parseInt(ms.substring(0, 3));
							//features.print(TAG, "Time string: " + time);
							//features.print(TAG, "Time shifted: " + (Long.parseLong(time) >> 16));
							features.print(TAG, "Time till hidden seconds: " + sec + "s");
							//features.print(TAG, "Data for " + poke.getPokemonData().getPokemonId() + ":\n" + poke.getPokemonData());
							showPokemonAt(poke.getPokemonData().getPokemonId().name(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), true);
						} else if (time < 0) {
							features.print(TAG, "No valid expiry time given");
							showPokemonAt(poke.getPokemonData().getPokemonId().name(), new CLLocationCoordinate2D(poke.getLatitude(), poke.getLongitude()), poke.getEncounterId(), false);
						}
					}
				}
			};
			features.runOnMainThread(r);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof LoginFailedException) failedScanLogins++;
			return false;
		}

	}



	public synchronized void showPokemonAt(String name, CLLocationCoordinate2D loc, long encounterid, boolean hasTime) {
		if (pokeMarkers.containsKey(encounterid)) return;

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
			name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
			if (hasTime) {
				ImageAnnotation temp = new ImageAnnotation(POKEMON_FOLDER + name.toLowerCase() + IMAGE_EXTENSION);
				System.out.println("Path for " + name + ": " + temp.imagePath);
				temp.setCoordinate(loc);
				temp.setTitle(name);
				mMap.addAnnotation(temp);
				pokeMarkers.put(encounterid, temp);
			}
			else {
				ImageAnnotation temp = new ImageAnnotation(POKEMON_FOLDER + name.toLowerCase() + IMAGE_EXTENSION);
				System.out.println("Path for " + name + ": " + temp.imagePath);
				temp.setCoordinate(loc);
				temp.setTitle(name);
				temp.setSubtitle(R.string.timeNotGiven);
				mMap.addAnnotation(temp);
				pokeMarkers.put(encounterid, temp);
			}
		} catch (Exception e) {
			features.longMessage("Cannot find image for \"" + name + "\". Please alert the developer.");
			name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
			if (hasTime) {
				MKPointAnnotation temp = new MKPointAnnotation();
				temp.setCoordinate(loc);
				temp.setTitle(name);
				mMap.addAnnotation(temp);
				pokeMarkers.put(encounterid, temp); }
			else {
				MKPointAnnotation temp = new MKPointAnnotation();
				temp.setCoordinate(loc);
				temp.setTitle(name);
				temp.setSubtitle(R.string.timeNotGiven);
				mMap.addAnnotation(temp);
				pokeMarkers.put(encounterid, temp);
			}
		}
	}



	private CLLocationCoordinate2D[] getBoundingBox(final double lat, final double lon, final int distanceInMeters) {

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
	}

	private void print(String message) {
		features.print("PokeFinder", message);
	}

	private Vector2[] getSearchPoints(int radius) {
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
												image.callout.setText(image.getSubtitle() + " ");
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
		final Preferences prefs = Gdx.app.getPreferences(IOSFeatures.PREFS_NAME);
		maxScanDistance = prefs.getFloat(PREF_MAX_SCAN_DISTANCE, 70);
		minScanTime = prefs.getFloat(PREF_MIN_SCAN_TIME, 5);

		try {
			maxScanDistance = MapController.features.getVisibleScanDistance();
			if (maxScanDistance <= 0) throw new RemoteServerException("Unable to get scan distance from server");
			prefs.putFloat(PREF_MAX_SCAN_DISTANCE, (float) maxScanDistance);
			prefs.flush();
			features.print("PokeFinder", "Server says max visible scan distance is " + maxScanDistance);
		} catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RemoteServerException) distanceFailed = true;
			maxScanDistance = prefs.getFloat(PREF_MAX_SCAN_DISTANCE, 70);
			if (maxScanDistance <= 0) maxScanDistance = 70;
		}

		MAX_SCAN_RADIUS = (int) maxScanDistance;

		try {
			minScanTime = MapController.features.getMinScanRefresh();
			if (minScanTime <= 0) throw new RemoteServerException("Unable to get scan delay from server");
			prefs.putFloat(PREF_MIN_SCAN_TIME, (float) minScanTime);
			prefs.flush();
			features.print("PokeFinder", "Server says min scan refresh is " + minScanTime);
		} catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RemoteServerException) timeFailed = true;
			minScanTime = prefs.getFloat(PREF_MIN_SCAN_TIME, 5);
			if (minScanTime <= 0) minScanTime = 5;
		}
		
		if (distanceFailed && timeFailed) {
            features.skipTOS();
            //updateScanSettings();
            //return;
        }
		
		int distancePerScan = (int) Math.sqrt(Math.pow(MAX_SCAN_RADIUS * 2, 2) / 2);
		int speed = (int) Math.ceil(distancePerScan / minScanTime);
		maxScanSpeed = speed;
		
		return !distanceFailed && !timeFailed;
	}
}
