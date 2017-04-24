package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.MapHelper;
import com.logickllc.pokesensor.api.Messenger;
import com.pokegoapi.api.settings.templates.TempFileTemplateProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.Mapped;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;
import org.moe.natj.objc.map.ObjCObjectMapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import apple.coregraphics.c.CoreGraphics;
import apple.coregraphics.struct.CGPoint;
import apple.coregraphics.struct.CGRect;
import apple.coregraphics.struct.CGSize;
import apple.corelocation.CLLocation;
import apple.corelocation.CLLocationManager;
import apple.corelocation.c.CoreLocation;
import apple.corelocation.enums.CLAuthorizationStatus;
import apple.corelocation.protocol.CLLocationManagerDelegate;
import apple.corelocation.struct.CLLocationCoordinate2D;
import apple.foundation.NSArray;
import apple.foundation.NSBundle;
import apple.foundation.NSProcessInfo;
import apple.mapkit.MKAnnotationView;
import apple.mapkit.MKCircle;
import apple.mapkit.MKCircleRenderer;
import apple.mapkit.MKMapView;
import apple.mapkit.MKOverlayRenderer;
import apple.mapkit.MKUserLocation;
import apple.mapkit.protocol.MKAnnotation;
import apple.mapkit.protocol.MKMapViewDelegate;
import apple.uikit.NSLayoutConstraint;
import apple.uikit.UIBarButtonItem;
import apple.uikit.UIColor;
import apple.uikit.UIControl;
import apple.uikit.UIFont;
import apple.uikit.UIGestureRecognizer;
import apple.uikit.UIImage;
import apple.uikit.UILabel;
import apple.uikit.UILongPressGestureRecognizer;
import apple.uikit.UIProgressView;
import apple.uikit.UISearchController;
import apple.uikit.UITextField;
import apple.uikit.UITextView;
import apple.uikit.UIToolbar;
import apple.uikit.UIView;
import apple.uikit.UIViewController;
import apple.uikit.c.UIKit;
import apple.uikit.enums.NSTextAlignment;
import apple.uikit.enums.UIBarButtonSystemItem;
import apple.uikit.enums.UIGestureRecognizerState;
import apple.uikit.protocol.UIGestureRecognizerDelegate;
import apple.uikit.protocol.UISearchControllerDelegate;
import apple.uikit.struct.UIEdgeInsets;

import static com.logickllc.pokesensor.AdHelper.PREF_SHOW_ADS;
import static com.logickllc.pokesensor.IOSFeatures.PREFS_NAME;
import static com.logickllc.pokesensor.IOSFeatures.PREF_FIRST_LOAD;
import static com.logickllc.pokesensor.IOSFeatures.PREF_USERNAME;
import static com.logickllc.pokesensor.IOSFeatures.PREF_USERNAME2;
import static com.logickllc.pokesensor.IOSFeatures.REMOVE_ADS_IAP_PRODUCT_ID;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_SCAN_DISTANCE;
import static com.logickllc.pokesensor.IOSMapHelper.PREF_SCAN_SPEED;

@Runtime(ObjCRuntime.class)
@ObjCClassName("MapController")
@RegisterOnStartup
public class MapController extends UIViewController {
	//@IBOutlet
	MKMapView map;

	//@IBOutlet
	UIProgressView scanBar;

	//@IBOutlet
	UILabel scanText;

	//@IBOutlet
    ClickableView scanView;

	//@IBOutlet
	public UILabel message;

	//@IBOutlet
	UIView messageContainer;

	//@IBOutlet
	NSLayoutConstraint messageContainerHeight, goodAccountsLabelHeight, rpmLabelHeight, rpmCountLabelHeight;

	//@IBOutlet
	GoodAccountsView goodAccountsView;

	//@IBOutlet
	RpmView rpmView;

	//@IBOutlet
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
	public final String PREF_FIRST_PREF_LOAD = "FirstPrefLoad";
	public boolean canAskForSupport = true;
	public boolean canAskForRemoveAds = true;
	public int appLoads = 0;
	public final int TARGET_APP_LOADS = 10;
	public final int TARGET_APP_LOADS_REMOVE_ADS = 4;
	public boolean justCreated = true;
	public boolean canDecode = false;
	public boolean didFetchMessages = false;

	public boolean inBackground = false;
	public long lastScanTime = System.currentTimeMillis();
	public boolean alwaysLocationAllowed = false;

	public boolean dontRefreshAccounts = false;
	public final String PREF_FIRST_RECOVERY_LOAD = "FirstRecoveryLoad";

	private long lastAccountRetryTime = System.currentTimeMillis();
	private final long ACCOUNT_RETRY_TIME = 1800000;

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

	public boolean SHOW_MAP_DIAGNOSTICS = true;

	protected MapController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewDidLoad() {
		try {
			searchResultsTable = (SearchTableController) this.storyboard().instantiateViewControllerWithIdentifier("SearchTable");
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
			Stopwatch.setEnabled(IOSLauncher.IS_AD_TESTING);
			Stopwatch.filter = "main";
			Stopwatch.minTime = 3;

			Stopwatch.readPreviousTimes();


			this.navigationController().setNavigationBarHidden(false);
			this.navigationController().setToolbarHiddenAnimated(false, true);

			if (!isLoaded) {
				//this.addStrongRef(map);
				AccountManager.accounts = null;
				message.setLayoutMargins(new UIEdgeInsets(5, 5, 5, 5));
				instance = this;

				mapHelper = new IOSMapHelper();
				features = new IOSFeatures();

				features.setMapHelper(mapHelper);
				mapHelper.setFeatures(features);

				TempFileTemplateProvider.tempDirectory = Gdx.files.local("/").file().getAbsolutePath();

				features.print("PokeFinder", "Settings temp directory to " + TempFileTemplateProvider.tempDirectory);

				Thread adThread = new Thread() {
					public void run() {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							ErrorReporter.logExceptionThreaded(e);
						}

						NativePreferences.lock();
						final boolean showAds = NativePreferences.getBoolean(PREF_SHOW_ADS, true);
						NativePreferences.unlock();

						Runnable adRunnable = new Runnable() {
							@Override
							public void run() {
								try {
									AdHelper.setShowAds(false);

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

				manager = CLLocationManager.alloc().init();

				requestLocationPermission();

				//this.addStrongRef(manager);

				manager.setDelegate(new CLLocationManagerDelegate() {

					@Override
					public void locationManagerDidChangeAuthorizationStatus(CLLocationManager manager, int status) {
						try {
							if (status == CLAuthorizationStatus.AuthorizedWhenInUse || status == CLAuthorizationStatus.AuthorizedAlways) {
								Runnable runnable = new Runnable() {
									@Override
									public void run() {
										initLocation();
									}
								};
								features.runOnMainThread(runnable);

								if (status == CLAuthorizationStatus.AuthorizedAlways) {
									alwaysLocationAllowed = true;
									features.print("PokeFinder","Got always location authorization!");
								} else {
									alwaysLocationAllowed = false;
									features.print("PokeFinder","Got in use location authorization!");
								}
							} else {
								features.print("PokeFinder","Location authorization conditional failed");
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logException(t);
						}
					}

					@Override
					public void locationManagerDidUpdateLocations(CLLocationManager manager, NSArray<? extends CLLocation> locations) {
						features.print("PokeFinder", "Received background location update");

						if (inBackground && mapHelper.backgroundScanning) {
							long currentTime = System.currentTimeMillis();
							long interval;
							try {
								interval = Integer.parseInt(mapHelper.backgroundInterval) * 60000;
							} catch (Throwable e) {
								e.printStackTrace();
								ErrorReporter.logExceptionThreaded(e);
								interval = 15 * 60000;
							}

							if (currentTime - lastScanTime >= interval) {
								CLLocation myLocation = locations.lastObject();
								mapHelper.moveMe(myLocation.coordinate().latitude(), myLocation.coordinate().longitude(), 0, true, false);
								if (!mapHelper.onlyScanSpawns) mapHelper.wideScanBackground();
								else mapHelper.wideSpawnScan(true);
							}
						}
						manager.allowDeferredLocationUpdatesUntilTraveledTimeout(CoreLocation.CLLocationDistanceMax(), 61); // wait 61s for another update
					}
				});

				final UILongPressGestureRecognizer gesture = UILongPressGestureRecognizer.alloc().initWithTargetAction(this, new SEL("handleMapGesture:"));

				gesture.setDelegate(new UIGestureRecognizerDelegate() {
					@Override
					public boolean gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer(UIGestureRecognizer gestureRecognizer, UIGestureRecognizer otherGestureRecognizer) {
						return true;
					}
				});

				gesture.setMinimumPressDuration(0.7);

				map.setDelegate(new MKMapViewDelegate() {

					@Override
					public void mapViewAnnotationViewCalloutAccessoryControlTapped(MKMapView mapView, MKAnnotationView view, UIControl control) {
						try {
							if (view.annotation() instanceof ImageAnnotation) {
								((ImageAnnotation) view.annotation()).control = control;
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
						}
					}

					@Override
					public MKOverlayRenderer mapViewRendererForOverlay(MKMapView mapView, @Mapped(ObjCObjectMapper.class) Object overlay) {
						Stopwatch.click("mapViewRendererForOverlay");
						try {
							if (SHOW_MAP_DIAGNOSTICS) features.print("Map Diagnostics","Entered mapViewRendererForOverlay");
							if (overlay instanceof MKCircle || overlay instanceof CustomCircle) {
								if (overlay instanceof CustomCircle) {
									features.print("PokeFinder", "Rendering as a CustomCircle");
									CustomCircle circle = (CustomCircle) overlay;
									if (circle.renderer == null) circle.renderer = MKCircleRenderer.alloc().initWithCircle(circle.circle);
									circle.renderer.setStrokeColor(circle.strokeColor);
									circle.renderer.setLineWidth(circle.lineWidth);
									circle.renderer.setFillColor(circle.fillColor);
									//circle.renderer.setStrokeColor(UIColor.blueColor());
									//circle.renderer.setFillColor(UIColor.clearColor());
									Stopwatch.click("mapViewRendererForOverlay");
									return circle.renderer;
								} else {
									features.print("PokeFinder", "Rendering as an MKCircle");
									MKCircleRenderer renderer = MKCircleRenderer.alloc().initWithCircle((MKCircle) overlay);
									renderer.setStrokeColor(UIColor.blueColor());
									renderer.setLineWidth(1);
									renderer.setFillColor(UIColor.clearColor());

									Stopwatch.click("mapViewRendererForOverlay");
									return renderer;
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
						}
						features.print("PokeFinder", "Falling back to our default circle renderer. Not good...");
						Stopwatch.click("mapViewRendererForOverlay");
						return MKCircleRenderer.alloc().init();
					}

					@Override
					public MKAnnotationView mapViewViewForAnnotation(MKMapView mapView, @Mapped(ObjCObjectMapper.class) Object annotationObject) {
						Stopwatch.click("mapViewViewForAnnotation");
						try {
							if (SHOW_MAP_DIAGNOSTICS) features.print("Map Diagnostics","Entered mapViewViewForAnnotation");
							MKAnnotation annotationMk = (MKAnnotation) annotationObject;
							if (annotationMk instanceof ImageAnnotation) {
								ImageAnnotation annotation = (ImageAnnotation) annotationMk;
								if (annotation.view != null) {
									Stopwatch.click("mapViewViewForAnnotation");
									return annotation.view;
								}

								MKAnnotationView view = MKAnnotationView.alloc().initWithAnnotationReuseIdentifier(annotation,
										annotation.imagePath);
								if (annotation.isCustom)
									view.setImage(UIImage.imageWithContentsOfFile(Gdx.files.local(annotation.imagePath).file().getAbsolutePath()));
								else
									view.setImage(UIImage.imageNamed(annotation.imagePath));

								view.setCanShowCallout(true);
								annotation.view = view;
								if (annotation.imagePath.equals("scan_point_icon.png")) {
									if (mapHelper.showScanDetails) view.setAlpha(0.5);
									view.layer().setAnchorPoint(new CGPoint(0.32f, 0.32f));
								}
								else if (annotation.imagePath.equals("spawn_icon.png")) {
									view.layer().setAnchorPoint(new CGPoint(0.5f, 0.5f));
									annotation.view.layer().setOpacity(0.5f);
									annotation.view.setUserInteractionEnabled(false);
									annotation.view.setCanShowCallout(false);
								} else {
									if (annotation.isCustom) {
										UILabel ivLabel = null;
										UIImage ivBox = UIImage.imageNamed(IOSMapHelper.NUMBER_MARKER_FOLDER + IV_IMAGE);
										if (mapHelper.showIvs && mapHelper.ivsAlwaysVisible) {
											double labelWidth = ivBox.size().width(); // * 0.88;
											double labelHeight = ivBox.size().height(); // * 0.72;
											double labelX = 0; //ivBox.getSize().getWidth() * 0.11;
											double labelY = 0; //ivBox.getSize().getHeight() * 0.20;

											String percent = annotation.ivs;
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

												ivLabel = UILabel.alloc().initWithFrame(new CGRect(new CGPoint(labelX, labelY), new CGSize(labelWidth, labelHeight)));
												ivLabel.setNumberOfLines(1);
												ivLabel.setAdjustsFontSizeToFitWidth(true);
												ivLabel.setTextColor(UIColor.whiteColor());
												//ivLabel.setBackgroundColor(UIColor.black().addAlpha(0.6));
												//ivLabel.setBackgroundColor(UIColor.clear());
												ivLabel.setText(" " + percent + " ");
												ivLabel.setFont(UIFont.boldSystemFontOfSize(10));
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
										UIKit.UIGraphicsBeginImageContextWithOptions(imageSize.size(), false, 0);
										//UIGraphics.getCurrentContext().setAllowsAntialiasing(false);
										view.image().drawInRect(imageSize);

										UIImage resizedImage = UIKit.UIGraphicsGetImageFromCurrentImageContext();
										UIKit.UIGraphicsEndImageContext();

										view.setImage(resizedImage);

										if (mapHelper.showIvs && mapHelper.ivsAlwaysVisible && ivLabel != null) {
											double x = imageSize.size().width() / 2.0 - ivLabel.bounds().size().width() / 2.0;
											double y = imageSize.size().height() * IV_OFFSET_Y_PERCENT;
											ivLabel.setFrame(new CGRect(new CGPoint(x, y), new CGSize(ivLabel.bounds().size().width(), ivLabel.bounds().size().height())));
											//UIImageView ivView = new UIImageView(ivBox);
											//ivView.setFrame(new CGRect(0, 0, ivLabel.getBounds().getWidth(), ivLabel.getBounds().getHeight()));
											//ivLabel.addSubview(ivView);
											//ivLabel.sendSubviewToBack(ivView);
											try {
												// This is known for a possible NullPointerException
												ivLabel.setBackgroundColor(UIColor.colorWithPatternImage(ivBox));
											} catch (Exception e) {
												e.printStackTrace();
												ErrorReporter.logExceptionThreaded(e);
												ivLabel.setBackgroundColor(UIColor.blackColor().colorWithAlphaComponent(0.6));
											}
											view.addSubview(ivLabel);
											//ivBox.draw(new CGRect(x, y, ivBox.getSize().getWidth(), ivBox.getSize().getHeight()));
										}


									} else {
										features.print("PokeFinder", "Image width: " + view.image().size().width());
										features.print("PokeFinder", "Image height: " + view.image().size().height());

										double labelWidth = view.image().size().width() * 0.78;
										double labelHeight = view.image().size().height() * 0.36;
										double labelX = view.image().size().width() * 0.11;
										double labelY = view.image().size().height() * 0.20;

										features.print("PokeFinder", "Label width: " + labelWidth);
										features.print("PokeFinder", "Label height: " + labelHeight);
										features.print("PokeFinder", "Label X: " + labelX);
										features.print("PokeFinder", "Label Y: " + labelY);

										UILabel nameLabel = UILabel.alloc().initWithFrame(new CGRect(new CGPoint(labelX, labelY), new CGSize(labelWidth, labelHeight)));
										nameLabel.setNumberOfLines(1);
										nameLabel.setAdjustsFontSizeToFitWidth(true);
										nameLabel.setTextColor(UIColor.blackColor());
										nameLabel.setText(annotation.name);
										if (mapHelper.showIvs && mapHelper.ivsAlwaysVisible) {
											String percent = annotation.ivs;
											int index = percent.indexOf("%");
											if (index >= 0) {
												percent = percent.substring(index - 3, index + 1).trim();
												if (percent.substring(0,1).equals("M")) percent = percent.substring(1).trim();
												nameLabel.setText(nameLabel.text() + " " + percent);
											}
										}
										nameLabel.setTextAlignment(NSTextAlignment.Center);
										view.addSubview(nameLabel);
									}

									if (IOSMapHelper.CAN_SHOW_IMAGES || annotation.isCustom)
										view.layer().setAnchorPoint(new CGPoint(0.5f, 0.5f));
									else view.layer().setAnchorPoint(new CGPoint(0.5f, 1.0f));
									annotation.initCallout();
									annotation.callout.setText(annotation.ivs);// + "Time not given");
									annotation.callout.setNumberOfLines(0);
								}

								annotation.view = view;
								Stopwatch.click("mapViewViewForAnnotation");
								return view;
							} else {
								Stopwatch.click("mapViewViewForAnnotation");
								return null;
							}
						} catch (Throwable e) {
							// Sometimes cancelling the scan can cause problems here
							e.printStackTrace();
							ConcurrentHashMap<String, String> extras = new ConcurrentHashMap<String, String>();
							extras.put("Aborted scan", Boolean.toString(mapHelper.isAbortScan()));
							ErrorReporter.logExceptionThreaded(e, extras);

							Stopwatch.click("mapViewViewForAnnotation");
							return null;
						}
					}

					@Override
					public void mapViewDidAddAnnotationViews(MKMapView mapView, NSArray<? extends MKAnnotationView> views) {
						Stopwatch.click("mapViewDidAddAnnotationViews");
						try {
							if (SHOW_MAP_DIAGNOSTICS) features.print("Map Diagnostics","Entered mapViewDidAddAnnotationViews");
							int size = views.size();
							for (int n = 0; n < size; n++) {
								try {
									if (n >= views.size()) break;
									MKAnnotationView view = views.get(n);
									if (view == null) break;
									if (view.annotation() instanceof ImageAnnotation) {
										ImageAnnotation image = (ImageAnnotation) view.annotation();
										if (image.imagePath.equals("spawn_icon.png")) {
											view.superview().sendSubviewToBack(view);
											view.setCanShowCallout(false);
											view.layer().setZPosition(-2);
											view.setUserInteractionEnabled(false);
										} else if (image.imagePath.equals("scan_point_icon.png")) {
											view.layer().setZPosition(-1.5);
										} else {
											view.superview().bringSubviewToFront(view);

											try {
												if (image.ivs != null && !image.ivs.equals("")) {
													String percent = image.ivs;
													int index = percent.indexOf("%");
													if (index >= 0) {
														percent = percent.substring(index - 3, index + 1).trim();
														if (percent.substring(0, 1).equals("M"))
															percent = percent.substring(1).trim();
														percent = percent.substring(0, percent.length() - 1);
														view.layer().setZPosition((100 - Integer.parseInt(percent)) / -100.0);
													}
												}
											} catch (Exception e) {
												e.printStackTrace();
											}
										}

										if (view.detailCalloutAccessoryView() != null) {
											view.detailCalloutAccessoryView().layer().setZPosition(1);
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
						Stopwatch.click("mapViewDidAddAnnotationViews");
					}

					@Override
					public void mapViewDidUpdateUserLocation(MKMapView mapView, MKUserLocation userLocation) {
						try {
							CLLocationCoordinate2D location = userLocation.coordinate();
							if (MapController.mapHelper.isLocationOverridden() == false) {
								MapController.mapHelper.moveMe(location.latitude(), location.longitude(), 0, firstLocationUpdate, false);
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
					public void mapViewDidSelectAnnotationView(MKMapView mapView, MKAnnotationView view) {
						try {
							if (view.annotation() instanceof ImageAnnotation) {
								ImageAnnotation image = (ImageAnnotation) view.annotation();
								if (!image.imagePath.equals("spawn_icon.png") && !image.imagePath.equals("scan_point_icon.png")) {
									view.layer().setZPosition(1);
								}
							}
						} catch (Throwable t) {
							t.printStackTrace();
							ErrorReporter.logExceptionThreaded(t);
						}
					}

					@Override
					public void mapViewDidDeselectAnnotationView(MKMapView mapView, MKAnnotationView view) {
						try {
							if (view.annotation() instanceof ImageAnnotation) {
								ImageAnnotation image = (ImageAnnotation) view.annotation();
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
												view.layer().setZPosition((100 - Integer.parseInt(percent)) / -100.0);
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

				/*final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
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
				}*/

				NativePreferences.lock("first load");
				boolean first = NativePreferences.getBoolean(PREF_FIRST_LOAD, true);
				NativePreferences.unlock();

				if (first)
					firstLoad();
				else
					initLocation();

				NativePreferences.lock("first multiaccount and copyright load");
				boolean multi = NativePreferences.getBoolean(PREF_FIRST_MULTIACCOUNT_LOAD, true);
				boolean copyright = NativePreferences.getBoolean(PREF_FIRST_COPYRIGHT_LOAD, true);
				boolean pref = NativePreferences.getBoolean(PREF_FIRST_PREF_LOAD, true);
				NativePreferences.unlock();

				if (multi) {
					firstMultiAccountLoad();
				}

				if (copyright) {
					firstCopyrightLoad();
				}

				// TODO Take this out before submitting!
				//pref = true;
				if (pref) {
					firstPrefLoad();
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

				features.loadNotificationFilter();

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

	@Selector("handleMapGesture:")
	void handleMapGesture(UIGestureRecognizer gestureRecognizer) {
		Stopwatch.click("handleMapGesture");
		try {
			if (gestureRecognizer.state() == UIGestureRecognizerState.Began) {
				try {
					CGPoint point = gestureRecognizer.locationInView(map);
					CLLocationCoordinate2D loc = map.convertPointToCoordinateFromView(point, map);
					MapController.mapHelper.setLocationOverride(true);
					MapController.mapHelper.moveMe(loc.latitude(), loc.longitude(), 0, false, false);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logExceptionThreaded(t);
		}
		Stopwatch.click("handleMapGesture");
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
		/*try {
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
		}*/
	}

	public void firstPrefLoad() {
		try {
			NativePreferences.lock();
			NativePreferences.putBoolean(PREF_FIRST_PREF_LOAD, false);
			NativePreferences.unlock();

			Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
			if (prefs.contains(PREF_SHOW_ADS)) {
				boolean showAds = prefs.getBoolean(PREF_SHOW_ADS, true);
				NativePreferences.lock();
				NativePreferences.putBoolean(PREF_SHOW_ADS, showAds);
				NativePreferences.unlock();
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
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
		/*NativePreferences.lock("actual first load");
		NativePreferences.putBoolean(PREF_FIRST_LOAD, false);
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
					}
				};
				DialogHelper.yesNoBox("", "", R.string.getStarted, negative, "Go to Twitter", positive).build().show();
			}
		};
		MapController.features.runOnMainThread(r);*/
	}

	public void initLocation() {
		try {
			if (CLLocationManager.authorizationStatus() == CLAuthorizationStatus.Denied
					&& CLLocationManager.authorizationStatus() == CLAuthorizationStatus.Restricted) {
				return;
			} else {
				map.setShowsUserLocation(true);
				CLLocationCoordinate2D loc = map.userLocation().coordinate();
				double altitude = 0;
				try {
					altitude = map.userLocation().location().altitude();
				} catch (Exception e) {
					//ErrorReporter.logExceptionThreaded(e);
					e.printStackTrace();
				}
				MapController.mapHelper.moveMe(loc.latitude(), loc.longitude(), altitude, true, true);
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
			if (IOSLauncher.IS_AD_TESTING) System.out.println("Changing message container height to " + 0);
			messageContainerHeight.setConstant(0);
			goodAccountsLabelHeight.setConstant(0);
			rpmLabelHeight.setConstant(0);
			rpmCountLabelHeight.setConstant(0);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public void requestLocationPermission() {
		manager.requestWhenInUseAuthorization();
	}

	public void setupSearch() {
		try {
			search = UISearchController.alloc().initWithSearchResultsController(searchResultsTable);
			search.setDelegate(new UISearchControllerDelegate() {

				@Override
				public void didDismissSearchController(UISearchController searchController) {

				}

			});

			search.setSearchResultsUpdater(searchResultsTable);

			search.setHidesNavigationBarDuringPresentation(false);
			search.setDimsBackgroundDuringPresentation(true);
			setDefinesPresentationContext(true);

			search.searchBar().sizeToFit();
			search.searchBar().setPlaceholder("Search for location");

		/*try {
			if (search.getPopoverPresentationController() != null) {
				search.getPopoverPresentationController().setSourceView(map);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
		}*/

			//this.getNavigationController().getNavigationItem().setTitleView(search.getSearchBar());
			this.navigationItem().setTitleView(search.searchBar());
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
				return new CGRect(CoreGraphics.CGPointZero(), new CGSize(32, 32));

			case 1:
				return new CGRect(CoreGraphics.CGPointZero(), new CGSize(64, 64));

			case 2:
				return new CGRect(CoreGraphics.CGPointZero(), new CGSize(96, 96));

			case 3:
				return new CGRect(CoreGraphics.CGPointZero(), new CGSize(128, 128));

			default:
				return new CGRect(CoreGraphics.CGPointZero(), new CGSize(96, 96));
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
							if (System.currentTimeMillis() - lastAccountRetryTime >= ACCOUNT_RETRY_TIME) {
								features.print("PokeFinder", "It's been " + ((System.currentTimeMillis() - lastAccountRetryTime) / 1000) + "s since last checking account connection. Trying to talk to server now...");
								lastAccountRetryTime = System.currentTimeMillis();
								AccountManager.tryTalkingToServer();
							}
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
			return Integer.parseInt(NSBundle.mainBundle().infoDictionary().get("CFBundleVersion").toString());
		} catch (Exception e) {
			return 0;
		}
	}

	public void appPause() {
		inBackground = true;
		if (mapHelper == null) return;
		if (mapHelper.backgroundScanning && inBackground) {
			if (features != null) features.print("PokeFinder", "PokeSensor entered background");

			if (!mapHelper.promptForApiKey()) return;

			// Start always location updates
			lastScanTime = System.currentTimeMillis();
			if (!alwaysLocationAllowed) manager.requestAlwaysAuthorization();
			else if (inBackground && mapHelper.backgroundScanning) {
				if (NSProcessInfo.processInfo().operatingSystemVersion().majorVersion() >= 9) manager.setAllowsBackgroundLocationUpdates(true);
				manager.setPausesLocationUpdatesAutomatically(false);
				manager.startUpdatingLocation();
			}
		}
	}

	public void appResume() {
		inBackground = false;
		if (features != null) features.print("PokeFinder", "PokeSensor entered foreground");

		// Stop always location updates
		manager.stopUpdatingLocation();
	}

	public static void addDoneButtonToKeyboard(final UITextField field) {
		UIToolbar doneView = UIToolbar.alloc().init();

		UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		CustomUIBarButtonItem done = CustomUIBarButtonItem.alloc().initWithBarButtonSystemItemTargetActionObject(UIBarButtonSystemItem.Done, MapController.instance, new SEL("doneTextField:"), field);
		UIBarButtonItem flexButton2 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);

		NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(flexButton1, done, flexButton2, null);

		doneView.setItems(items);

		doneView.sizeToFit();

		field.setInputAccessoryView(doneView);
	}

	public static void addDoneButtonToKeyboard(final UITextView field) {
		UIToolbar doneView = UIToolbar.alloc().init();

		UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		CustomUIBarButtonItem done = CustomUIBarButtonItem.alloc().initWithBarButtonSystemItemTargetActionObject(UIBarButtonSystemItem.Done, MapController.instance, new SEL("doneTextView:"), field);
		UIBarButtonItem flexButton2 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);

		NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(flexButton1, done, flexButton2, null);

		doneView.setItems(items);

		doneView.sizeToFit();

		field.setInputAccessoryView(doneView);
	}

	@Selector("doneTextField:")
	void doneTextField(CustomUIBarButtonItem sender) {
		((UITextField) sender.object).resignFirstResponder();
	}

	@Selector("doneTextView:")
	void doneTextView(CustomUIBarButtonItem sender) {
		((UITextView) sender.object).resignFirstResponder();
	}

	// Selector for scanView
	@Selector("abortWideScan:")
	void abortWideScan(ClickableView sender) {
		mapHelper.abortWideScan(sender);
	}

	// Selector for scanView
	@Selector("abortSpawnScan:")
	void abortSpawnScan(ClickableView sender) {
		mapHelper.abortSpawnScan(sender);
	}

	// IAP Stuff

	public void setupIAP() {
		try {
			ArrayList<IAPOffer> offers = new ArrayList<IAPOffer>();

			offers.add(IAPHelper.createEntitlementOffer(REMOVE_ADS_IAP_PRODUCT_ID, new Lambda() {

				@Override
				public void execute() {
					NativePreferences.lock();
					NativePreferences.putBoolean(PREF_SHOW_ADS, false);
					NativePreferences.unlock();

					AdHelper.removeAds(PREFS_NAME);

					Runnable r = new Runnable() {
						@Override
						public void run() {
							Runnable runnable = new Runnable() {
								@Override
								public void run() {
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

	//IBOutlets

	@IBOutlet
	@Property
	@Selector("map")
	public MKMapView getMap() { return map; }

	@IBOutlet
	@Property
	@Selector("setMap:")
	public void setMap(MKMapView map) { this.map = map; }

	@IBOutlet
	@Property
	@Selector("scanBar")
	public UIProgressView getScanBar() { return scanBar; }

	@IBOutlet
	@Property
	@Selector("setScanBar:")
	public void setScanBar(UIProgressView scanBar) { this.scanBar = scanBar; }

	@IBOutlet
	@Property
	@Selector("scanText")
	public UILabel getScanText() { return scanText; }

	@IBOutlet
	@Property
	@Selector("setScanText:")
	public void setScanText(UILabel scanText) { this.scanText = scanText; }

	@IBOutlet
	@Property
	@Selector("scanView")
	public ClickableView getScanView() { return scanView; }

	@IBOutlet
	@Property
	@Selector("setScanView:")
	public void setScanView(ClickableView scanView) { this.scanView = scanView; }

	@IBOutlet
	@Property
	@Selector("message")
	public UILabel getMessage() { return message; }

	@IBOutlet
	@Property
	@Selector("setMessage:")
	public void setMessage(UILabel message) { this.message = message; }

	@IBOutlet
	@Property
	@Selector("messageContainer")
	public UIView getMessageContainer() { return messageContainer; }

	@IBOutlet
	@Property
	@Selector("setMessageContainer:")
	public void setMessageContainer(UIView messageContainer) { this.messageContainer = messageContainer; }

	@IBOutlet
	@Property
	@Selector("messageContainerHeight")
	public NSLayoutConstraint getMessageContainerHeight() { return messageContainerHeight; }

	@IBOutlet
	@Property
	@Selector("setMessageContainerHeight:")
	public void setMessageContainerHeight(NSLayoutConstraint messageContainerHeight) { this.messageContainerHeight = messageContainerHeight; }

	@IBOutlet
	@Property
	@Selector("goodAccountsLabelHeight")
	public NSLayoutConstraint getGoodAccountsLabelHeight() { return goodAccountsLabelHeight; }

	@IBOutlet
	@Property
	@Selector("setGoodAccountsLabelHeight:")
	public void setGoodAccountsLabelHeight(NSLayoutConstraint goodAccountsLabelHeight) { this.goodAccountsLabelHeight = goodAccountsLabelHeight; }

	@IBOutlet
	@Property
	@Selector("rpmLabelHeight")
	public NSLayoutConstraint getRpmLabelHeight() { return rpmLabelHeight; }

	@IBOutlet
	@Property
	@Selector("setRpmLabelHeight:")
	public void setRpmLabelHeight(NSLayoutConstraint rpmLabelHeight) { this.rpmLabelHeight = rpmLabelHeight; }

	@IBOutlet
	@Property
	@Selector("rpmCountLabelHeight")
	public NSLayoutConstraint getRpmCountLabelHeight() { return rpmCountLabelHeight; }

	@IBOutlet
	@Property
	@Selector("setRpmCountLabelHeight:")
	public void setRpmCountLabelHeight(NSLayoutConstraint rpmCountLabelHeight) { this.rpmCountLabelHeight = rpmCountLabelHeight; }

	@IBOutlet
	@Property
	@Selector("goodAccountsView")
	public GoodAccountsView getGoodAccountsView() { return goodAccountsView; }

	@IBOutlet
	@Property
	@Selector("setGoodAccountsView:")
	public void setGoodAccountsView(GoodAccountsView goodAccountsView) { this.goodAccountsView = goodAccountsView; }

	@IBOutlet
	@Property
	@Selector("rpmView")
	public RpmView getRpmView() { return rpmView; }

	@IBOutlet
	@Property
	@Selector("setRpmView:")
	public void setRpmView(RpmView rpmView) { this.rpmView = rpmView; }

	@IBOutlet
	@Property
	@Selector("rpmCountView")
	public RpmCountView getRpmCountView() { return rpmCountView; }

	@IBOutlet
	@Property
	@Selector("setRpmCountView:")
	public void setRpmCountView(RpmCountView rpmCountView) { this.rpmCountView = rpmCountView; }
}
