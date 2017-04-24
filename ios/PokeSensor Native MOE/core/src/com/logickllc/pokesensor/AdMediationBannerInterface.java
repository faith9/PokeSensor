package com.logickllc.pokesensor;

import com.badlogic.gdx.math.Vector2;


/**
 * Contains all the important methods that must be overridden for an {@link AdMediationBanner}.
 * 
 * @author Patrick Ballard
 */
public interface AdMediationBannerInterface {	
	// All of these methods will have to be overriden for each specific banner
	// Then the app can use this common interface to easily control mediation
	
	// Initialize the banner
	public abstract void init();
	
	// Load a new ad
	public abstract void load();
	
	// Show the actual banner view
	public abstract void show();
	
	// Hide the banner view
	public abstract void hide();
	
	// Get the size of the banner view
	public abstract Vector2 getSize();
	
	// Change the screen orientation of the banner
	public abstract void changeOrientation(boolean portrait);
}
