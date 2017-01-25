package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.Map;
import com.logickllc.pokesensor.api.AccountManager;
import com.pokegoapi.util.Signature;

import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UISegmentedControl;
import org.robovm.apple.uikit.UIStatusBarStyle;
import org.robovm.apple.uikit.UISwitch;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("PreferencesController")
public class PreferencesController extends UITableViewController {

	@IBOutlet
	UISwitch collectSpawns, showIvs, showSpawns, showScanDetails, ivsAlwaysVisible, clearMapOnScan, use2Captcha, useNewApi, fallbackApi;

	@IBOutlet
	UISegmentedControl captchaMode, imageSize, gpsMode;

	@IBOutlet
	UILabel ivsAlwaysVisibleLabel, fallbackApiLabel;

	public static PreferencesController instance = null;

	private boolean askForKey = true;

	@Override
	public UIStatusBarStyle getPreferredStatusBarStyle() {
		return UIStatusBarStyle.Default;
	}

	@Override
	public void viewDidLoad() {
		super.viewDidLoad();
		this.setNeedsStatusBarAppearanceUpdate();
		this.getNavigationController().setToolbarHidden(true, true);
	}

	@Override
	public void viewWillDisappear(boolean b) {
		MapController.mapHelper.collectSpawns = collectSpawns.isOn();
		MapController.mapHelper.showIvs = showIvs.isOn();
		MapController.mapHelper.showSpawns = showSpawns.isOn();
		MapController.mapHelper.showScanDetails = showScanDetails.isOn();
		MapController.mapHelper.ivsAlwaysVisible = ivsAlwaysVisible.isOn();
		MapController.mapHelper.clearMapOnScan = clearMapOnScan.isOn();
		MapController.mapHelper.use2Captcha = use2Captcha.isOn();
		MapController.mapHelper.useNewApi = useNewApi.isOn();
		MapController.mapHelper.fallbackApi = fallbackApi.isOn();

		MapController.mapHelper.gpsModeNormal = gpsMode.getSelectedSegment() == 1;

		if (captchaMode.getSelectedSegment() == 0) MapController.mapHelper.captchaModePopup = true;
		else MapController.mapHelper.captchaModePopup = false;

		MapController.mapHelper.imageSize = imageSize.getSelectedSegment();

		//MapController.mapHelper.defaultMarkersMode = defaultMarkersMode.getSelectedSegment();

		if (showSpawns.isOn()) MapController.mapHelper.showSpawnsOnMap();
		else MapController.mapHelper.hideSpawnsOnMap();

		NativePreferences.lock();
		NativePreferences.putBoolean(IOSMapHelper.PREF_COLLECT_SPAWNS, MapController.mapHelper.collectSpawns);
		NativePreferences.putBoolean(IOSMapHelper.PREF_SHOW_IVS, MapController.mapHelper.showIvs);
		NativePreferences.putBoolean(IOSMapHelper.PREF_IVS_ALWAYS_VISIBLE, MapController.mapHelper.ivsAlwaysVisible);
		NativePreferences.putBoolean(IOSMapHelper.PREF_SHOW_SPAWNS, MapController.mapHelper.showSpawns);
		NativePreferences.putBoolean(IOSMapHelper.PREF_SHOW_SCAN_DETAILS, MapController.mapHelper.showScanDetails);
		NativePreferences.putBoolean(IOSMapHelper.PREF_CAPTCHA_MODE_POPUP, MapController.mapHelper.captchaModePopup);
		NativePreferences.putBoolean(IOSMapHelper.PREF_CLEAR_MAP_ON_SCAN, MapController.mapHelper.clearMapOnScan);
		NativePreferences.putLong(IOSMapHelper.PREF_IMAGE_SIZE, MapController.mapHelper.imageSize);
		NativePreferences.putBoolean(IOSMapHelper.PREF_GPS_MODE_NORMAL, MapController.mapHelper.gpsModeNormal);
		NativePreferences.putBoolean(IOSMapHelper.PREF_USE_2CAPTCHA, MapController.mapHelper.use2Captcha);
		NativePreferences.putBoolean(IOSMapHelper.PREF_USE_NEW_API, MapController.mapHelper.useNewApi);
		NativePreferences.putBoolean(IOSMapHelper.PREF_FALLBACK_API, MapController.mapHelper.fallbackApi);
		//NativePreferences.putLong(IOSMapHelper.PREF_DEFAULT_MARKERS_MODE, MapController.mapHelper.defaultMarkersMode);

		NativePreferences.unlock();

		MapController.features.print("PokeFinder", "Collect spawns? " + MapController.mapHelper.collectSpawns + "\nShow IVs? " + MapController.mapHelper.showIvs + "\nShow spawns? " + MapController.mapHelper.showSpawns);

		AccountManager.switchHashProvider();
		//Signature.fallbackApi = MapController.mapHelper.fallbackApi;
		Signature.fallbackApi = false;
	}

	@Override
	public void viewWillAppear(boolean animated) {
		instance = this;
		collectSpawns.setOn(MapController.mapHelper.collectSpawns);
		showIvs.setOn(MapController.mapHelper.showIvs);
		showSpawns.setOn(MapController.mapHelper.showSpawns);
		showScanDetails.setOn(MapController.mapHelper.showScanDetails);
		ivsAlwaysVisible.setOn(MapController.mapHelper.ivsAlwaysVisible);
		clearMapOnScan.setOn(MapController.mapHelper.clearMapOnScan);
		use2Captcha.setOn(MapController.mapHelper.use2Captcha);
		useNewApi.setOn(MapController.mapHelper.useNewApi);
		//fallbackApi.setOn(MapController.mapHelper.fallbackApi);
		fallbackApi.setOn(false);

		if (MapController.mapHelper.captchaModePopup) captchaMode.setSelectedSegment(0);
		else captchaMode.setSelectedSegment(1);

		if (MapController.mapHelper.gpsModeNormal) gpsMode.setSelectedSegment(1);
		else gpsMode.setSelectedSegment(0);

		imageSize.setSelectedSegment(MapController.mapHelper.imageSize);

		//defaultMarkersMode.setSelectedSegment(MapController.mapHelper.defaultMarkersMode);

		toggleIvsAlwaysVisible();

		showIvs.addOnValueChangedListener(new UIControl.OnValueChangedListener() {
			@Override
			public void onValueChanged(UIControl uiControl) {
				toggleIvsAlwaysVisible();
			}
		});

		toggleFallbackApi();

		useNewApi.addOnValueChangedListener(new UIControl.OnValueChangedListener() {
			@Override
			public void onValueChanged(UIControl uiControl) {
				toggleFallbackApi();

				if (useNewApi.isOn()) {
					if (!MapController.mapHelper.newApiKey.equals("")) {
						Runnable no = new Runnable() {
							@Override
							public void run() {
								askForApiKey();
							}
						};

						DialogHelper.yesNoBox("Reuse Key?", "Would you like to use the last key you entered?\nKey: " + MapController.mapHelper.newApiKey, null, no).build().show();
					} else {
						askForApiKey();
					}
				}
			}
		});

		use2Captcha.addOnValueChangedListener(new UIControl.OnValueChangedListener() {
			@Override
			public void onValueChanged(UIControl uiControl) {
				if (use2Captcha.isOn()) {
					if (!MapController.mapHelper.captchaKey.equals("")) {
						Runnable no = new Runnable() {
							@Override
							public void run() {
								askForCaptchaKey();
							}
						};

						DialogHelper.yesNoBox("Reuse Key?", "Would you like to use the last key you entered?\nKey: " + MapController.mapHelper.captchaKey, null, no).build().show();
					} else {
						askForCaptchaKey();
					}
				}
			}
		});
	}

	@Override
	public boolean isMovingFromParentViewController() {
		return super.isMovingFromParentViewController();
	}

	@Override
	public boolean prefersStatusBarHidden() {
		return false;
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
		if (fromInterfaceOrientation == UIInterfaceOrientation.Portrait
				|| fromInterfaceOrientation == UIInterfaceOrientation.PortraitUpsideDown)
			IOSLauncher.instance.changeOrientation(false);
		else
			IOSLauncher.instance.changeOrientation(true);
	}

	public void toggleIvsAlwaysVisible() {
		ivsAlwaysVisible.setEnabled(showIvs.isOn());
		ivsAlwaysVisibleLabel.setEnabled(showIvs.isOn());
	}

	public void toggleFallbackApi() {
		fallbackApi.setEnabled(false);
		fallbackApiLabel.setEnabled(false);
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
							useNewApi.setOn(false);
							toggleFallbackApi();
							return;
						}

						MapController.mapHelper.newApiKey = text;
						Signature.validApiKey = null;
						NativePreferences.lock();
						NativePreferences.putString(IOSMapHelper.PREF_NEW_API_KEY, MapController.mapHelper.newApiKey);
						NativePreferences.unlock();
					}
				};

				Lambda negative = new Lambda() {
					@Override
					public void execute() {
						useNewApi.setOn(false);
						toggleFallbackApi();
					}
				};
				DialogHelper.textPrompt("Enter API Key", "Enter your PokeHash API key. This will let you use the latest reversed API provided by the PokeFarmer devs for a fee. Note this is not my system. It can be buggy/unavailable at times and I can't do anything about it.", MapController.mapHelper.newApiKey, "Confirm", positive, negative).build().show();
			}
		};

		MapController.features.runOnMainThread(runnable);
	}

	public void askForCaptchaKey() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Lambda positive = new Lambda() {
					@Override
					public void execute() {
						String text = (String) params.get("Text");

						if (text == null || text.equals("")) {
							use2Captcha.setOn(false);
							return;
						}

						MapController.mapHelper.captchaKey = text;
						NativePreferences.lock();
						NativePreferences.putString(IOSMapHelper.PREF_2CAPTCHA_KEY, MapController.mapHelper.captchaKey);
						NativePreferences.unlock();
					}
				};

				Lambda negative = new Lambda() {
					@Override
					public void execute() {
						use2Captcha.setOn(false);
					}
				};
				DialogHelper.textPrompt("Enter API Key", "Enter your 2Captcha API key. 2Captcha is a paid service that will automatically solve captchas for you.", MapController.mapHelper.captchaKey, "Confirm", positive, negative).build().show();
			}
		};

		MapController.features.runOnMainThread(runnable);
	}
}
