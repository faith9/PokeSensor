package com.logickllc.pokesensor;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/** This controls ad mediation for both banner ads and interstitial ads. Just add some
 * banners or interstitials and supply the relevant info (mediation order, refresh rate, etc.)
 * and this will handle mediation between them.
 * <br><br>
 * It will try ads in order of their mediation order (starting at 1). If an ad fails, it calls the next
 * and so on until an ad loads or all ads fail. At that point, it will start over at the beginning.
 * Note that banner mediation will pause for a short amount of time after all ads fail, supplied
 * by {@link AdMediator#BANNER_MEDIATION_RESET_RATE}. Interstitial mediation just
 * won't show an ad until it's requested again if all the ads fail.
 * <br><br>
 * The {@link AdMediator#pauseMediation()} and {@link AdMediator#resumeMediation()} methods
 * should be called from the pause() and resume() methods of the app, respectively.
 * 
 * @author Patrick Ballard
 */
public class AdMediator {
	ArrayList<AdMediationBanner> banners = new ArrayList<AdMediationBanner>(); // List of all our banners
	AdMediationBanner currentBanner;
	Timer bannerTimer;
	final long BANNER_MEDIATION_RESET_RATE = 15000; // Wait 15 seconds before resetting mediation
	
	ArrayList<AdMediationInterstitial> interstitials = new ArrayList<AdMediationInterstitial>(); // List of all our interstitials
	AdMediationInterstitial currentInterstitial;
	boolean isInterstitialLoaded = false;
	boolean isInterstitialFailed = false;
	
	private boolean stopped = false;
	
	// TODO ** BEGIN BANNER FUNCTIONS ** //
	/** Adds a banner into the mediation. The banner must at least have a mediation order
	 * at this point and the banner's order must be 1 more than the last added banner's order, or
	 * 1 if it is the first banner added. It needs to have the name and refresh rate set before
	 * starting mediation or it will have the default values.
	 * @param banner The banner to add to mediation.
	 */
	public void addBanner(AdMediationBanner banner) {
		// Mediation order starts at 1 and goes up
		if (banner.getMediationOrder() <= 0) {
			System.out.println("Invalid mediation order " + Integer.toString(banner.getMediationOrder()) + " for banner " + banner.getName());
			return;
		}
		
		System.out.println("About to add banner");
		banners.add(banner.getMediationOrder() - 1, banner);
	}
	
	/** Initialize all banners and start showing ads. This starts a chain reaction
	 * which is self-sustaining, so after it starts nothing else needs to be done.
	 */
	public void startBannerMediation() {
		// Initialize all the banners
		for (AdMediationBanner banner : banners) banner.init();
		
		currentBanner = banners.get(0); // Start with the first banner in the waterfall
		currentBanner.load();
		currentBanner.show();
	}
	
	/** Starts a timer set to the refresh rate of the current banner. When it runs, it tries to
	 * load the first banner in mediation. If the current banner refreshes automatically
	 * by the ad server, this method will do nothing. 
	 */
	public void startBannerTimer() {
		if (currentBanner.getRefreshRate() == 0) return; // This means the ads will refresh automatically
		
		System.out.println("Starting banner timer");
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				// Load and show the first banner in mediation
				currentBanner = banners.get(0);
				currentBanner.load();
				currentBanner.show();
			}
		};
		
		// Kill the old timer and make a new one
		if (bannerTimer != null) bannerTimer.cancel();
		bannerTimer = new Timer();
		
		// Schedule the task to take place after the current banner's refresh rate
		bannerTimer.schedule(task, currentBanner.getRefreshRate());
	}
	
	//** Callbacks **//
	// These need to be called from the corresponding callbacks in each banner
	/** What to do when the banner loads. Call this from the onLoad() listener of
	 * the actual banner object. 
	 */
	public void onBannerLoad() {
		// Start the timer to load another banner when this banner's time is up
		hideInactiveBanners();
		startBannerTimer();
		System.out.println(currentBanner.getName() + " loaded");
	}
	
	/** What to do when the banner fails. Call this from the onFail() listener of
	 * the actual banner object. 
	 */
	public void onBannerFail() {
		System.out.println(currentBanner.getName() + " failed to load");
		
		if (isStopped()) return; // Do nothing, we're done here
		
		// Keep a handle to the current banner and get the next banner in mediation
		final AdMediationBanner previousBanner = currentBanner;
		currentBanner = banners.get(currentBanner.getMediationOrder() % banners.size());
		
		if (currentBanner.getMediationOrder() == 1) {
			// We failed all banners and wrapped to the beginning
			System.out.println(banners.toString());
			
			// Reset mediation after a small delay
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					// Hide the previous banner so we can show the new one
					previousBanner.hide();
					currentBanner.load();
					currentBanner.show();
				}
			};
			
			// Make a new timer and schedule the reset task on the specified interval
			bannerTimer = new Timer();
			bannerTimer.schedule(task, BANNER_MEDIATION_RESET_RATE);
			currentBanner.show();
		} else {
			// We stil have more options, try those immediately
			previousBanner.hide();
			currentBanner.show();
			currentBanner.load();
		}
	}
	
	//** Utility functions **//
	/** Finds the tallest banner and returns its size. This makes sure that you can allocate screen space once
	 * to accomodate all banners in the mediation. That way you can tell how much dedicated screen space your app will
	 * have at all times so nothing gets covered up by a large banner.
	 * @return The size of the tallest banner this mediator has.
	 */
	public Vector2 getBannerSize() {
		// We need to figure out the max room in the Y dimension that we need to show all banners
		// So we can set our banner holder to that and not have it changing size
		float maxY = 0;
		AdMediationBanner biggestBanner = null;
		
		// Loop thru all the banners and find the tallest one
		for (AdMediationBanner banner : banners) {
			if (banner.getSize().y > maxY) {
				maxY = banner.getSize().y;
				biggestBanner = banner;
			}
		}
		
		if (biggestBanner != null) return biggestBanner.getSize();
		else return Vector2.Zero;
	}
	
	/** Change the orientation of the banner to match the screen orientation */
	public void changeBannerOrientation(boolean portrait) {
		for (AdMediationBanner banner : banners)	banner.changeOrientation(portrait);
		
		if (currentBanner == null) return;
		hideInactiveBanners();
		currentBanner.setShowing(false);
		hideBanner();
		resumeMediation();
		//currentBanner.load();
		//currentBanner.show(); // Refresh the position of the current banner to account for status bar changes
	}
	
	/** Shows the current banner if it was previously hidden.
	 */
	public void showBanner() {
		hideInactiveBanners();
		currentBanner.load();
		currentBanner.show();
	}
	
	/** Hides the banner and pauses mediation.
	 */
	public void hideBanner() {
		currentBanner.hide();
		pauseMediation();
	}
	
	private void hideInactiveBanners() {
		// Hides all but the current banner
		for (AdMediationBanner banner : banners) {
			if (!banner.equals(currentBanner)) banner.hide();
		}
	}
	
	public AdMediationBanner getCurrentBanner() {
		return currentBanner;
	}

	/** You should normally let the mediator handle this. Use with caution.
	 * @param currentBanner The banner that you want to be the current banner
	 */
	public void setCurrentBanner(AdMediationBanner currentBanner) {
		this.currentBanner = currentBanner;
	}
	
	private void resumeBannerMediation() {
		currentBanner = banners.get(0); // Start with the first banner in the waterfall
		showBanner();
	}
	// ** END BANNER FUNCTIONS ** //
	
	
	// TODO ** BEGIN INTERSTITIAL FUNCTIONS ** //
	
	/** Adds an interstitial into the mediation. The interstitial must at least have a mediation order
	 * at this point and the interstitial's order must be 1 more than the last added interstitial's order, or
	 * 1 if it is the first interstitial added. It needs to have the name set before
	 * starting mediation or it will have the default value of "".
	 * @param interstitial The interstitial to add to mediation.
	 */
	public void addInterstitial(AdMediationInterstitial interstitial) {
		// Mediation order starts at 1 and goes up
		if (interstitial.getMediationOrder() <= 0) {
			System.out.println("Invalid mediation order " + Integer.toString(interstitial.getMediationOrder()) + " for interstitial " + interstitial.getName());
			return;
		}
		
		// Add this interstitial into the list of our interstitials
		System.out.println("About to add interstitial");
		interstitials.add(interstitial.getMediationOrder() - 1, interstitial);
	}
	
	/** Initializes all interstitials so they will be ready when they are wanted. 
	 */
	public void startInterstitialMediation() {
		// Initialize all the interstitials
		for (AdMediationInterstitial interstitial : interstitials) interstitial.init();
		
		currentInterstitial = interstitials.get(0); // Start with the first interstitial in the waterfall
	}
	
	public void loadInterstitial() {
		// Reset the flags before trying to load
		isInterstitialLoaded = false;
		isInterstitialFailed = false;
		
		currentInterstitial = interstitials.get(0);
		currentInterstitial.load();
	}
	
	public void showInterstitial() {
		currentInterstitial.show();
		isInterstitialLoaded = false;
		isInterstitialFailed = false;
	}
	
	public boolean isInterstitialLoaded() {
		return isInterstitialLoaded;
	}
	
	public void onInterstitialLoad() {
		System.out.println(currentInterstitial.getName() + " loaded");
		isInterstitialLoaded = true;
		isInterstitialFailed = false;
	}
	
	/** This should only be true if all interstitials failed to load.
	 * @return Whether or not all interstitials in mediation have failed to load.
	 */
	public boolean isInterstitialFailed() {
		return isInterstitialFailed;
	}
	
	public void onInterstitialFail() {
		System.out.println(currentInterstitial.getName() + " failed to load");
		
		if (isStopped()) return; // Do nothing, we're done here
		
		// Get the next interstitial in the mediation
		currentInterstitial = interstitials.get(currentInterstitial.getMediationOrder() % interstitials.size());
		
		if (currentInterstitial.getMediationOrder() == 1) {
			// All interstitials failed and we wrapped around to the beginning of mediation
			// Mark this as a failure
			isInterstitialFailed = true;
			isInterstitialLoaded = false;
		} else {
			currentInterstitial.load();
		}
	}

	// Lifecycle functions. These should be called from AdHelper
	/** Pauses the mediation so it won't be trying to load ads. Call this from the
	 * app's pause() method so mediation won't be running when the app is in
	 * the background.
	 */
	public void pauseMediation() {
		try {
			if (bannerTimer != null) bannerTimer.cancel();
			System.out.println("Mediation paused");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Resumes the mediation after it has been paused. Call this from the app's
	 * resume() method so mediation can resume when the app comes back from being
	 * in the background. 
	 */
	public void resumeMediation() {
		try {
			resumeBannerMediation();
			System.out.println("Mediation resumed");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isStopped() { return stopped; }
	
	/** Stops the mediation and hides all traces of ads. This is usually
	 * called when the user has just purchased the Remove Ads IAP. 
	 */
	public void stopMediation() {
		pauseMediation();
		hideBanner();
		banners.clear();
		interstitials.clear();
		stopped = true;
	}
}
