package com.logickllc.pokesensor;

import com.badlogic.gdx.math.Vector2;

/**
 * Provides a platform-independent way to control mobile advertisements
 * from any Java class. For LibGDX, this should be implemented by the Android
 * and iOS launchers. The launcher should then be passed to the main GDX class
 * via its constructor.
 * 
 * @author Patrick Ballard
 */
public interface AdController {

	public abstract void initAds();
	
	public abstract void initBanners();
	
	public abstract void startMediation();
	
	public abstract void showBanner();
	
	public abstract void hideBanner();
	
	public abstract Vector2 getBannerSize();
	
	public abstract float getAccurateScreenHeight();
	
	public abstract void initInterstitial();
	
	public abstract void loadInterstitial();
	
	public abstract void showInterstitial();
	
	public abstract boolean isInterstitialLoaded();
	
	public abstract boolean isInterstitialFailed();
	
	public abstract boolean isUIViewLoaded();
	
	public abstract boolean isInterstitialShowing();

	public abstract void resumeMediation();
	
	public abstract void pauseMediation();

	public abstract void stopMediation();

	public abstract void delayBannerRefresh();
}
