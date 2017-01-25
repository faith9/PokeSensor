package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.MapHelper;

import org.robovm.apple.avfoundation.AVAudioSession;
import org.robovm.apple.avfoundation.AVAudioSessionCategory;
import org.robovm.apple.corelocation.CLAuthorizationStatus;
import org.robovm.apple.corelocation.CLLocationManager;
import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSErrorException;
import org.robovm.apple.uikit.UIActionSheet;
import org.robovm.apple.uikit.UIActionSheetDelegateAdapter;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIBarButtonItem;
import org.robovm.apple.uikit.UIBarButtonItem.OnClickListener;
import org.robovm.apple.uikit.UIBarButtonItemStyle;
import org.robovm.apple.uikit.UIBarButtonSystemItem;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UINavigationController;
import org.robovm.apple.uikit.UIToolbar;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

import java.util.concurrent.ConcurrentHashMap;

@CustomClass("NavigationController")
public class NavigationController extends UINavigationController {
	@IBOutlet
	private UIToolbar customToolbar;

	private final long UNDO_TAG = 1;
	private final long NEW_GAME_TAG = 2;
	private final long SETTINGS_TAG = 3;
	private boolean isLoaded = false;

	private final String SETTINGS_VIEW_CONTROLLER = "Settings";
	private final String SCAN_DETAILS_VIEW_CONTROLLER = "ScanDetails";
	private int count = 0;

	@Override
	public void viewDidAppear(boolean arg) {
		if (!isLoaded) {
			this.setNavigationBarHidden(false);
			IOSLauncher.navigationController = this;
			//Utilities.debug("Got a handle to the Navigation Controller");
			//DefaultIOSLauncher.myApp.getUIWindow().setRootViewController(this);
			//this.showViewController(DefaultIOSLauncher.myApp.getUIViewController(), null);
			//DefaultIOSLauncher.showViewController(this, DefaultIOSLauncher.myApp.getUIViewController());
			//showViewInNavigationController(DefaultIOSLauncher.myApp.getUIViewController());

			MapController mapController = (MapController) this.getStoryboard().instantiateViewController("MapController");
			this.showViewController(mapController, null);

			UIBarButtonItem scanDetailsButton = new UIBarButtonItem(UIImage.create("scan_details.png"), UIBarButtonItemStyle.Plain);
			UIBarButtonItem flexButton1 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
			final UIBarButtonItem locateButton = new UIBarButtonItem(UIImage.create("locate.png"), UIBarButtonItemStyle.Plain);
			UIBarButtonItem flexButton2 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
			UIBarButtonItem scanButton = new UIBarButtonItem(UIImage.create("scan.png"), UIBarButtonItemStyle.Plain);
			UIBarButtonItem flexButton3 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
			UIBarButtonItem settingsButton = new UIBarButtonItem(UIImage.create("settings.png"), UIBarButtonItemStyle.Plain);

			//NSArray<UIBarButtonItem> items = customToolbar.getItems();
			NSArray<UIBarButtonItem> items = new NSArray<UIBarButtonItem>(scanDetailsButton, flexButton1, locateButton,
					flexButton2, scanButton, flexButton3, settingsButton);

			scanDetailsButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(UIBarButtonItem arg0) {
					ScanDetailsController scanDetailsController = (ScanDetailsController) IOSLauncher.navigationController.getStoryboard().instantiateViewController(SCAN_DETAILS_VIEW_CONTROLLER);
					showViewInNavigationController(scanDetailsController);
					//scanDetailsController.getNavigationController().setNavigationBarHidden(false);
					IOSLauncher.navigationController.setNavigationBarHidden(false);
				}

			});
			locateButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(UIBarButtonItem arg0) {
					//if (true) throw new RuntimeException("This was caught by the exception handler!");
					if (IOSLauncher.IS_AD_TESTING) {
						//NativePreferences.printPrefs();
						Exception e = new Exception("Testing extra parameters...");
						ConcurrentHashMap<String, String> extras = new ConcurrentHashMap<>();
						extras.put("Fatal", "false");
						ErrorReporter.logExceptionThreaded(e, extras);

						if (true) return;
						final Runnable runnable = new Runnable() {
							@Override
							public void run() {
								String bob = true ? null : "";
								bob = bob.toLowerCase();
							}
						};

						switch (count++) {
							case 0:
								MapController.features.print("Crasher","Crashing on GDX Runnable");
								Gdx.app.postRunnable(runnable);
								break;
							case 1:
								MapController.features.print("Crasher","Crashing on background thread");
								Thread thread = new Thread() {
									public void run() {
										runnable.run();
									}
								};
								thread.start();
								break;
							case 2:
								MapController.features.print("Crasher","Crashing on native main thread");
								MapController.features.runOnMainThread(runnable);
								break;
							case 3:
								MapController.features.print("Crasher","Crashing on current thread");
								runnable.run();
								break;
						}

						if (true) return;
						if (Features.NUM_POKEMON == 151) {
							MapController.features.shortMessage("Gen 2 Activated");
							MapController.mapHelper.activateGen2();
						}
						else {
							MapController.features.shortMessage("Gen 2 De-activated after a restart");
							Features.NUM_POKEMON = 151;
							NativePreferences.lock();
							NativePreferences.putInteger(IOSMapHelper.PREF_NUM_POKEMON, Features.NUM_POKEMON);
							NativePreferences.unlock();
						}
					}
					else {
						if (CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Denied
								|| CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Restricted) {
							MapController.instance.deniedLocationPermission();
						} else {
							MapController.instance.initLocation();
						}
					}
				}

			});

			scanButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(UIBarButtonItem arg0) {
					MapController.mapHelper.wideScan();
				}

			});

			settingsButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(UIBarButtonItem arg0) {
					SettingsController settingsController = (SettingsController) IOSLauncher.navigationController.getStoryboard().instantiateViewController(SETTINGS_VIEW_CONTROLLER);
					//DefaultIOSLauncher.navigationController.showViewController(settingsController, null);
					showViewInNavigationController(settingsController);
					//settingsController.getNavigationController().setNavigationBarHidden(false);
					IOSLauncher.navigationController.setNavigationBarHidden(false);
				}

			});

			//customToolbar.setItems(items);

			//DefaultIOSLauncher.myApp.getUIViewController().setToolbarItems(customToolbar.getItems());
			mapController.setToolbarItems(items);
			isLoaded = true;
			DialogHelper.initialize();
		}
	}

	public void showViewInNavigationController(UIViewController controller) {
		if (Foundation.getMajorSystemVersion() >= 8) {
			this.showViewController(controller, null);
		} else {
			this.pushViewController(controller, true);
		}
	}

	@Override
	public void viewWillAppear(boolean animated) {
		super.viewWillAppear(animated);

		try {
			AVAudioSession.getSharedInstance().setCategory(AVAudioSessionCategory.Ambient);
		} catch (NSErrorException e) {
			e.printStackTrace();
		}
	}
}
