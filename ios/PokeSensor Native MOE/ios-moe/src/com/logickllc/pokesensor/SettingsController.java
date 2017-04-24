package com.logickllc.pokesensor;


import com.badlogic.gdx.Gdx;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.foundation.NSIndexPath;
import apple.uikit.UILabel;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.UITableViewController;

@Runtime(ObjCRuntime.class)
@ObjCClassName("SettingsController")
@RegisterOnStartup
public class SettingsController extends UITableViewController {

	//@IBOutlet
	UITableViewCell contactButton, accountsButton, twitterButton, facebookButton, moreAppsButton, aboutButton, helpButton,
			reviewButton, mySpawnsButton, discordButton, prefsButton, removeAdsButton, restorePurchasesButton, refreshAccountsButton, paidApiKeyButton;

	//@IBOutlet
	UILabel backgroundScanningLabel, paidApiLabel;

	public static SettingsController instance = null;

	protected SettingsController(Pointer peer) {
		super(peer);
	}
	
	@Override
	public void viewDidLoad() {
		super.viewDidLoad();
		this.setNeedsStatusBarAppearanceUpdate();
		this.navigationController().setToolbarHiddenAnimated(true, true);
	}

	@Override
	public void viewWillAppear(boolean animated) {
		instance = this;

		paidApiLabel.setText("Paid API Key (" + PokeHashProvider.VERSION_STRING + ")");
	}

	@Override
	public void tableViewDidSelectRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
		UITableViewCell selected = tableView.cellForRowAtIndexPath(indexPath);
		tableView.deselectRowAtIndexPathAnimated(indexPath, true);
		if (selected == contactButton) {

		} else if (selected == helpButton) {
			Gdx.net.openURI("https://www.reddit.com/r/pokesensor/comments/4ymkuj/pokesensor_faq_and_troubleshooting/");
		} else if (selected == twitterButton) {

		} else if (selected == facebookButton) {

		} else if (selected == moreAppsButton) {
			Gdx.net.openURI("https://itunes.apple.com/us/developer/patrick-ballard/id1026470545");
		} else if (selected == reviewButton) {

		} else if (selected == discordButton) {
			Gdx.net.openURI("https://discord.gg/69m8NKW");
		} else if (selected == removeAdsButton) {
			MapController.instance.removeAds();
		} else if (selected == restorePurchasesButton) {
			MapController.instance.restorePurchases();
		} else if (selected == refreshAccountsButton) {
			MapController.features.refreshAccounts();
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

	@IBOutlet
	@Property
	@Selector("contactButton")
	public UITableViewCell getContactButton() { return contactButton; }

	@IBOutlet
	@Property
	@Selector("setContactButton:")
	public void setContactButton(UITableViewCell contactButton) { this.contactButton = contactButton; }

	@IBOutlet
	@Property
	@Selector("accountsButton")
	public UITableViewCell getAccountsButton() { return accountsButton; }

	@IBOutlet
	@Property
	@Selector("setAccountsButton:")
	public void setAccountsButton(UITableViewCell accountsButton) { this.accountsButton = accountsButton; }

	@IBOutlet
	@Property
	@Selector("twitterButton")
	public UITableViewCell getTwitterButton() { return twitterButton; }

	@IBOutlet
	@Property
	@Selector("setTwitterButton:")
	public void setTwitterButton(UITableViewCell twitterButton) { this.twitterButton = twitterButton; }

	@IBOutlet
	@Property
	@Selector("facebookButton")
	public UITableViewCell getFacebookButton() { return facebookButton; }

	@IBOutlet
	@Property
	@Selector("setFacebookButton:")
	public void setFacebookButton(UITableViewCell facebookButton) { this.facebookButton = facebookButton; }

	@IBOutlet
	@Property
	@Selector("moreAppsButton")
	public UITableViewCell getMoreAppsButton() { return moreAppsButton; }

	@IBOutlet
	@Property
	@Selector("setMoreAppsButton:")
	public void setMoreAppsButton(UITableViewCell moreAppsButton) { this.moreAppsButton = moreAppsButton; }

	@IBOutlet
	@Property
	@Selector("aboutButton")
	public UITableViewCell getAboutButton() { return aboutButton; }

	@IBOutlet
	@Property
	@Selector("setAboutButton:")
	public void setAboutButton(UITableViewCell aboutButton) { this.aboutButton = aboutButton; }

	@IBOutlet
	@Property
	@Selector("helpButton")
	public UITableViewCell getHelpButton() { return helpButton; }

	@IBOutlet
	@Property
	@Selector("setHelpButton:")
	public void setHelpButton(UITableViewCell helpButton) { this.helpButton = helpButton; }

	@IBOutlet
	@Property
	@Selector("reviewButton")
	public UITableViewCell getReviewButton() { return reviewButton; }

	@IBOutlet
	@Property
	@Selector("setReviewButton:")
	public void setReviewButton(UITableViewCell reviewButton) { this.reviewButton = reviewButton; }

	@IBOutlet
	@Property
	@Selector("mySpawnsButton")
	public UITableViewCell getMySpawnsButton() { return mySpawnsButton; }

	@IBOutlet
	@Property
	@Selector("setMySpawnsButton:")
	public void setMySpawnsButton(UITableViewCell mySpawnsButton) { this.mySpawnsButton = mySpawnsButton; }

	@IBOutlet
	@Property
	@Selector("discordButton")
	public UITableViewCell getDiscordButton() { return discordButton; }

	@IBOutlet
	@Property
	@Selector("setDiscordButton:")
	public void setDiscordButton(UITableViewCell discordButton) { this.discordButton = discordButton; }

	@IBOutlet
	@Property
	@Selector("prefsButton")
	public UITableViewCell getPrefsButton() { return prefsButton; }

	@IBOutlet
	@Property
	@Selector("setPrefsButton:")
	public void setPrefsButton(UITableViewCell prefsButton) { this.prefsButton = prefsButton; }

	@IBOutlet
	@Property
	@Selector("removeAdsButton")
	public UITableViewCell getRemoveAdsButton() { return removeAdsButton; }

	@IBOutlet
	@Property
	@Selector("setRemoveAdsButton:")
	public void setRemoveAdsButton(UITableViewCell removeAdsButton) { this.removeAdsButton = removeAdsButton; }

	@IBOutlet
	@Property
	@Selector("restorePurchasesButton")
	public UITableViewCell getRestorePurchasesButton() { return restorePurchasesButton; }

	@IBOutlet
	@Property
	@Selector("setRestorePurchasesButton:")
	public void setRestorePurchasesButton(UITableViewCell restorePurchasesButton) { this.restorePurchasesButton = restorePurchasesButton; }

	@IBOutlet
	@Property
	@Selector("refreshAccountsButton")
	public UITableViewCell getRefreshAccountsButton() { return refreshAccountsButton; }

	@IBOutlet
	@Property
	@Selector("setRefreshAccountsButton:")
	public void setRefreshAccountsButton(UITableViewCell refreshAccountsButton) { this.refreshAccountsButton = refreshAccountsButton; }

	@IBOutlet
	@Property
	@Selector("paidApiKeyButton")
	public UITableViewCell getPaidApiKeyButton() { return paidApiKeyButton; }

	@IBOutlet
	@Property
	@Selector("setPaidApiKeyButton:")
	public void setPaidApiKeyButton(UITableViewCell paidApiKeyButton) { this.paidApiKeyButton = paidApiKeyButton; }

	@IBOutlet
	@Property
	@Selector("backgroundScanningLabel")
	public UILabel getBackgroundScanningLabel() { return backgroundScanningLabel; }

	@IBOutlet
	@Property
	@Selector("setBackgroundScanningLabel:")
	public void setBackgroundScanningLabel(UILabel backgroundScanningLabel) { this.backgroundScanningLabel = backgroundScanningLabel; }

	@IBOutlet
	@Property
	@Selector("paidApiLabel")
	public UILabel getPaidApiLabel() { return paidApiLabel; }

	@IBOutlet
	@Property
	@Selector("setPaidApiLabel:")
	public void setPaidApiLabel(UILabel paidApiLabel) { this.paidApiLabel = paidApiLabel; }
}
