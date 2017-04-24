package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.Features;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import java.util.concurrent.ConcurrentHashMap;

import apple.avfoundation.AVAudioSession;
import apple.corelocation.CLLocationManager;
import apple.corelocation.enums.CLAuthorizationStatus;
import apple.foundation.NSArray;
import apple.foundation.NSString;
import apple.uikit.UIActivityViewController;
import apple.uikit.UIBarButtonItem;
import apple.uikit.UIImage;
import apple.uikit.UINavigationController;
import apple.uikit.UIToolbar;
import apple.uikit.UIViewController;
import apple.uikit.enums.UIBarButtonItemStyle;
import apple.uikit.enums.UIBarButtonSystemItem;

@Runtime(ObjCRuntime.class)
@ObjCClassName("NavigationController")
@RegisterOnStartup
public class NavigationController extends UINavigationController {
	//@IBOutlet
	private UIToolbar customToolbar;

	private final long UNDO_TAG = 1;
	private final long NEW_GAME_TAG = 2;
	private final long SETTINGS_TAG = 3;
	private boolean isLoaded = false;

	private final String SETTINGS_VIEW_CONTROLLER = "Settings";
	private final String SCAN_DETAILS_VIEW_CONTROLLER = "ScanDetails";
	private int count = 0;

	protected NavigationController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewDidAppear(boolean arg) {
		if (!isLoaded) {
			this.setNavigationBarHidden(false);
			IOSLauncher.navigationController = this;
			//DefaultIOSLauncher.myApp.getUIWindow().setRootViewController(this);
			//this.showViewController(DefaultIOSLauncher.myApp.getUIViewController(), null);
			//DefaultIOSLauncher.showViewController(this, DefaultIOSLauncher.myApp.getUIViewController());
			//showViewInNavigationController(DefaultIOSLauncher.myApp.getUIViewController());

			MapController mapController = (MapController) this.storyboard().instantiateViewControllerWithIdentifier("MapController");
			this.showViewControllerSender(mapController, null);

			UIBarButtonItem scanDetailsButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("scan_details.png"), UIBarButtonItemStyle.Plain, this, new SEL("scanDetailsClicked:"));
			UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
			final UIBarButtonItem locateButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("locate.png"), UIBarButtonItemStyle.Plain, this, new SEL("locateClicked:"));
			UIBarButtonItem flexButton2 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
			UIBarButtonItem scanButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("scan.png"), UIBarButtonItemStyle.Plain, this, new SEL("scanClicked:"));
			UIBarButtonItem flexButton3 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
			UIBarButtonItem settingsButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("settings.png"), UIBarButtonItemStyle.Plain, this, new SEL("settingsClicked:"));
			UIBarButtonItem flexButton4 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
			UIBarButtonItem spawnScanButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("spawn_scan.png"), UIBarButtonItemStyle.Plain, this, new SEL("spawnScanClicked:"));

			//NSArray<UIBarButtonItem> items = customToolbar.getItems();
			NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(scanDetailsButton, flexButton1, locateButton,
					flexButton2, scanButton, flexButton3, spawnScanButton, flexButton4, settingsButton, null);

			mapController.setToolbarItems(items);
			isLoaded = true;
			DialogHelper.initialize();
		}
	}

	@Selector("scanDetailsClicked:")
	void scanDetailsClicked(UIBarButtonItem sender) {
		ScanDetailsController scanDetailsController = (ScanDetailsController) IOSLauncher.navigationController.storyboard().instantiateViewControllerWithIdentifier(SCAN_DETAILS_VIEW_CONTROLLER);
		showViewInNavigationController(scanDetailsController);
		//scanDetailsController.getNavigationController().setNavigationBarHidden(false);
		IOSLauncher.navigationController.setNavigationBarHidden(false);
	}

	@Selector("locateClicked:")
	void locateClicked(UIBarButtonItem sender) {
		//if (true) throw new RuntimeException("This was caught by the exception handler!");
		if (IOSLauncher.IS_AD_TESTING) {
			Stopwatch.printAllTimes();
			shareText(Stopwatch.times, sender);
			if (true) return;

			MapController.mapHelper.newApiKey = "";
			if (true) return;

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
			if (CLLocationManager.authorizationStatus() == CLAuthorizationStatus.Denied
					|| CLLocationManager.authorizationStatus() == CLAuthorizationStatus.Restricted) {
				MapController.instance.deniedLocationPermission();
			} else {
				MapController.instance.initLocation();
			}
		}
	}

	@Selector("scanClicked:")
	void scanClicked(UIBarButtonItem sender) {
		MapController.mapHelper.wideScan();
	}

	@Selector("settingsClicked:")
	void settingsClicked(UIBarButtonItem sender) {
		SettingsController settingsController = (SettingsController) IOSLauncher.navigationController.storyboard().instantiateViewControllerWithIdentifier(SETTINGS_VIEW_CONTROLLER);
		//DefaultIOSLauncher.navigationController.showViewController(settingsController, null);
		showViewInNavigationController(settingsController);
		//settingsController.getNavigationController().setNavigationBarHidden(false);
		IOSLauncher.navigationController.setNavigationBarHidden(false);
	}

	@Selector("spawnScanClicked:")
	void spawnScanClicked(UIBarButtonItem sender) {
		MapController.instance.dontRefreshAccounts = true;
		MapController.mapHelper.wideSpawnScan(false);
	}

	public void shareText(String text, UIBarButtonItem sender) {
		NSString nsText = NSString.stringWithString(text);
		NSArray<?> items = NSArray.arrayWithObject(nsText);

		UIActivityViewController controller = UIActivityViewController.alloc().initWithActivityItemsApplicationActivities(items, null);

		try {
			if (controller.popoverPresentationController() != null) {
				controller.popoverPresentationController().setBarButtonItem(sender);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
		}

		presentViewControllerAnimatedCompletion(controller, true, null);
	}

	public void showViewInNavigationController(UIViewController controller) {
		this.showViewControllerSender(controller, null);
	}

	@Override
	public void viewWillAppear(boolean animated) {
		super.viewWillAppear(animated);
		AVAudioSession.sharedInstance().setCategoryError("AVAudioSessionCategoryAmbient", null);
	}

	@IBOutlet
	@Property
	@Selector("customToolbar")
	public UIToolbar getCustomToolbar() { return customToolbar; }

	@IBOutlet
	@Property
	@Selector("setCustomToolbar:")
	public void setCustomToolbar(UIToolbar customToolbar) { this.customToolbar = customToolbar; }
}
