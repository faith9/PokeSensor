package com.logickllc.pokesensor;

/**
 * A generic representation of a banner ad that is network-independent. Implements 
 * {@link AdMediationBannerInterface} so that each instantiation will require certain key
 * methods to be implemented. This class stores refresh rate, mediation order, network name, 
 * and whether or not the banner is currently showing. 
 * <br><br>
 * Each instance of this class is meant to be controlled by an {@link AdMediator} instance.
 * 
 * @author Patrick Ballard
 */
public abstract class AdMediationBanner implements AdMediationBannerInterface {
	private long refreshRate = 30000; // Use 0 if the ad network automatically refreshes ads
	private int mediationOrder = 0;
	private String name = "";
	private boolean isShowing = false;
	
	public long getRefreshRate() {
		return refreshRate;
	}
	
	public void setRefreshRate(long refreshRate) {
		this.refreshRate = refreshRate;
	}
	
	/** Gets the banner's order in the mediation waterfall.
	 * @return Mediation order of this banner, with 1 being tried first and so on.
	 * @see AdMediator
	 */
	public int getMediationOrder() {
		return mediationOrder;
	}
	
	/** Sets the banner's order in the mediation waterfall.
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

	public boolean isShowing() {
		return isShowing;
	}

	public synchronized void setShowing(boolean isShowing) {
		this.isShowing = isShowing;
	}
}
