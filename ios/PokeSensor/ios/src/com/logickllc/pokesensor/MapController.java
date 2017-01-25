package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector2;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.MapHelper;
import com.logickllc.pokesensor.api.Messenger;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import org.robovm.apple.coregraphics.CGPoint;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.corelocation.CLAuthorizationStatus;
import org.robovm.apple.corelocation.CLLocationCoordinate2D;
import org.robovm.apple.corelocation.CLLocationManager;
import org.robovm.apple.corelocation.CLLocationManagerDelegateAdapter;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSDictionary;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSString;
import org.robovm.apple.mapkit.MKAnnotation;
import org.robovm.apple.mapkit.MKAnnotationView;
import org.robovm.apple.mapkit.MKCircle;
import org.robovm.apple.mapkit.MKCircleRenderer;
import org.robovm.apple.mapkit.MKMapView;
import org.robovm.apple.mapkit.MKMapViewDelegateAdapter;
import org.robovm.apple.mapkit.MKOverlay;
import org.robovm.apple.mapkit.MKOverlayRenderer;
import org.robovm.apple.mapkit.MKUserLocation;
import org.robovm.apple.uikit.NSLayoutConstraint;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIEdgeInsets;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UIGestureRecognizer;
import org.robovm.apple.uikit.UIGestureRecognizerDelegateAdapter;
import org.robovm.apple.uikit.UIGestureRecognizerState;
import org.robovm.apple.uikit.UIGraphics;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UILongPressGestureRecognizer;
import org.robovm.apple.uikit.UIProgressView;
import org.robovm.apple.uikit.UISearchController;
import org.robovm.apple.uikit.UISearchControllerDelegateAdapter;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.bindings.aerserv.ASAdView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static com.logickllc.pokesensor.IOSFeatures.PREFS_NAME;
import static com.logickllc.pokesensor.IOSFeatures.PREF_FIRST_LOAD;
import static com.logickllc.pokesensor.IOSFeatures.PREF_PASSWORD;
import static com.logickllc.pokesensor.IOSFeatures.PREF_PASSWORD2;
import static com.logickllc.pokesensor.IOSFeatures.PREF_TOKEN;
import static com.logickllc.pokesensor.IOSFeatures.PREF_TOKEN2;
import static com.logickllc.pokesensor.IOSFeatures.PREF_USERNAME;
import static com.logickllc.pokesensor.IOSFeatures.PREF_USERNAME2;
import static com.logickllc.pokesensor.IOSFeatures.REMOVE_ADS_IAP_PRODUCT_ID;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_CAPTCHA_MODE_POPUP;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_COLLECT_SPAWNS;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_SCAN_DISTANCE;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_SCAN_SPEED;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_SCAN_TIME;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_SHOW_IVS;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_SHOW_SPAWNS;
import static com.logickllc.pokesensor.api.Features.PREF_FILTER_STRING;
import static com.logickllc.pokesensor.api.MapHelper.PREF_MAX_SCAN_DISTANCE;
import static com.logickllc.pokesensor.api.MapHelper.PREF_MIN_ATTACK;
import static com.logickllc.pokesensor.api.MapHelper.PREF_MIN_DEFENSE;
import static com.logickllc.pokesensor.api.MapHelper.PREF_MIN_PERCENT;
import static com.logickllc.pokesensor.api.MapHelper.PREF_MIN_SCAN_TIME;
import static com.logickllc.pokesensor.api.MapHelper.PREF_MIN_STAMINA;

@CustomClass("MapController")
public class MapController extends UIViewController implements ASAdView.ASAdViewDelegate {
	@IBOutlet
	MKMapView map;

	@IBOutlet
	UIProgressView scanBar;

	@IBOutlet
	UILabel scanText;

	@IBOutlet
	ClickableView scanView;

	@IBOutlet
	public UILabel message;

	@IBOutlet
	UIView messageContainer;

	@IBOutlet
	NSLayoutConstraint messageContainerHeight, goodAccountsLabelHeight, rpmLabelHeight, rpmCountLabelHeight;

	@IBOutlet
	GoodAccountsView goodAccountsView;

	@IBOutlet
	RpmView rpmView;

	@IBOutlet
	RpmCountView rpmCountView;

	public static MapController instance;
	public static CLLocationManager manager;
	public boolean isLoaded = false;
	public boolean firstLocationUpdate = true;
	private boolean messageShowing = false;
	public ConcurrentHashMap<String, Long> messages = new ConcurrentHashMap<String, Long>();
	private Runnable nextRunnable = null;
	private UISearchController search;
	private SearchTableController searchResultsTable;

	public static IOSMapHelper mapHelper;

	public static IOSFeatures features;

	public final String PREF_APP_LOADS = "AppLoads";
	public final String PREF_ASK_FOR_SUPPORT = "AskForSupport";
	public final String PREF_ASK_FOR_REMOVE_ADS = "AskForRemoveAds";
	public final String PREF_FIRST_MULTIACCOUNT_LOAD = "FirstMultiAccountLoad";
	public final String PREF_FIRST_COPYRIGHT_LOAD = "FirstCopyrightLoad";
	public final String PREF_FIRST_DECODE_LOAD = "FirstDecodeLoad";
	public boolean canAskForSupport = true;
	public boolean canAskForRemoveAds = true;
	public int appLoads = 0;
	public final int TARGET_APP_LOADS = 10;
	public final int TARGET_APP_LOADS_REMOVE_ADS = 4;
	public boolean justCreated = true;
	public boolean canDecode = false;
	public boolean didFetchMessages = false;

	public boolean dontRefreshAccounts = false;
	public final String PREF_FIRST_RECOVERY_LOAD = "FirstRecoveryLoad";

	public static final double LABEL_X_PERCENT = 0.11;
	public static final double LABEL_Y_PERCENT = 0.20;
	public static final double LABEL_WIDTH_PERCENT = 0.78;
	public static final double LABEL_HEIGHT_PERCENT = 0.36;

	public static final double LABEL_X_SMALL_PERCENT = 0.101;
	public static final double LABEL_Y_SMALL_PERCENT = 0.218;
	public static final double LABEL_WIDTH_SMALL_PERCENT = 0.773;
	public static final double LABEL_HEIGHT_SMALL_PERCENT = 0.33;

	public static final double IV_X_PERCENT = 0.15;
	public static final double IV_Y_PERCENT = 0.17;
	public static final double IV_WIDTH_PERCENT = 0.71;
	public static final double IV_HEIGHT_PERCENT = 0.66;
	public static final String IV_IMAGE = "iv_box_rounded.png";
	public static final double IV_OFFSET_Y_PERCENT = 0.73;

	@Override
	public void viewDidLoad() {
		try {
			if ((Utilities.canScan = null) == null) {
				// nothing
			}
			searchResultsTable = (SearchTableController) this.getStoryboard().instantiateViewController("SearchTable");
			setupSearch();
			refreshMessageContainerHeight();
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	@Override
	public void viewWillAppear(boolean animated) {
		try {
			NativePreferences.printLockStatus("MapController willAppear");

			this.getNavigationController().setNavigationBarHidden(false);
			this.getNavigationController().setToolbarHidden(false, true);

			if (!isLoaded) {
				//this.addStrongRef(map);
				AccountManager.accounts = null;
				message.setLayoutMargins(new UIEdgeInsets(5, 5, 5, 5));
				instance = this;

				mapHelper = new IOSMapHelper();
				features = new IOSFeatures();

				features.setMapHelper(mapHelper);
				mapHelper.setFeatures(features);

				Thread adThread = new Thread() {
					public void run() {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							ErrorReporter.logExceptionThreaded(e);
						}
						Runnable adRunnable = new Runnable() {
							@Override
							public void run() {
								try {
									AdHelper.initShowAds(PREFS_NAME);
									if (AdHelper.isShowAds()) {
										IOSLauncher.instance.initAds();
										IOSLauncher.instance.startMediation();
									}

									refreshMessageContainerHeight();
								} catch (Throwable t) {
									t.printStackTrace();
									ErrorReporter.logException(t);
								}
							}
						};
						IOSLauncher.instance.postNativeRunnable(adRunnable);
					}
				};

				adThread.start();

				ErrorReporter.trySendingLeftovers();

				NativePreferences.lock("gpsModeNormal");
				mapHelper.gpsModeNormal = NativePreferences.getBoolean(IOSMapHelper.PREF_GPS_MODE_NORMAL, true);
				NativePreferences.unlock();

				manager = new CLLocationManager();

				requestLocationPermission();

				this.addStrongRef(manager);

				manager.setDelegate(new CLLocationManagerDelegateAdapter() {

					@Override
					public void didChangeAuthorizationStatus(CLLocationManager manager, CLAuthorizationStatus status) {
						try {
							Runnable runnable = new Runnable() {
								@Override
								public void run() {
									initLocation();
								}
							};
							features.runOnMainThread(runnable);

							super.didChangeAuthorizationStatus(manager, status);
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logException(t);
						}
					}

				});

				final UILongPressGestureRecognizer gesture = new UILongPressGestureRecognizer(
						new UIGestureRecognizer.OnGestureListener() {

							@Override
							public void onGesture(UIGestureRecognizer gestureRecognizer) {
								try {
									if (gestureRecognizer.getState() == UIGestureRecognizerState.Began) {
										try {
											CGPoint point = gestureRecognizer.getLocationInView(map);
											CLLocationCoordinate2D loc = map.convertPointToCoordinateFromView(point, map);
											MapController.mapHelper.setLocationOverride(true);
											MapController.mapHelper.moveMe(loc.getLatitude(), loc.getLongitude(), map.getUserLocation().getLocation().getAltitude(), false, false);
										} catch(Exception e) {
											e.printStackTrace();
										}
									}
								} catch (Throwable t) {
									t.printStackTrace();
									ErrorReporter.logExceptionThreaded(t);
								}
							}
						});

				gesture.setDelegate(new UIGestureRecognizerDelegateAdapter() {
					@Override
					public boolean shouldRecognizeSimultaneously(UIGestureRecognizer gestureRecognizer, UIGestureRecognizer otherGestureRecognizer) {
						return true;
					}
				});

				gesture.setMinimumPressDuration(0.7);

				map.setDelegate(new MKMapViewDelegateAdapter() {

					@Override
					public void calloutAccessoryControlTapped(MKMapView mapView, MKAnnotationView view, UIControl control) {
						try {
							if (view.getAnnotation() instanceof ImageAnnotation) {
								((ImageAnnotation) view.getAnnotation()).control = control;
							}
							super.calloutAccessoryControlTapped(mapView, view, control);
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
						}
					}

					@Override
					public MKOverlayRenderer getOverlayRenderer(MKMapView mapView, MKOverlay overlay) {
						try {
							if (overlay instanceof MKCircle) {
								if (overlay instanceof CustomCircle) {
									CustomCircle circle = (CustomCircle) overlay;
									MKCircleRenderer renderer = new MKCircleRenderer(circle);
									renderer.setStrokeColor(circle.strokeColor);
									renderer.setLineWidth(circle.lineWidth);
									renderer.setFillColor(circle.fillColor);
									return renderer;
								} else {
									MKCircleRenderer renderer = new MKCircleRenderer((MKCircle) overlay);
									renderer.setStrokeColor(UIColor.blue());
									renderer.setLineWidth(1);
									renderer.setFillColor(UIColor.clear());

									return renderer;
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
						}
						return super.getOverlayRenderer(mapView, overlay);
					}

					@Override
					public MKAnnotationView getAnnotationView(MKMapView mapView, MKAnnotation annotation) {
						try {
							if (annotation instanceof ImageAnnotation) {
								MKAnnotationView view = new MKAnnotationView(annotation,
										((ImageAnnotation) annotation).imagePath);
								if (((ImageAnnotation) annotation).isCustom)
									view.setImage(UIImage.getImage(Gdx.files.local(((ImageAnnotation) annotation).imagePath).file()));
								else
									view.setImage(UIImage.create(((ImageAnnotation) annotation).imagePath));

								view.setCanShowCallout(true);
								((ImageAnnotation) annotation).view = view;
								if (((ImageAnnotation) annotation).imagePath.equals("scan_point_icon.png")) {
									if (mapHelper.showScanDetails) view.setAlpha(0.5);
									view.getLayer().setAnchorPoint(new CGPoint(0.32f, 0.32f));
								}
								else if (((ImageAnnotation) annotation).imagePath.equals("spawn_icon.png")) {
									view.getLayer().setAnchorPoint(new CGPoint(0.5f, 0.5f));
									((ImageAnnotation) annotation).view.getLayer().setOpacity(0.5f);
									((ImageAnnotation) annotation).view.setUserInteractionEnabled(false);
									((ImageAnnotation) annotation).view.setCanShowCallout(false);
								} else {
									if (((ImageAnnotation) annotation).isCustom) {
										UILabel ivLabel = null;
										UIImage ivBox = UIImage.create(IOSMapHelper.NUMBER_MARKER_FOLDER + IV_IMAGE);
										if (mapHelper.showIvs && mapHelper.ivsAlwaysVisible) {
											double labelWidth = ivBox.getSize().getWidth(); // * 0.88;
											double labelHeight = ivBox.getSize().getHeight(); // * 0.72;
											double labelX = 0; //ivBox.getSize().getWidth() * 0.11;
											double labelY = 0; //ivBox.getSize().getHeight() * 0.20;

											String percent = ((ImageAnnotation) annotation).ivs;
											int index = percent.indexOf("%");

											if (index >= 0) {
												try {
													percent = percent.substring(index - 3, index + 1).trim();
													if (percent.substring(0, 1).equals("M"))
														percent = percent.substring(1).trim();
												} catch (Exception e) {
													e.printStackTrace();
													HashMap<String, String> extras = new HashMap<String, String>();
													extras.put("percent", percent);
													ErrorReporter.logExceptionThreaded(e, extras);
												}

												ivLabel = new UILabel(new CGRect(labelX, labelY, labelWidth, labelHeight));
												ivLabel.setNumberOfLines(1);
												ivLabel.setAdjustsFontSizeToFitWidth(true);
												ivLabel.setTextColor(UIColor.white());
												//ivLabel.setBackgroundColor(UIColor.black().addAlpha(0.6));
												//ivLabel.setBackgroundColor(UIColor.clear());
												ivLabel.setText(" " + percent + " ");
												ivLabel.setFont(UIFont.getBoldSystemFont(10));
												//ivLabel.getLayer().setMasksToBounds(true);
												//ivLabel.getLayer().setCornerRadius(10);
												ivLabel.setTextAlignment(NSTextAlignment.Center);
												//view.addSubview(ivLabel);

											/*UIGraphics.beginImageContext(ivBox.getSize());
											//ivBox.draw(new CGRect(0, 0, ivBox.getSize().getWidth(), ivBox.getSize().getHeight()));
											CGRect rect = new CGRect(labelX, labelY, labelWidth, labelHeight);
											//UIColor.black().addAlpha(0.5).setFill();
											//UIColor.white().setStroke();
											//UIFont font = UIFont.getSystemFont(12);
											//NSDictionary attributes = new NSDictionary();
											//attributes.put("String", font);
											//new NSString(percent).draw(rect, font);
											ivLabel.draw(rect);

											ivBox = UIGraphics.getImageFromCurrentImageContext();
											UIGraphics.endImageContext();*/
											}
										}

										CGRect imageSize = getImageSize();
										//UIGraphics.beginImageContext(imageSize.getSize());
										UIGraphics.beginImageContext(imageSize.getSize(), false, 0);
										//UIGraphics.getCurrentContext().setAllowsAntialiasing(false);
										view.getImage().draw(imageSize);

										UIImage resizedImage = UIGraphics.getImageFromCurrentImageContext();
										UIGraphics.endImageContext();

										view.setImage(resizedImage);

										if (mapHelper.showIvs && mapHelper.ivsAlwaysVisible && ivLabel != null) {
											double x = imageSize.getWidth() / 2.0 - ivLabel.getBounds().getWidth() / 2.0;
											double y = imageSize.getHeight() * IV_OFFSET_Y_PERCENT;
											ivLabel.setFrame(new CGRect(x, y, ivLabel.getBounds().getWidth(), ivLabel.getBounds().getHeight()));
											//UIImageView ivView = new UIImageView(ivBox);
											//ivView.setFrame(new CGRect(0, 0, ivLabel.getBounds().getWidth(), ivLabel.getBounds().getHeight()));
											//ivLabel.addSubview(ivView);
											//ivLabel.sendSubviewToBack(ivView);
											try {
												// This is known for a possible NullPointerException
												ivLabel.setBackgroundColor(UIColor.fromPatternImage(ivBox));
											} catch (Exception e) {
												e.printStackTrace();
												ErrorReporter.logExceptionThreaded(e);
												ivLabel.setBackgroundColor(UIColor.black().addAlpha(0.6));
											}
											view.addSubview(ivLabel);
											//ivBox.draw(new CGRect(x, y, ivBox.getSize().getWidth(), ivBox.getSize().getHeight()));
										}


									} else {
										features.print("PokeFinder", "Image width: " + view.getImage().getSize().getWidth());
										features.print("PokeFinder", "Image height: " + view.getImage().getSize().getHeight());

										double labelWidth = view.getImage().getSize().getWidth() * 0.78;
										double labelHeight = view.getImage().getSize().getHeight() * 0.36;
										double labelX = view.getImage().getSize().getWidth() * 0.11;
										double labelY = view.getImage().getSize().getHeight() * 0.20;

										features.print("PokeFinder", "Label width: " + labelWidth);
										features.print("PokeFinder", "Label height: " + labelHeight);
										features.print("PokeFinder", "Label X: " + labelX);
										features.print("PokeFinder", "Label Y: " + labelY);

										UILabel nameLabel = new UILabel(new CGRect(labelX, labelY, labelWidth, labelHeight));
										nameLabel.setNumberOfLines(1);
										nameLabel.setAdjustsFontSizeToFitWidth(true);
										nameLabel.setTextColor(UIColor.black());
										nameLabel.setText(((ImageAnnotation) annotation).name);
										if (mapHelper.showIvs && mapHelper.ivsAlwaysVisible) {
											String percent = ((ImageAnnotation) annotation).ivs;
											int index = percent.indexOf("%");
											if (index >= 0) {
												percent = percent.substring(index - 3, index + 1).trim();
												if (percent.substring(0,1).equals("M")) percent = percent.substring(1).trim();
												nameLabel.setText(nameLabel.getText() + " " + percent);
											}
										}
										nameLabel.setTextAlignment(NSTextAlignment.Center);
										view.addSubview(nameLabel);
									}

									if (IOSMapHelper.CAN_SHOW_IMAGES || ((ImageAnnotation) annotation).isCustom)
										view.getLayer().setAnchorPoint(new CGPoint(0.5f, 0.5f));
									else view.getLayer().setAnchorPoint(new CGPoint(0.5f, 1.0f));
									((ImageAnnotation) annotation).initCallout();
									((ImageAnnotation) annotation).callout.setText(((ImageAnnotation) annotation).ivs);// + "Time not given");
									((ImageAnnotation) annotation).callout.setNumberOfLines(0);
								}

								return view;
							} else {
								return super.getAnnotationView(mapView, annotation);
							}
						} catch (Throwable e) {
							// Sometimes cancelling the scan can cause problems here
							e.printStackTrace();
							ConcurrentHashMap<String, String> extras = new ConcurrentHashMap<String, String>();
							extras.put("Aborted scan", Boolean.toString(mapHelper.isAbortScan()));
							ErrorReporter.logExceptionThreaded(e, extras);
							return super.getAnnotationView(mapView, annotation);
						}
					}

					@Override
					public void didAddAnnotationViews(MKMapView mapView, NSArray<MKAnnotationView> views) {
						try {
							int size = views.size();
							for (int n = 0; n < size; n++) {
								try {
									if (n >= views.size()) break;
									MKAnnotationView view = views.get(n);
									if (view == null) break;
									if (view.getAnnotation() instanceof ImageAnnotation) {
										ImageAnnotation image = (ImageAnnotation) view.getAnnotation();
										if (image.imagePath.equals("spawn_icon.png")) {
											view.getSuperview().sendSubviewToBack(view);
											view.setCanShowCallout(false);
											view.getLayer().setZPosition(-2);
											view.setUserInteractionEnabled(false);
										} else if (image.imagePath.equals("scan_point_icon.png")) {
											view.getLayer().setZPosition(-1.5);
										} else {
											view.getSuperview().bringSubviewToFront(view);

											try {
												if (image.ivs != null && !image.ivs.equals("")) {
													String percent = image.ivs;
													int index = percent.indexOf("%");
													if (index >= 0) {
														percent = percent.substring(index - 3, index + 1).trim();
														if (percent.substring(0, 1).equals("M"))
															percent = percent.substring(1).trim();
														percent = percent.substring(0, percent.length() - 1);
														view.getLayer().setZPosition((100 - Integer.parseInt(percent)) / -100.0);
													}
												}
											} catch (Exception e) {
												e.printStackTrace();
											}
										}

										if (view.getDetailCalloutAccessoryView() != null) {
											view.getDetailCalloutAccessoryView().getLayer().setZPosition(1);
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
									ErrorReporter.logExceptionThreaded(e);
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
						}
					}

					@Override
					public void didUpdateUserLocation(MKMapView mapView, MKUserLocation userLocation) {
						try {
							CLLocationCoordinate2D location = userLocation.getCoordinate();
							if (MapController.mapHelper.isLocationOverridden() == false) {
								MapController.mapHelper.moveMe(location.getLatitude(), location.getLongitude(), userLocation.getLocation().getAltitude(), firstLocationUpdate, false);
								firstLocationUpdate = false;
							}
							map.setShowsUserLocation(mapHelper.gpsModeNormal);
						} catch (Throwable e) {
							e.printStackTrace();
							ConcurrentHashMap<String, String> extras = new ConcurrentHashMap<>();
							extras.put("Fatal", "false");
							ErrorReporter.logExceptionThreaded(e, extras);
						}
						//if (mapHelper.gpsModeNormal) map.setUserTrackingMode(MKUserTrackingMode.FollowWithHeading);
						//else map.setUserTrackingMode(MKUserTrackingMode.None);
						//if (!MapController.mapHelper.isSearched()) MapController.mapHelper.wideScan();
					}

					@Override
					public void didSelectAnnotationView(MKMapView mapView, MKAnnotationView view) {
						try {
							if (view.getAnnotation() instanceof ImageAnnotation) {
								ImageAnnotation image = (ImageAnnotation) view.getAnnotation();
								if (!image.imagePath.equals("spawn_icon.png") && !image.imagePath.equals("scan_point_icon.png")) {
									view.getLayer().setZPosition(1);
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
						}
					}

					@Override
					public void didDeselectAnnotationView(MKMapView mapView, MKAnnotationView view) {
						try {
							if (view.getAnnotation() instanceof ImageAnnotation) {
								ImageAnnotation image = (ImageAnnotation) view.getAnnotation();
								if (!image.imagePath.equals("spawn_icon.png") && !image.imagePath.equals("scan_point_icon.png")) {
									try {
										if (image.ivs != null && !image.ivs.equals("")) {
											String percent = image.ivs;
											int index = percent.indexOf("%");
											if (index >= 0) {
												percent = percent.substring(index - 3, index + 1).trim();
												if (percent.substring(0, 1).equals("M"))
													percent = percent.substring(1).trim();
												percent = percent.substring(0, percent.length() - 1);
												view.getLayer().setZPosition((100 - Integer.parseInt(percent)) / -100.0);
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
						}
					}
				});
				map.addGestureRecognizer(gesture);

				MapController.mapHelper.setmMap(map);

				map.setZoomEnabled(true);

				NativePreferences.lock("first recovery load");
				boolean firstRecoveryLoad = NativePreferences.getBoolean(PREF_FIRST_RECOVERY_LOAD, true);
				NativePreferences.unlock();

				final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
				if (!prefs.get().isEmpty() && firstRecoveryLoad && false) {
					NativePreferences.lock("recovering old prefs");
					//NativePreferences.copyPrefs(prefs);
					if (prefs.contains(PREF_FIRST_LOAD)) NativePreferences.putBoolean(PREF_FIRST_LOAD, prefs.getBoolean(PREF_FIRST_LOAD));
					if (prefs.contains(PREF_FIRST_MULTIACCOUNT_LOAD)) NativePreferences.putBoolean(PREF_FIRST_MULTIACCOUNT_LOAD, prefs.getBoolean(PREF_FIRST_MULTIACCOUNT_LOAD));
					if (prefs.contains(PREF_ASK_FOR_REMOVE_ADS)) NativePreferences.putBoolean(PREF_ASK_FOR_REMOVE_ADS, prefs.getBoolean(PREF_ASK_FOR_REMOVE_ADS));
					if (prefs.contains(PREF_ASK_FOR_SUPPORT)) NativePreferences.putBoolean(PREF_ASK_FOR_SUPPORT, prefs.getBoolean(PREF_ASK_FOR_SUPPORT));
					if (prefs.contains(PREF_APP_LOADS)) NativePreferences.putInteger(PREF_APP_LOADS, prefs.getInteger(PREF_APP_LOADS));
					if (prefs.contains(PREF_COLLECT_SPAWNS)) NativePreferences.putBoolean(PREF_COLLECT_SPAWNS, prefs.getBoolean(PREF_COLLECT_SPAWNS));
					if (prefs.contains(PREF_SHOW_IVS)) NativePreferences.putBoolean(PREF_SHOW_IVS, prefs.getBoolean(PREF_SHOW_IVS));
					if (prefs.contains(PREF_SHOW_SPAWNS)) NativePreferences.putBoolean(PREF_SHOW_SPAWNS, prefs.getBoolean(PREF_SHOW_SPAWNS));
					if (prefs.contains(PREF_CAPTCHA_MODE_POPUP)) NativePreferences.putBoolean(PREF_CAPTCHA_MODE_POPUP, prefs.getBoolean(PREF_CAPTCHA_MODE_POPUP));
					if (prefs.contains(PREF_SCAN_DISTANCE)) NativePreferences.putInteger(PREF_SCAN_DISTANCE, prefs.getInteger(PREF_SCAN_DISTANCE));
					if (prefs.contains(PREF_SCAN_TIME)) NativePreferences.putInteger(PREF_SCAN_TIME, prefs.getInteger(PREF_SCAN_TIME));
					if (prefs.contains(PREF_SCAN_SPEED)) NativePreferences.putInteger(PREF_SCAN_SPEED, prefs.getInteger(PREF_SCAN_SPEED));
					if (prefs.contains(PREF_TOKEN)) NativePreferences.putString(PREF_TOKEN, prefs.getString(PREF_TOKEN));
					if (prefs.contains(PREF_USERNAME)) NativePreferences.putString(PREF_USERNAME, prefs.getString(PREF_USERNAME));
					if (prefs.contains(PREF_PASSWORD)) NativePreferences.putString(PREF_PASSWORD, prefs.getString(PREF_PASSWORD));
					if (prefs.contains(PREF_TOKEN2)) NativePreferences.putString(PREF_TOKEN2, prefs.getString(PREF_TOKEN2));
					if (prefs.contains(PREF_USERNAME2)) NativePreferences.putString(PREF_USERNAME2, prefs.getString(PREF_USERNAME2));
					if (prefs.contains(PREF_PASSWORD2)) NativePreferences.putString(PREF_PASSWORD2, prefs.getString(PREF_PASSWORD2));
					if (prefs.contains(PREF_FILTER_STRING)) NativePreferences.putString(PREF_FILTER_STRING, prefs.getString(PREF_FILTER_STRING));
					if (prefs.contains("SpawnStorage")) NativePreferences.putString("SpawnStorage", prefs.getString("SpawnStorage"));
					if (prefs.contains(PREF_MIN_ATTACK)) NativePreferences.putInteger(PREF_MIN_ATTACK, prefs.getInteger(PREF_MIN_ATTACK));
					if (prefs.contains(PREF_MIN_DEFENSE)) NativePreferences.putInteger(PREF_MIN_DEFENSE, prefs.getInteger(PREF_MIN_DEFENSE));
					if (prefs.contains(PREF_MIN_STAMINA)) NativePreferences.putInteger(PREF_MIN_STAMINA, prefs.getInteger(PREF_MIN_STAMINA));
					if (prefs.contains(PREF_MIN_PERCENT)) NativePreferences.putInteger(PREF_MIN_PERCENT, prefs.getInteger(PREF_MIN_PERCENT));
					if (prefs.contains(PREF_MIN_SCAN_TIME)) NativePreferences.putInteger(PREF_MIN_SCAN_TIME, prefs.getInteger(PREF_MIN_SCAN_TIME));
					if (prefs.contains(PREF_MAX_SCAN_DISTANCE)) NativePreferences.putInteger(PREF_MAX_SCAN_DISTANCE, prefs.getInteger(PREF_MAX_SCAN_DISTANCE));
					if (prefs.contains(AccountManager.PREF_NUM_ACCOUNTS)) NativePreferences.putInteger(AccountManager.PREF_NUM_ACCOUNTS, prefs.getInteger(AccountManager.PREF_NUM_ACCOUNTS));

					NativePreferences.putBoolean(PREF_FIRST_RECOVERY_LOAD, false);

					int numAccounts = NativePreferences.getInteger(AccountManager.PREF_NUM_ACCOUNTS, 0);
					if (numAccounts > 0) {
						for (int n = 1; n <= numAccounts; n++) {
							String suffix = n == 1 ? "" : (n + "");
							NativePreferences.putString(AccountManager.PREF_USERNAME_PREFIX + suffix, prefs.getString(AccountManager.PREF_USERNAME_PREFIX + suffix));
							NativePreferences.putString(AccountManager.PREF_PASSWORD_PREFIX + suffix, prefs.getString(AccountManager.PREF_PASSWORD_PREFIX + suffix));
							NativePreferences.putString(AccountManager.PREF_TOKEN_PREFIX + suffix, prefs.getString(AccountManager.PREF_TOKEN_PREFIX + suffix));
						}
					}

					NativePreferences.unlock();
				}

				NativePreferences.lock("first load");
				boolean first = NativePreferences.getBoolean(IOSFeatures.PREF_FIRST_LOAD, true);
				NativePreferences.unlock();

				if (first)
					firstLoad();
				else
					initLocation();

				NativePreferences.lock("first multiaccount and copyright load");
				boolean multi = NativePreferences.getBoolean(PREF_FIRST_MULTIACCOUNT_LOAD, true);
				boolean copyright = NativePreferences.getBoolean(PREF_FIRST_COPYRIGHT_LOAD, true);
				NativePreferences.unlock();

				if (multi) {
					firstMultiAccountLoad();
				}

				if (copyright) {
					firstCopyrightLoad();
				}

				NativePreferences.printLockStatus("MapController before first refreshPrefs");
				mapHelper.refreshPrefs();
				NativePreferences.printLockStatus("MapController after first refreshPrefs");

				NativePreferences.printLockStatus("MapController before loadSpawns");
				mapHelper.loadSpawns();
				NativePreferences.printLockStatus("MapController after loadSpawns");

				NativePreferences.printLockStatus("MapController before loadFilter");
				features.loadFilter();
				NativePreferences.printLockStatus("MapController after loadFilter");

				features.loadFilterOverrides();

				features.loadCustomImageUrls();

				NativePreferences.printLockStatus("MapController before begging");
				NativePreferences.lock();
				canAskForSupport = NativePreferences.getBoolean(PREF_ASK_FOR_SUPPORT, true);
				canAskForRemoveAds = NativePreferences.getBoolean(PREF_ASK_FOR_REMOVE_ADS, true);
				appLoads = NativePreferences.getInteger(PREF_APP_LOADS, 0);
				if (appLoads < TARGET_APP_LOADS || appLoads < TARGET_APP_LOADS_REMOVE_ADS) {
					appLoads++;
					NativePreferences.putInteger(PREF_APP_LOADS, appLoads);
				}

				NativePreferences.unlock();
				NativePreferences.printLockStatus("MapController after begging");

				justCreated = true;

				isLoaded = true;

				Thread runnable = new Thread() {
					@Override
					public void run() {
						try {
							if (dontRefreshAccounts) {
								dontRefreshAccounts = false;
								return;
							}

							if (!mapHelper.scanning) {
								if (AccountManager.accounts == null) {
									features.login();
								} else {
									AccountManager.tryTalkingToServer();
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logException(t);
						}
					}
				};
				runnable.start();
			} else {
				IOSLauncher.instance.resumeMediation();
				AccountManager.loginErrorAccounts();
			}

			NativePreferences.printLockStatus("MapController before second refreshPrefs");
			mapHelper.refreshPrefs();
			NativePreferences.printLockStatus("MapController after second refreshPrefs");

			map.setShowsUserLocation(mapHelper.gpsModeNormal);
			//if (mapHelper.gpsModeNormal) map.setUserTrackingMode(MKUserTrackingMode.FollowWithHeading);
			//else map.setUserTrackingMode(MKUserTrackingMode.None);

			// resumeAds();
			System.out.println("PokeFinderActivity.onResume()");
			NativePreferences.printLockStatus("MapController before distance and speed");
			NativePreferences.lock();
			MapController.mapHelper.setScanDistance(
					NativePreferences.getInteger(PREF_SCAN_DISTANCE, MapHelper.DEFAULT_SCAN_DISTANCE));

			MapController.mapHelper
					.setScanSpeed(NativePreferences.getInteger(PREF_SCAN_SPEED, MapHelper.DEFAULT_SCAN_SPEED));

			NativePreferences.unlock();
			NativePreferences.printLockStatus("MapController after distance and speed");

			if (MapController.mapHelper.getScanDistance() > MapHelper.MAX_SCAN_DISTANCE)
				MapController.mapHelper.setScanDistance(MapHelper.MAX_SCAN_DISTANCE);

			if (MapHelper.maxScanSpeed != 0 && MapController.mapHelper.getScanSpeed() > MapHelper.maxScanSpeed)
				MapController.mapHelper.setScanSpeed(MapHelper.maxScanSpeed);

			MapController.mapHelper.startCountdownTimer();

			if (!justCreated && appLoads >= TARGET_APP_LOADS && canAskForSupport) askForSupport();
			if (!justCreated && appLoads >= TARGET_APP_LOADS_REMOVE_ADS && canAskForRemoveAds) askForRemoveAds();
			if (justCreated) justCreated = false;
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public void firstMultiAccountLoad() {
		NativePreferences.lock("actual multiaccount load");
		try {
			// Handle the discrepancies between this version and the last version
			if (!NativePreferences.getString(PREF_USERNAME, "").equals("")) {
				if (!NativePreferences.getString(PREF_USERNAME2, "").equals("")) {
					NativePreferences.putInteger(AccountManager.PREF_NUM_ACCOUNTS, 2);
				} else {
					NativePreferences.putInteger(AccountManager.PREF_NUM_ACCOUNTS, 1);
				}
			}

			NativePreferences.putBoolean(PREF_FIRST_MULTIACCOUNT_LOAD, false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		NativePreferences.unlock();
	}

	public void firstCopyrightLoad() {
		try {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					NativePreferences.lock("actual first copyright load");
					NativePreferences.putBoolean(PREF_FIRST_COPYRIGHT_LOAD, false);
					NativePreferences.unlock();
				}
			};

			DialogHelper.messageBox("No More Images","Upon request of The Pokemon Company, I have removed all Pokemon images from the app. I replaced them with the Pokemon names. You can add your own custom images the Settings menu, but don't use any images that you don't have the rights to use.", "Got It!", runnable).build().show();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void askForSupport() {
		Runnable positiveRunnable = new Runnable() {
			public void run() {
				Gdx.net.openURI("https://itunes.apple.com/us/developer/patrick-ballard/id1026470545");
			}
		};

		DialogHelper.yesNoBox("Thank you!", "Thanks for using PokeSensor! Please show your support for Logick LLC by checking out my other free apps. All downloads help me out, even if you don't keep the app. Thanks!", "Show me more!", positiveRunnable, "No thanks", null).build().show();

		NativePreferences.lock("asking for support");
		NativePreferences.putBoolean(PREF_ASK_FOR_SUPPORT, false);
		NativePreferences.unlock();

		canAskForSupport = false;
	}

	public void askForRemoveAds() {
		Runnable positiveRunnable = new Runnable() {
			public void run() {
				removeAds();
			}
		};

		DialogHelper.yesNoBox("Tired of Ads?", "Thanks for using PokeSensor! Would you like to remove ads to help support me as an independent developer?", "Remove Ads ($1.99)", positiveRunnable, "No thanks", null).build().show();

		NativePreferences.lock("asking for remove ads");
		NativePreferences.putBoolean(PREF_ASK_FOR_REMOVE_ADS, false);
		NativePreferences.unlock();

		canAskForRemoveAds = false;
	}

	@Override
	public void viewWillDisappear(boolean animated) {
		try {
			NativePreferences.printLockStatus("MapController willDisappear before saveSpawns");
			MapController.mapHelper.stopCountdownTimer();
			IOSLauncher.instance.pauseMediation();
			mapHelper.saveSpawns();
			NativePreferences.printLockStatus("MapController willDisappear after saveSpawns");
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	@Override
	public void viewDidAppear(boolean b) {
		try {
			map.setShowsUserLocation(mapHelper.gpsModeNormal);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
		//if (mapHelper.gpsModeNormal) map.setUserTrackingMode(MKUserTrackingMode.FollowWithHeading);
		//else map.setUserTrackingMode(MKUserTrackingMode.None);
	}

	public void tryFetchingMessages() {
		if (!didFetchMessages) {
			didFetchMessages = true;
			Thread thread = new Thread() {
				public void run() {
					Messenger.fetchMessages();
				}
			};
			thread.start();
		}
	}

	public void firstLoad() {
		NativePreferences.lock("actual first load");
		NativePreferences.putBoolean(IOSFeatures.PREF_FIRST_LOAD, false);
		NativePreferences.unlock();

		Runnable r = new Runnable() {
			@Override
			public void run() {
				Runnable negative = new Runnable() {
					public void run() {
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								initLocation();
							}
						};
						features.runOnMainThread(runnable);
					}
				};
				Runnable positive = new Runnable() {
					@Override
					public void run() {
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								initLocation();
							}
						};
						features.runOnMainThread(runnable);
						Gdx.net.openURI("https://twitter.com/LogickLLC");
					}
				};
				DialogHelper.yesNoBox(R.string.welcomeTitle, R.string.welcomeMessage, R.string.getStarted, negative, "Go to Twitter", positive).build().show();
			}
		};
		MapController.features.runOnMainThread(r);
	}

	public void initLocation() {
		try {
			if (CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Denied
					&& CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Restricted) {
				return;
			} else {
				map.setShowsUserLocation(true);
				CLLocationCoordinate2D loc = map.getUserLocation().getCoordinate();
				double altitude = 0;
				try {
					altitude = map.getUserLocation().getLocation().getAltitude();
				} catch (Exception e) {
					//ErrorReporter.logExceptionThreaded(e);
					e.printStackTrace();
				}
				MapController.mapHelper.moveMe(loc.getLatitude(), loc.getLongitude(), altitude, true, true);
				MapController.mapHelper.setLocationInitialized(true);

				Timer timer = new Timer();
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						Runnable runnable = new Runnable() {
							public void run() {
								//if (!MapController.mapHelper.isSearched()) MapController.mapHelper.wideScan();
							}
						};
						features.runOnMainThread(runnable);
					}
				};
				//timer.schedule(task, 2000);

				System.out.println("Location initialized. Trying to scan in 2 seconds...");
			}
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logExceptionThreaded(t);
		}
	}

	public void deniedLocationPermission() {
		try {
			MapController.features.longMessage("Location permissions denied. Please go to Settings > Privacy > Location Services and enable them for this app.");
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public void setMessage(String messageText) {
		message.setText(messageText);
	}

	public boolean isMessageShowing() {
		return messageShowing;
	}

	public synchronized void showMessageFor(final String messageText, final long millis) {
		if (messageShowing) {
			messages.put(messageText, millis);
			return;
		}

		messageShowing = true;
		Runnable showRunnable = new Runnable() {
			public void run() {
				message.setText(messageText);
				refreshMessageContainerHeight();
				messageContainer.setHidden(false);
			}
		};
		features.runOnMainThread(showRunnable);

		final Runnable hideRunnable = new Runnable() {
			public void run() {
				// TODO Add a fade here so it isn't so abrupt
				messageContainer.setHidden(true);
			}
		};

		final Timer timer = new Timer();
		final TimerTask task = new TimerTask() {

			@Override
			public void run() {
				if (!messages.isEmpty()) {
					features.runOnMainThread(nextRunnable);
				}
				features.runOnMainThread(hideRunnable);
				messageShowing = false;
			}

		};
		timer.schedule(task, millis);

		nextRunnable = new Runnable() {
			public void run() {
				if (messages.isEmpty()) {
					messageShowing = false;
					return;
				} else {
					String text = messages.keys().nextElement();
					long time = messages.remove(text);
					refreshMessageContainerHeight();
					messageContainer.setHidden(false);
					message.setText(text);
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							if (!messages.isEmpty()) {
								features.runOnMainThread(nextRunnable);
							}
							features.runOnMainThread(hideRunnable);
							messageShowing = false;
						}
					}, time);
				}
			}
		};
	}

	public void refreshMessageContainerHeight() {
		try {
			messageContainerHeight.setConstant(IOSLauncher.instance.getSize().y);
			goodAccountsLabelHeight.setConstant(IOSLauncher.instance.getSize().y);
			rpmLabelHeight.setConstant(IOSLauncher.instance.getSize().y);
			rpmCountLabelHeight.setConstant(IOSLauncher.instance.getSize().y);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public void requestLocationPermission() {
		manager.requestWhenInUseAuthorization();
	}

	@Override
	public void willRotate(UIInterfaceOrientation arg0, double arg1) {
		super.willRotate(arg0, arg1);
		IOSLauncher.instance.hideBanner();
	}

	@Override
	public void didRotate(UIInterfaceOrientation fromInterfaceOrientation) {
		super.didRotate(fromInterfaceOrientation);
		System.out.println("Did update to " + fromInterfaceOrientation);
		if (fromInterfaceOrientation == UIInterfaceOrientation.Portrait || fromInterfaceOrientation == UIInterfaceOrientation.PortraitUpsideDown)
			IOSLauncher.instance.changeOrientation(false);
		else
			IOSLauncher.instance.changeOrientation(true);
	}

	public void setupSearch() {
		try {
			search = new UISearchController(searchResultsTable);
			search.setDelegate(new UISearchControllerDelegateAdapter() {

				@Override
				public void didDismiss(UISearchController searchController) {
					super.didDismiss(searchController);
				}

			});

			search.setSearchResultsUpdater(searchResultsTable);

			search.setHidesNavigationBarDuringPresentation(false);
			search.setDimsBackgroundDuringPresentation(true);
			setDefinesPresentationContext(true);

			search.getSearchBar().sizeToFit();
			search.getSearchBar().setPlaceholder("Search for location");

		/*try {
			if (search.getPopoverPresentationController() != null) {
				search.getPopoverPresentationController().setSourceView(map);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
		}*/

			//this.getNavigationController().getNavigationItem().setTitleView(search.getSearchBar());
			this.getNavigationItem().setTitleView(search.getSearchBar());
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public void updateGoodAccountsLabelText() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					goodAccountsView.setText(AccountManager.getGoodAccounts().size() + "/" + AccountManager.getNumAccounts());
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void resetGoodAccountsLabelText() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					goodAccountsView.setText("0/" + AccountManager.getNumAccounts());
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void hideGoodAccountsLabel() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					goodAccountsView.setHidden(true);
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void updateRpmLabelText() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					rpmView.setText((PokeHashProvider.rpm - PokeHashProvider.requestsRemaining) + "/" + PokeHashProvider.rpm + " rpm");
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void resetRpmLabelText() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					rpmView.setText("0/0 rpm");
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void showRpmLabel() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					rpmView.setHidden(false);
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void hideRpmLabel() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					rpmView.setHidden(true);
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void updateRpmCountLabelText() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					long timeLeft = (PokeHashProvider.rpmTimeLeft - Calendar.getInstance().getTime().getTime() / 1000);
					if (timeLeft < 0) timeLeft = 0;
					rpmCountView.setText(timeLeft + "s");
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void resetRpmCountLabelText() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					rpmCountView.setText("0s");
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void showRpmCountLabel() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					rpmCountView.setHidden(false);
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public void hideRpmCountLabel() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					rpmCountView.setHidden(true);
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logExceptionThreaded(t);
				}
			}
		};
		features.runOnMainThread(runnable);
	}

	public CGRect getImageSize() {
		switch ((int) mapHelper.imageSize) {
			case 0:
				return new CGRect(CGPoint.Zero(), new CGSize(32, 32));

			case 1:
				return new CGRect(CGPoint.Zero(), new CGSize(64, 64));

			case 2:
				return new CGRect(CGPoint.Zero(), new CGSize(96, 96));

			case 3:
				return new CGRect(CGPoint.Zero(), new CGSize(128, 128));

			default:
				return new CGRect(CGPoint.Zero(), new CGSize(96, 96));
		}
	}

	public void tryTalkingToServer() {
		Thread runnable = new Thread() {
			@Override
			public void run() {
				try {
					if (!mapHelper.scanning) {
						if (AccountManager.accounts == null) {
							features.login();
						} else {
							AccountManager.tryTalkingToServer();
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logException(t);
				}
			}
		};
		runnable.start();
	}

	public int getAppVersion() {
		try {
			return Integer.parseInt(NSBundle.getMainBundle().getInfoDictionary().get(new NSString("CFBundleVersion")).toString());
		} catch (Exception e) {
			return 0;
		}
	}

	// IAP Stuff

	public void setupIAP() {
		try {
			ArrayList<IAPOffer> offers = new ArrayList<IAPOffer>();

			offers.add(IAPHelper.createEntitlementOffer(REMOVE_ADS_IAP_PRODUCT_ID, new Lambda() {

				@Override
				public void execute() {
					AdHelper.removeAds(PREFS_NAME);

					Runnable r = new Runnable() {
						@Override
						public void run() {
							Runnable runnable = new Runnable() {
								@Override
								public void run() {
									IOSLauncher.instance.removeAds();
									MapController.instance.refreshMessageContainerHeight();
								}
							};
							features.runOnMainThread(runnable);
						}
					};

					Gdx.app.postRunnable(r);
				}

			}));

			IAPHelper.init(offers);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public void removeAds() {
		setupIAP();
		purchaseRemoveAds();
	}

	public void restorePurchases() {
		setupIAP();
		IAPHelper.restorePurchases();
	}

	public void purchaseRemoveAds() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					while (IAPHelper.isInstalled() == null) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (IAPHelper.isInstalled()) {
						//if (Utilities.isAndroid() && Utilities.isTesting()) IAPHelper.purchase(IAPHelper.TEST_GOOGLE_PURCHASE_SUCCESSFUL_ID);
						IAPHelper.purchase(REMOVE_ADS_IAP_PRODUCT_ID);
					}
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logException(t);
				}
			}
		};
		thread.start();
	}

	// END IAP Stuff

	// Aerserv delegate callbacks

	@Override
	public UIViewController getViewControllerForPresentingModalView() {
		return MapController.instance;
	}

	@Override
	public void adViewDidLoadAd(ASAdView asAdView) {
		try {
			IOSLauncher.instance.aerservLoaded = true;
			asAdView.setOutlineAd(false);
			IOSLauncher.instance.mediator.onBannerLoad();
			//IOSLauncher.instance.size = new Vector2((float) IOSLauncher.instance.aerservBanner.getBounds().getWidth(), (float) IOSLauncher.instance.aerservBanner.getBounds().getHeight());
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					MapController.instance.refreshMessageContainerHeight();
				}
			};
			IOSLauncher.instance.postNativeRunnable(runnable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void adViewDidFailToLoadAd(ASAdView asAdView, NSError nsError) {
		try {
			IOSLauncher.instance.aerservFailed = true;
			IOSLauncher.instance.mediator.onBannerFail();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void adViewDidPreloadAd(ASAdView asAdView) {

	}

	@Override
	public void willPresentModalViewForAd(ASAdView asAdView) {

	}

	@Override
	public void didDismissModalViewForAd(ASAdView asAdView) {

	}

	@Override
	public void willLeaveApplicatonFromAd(ASAdView asAdView) {

	}

	@Override
	public void adViewDidCompletePlayingWithVastAd(ASAdView asAdView) {

	}

	@Override
	public void adSizedChanged(ASAdView asAdView) {
		if (IOSLauncher.IS_AD_TESTING) System.out.println("Aerserv banner changed size!");
	}

	@Override
	public void adWasClicked(ASAdView asAdView) {

	}

	@Override
	public void didFireAdvertiserEventWithMessage(ASAdView asAdView, String s) {

	}

	@Override
	public void didShowAdWithTransactionInfo(ASAdView asAdView, NSDictionary nsDictionary) {

	}
}
