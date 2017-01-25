package com.logickllc.pokesensor;

import com.badlogic.gdx.pay.OfferType;

import java.util.ArrayList;

/** Encapsulates basic IAP offer info for ease of use by the core class. 
 * 
 * @author Patrick Ballard
 */
public class IAPOffer {
	private String id;
	private OfferType type;
	private Lambda purchaseFunction;
	
	/** Makes a new {@link IAPOffer} with the given id (aka SKU) which is usually in the
	 * format "com.example.blahblah" and the given {@link OfferType}. The purchaseFunction
	 * will be executed whenever the offer is purchased or restored.
	 * @param id
	 * @param type
	 * @param purchaseFunction
	 */
	public IAPOffer(String id, OfferType type, Lambda purchaseFunction) {
		this.id = id;
		this.type = type;
		this.purchaseFunction = purchaseFunction;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public OfferType getType() {
		return type;
	}

	public void setType(OfferType type) {
		this.type = type;
	}

	public Lambda getPurchaseFunction() {
		return purchaseFunction;
	}

	public void setPurchaseFunction(Lambda purchaseFunction) {
		this.purchaseFunction = purchaseFunction;
	}
	
	/** Finds the offer in a list of {@link IAPOffer}s with that matches the given id.
	 * @param id
	 * @param offers
	 * @return The matching offer or <code>null</code> if no offer matches.
	 */
	public static IAPOffer getOfferById(String id, ArrayList<IAPOffer> offers) {
		for (IAPOffer offer : offers) {
			if (offer.getId().equals(id)) return offer;
		}
		
		return null;
	}
}
