package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector2;

/**
 * Provides a static way to control ads from anywhere. Just needs to be initialized
 * with an {@link AdController} before it can be used.
 * 
 * @author Patrick Ballard
 */
public class AdHelper {
	private static AdController adController;
	private static boolean showAds = false;
	public static String PREF_SHOW_ADS = "ShowAds";

	public static void initAds() {
		if (adController != null) adController.initAds();
	}
	
	public static void startMediation() {
		if (adController != null && showAds) adController.startMediation();
	}

	public static void initialize(AdController controller) {
		adController = controller;
	}

	public static void initShowAds(String prefsName) {
		try {
			// Find out whether the user has purchased the Remove Ads IAP
			Preferences prefs = Gdx.app.getPreferences(prefsName);
			setShowAds(prefs.getBoolean(PREF_SHOW_ADS, true));
			// TODO Comment this out before submitting!
			//setShowAds(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void showBanner() {
		if (adController != null && showAds) adController.showBanner();
	}

	public static void hideBanner() {
		if (adController != null) adController.hideBanner();
	}

	public static void initBanner() {
		if (adController != null) adController.initBanners();
	}

	public static boolean isShowAds() {
		return showAds;
	}

	public static void setShowAds(boolean showAds) {
		AdHelper.showAds = showAds;
	}

	/**
	 * Calculates the expected height of an Admob Smart Banner so the banner knows
	 * how much space the app should reserve for it.
	 * @return The expected height (in exact pixels) of an Admob Smart Banner on this device.
	 */
	public static float getSmartBannerHeight() {
		float height = Gdx.graphics.getHeight();     // Screen height of this device (in pixels)
		float density = Gdx.graphics.getDensity();  // Screen density

		height /= density;

		// These numbers are from Admob. Returns different sizes based on how tall the screen is.
		if (height < 400) {
			return 32f;
		} else if (height <= 720) {
			return 50f;
		} else {
			return 90f;
		}
	}

	public static Vector2 getBannerSize() {
		if (adController != null) return adController.getBannerSize();
		else return Vector2.Zero;
	}
	
	public static float getAccurateScreenHeight() {
		if (adController != null) return adController.getAccurateScreenHeight();
		else return Gdx.graphics.getHeight();
	}

	public static float getBannerHeight() {
		if (adController != null) return getBannerSize().y;
		else return 0;
	}
	
	public static void delayBannerRefresh() {
		if (adController != null) adController.delayBannerRefresh();
	}

	public static void loadInterstitial() {
		if (adController != null && showAds) adController.loadInterstitial();
	}

	public static void showInterstitial() {
		if (adController != null && showAds) adController.showInterstitial();
	}

	public static boolean isInterstitialLoaded() {
		if (adController != null) return adController.isInterstitialLoaded();
		else return false;
	}

	public static boolean isInterstitialFailed() {
		if (adController != null) return adController.isInterstitialFailed();
		else return true;
	}


	/** Used to make sure the iOS UIView is loaded before any operations are performed.
	 * @return Whether or not the iOS UIView is loaded
	 */
	public static boolean isUIViewLoaded() {
		if (adController != null) return adController.isUIViewLoaded();
		return true;
	}

	public static boolean isInterstitialShowing() {
		if (adController != null) return adController.isInterstitialShowing();
		return false;
	}

	/** Resumes the mediation after it has been paused. Call this from the app's
	 * resume() method so mediation can resume when the app comes back from being
	 * in the background. 
	 */
	public static void resumeMediation() {
		if (adController != null) adController.resumeMediation();
	}

	/** Pauses the mediation so it won't be trying to load ads. Call this from the
	 * app's pause() method so mediation won't be running when the app is in
	 * the background.
	 */
	public static void pauseMediation() {
		if (adController != null) adController.pauseMediation();
	}

	/** Removes the ads from the app and saves it as a preference so they will be gone forever.
	 * This should be called after the user purchases the Remove Ads IAP or when they restore
	 * their purchases if they have purchased it in the past. After this, the screen should be
	 * reloaded so the banner will disappear.
	 * @param prefsName The name of the preferences used by the main app.
	 */
	public static void removeAds(String prefsName) {
		AdHelper.stopMediation();
		AdHelper.setShowAds(false);
		AdHelper.dispose();
	}

	private static void stopMediation() {
		if (adController != null) adController.stopMediation(); 
	}

	public static void dispose() {
		adController = null;
	}
}
