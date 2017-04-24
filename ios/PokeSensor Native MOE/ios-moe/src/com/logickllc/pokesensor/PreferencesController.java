package com.logickllc.pokesensor;


import com.logickllc.pokesensor.api.AccountManager;
import com.pokegoapi.util.Signature;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UILabel;
import apple.uikit.UISegmentedControl;
import apple.uikit.UISwitch;
import apple.uikit.UITableViewController;
import apple.uikit.enums.UIControlEvents;

@Runtime(ObjCRuntime.class)
@ObjCClassName("PreferencesController")
@RegisterOnStartup
public class PreferencesController extends UITableViewController {

	//@IBOutlet
	UISwitch collectSpawns, showIvs, showSpawns, showScanDetails, ivsAlwaysVisible, clearMapOnScan, use2Captcha, showMovesets, showHeightWeight;

	//@IBOutlet
	UISegmentedControl captchaMode, imageSize, gpsMode;

	//@IBOutlet
	UILabel ivsAlwaysVisibleLabel, showMovesetsLabel, showHeightWeightLabel;

	public static PreferencesController instance = null;

	private boolean askForKey = true;

	protected PreferencesController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewDidLoad() {
		super.viewDidLoad();
		this.setNeedsStatusBarAppearanceUpdate();
		this.navigationController().setToolbarHiddenAnimated(true, true);
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
		//MapController.mapHelper.useNewApi = useNewApi.isOn();
		MapController.mapHelper.showMovesets = showMovesets.isOn();
		MapController.mapHelper.showHeightWeight = showHeightWeight.isOn();
		//MapController.mapHelper.fallbackApi = fallbackApi.isOn();

		MapController.mapHelper.gpsModeNormal = gpsMode.selectedSegmentIndex() == 1;

		if (captchaMode.selectedSegmentIndex() == 0) MapController.mapHelper.captchaModePopup = true;
		else MapController.mapHelper.captchaModePopup = false;

		MapController.mapHelper.imageSize = imageSize.selectedSegmentIndex();

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
		//NativePreferences.putBoolean(IOSMapHelper.PREF_USE_NEW_API, MapController.mapHelper.useNewApi);
		NativePreferences.putBoolean(IOSMapHelper.PREF_FALLBACK_API, MapController.mapHelper.fallbackApi);
		NativePreferences.putBoolean(IOSMapHelper.PREF_SHOW_MOVESETS, MapController.mapHelper.showMovesets);
		NativePreferences.putBoolean(IOSMapHelper.PREF_SHOW_HEIGHT_WEIGHT, MapController.mapHelper.showHeightWeight);
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
		//useNewApi.setOn(MapController.mapHelper.useNewApi);
		showMovesets.setOn(MapController.mapHelper.showMovesets);
		showHeightWeight.setOn(MapController.mapHelper.showHeightWeight);
		//fallbackApi.setOn(MapController.mapHelper.fallbackApi);
		//fallbackApi.setOn(false);

		if (MapController.mapHelper.captchaModePopup) captchaMode.setSelectedSegmentIndex(0);
		else captchaMode.setSelectedSegmentIndex(1);

		if (MapController.mapHelper.gpsModeNormal) gpsMode.setSelectedSegmentIndex(1);
		else gpsMode.setSelectedSegmentIndex(0);

		imageSize.setSelectedSegmentIndex(MapController.mapHelper.imageSize);

		//defaultMarkersMode.setSelectedSegment(MapController.mapHelper.defaultMarkersMode);

		toggleIvsAlwaysVisible();

		showIvs.addTargetActionForControlEvents(this, new SEL("showIvsSwitched:"), UIControlEvents.ValueChanged);

		use2Captcha.addTargetActionForControlEvents(this, new SEL("use2CaptchaSwitched:"), UIControlEvents.ValueChanged);
	}

	@Selector("showIvsSwitched:")
	void showIvsSwitched(UISwitch sender) {
		toggleIvsAlwaysVisible();
	}

	@Selector("use2CaptchaSwitched:")
	void use2CaptchaSwitched(UISwitch sender) {
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

	@Override
	public boolean isMovingFromParentViewController() {
		return super.isMovingFromParentViewController();
	}

	@Override
	public boolean prefersStatusBarHidden() {
		return false;
	}

	public void toggleIvsAlwaysVisible() {
		ivsAlwaysVisible.setEnabled(showIvs.isOn());
		ivsAlwaysVisibleLabel.setEnabled(showIvs.isOn());
		showMovesets.setEnabled(showIvs.isOn());
		showMovesetsLabel.setEnabled(showIvs.isOn());
		showHeightWeight.setEnabled(showIvs.isOn());
		showHeightWeightLabel.setEnabled(showIvs.isOn());
	}

	/*public void toggleFallbackApi() {
		fallbackApi.setEnabled(false);
		fallbackApiLabel.setEnabled(false);
	}*/

	/*public void askForApiKey() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Lambda positive = new Lambda() {
					@Override
					public void execute() {
						String text = (String) params.get("Text");

						if (text == null || text.equals("")) {
							useNewApi.setOn(false);
							//toggleFallbackApi();
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
						//toggleFallbackApi();
					}
				};
				DialogHelper.textPrompt("Enter API Key", "Enter your PokeHash API key. This will let you use the latest reversed API provided by the PokeFarmer devs for a fee. Note this is not my system. It can be buggy/unavailable at times and I can't do anything about it.", MapController.mapHelper.newApiKey, "Confirm", positive, negative).build().show();
			}
		};

		MapController.features.runOnMainThread(runnable);
	}*/

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

	@IBOutlet
	@Property
	@Selector("collectSpawns")
	public UISwitch getCollectSpawns() { return collectSpawns; }

	@IBOutlet
	@Property
	@Selector("setCollectSpawns:")
	public void setCollectSpawns(UISwitch collectSpawns) { this.collectSpawns = collectSpawns; }

	@IBOutlet
	@Property
	@Selector("showIvs")
	public UISwitch getShowIvs() { return showIvs; }

	@IBOutlet
	@Property
	@Selector("setShowIvs:")
	public void setShowIvs(UISwitch showIvs) { this.showIvs = showIvs; }

	@IBOutlet
	@Property
	@Selector("showSpawns")
	public UISwitch getShowSpawns() { return showSpawns; }

	@IBOutlet
	@Property
	@Selector("setShowSpawns:")
	public void setShowSpawns(UISwitch showSpawns) { this.showSpawns = showSpawns; }

	@IBOutlet
	@Property
	@Selector("showScanDetails")
	public UISwitch getShowScanDetails() { return showScanDetails; }

	@IBOutlet
	@Property
	@Selector("setShowScanDetails:")
	public void setShowScanDetails(UISwitch showScanDetails) { this.showScanDetails = showScanDetails; }

	@IBOutlet
	@Property
	@Selector("ivsAlwaysVisible")
	public UISwitch getIvsAlwaysVisible() { return ivsAlwaysVisible; }

	@IBOutlet
	@Property
	@Selector("setIvsAlwaysVisible:")
	public void setIvsAlwaysVisible(UISwitch ivsAlwaysVisible) { this.ivsAlwaysVisible = ivsAlwaysVisible; }

	@IBOutlet
	@Property
	@Selector("clearMapOnScan")
	public UISwitch getClearMapOnScan() { return clearMapOnScan; }

	@IBOutlet
	@Property
	@Selector("setClearMapOnScan:")
	public void setClearMapOnScan(UISwitch clearMapOnScan) { this.clearMapOnScan = clearMapOnScan; }

	@IBOutlet
	@Property
	@Selector("use2Captcha")
	public UISwitch getUse2Captcha() { return use2Captcha; }

	@IBOutlet
	@Property
	@Selector("setUse2Captcha:")
	public void setUse2Captcha(UISwitch use2Captcha) { this.use2Captcha = use2Captcha; }

	@IBOutlet
	@Property
	@Selector("showMovesets")
	public UISwitch getShowMovesets() { return showMovesets; }

	@IBOutlet
	@Property
	@Selector("setShowMovesets:")
	public void setShowMovesets(UISwitch showMovesets) { this.showMovesets = showMovesets; }

	@IBOutlet
	@Property
	@Selector("showHeightWeight")
	public UISwitch getShowHeightWeight() { return showHeightWeight; }

	@IBOutlet
	@Property
	@Selector("setShowHeightWeight:")
	public void setShowHeightWeight(UISwitch showHeightWeight) { this.showHeightWeight = showHeightWeight; }

	@IBOutlet
	@Property
	@Selector("captchaMode")
	public UISegmentedControl getCaptchaMode() { return captchaMode; }

	@IBOutlet
	@Property
	@Selector("setCaptchaMode:")
	public void setCaptchaMode(UISegmentedControl captchaMode) { this.captchaMode = captchaMode; }

	@IBOutlet
	@Property
	@Selector("imageSize")
	public UISegmentedControl getImageSize() { return imageSize; }

	@IBOutlet
	@Property
	@Selector("setImageSize:")
	public void setImageSize(UISegmentedControl imageSize) { this.imageSize = imageSize; }

	@IBOutlet
	@Property
	@Selector("gpsMode")
	public UISegmentedControl getGpsMode() { return gpsMode; }

	@IBOutlet
	@Property
	@Selector("setGpsMode:")
	public void setGpsMode(UISegmentedControl gpsMode) { this.gpsMode = gpsMode; }

	@IBOutlet
	@Property
	@Selector("ivsAlwaysVisibleLabel")
	public UILabel getIvsAlwaysVisibleLabel() { return ivsAlwaysVisibleLabel; }

	@IBOutlet
	@Property
	@Selector("setIvsAlwaysVisibleLabel:")
	public void setIvsAlwaysVisibleLabel(UILabel ivsAlwaysVisibleLabel) { this.ivsAlwaysVisibleLabel = ivsAlwaysVisibleLabel; }

	@IBOutlet
	@Property
	@Selector("showMovesetsLabel")
	public UILabel getShowMovesetsLabel() { return showMovesetsLabel; }

	@IBOutlet
	@Property
	@Selector("setShowMovesetsLabel:")
	public void setShowMovesetsLabel(UILabel showMovesetsLabel) { this.showMovesetsLabel = showMovesetsLabel; }

	@IBOutlet
	@Property
	@Selector("showHeightWeightLabel")
	public UILabel getShowHeightWeightLabel() { return showHeightWeightLabel; }

	@IBOutlet
	@Property
	@Selector("setShowHeightWeightLabel:")
	public void setShowHeightWeightLabel(UILabel showHeightWeightLabel) { this.showHeightWeightLabel = showHeightWeightLabel; }
}
