package com.logickllc.pokesensor;

/**
 * A generic representation of an interstitial (pop-up) ad that is network-independent. Is 
 * abstract so that each instantiation will require certain key
 * methods to be implemented. This class stores mediation order and network name.
 * <br><br>
 * Each instance of this class is meant to be controlled by an {@link AdMediator} instance.
 * 
 * @author Patrick Ballard
 */
public abstract class AdMediationInterstitial {
	private int mediationOrder = 0;
	private String name = "";
	private AdMediator mediator;
	
	/**
	 * @param mediator The {@link AdMediator} that will control this ad.
	 * @param mediationOrder The order of this ad in the mediation waterfall. 1 is tried first and so on.
	 * @param name The network name for this ad (mainly used for debug output)
	 */
	public AdMediationInterstitial(AdMediator mediator, int mediationOrder, String name) {
		this.mediator = mediator;
		this.mediationOrder = mediationOrder;
		this.name = name;
	}
	
	/** Gets the ad's order in the mediation waterfall.
	 * @return Mediation order of this banner, with 1 being tried first and so on.
	 * @see AdMediator
	 */
	public int getMediationOrder() {
		return mediationOrder;
	}
	
	/** Sets the ad's order in the mediation waterfall.
	 * @param mediationOrder Mediation order of this banner, with 1 being tried first and so on.
	 * @see AdMediator
	 */
	public void setMediationOrder(int mediationOrder) {
		this.mediationOrder = mediationOrder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * What to do when the interstitial loads. Make sure to call this
	 * in the onLoad() listener of the actual interstitial object.
	 */
	public void onLoad() {
		mediator.onInterstitialLoad();
	}
	
	/**
	 * What to do when the interstitial fails. Make sure to call this
	 * in the onFail() listener of the actual interstitial object.
	 */
	public void onFail() {
		mediator.onInterstitialFail();
	}
	
	// All of these methods will have to be overriden for each specific interstitial
	// Then the app can use this common interface to easily control mediation

	// Initialize the interstitial
	public abstract void init();

	// Load a new ad
	public abstract void load();

	// Show the interstitial
	public abstract void show();

	// Hide the interstitial
	public abstract void hide();
}
