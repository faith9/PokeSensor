package com.logickllc.pokesensor;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.robovm.apple.coregraphics.CGPoint;
import org.robovm.apple.corelocation.CLAuthorizationStatus;
import org.robovm.apple.corelocation.CLLocationCoordinate2D;
import org.robovm.apple.corelocation.CLLocationManager;
import org.robovm.apple.corelocation.CLLocationManagerDelegateAdapter;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.mapkit.MKAnnotation;
import org.robovm.apple.mapkit.MKAnnotationView;
import org.robovm.apple.mapkit.MKCircle;
import org.robovm.apple.mapkit.MKCircleRenderer;
import org.robovm.apple.mapkit.MKLocalSearch;
import org.robovm.apple.mapkit.MKLocalSearchRequest;
import org.robovm.apple.mapkit.MKLocalSearchResponse;
import org.robovm.apple.mapkit.MKMapItem;
import org.robovm.apple.mapkit.MKMapView;
import org.robovm.apple.mapkit.MKMapViewDelegateAdapter;
import org.robovm.apple.mapkit.MKOverlay;
import org.robovm.apple.mapkit.MKOverlayRenderer;
import org.robovm.apple.mapkit.MKUserLocation;
import org.robovm.apple.uikit.NSLayoutConstraint;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIEdgeInsets;
import org.robovm.apple.uikit.UIGestureRecognizer;
import org.robovm.apple.uikit.UIGestureRecognizerState;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UILongPressGestureRecognizer;
import org.robovm.apple.uikit.UIProgressView;
import org.robovm.apple.uikit.UISearchBarDelegateAdapter;
import org.robovm.apple.uikit.UISearchController;
import org.robovm.apple.uikit.UISearchControllerDelegateAdapter;
import org.robovm.apple.uikit.UISearchResultsUpdatingAdapter;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;
import org.robovm.objc.block.VoidBlock2;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.logickllc.pokesensor.api.MapHelper;

@CustomClass("MapController")
public class MapController extends UIViewController {
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
	NSLayoutConstraint messageContainerHeight;

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

	
	
	@Override
	public void viewDidLoad() {
		super.viewDidLoad();
		searchResultsTable = (SearchTableController) this.getStoryboard().instantiateViewController("SearchTable");
		setupSearch();
	}

	@Override
	public void viewWillAppear(boolean animated) {
		this.getNavigationController().setNavigationBarHidden(false);
		this.getNavigationController().setToolbarHidden(false, true);

		if (!isLoaded) {
			message.setLayoutMargins(new UIEdgeInsets(5, 5, 5, 5));
			instance = this;
			IOSLauncher.instance.initAds();
			mapHelper = new IOSMapHelper();
			features = new IOSFeatures();
			manager = new CLLocationManager();
			
			requestLocationPermission();
			
			this.addStrongRef(manager);
			
			manager.setDelegate(new CLLocationManagerDelegateAdapter() {

				@Override
				public void didChangeAuthorizationStatus(CLLocationManager manager, CLAuthorizationStatus status) {
					initLocation();
					super.didChangeAuthorizationStatus(manager, status);
				}
				
			});

			features.setMapHelper(mapHelper);
			mapHelper.setFeatures(features);

			map.setDelegate(new MKMapViewDelegateAdapter() {

				@Override
				public void calloutAccessoryControlTapped(MKMapView mapView, MKAnnotationView view, UIControl control) {
					if (view.getAnnotation() instanceof ImageAnnotation) {
						((ImageAnnotation) view.getAnnotation()).control = control;
					}
					super.calloutAccessoryControlTapped(mapView, view, control);
				}

				@Override
				public MKOverlayRenderer getOverlayRenderer(MKMapView mapView, MKOverlay overlay) {
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
					return super.getOverlayRenderer(mapView, overlay);
				}

				@Override
				public MKAnnotationView getAnnotationView(MKMapView mapView, MKAnnotation annotation) {
					if (annotation instanceof ImageAnnotation) {
						MKAnnotationView view = new MKAnnotationView(annotation,
								((ImageAnnotation) annotation).imagePath);
						view.setImage(UIImage.create(((ImageAnnotation) annotation).imagePath));
						view.setCanShowCallout(true);
						((ImageAnnotation) annotation).view = view;
						if (((ImageAnnotation) annotation).imagePath.equals("scan_point_icon.png")) view.getLayer().setAnchorPoint(new CGPoint(0.32f, 0.32f));
						else {
							((ImageAnnotation) annotation).initCallout();
							((ImageAnnotation) annotation).callout.setText("Time not given");
						}
						return view;
					} else {
						return super.getAnnotationView(mapView, annotation);
					}
				}

				@Override
				public void didUpdateUserLocation(MKMapView mapView, MKUserLocation userLocation) {
					CLLocationCoordinate2D location = userLocation.getCoordinate();
					if (MapController.mapHelper.isLocationOverridden() == false) {
						MapController.mapHelper.moveMe(location.getLatitude(), location.getLongitude(), firstLocationUpdate, false);
						firstLocationUpdate = false;
					}
					if (!MapController.mapHelper.isSearched())
						MapController.mapHelper.wideScan();
				}

			});
			final UILongPressGestureRecognizer gesture = new UILongPressGestureRecognizer(
					new UIGestureRecognizer.OnGestureListener() {

						@Override
						public void onGesture(UIGestureRecognizer gestureRecognizer) {
							if (gestureRecognizer.getState() == UIGestureRecognizerState.Began) {
								CGPoint point = gestureRecognizer.getLocationInView(map);
								CLLocationCoordinate2D loc = map.convertPointToCoordinateFromView(point, map);
								MapController.mapHelper.setLocationOverride(true);
								MapController.mapHelper.moveMe(loc.getLatitude(), loc.getLongitude(), true, false);
							}
						}
					});

			gesture.setMinimumPressDuration(0.7);
			map.addGestureRecognizer(gesture);

			MapController.mapHelper.setmMap(map);

			map.setZoomEnabled(true);

			final Preferences prefs = Gdx.app.getPreferences(IOSFeatures.PREFS_NAME);
			if (prefs.getBoolean(IOSFeatures.PREF_FIRST_LOAD, true))
				firstLoad();
			else
				initLocation();

			isLoaded = true;
		} else {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					// TODO This gets annoying so try going without it
					MapController.features.loggedIn();
				}
			};
			//runnable.run();


		}
		// resumeAds();
		System.out.println("PokeFinderActivity.onResume()");
		final Preferences prefs = Gdx.app.getPreferences(IOSFeatures.PREFS_NAME);
		MapController.mapHelper.setScanDistance(
				prefs.getInteger(IOSMapHelper.PREF_SCAN_DISTANCE, MapHelper.DEFAULT_SCAN_DISTANCE));
		MapController.mapHelper
		.setScanSpeed(prefs.getInteger(IOSMapHelper.PREF_SCAN_SPEED, MapHelper.DEFAULT_SCAN_SPEED));
		if (MapController.mapHelper.getScanDistance() > MapHelper.MAX_SCAN_DISTANCE)
			MapController.mapHelper.setScanDistance(MapHelper.MAX_SCAN_DISTANCE);
		if (MapHelper.maxScanSpeed != 0 && MapController.mapHelper.getScanSpeed() > MapHelper.maxScanSpeed)
			MapController.mapHelper.setScanSpeed(MapHelper.maxScanSpeed);

		MapController.mapHelper.startCountdownTimer();
	}

	@Override
	public void viewWillDisappear(boolean animated) {
		MapController.mapHelper.stopCountdownTimer();
	}

	public void firstLoad() {
		final Preferences prefs = Gdx.app.getPreferences(IOSFeatures.PREFS_NAME);
		prefs.putBoolean(IOSFeatures.PREF_FIRST_LOAD, false);
		prefs.flush();

		Runnable r = new Runnable() {
			@Override
			public void run() {
				Runnable runnable = new Runnable() {
					public void run() {
						initLocation();
					}
				};
				DialogHelper.messageBox(R.string.welcomeTitle, R.string.welcomeMessage, R.string.getStarted, runnable)
				.build().show();
			}
		};
		MapController.features.runOnMainThread(r);
	}

	public void initLocation() {
		if (CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Denied
				&& CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Restricted) {
			return;
		} else {
			map.setShowsUserLocation(true);
			CLLocationCoordinate2D loc = map.getUserLocation().getCoordinate();
			MapController.mapHelper.moveMe(loc.getLatitude(), loc.getLongitude(), true, true);
			MapController.mapHelper.setLocationInitialized(true);
			
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					Runnable runnable = new Runnable() {
						public void run() {
							if (!MapController.mapHelper.isSearched())
								MapController.mapHelper.wideScan();
						}
					};
					features.runOnMainThread(runnable);
				}
			};
			timer.schedule(task, 2000);
			
			System.out.println("Location initialized. Trying to scan in 2 seconds...");
		}
	}

	public void deniedLocationPermission() {
		MapController.features.longMessage("Location permissions denied. Please go to Settings > Privacy > Location Services and enable them for this app.");
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
		messageContainerHeight.setConstant(IOSLauncher.instance.getSize().y);
	}

	public void requestLocationPermission() {
		manager.requestWhenInUseAuthorization();
	}

	@Override
	public void willRotate(UIInterfaceOrientation arg0, double arg1) {
		super.willRotate(arg0, arg1);
		IOSLauncher.instance.hide();
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
		search = new UISearchController(searchResultsTable);
		search.setDelegate(new UISearchControllerDelegateAdapter() {

			@Override
			public void didDismiss(UISearchController searchController) {
				super.didDismiss(searchController);
				features.unlockLogin();
			}
			
		});
		
		search.setSearchResultsUpdater(searchResultsTable);
		
		search.setHidesNavigationBarDuringPresentation(false);
		search.setDimsBackgroundDuringPresentation(true);
		setDefinesPresentationContext(true);

		search.getSearchBar().sizeToFit();
		search.getSearchBar().setPlaceholder("Search for location");
		//this.getNavigationController().getNavigationItem().setTitleView(search.getSearchBar());
		this.getNavigationItem().setTitleView(search.getSearchBar());
	}
}
