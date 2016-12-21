package com.logickllc.pokesensor;

import java.util.ArrayList;
import java.util.Calendar;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.bindings.admob.GADAdSize;
import org.robovm.bindings.admob.GADBannerView;
import org.robovm.bindings.admob.GADBannerViewDelegateAdapter;
import org.robovm.bindings.admob.GADGender;
import org.robovm.bindings.admob.GADRequest;
import org.robovm.bindings.admob.GADRequestError;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;

public class IOSLauncher extends IOSApplication.Delegate {
	public static NavigationController navigationController;
	public static final boolean IS_AD_TESTING = false;
	private static final String TEST_IPHONESE_ID = ""; // My iPhone SE
	private static final String TEST_IPAD_ID = ""; // iPad 3
	private static final String BANNER_ID = "";
	private static ArrayList<String> testDevices;
	private ArrayList<String> keywords;
	private GADBannerView admobBanner;
	private boolean isPortrait = true;
	public static IOSLauncher instance;

	@Override
	protected IOSApplication createApplication() {
		instance = this;
		IOSApplicationConfiguration config = new IOSApplicationConfiguration();
		return new IOSApplication(new PokeSensor(), config);
	}

	public static void main(String[] argv) {
		NSAutoreleasePool pool = new NSAutoreleasePool();
		UIApplication.main(argv, null, IOSLauncher.class);
		pool.close();
	}

	public void initAds() {
		String simID = GADRequest.GAD_SIMULATOR_ID;
		testDevices = new ArrayList<String>();
		testDevices.add(TEST_IPHONESE_ID);
		testDevices.add(TEST_IPAD_ID);
		testDevices.add(simID);
		keywords = new ArrayList<String>();
		keywords.add("Pokemon");
		keywords.add("Pokemon GO");
		keywords.add("Yugioh");
		keywords.add("Card game");
		keywords.add("Trading cards");
		keywords.add("Video games");
		keywords.add("Digimon");
		initAdmobBanner();
	}

	public void initAdmobBanner() {
		if (isPortrait)
			admobBanner = new GADBannerView(GADAdSize.smartBannerPortrait());
		else
			admobBanner = new GADBannerView(GADAdSize.smartBannerLandscape());
		admobBanner.setAdUnitID(BANNER_ID);
		// Notify the mediator when the banner loads or fails
		admobBanner.setDelegate(new GADBannerViewDelegateAdapter() {

			@Override
			public void didReceiveAd(GADBannerView view) {
				try {
					System.out.println("Loaded admob");
					show();
					super.didReceiveAd(view);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void didFailToReceiveAd(GADBannerView view, GADRequestError error) {
				try {
					super.didFailToReceiveAd(view, error);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});

		// Add it to the screen
		admobBanner.setRootViewController(MapController.instance);
		MapController.instance.getView().addSubview(admobBanner);
		// Make it start requesting ads
		GADRequest request = new GADRequest();
		request.setGender(GADGender.Male);
		request.setBirthday(1, 1, Calendar.getInstance().get(Calendar.YEAR) - 1900 - 18);
		request.setKeywords(keywords);
		request.setTestDevices(testDevices);
		admobBanner.loadRequest(request);
	}

	public void show() {
		try {
			CGSize screenSize = UIScreen.getMainScreen().getBounds().getSize();
			double screenWidth = screenSize.getWidth();
			double screenHeight = screenSize.getHeight();

			CGSize adSize = admobBanner.getBounds().getSize();
			double adHeight = adSize.getHeight();
			double adWidth = adSize.getWidth();

			float bannerWidth = (float) screenWidth;
			float bannerHeight = (float) (bannerWidth / adWidth * adHeight);
			admobBanner.setFrame(new CGRect(screenWidth / 2 - adWidth / 2, 0, bannerWidth, bannerHeight));

			System.out.println("Showing Admob banner");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void hide() {
		try {
			CGSize screenSize = UIScreen.getMainScreen().getBounds().getSize();
			double screenWidth = screenSize.getWidth();
			double screenHeight = screenSize.getHeight();

			CGSize adSize = admobBanner.getBounds().getSize();
			double adHeight = adSize.getHeight();
			double adWidth = adSize.getWidth();

			float bannerWidth = (float) screenWidth;
			float bannerHeight = (float) (bannerWidth / adWidth * adHeight);

			// Set the coords and size of the admobBanner
			admobBanner.setFrame(new CGRect(0, (-5) * bannerHeight, bannerWidth, bannerHeight));
			System.out.println("Hiding Admob banner");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void changeOrientation(boolean portrait) {
		isPortrait = portrait;
		if (admobBanner == null)
			return;

		if (portrait)
			admobBanner.setAdSize(GADAdSize.smartBannerPortrait());
		else
			admobBanner.setAdSize(GADAdSize.smartBannerLandscape());
		
		hide();
		
		GADRequest request = new GADRequest();
		request.setGender(GADGender.Male);
		request.setBirthday(1, 1, Calendar.getInstance().get(Calendar.YEAR) - 1900 - 18);
		request.setKeywords(keywords);
		request.setTestDevices(testDevices);
		admobBanner.loadRequest(request);
	}
	
	public Vector2 getSize() {
		// Sometimes the iPhone scales the app's display to account for retina graphics. This is how I account for that.
		if (isPortrait) return new Vector2((float) (GADAdSize.smartBannerPortrait().toCGSize().getWidth()), (float) (GADAdSize.smartBannerPortrait().toCGSize().getHeight()));
		else return new Vector2((float) (GADAdSize.smartBannerLandscape().toCGSize().getWidth()), (float) (GADAdSize.smartBannerLandscape().toCGSize().getHeight()));
	}
}
