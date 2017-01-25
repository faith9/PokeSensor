package com.logickllc.pokesensor;

import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIStatusBarStyle;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;

import static com.logickllc.pokesensor.IOSFeatures.PREFS_NAME;
import static com.logickllc.pokesensor.IOSFeatures.REMOVE_ADS_IAP_PRODUCT_ID;

@CustomClass("SettingsController")
public class SettingsController extends  UITableViewController {

	@IBOutlet
	UITableViewCell contactButton, accountsButton, twitterButton, facebookButton, moreAppsButton, aboutButton, helpButton,
			reviewButton, mySpawnsButton, spawnScanButton, prefsButton, removeAdsButton, restorePurchasesButton, refreshAccountsButton;

	boolean canScan = false;

	public static SettingsController instance = null;

	// TODO Update this every time you add a button
	public final long NUM_ITEMS = 13;

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
	public void viewWillAppear(boolean animated) {
		instance = this;

		if (Utilities.canScan == null)
			canScan = Utilities.scan();
		else
			canScan = Utilities.canScan;
	}

	@Override
	public void didSelectRow(UITableView tableView, NSIndexPath indexPath) {
		UITableViewCell selected = tableView.getCellForRow(indexPath);
		tableView.deselectRow(indexPath, true);
		if (selected == contactButton) {
			Gdx.net.openURI("mailto:logickllc@gmail.com");
		} else if (selected == helpButton) {
			Gdx.net.openURI("https://www.reddit.com/r/pokesensor/comments/4ymkuj/pokesensor_faq_and_troubleshooting/");
		} else if (selected == twitterButton) {
			Gdx.net.openURI("https://twitter.com/LogickLLC");
		} else if (selected == facebookButton) {
			Gdx.net.openURI("https://www.facebook.com/Logick-LLC-984234335029611/");
		} else if (selected == moreAppsButton) {
			Gdx.net.openURI("https://itunes.apple.com/us/developer/patrick-ballard/id1026470545");
		} else if (selected == reviewButton) {
			Gdx.net.openURI("https://appsto.re/us/Mef-db.i");
		} else if (selected == spawnScanButton) {
			MapController.instance.dontRefreshAccounts = true;
			this.getNavigationController().popToRootViewController(true);
			MapController.mapHelper.wideSpawnScan();
		} else if (selected == removeAdsButton) {
			MapController.instance.removeAds();
		} else if (selected == restorePurchasesButton) {
			MapController.instance.restorePurchases();
		} else if (selected == refreshAccountsButton) {
			MapController.features.refreshAccounts();
		}
	}

	@Override
	public void viewWillDisappear(boolean animated) {
		// this.getNavigationController().setNavigationBarHidden(true);
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
}
