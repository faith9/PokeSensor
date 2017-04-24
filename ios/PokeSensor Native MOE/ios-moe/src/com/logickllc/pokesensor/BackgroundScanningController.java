package com.logickllc.pokesensor;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.foundation.NSIndexPath;
import apple.uikit.UIApplication;
import apple.uikit.UISwitch;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.UITableViewController;
import apple.uikit.UITextField;
import apple.uikit.UIUserNotificationSettings;
import apple.uikit.enums.UIControlEvents;
import apple.uikit.enums.UIStatusBarStyle;
import apple.uikit.enums.UIUserNotificationType;

@Runtime(ObjCRuntime.class)
@ObjCClassName("BackgroundScanningController")
@RegisterOnStartup
public class BackgroundScanningController extends UITableViewController {

	//@IBOutlet
	UISwitch backgroundScanning, includeNearbyPokemon, captchaNotifications, scanIvs, notificationSound, onlyScanSpawns;

	//@IBOutlet
	UITextField scanInterval;

	//@IBOutlet
	UITableViewCell captchaButton;

	protected BackgroundScanningController(Pointer peer) {
		super(peer);
	}

	@Override
	public long preferredStatusBarStyle() {
		return UIStatusBarStyle.Default;
	}

	@Override
	public void viewDidLoad() {
		super.viewDidLoad();
		this.setNeedsStatusBarAppearanceUpdate();
		this.navigationController().setToolbarHiddenAnimated(true, true);

		MapController.addDoneButtonToKeyboard(scanInterval);
	}

	@Override
	public void viewWillDisappear(boolean b) {
		MapController.mapHelper.backgroundScanning = backgroundScanning.isOn();
		MapController.mapHelper.backgroundScanIvs = scanIvs.isOn();
		MapController.mapHelper.backgroundIncludeNearby = includeNearbyPokemon.isOn();
		MapController.mapHelper.captchaNotifications = captchaNotifications.isOn();
		MapController.mapHelper.backgroundNotificationSound = notificationSound.isOn();
		MapController.mapHelper.onlyScanSpawns = onlyScanSpawns.isOn();

		try {
			String text = scanInterval.text();
			if (Integer.parseInt(text) > 0) {
				MapController.mapHelper.backgroundInterval = text;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		NativePreferences.lock();

		NativePreferences.putBoolean(IOSMapHelper.PREF_BACKGROUND_SCANNING, MapController.mapHelper.backgroundScanning);
		NativePreferences.putBoolean(IOSMapHelper.PREF_BACKGROUND_SCAN_IVS, MapController.mapHelper.backgroundScanIvs);
		NativePreferences.putBoolean(IOSMapHelper.PREF_BACKGROUND_INCLUDE_NEARBY, MapController.mapHelper.backgroundIncludeNearby);
		NativePreferences.putBoolean(IOSMapHelper.PREF_CAPTCHA_NOTIFICATIONS, MapController.mapHelper.captchaNotifications);
		NativePreferences.putString(IOSMapHelper.PREF_BACKGROUND_INTERVAL, MapController.mapHelper.backgroundInterval);
		NativePreferences.putBoolean(IOSMapHelper.PREF_BACKGROUND_NOTIFICATION_SOUND, MapController.mapHelper.backgroundNotificationSound);
		NativePreferences.putBoolean(IOSMapHelper.PREF_ONLY_SCAN_SPAWNS, MapController.mapHelper.onlyScanSpawns);

		NativePreferences.unlock();
	}

	@Override
	public void viewWillAppear(boolean animated) {
		backgroundScanning.setOn(MapController.mapHelper.backgroundScanning);
		scanIvs.setOn(MapController.mapHelper.backgroundScanIvs);
		includeNearbyPokemon.setOn(MapController.mapHelper.backgroundIncludeNearby);
		captchaNotifications.setOn(MapController.mapHelper.captchaNotifications);
		scanInterval.setText(MapController.mapHelper.backgroundInterval);
		notificationSound.setOn(MapController.mapHelper.backgroundNotificationSound);
		onlyScanSpawns.setOn(MapController.mapHelper.onlyScanSpawns);

		backgroundScanning.addTargetActionForControlEvents(this, new SEL("backgroundScanningSwitched:"), UIControlEvents.ValueChanged);
	}

	@Selector("backgroundScanningSwitched:")
	void backgroundScanningSwitched(UISwitch sender) {
		if (backgroundScanning.isOn()) {
			MapController.manager.requestAlwaysAuthorization();
			UIApplication.sharedApplication().registerUserNotificationSettings(UIUserNotificationSettings.settingsForTypesCategories(UIUserNotificationType.Alert | UIUserNotificationType.Sound | UIUserNotificationType.Badge, null));
			MapController.mapHelper.backgroundScanning = backgroundScanning.isOn();
		}
	}

	@Override
	public void tableViewDidSelectRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
		UITableViewCell selected = tableView.cellForRowAtIndexPath(indexPath);
		tableView.deselectRowAtIndexPathAnimated(indexPath, true);
	}

	@Override
	public UITableViewCell tableViewCellForRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
		return super.tableViewCellForRowAtIndexPath(tableView, indexPath);
	}

	@Override
	public double tableViewHeightForRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
			return super.tableViewHeightForRowAtIndexPath(tableView, indexPath);
	}

	@Override
	public boolean isMovingFromParentViewController() {
		return super.isMovingFromParentViewController();
	}

	@Override
	public boolean prefersStatusBarHidden() {
		return false;
	}

	@IBOutlet
	@Property
	@Selector("backgroundScanning")
	public UISwitch getBackgroundScanning() { return backgroundScanning; }

	@IBOutlet
	@Property
	@Selector("setBackgroundScanning:")
	public void setBackgroundScanning(UISwitch backgroundScanning) { this.backgroundScanning = backgroundScanning; }

	@IBOutlet
	@Property
	@Selector("includeNearbyPokemon")
	public UISwitch getIncludeNearbyPokemon() { return includeNearbyPokemon; }

	@IBOutlet
	@Property
	@Selector("setIncludeNearbyPokemon:")
	public void setIncludeNearbyPokemon(UISwitch includeNearbyPokemon) { this.includeNearbyPokemon = includeNearbyPokemon; }

	@IBOutlet
	@Property
	@Selector("captchaNotifications")
	public UISwitch getCaptchaNotifications() { return captchaNotifications; }

	@IBOutlet
	@Property
	@Selector("setCaptchaNotifications:")
	public void setCaptchaNotifications(UISwitch captchaNotifications) { this.captchaNotifications = captchaNotifications; }

	@IBOutlet
	@Property
	@Selector("scanIvs")
	public UISwitch getScanIvs() { return scanIvs; }

	@IBOutlet
	@Property
	@Selector("setScanIvs:")
	public void setScanIvs(UISwitch scanIvs) { this.scanIvs = scanIvs; }

	@IBOutlet
	@Property
	@Selector("notificationSound")
	public UISwitch getNotificationSound() { return notificationSound; }

	@IBOutlet
	@Property
	@Selector("setNotificationSound:")
	public void setNotificationSound(UISwitch notificationSound) { this.notificationSound = notificationSound; }

	@IBOutlet
	@Property
	@Selector("onlyScanSpawns")
	public UISwitch getOnlyScanSpawns() { return onlyScanSpawns; }

	@IBOutlet
	@Property
	@Selector("setOnlyScanSpawns:")
	public void setOnlyScanSpawns(UISwitch onlyScanSpawns) { this.onlyScanSpawns = onlyScanSpawns; }

	@IBOutlet
	@Property
	@Selector("scanInterval")
	public UITextField getScanInterval() { return scanInterval; }

	@IBOutlet
	@Property
	@Selector("setScanInterval:")
	public void setScanInterval(UITextField scanInterval) { this.scanInterval = scanInterval; }

	@IBOutlet
	@Property
	@Selector("captchaButton")
	public UITableViewCell getCaptchaButton() { return captchaButton; }

	@IBOutlet
	@Property
	@Selector("setCaptchaButton:")
	public void setCaptchaButton(UITableViewCell captchaButton) { this.captchaButton = captchaButton; }
}
