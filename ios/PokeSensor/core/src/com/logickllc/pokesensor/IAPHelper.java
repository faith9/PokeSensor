package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.PurchaseSystem;
import com.badlogic.gdx.pay.Transaction;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/** Handles all the in-app purchasing for the app. You only need to supply it with a list of
 * {@link IAPOffer}s that will be offered by the app and tell it whether or not the app is
 * for Google Play or Amazon (doesn't matter what the value is if the app is for iOS). See
 * {@link IAPOffer} to find out what info you must include with each offer. Call init(boolean, ArrayList)
 * to initialize the in-app purchase system and then you can purchase any of the offers from the main
 * app and this class will handle the rest.
 * 
 * NOTE: For Amazon builds, you must pass a specific purchase manager from the Android launcher, known as
 * PurchaseManagerAndroidAmazon. It just needs a reference to the Android Activity so nothing huge. Just
 * use {@link #setManager(PurchaseManager)} from the Android launcher before calling init(boolean, ArrayList)
 * and this class will handle the rest of the Amazon setup for you.
 * 
 * @author Patrick Ballard
 */
public class IAPHelper {
	static String message = "";
	static PurchaseManagerConfig config = null;
	static PurchaseManager manager = null;
	static String appStore = null;
	public final static int MANAGER_REQUEST_CODE = 1337;
	static Boolean installed;
	
	public static void init(final ArrayList<IAPOffer> offers) {
		// test the purchase manager if there is one (if you use the default APK install it should find Google!)
		if (manager == null) {
			if (!PurchaseSystem.hasManager()) {
				System.out.println(" - no purchase manager found.\n");
				return;
			} else {
				manager = PurchaseSystem.getManager();
			}
		} else {
			PurchaseSystem.setManager(manager);
		}
		// build our purchase configuration: all your products and types need to be listed here
		config = new PurchaseManagerConfig();

		
			appStore = PurchaseManagerConfig.STORE_NAME_IOS_APPLE;
		

		for (IAPOffer offer : offers) {
			config.addOffer(new Offer().setType(offer.getType()).setIdentifier(offer.getId()));
		}

		installed = null;

		// install the observer
		PurchaseObserver observer = new PurchaseObserver() {
			@Override
			public void handleRestore (Transaction[] transactions) {
				// keep note of our purchases
				System.out.println(" - totally " + transactions.length + " purchased products\n");
				for (int i = 0; i < transactions.length; i++) {
					Transaction transaction = transactions[i];
					final boolean last = i == transactions.length - 1;

					if (transaction.isPurchased()) {
						final IAPOffer purchasedOffer = IAPOffer.getOfferById(transaction.getIdentifier(), offers);

						System.out.println(" - restored: " + transaction.getIdentifier() + "\n");

						Timer timer = new Timer();

						TimerTask task = new TimerTask() {

							@Override
							public void run() {
								/*Gdx.app.postRunnable(new Thread() {
									@Override
									public void run() {*/
										purchasedOffer.getPurchaseFunction().execute();	
										if (last) {
											Runnable runnable = new Runnable() {
												@Override
												public void run() {
													showDialog("Success!", "Your purchases have been restored!");
												}
											};
											Gdx.app.postRunnable(runnable);
										}
								/*	}					
								});*/
							}
						};

						timer.schedule(task, 2000);
					}
				}

				// dispose the purchase system
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run () {
						System.out.println(" - disposing the purchase manager.\n");
						manager.dispose();
						System.out.println("InApp System: DISPOSED\n");
					}
				});
			}

			@Override
			public void handleRestoreError (Throwable e) {
				System.out.println(" - error during purchase manager restore: " + e + "\n");

				//showErrorDialog(e.getMessage());
				showErrorDialog("Error restoring purchases. Please make sure you are connected to the Internet and try again.");
			}

			@Override
			public void handleInstall () {
				System.out.println(" - purchase manager installed: " + manager.storeName() + ".\n");

				installed = true;
			}

			@Override
			public void handleInstallError (Throwable e) {
				System.out.println(" - error installing purchase manager: " + e + "\n");

				//showErrorDialog(e.getMessage());
				showErrorDialog("Error loading purchasing system. Please make sure you are connected to the Internet and try again.");

				installed = false;
			}

			@Override
			public void handlePurchase (Transaction transaction) {
				if (!transaction.isPurchased()) {
					showErrorDialog("Error making purchase. Please check your Internet connection and try again.");
					return;
				}
				
				final IAPOffer purchasedOffer = IAPOffer.getOfferById(transaction.getIdentifier(), offers);
				
				System.out.println(" - purchased: " + transaction.getIdentifier() + "\n");

				//showDialog("Success!", "You purchased " + transaction.getIdentifier());

				// dispose the purchase system
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run () {
						System.out.println(" - disposing the purchase manager.\n");
						manager.dispose();
						System.out.println("InApp System: DISPOSED\n");
					}
				});

				Timer timer = new Timer();

				TimerTask task = new TimerTask() {

					@Override
					public void run() {
						/*Gdx.app.postRunnable(new Thread() {
							@Override
							public void run() {*/
								purchasedOffer.getPurchaseFunction().execute();					
						/*	}					
						});*/
					}
				};

				timer.schedule(task, 2000);
			}

			@Override
			public void handlePurchaseError (Throwable e) {
				// Check to see if the error is that the user already has purchased this offer
				if (e.getMessage().toUpperCase().contains("ALREADY")) {
					showDialog("Unable to Purchase", "If you have already purchased this item, go to Settings > Restore Purchases to restore your purchase.");
				} else {
					showErrorDialog("Error making purchase. Please make sure you are connected to the Internet and try again.");
				}
				
				System.out.println(" - error purchasing: " + e + "\n");

				//showErrorDialog(e.getMessage());

				// Buy it anyway to see what happens
				//offers.get(0).getPurchaseFunction().execute();
			}

			@Override
			public void handlePurchaseCanceled () {
				System.out.println(" - purchase cancelled.\n");

				// dispose the purchase system
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run () {
						System.out.println(" - user canceled! - disposing the purchase manager.\n");
						manager.dispose();
						System.out.println("Testing InApp System: COMPLETED\n");
					}
				});
			}
		};

		manager.install(observer, config, false);
	}
	
	public static void restorePurchases() {
		Thread thread = new Thread() {

			@Override
			public void run() {
				while (isInstalled() == null) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				if (isInstalled()) {
					manager.purchaseRestore();
				}
			}
			
		};
		
		thread.start();
	}
	
	/** Creates an IAP offer that is consumable. I.e. It's something that is consumed once it
	 * is used in the app, like virtual currency.
	 * @param id SKU of the item.
	 * @param purchaseFunction The function that will execute when the offer is purchased
	 * or restored.
	 * @return An {@link IAPOffer} with the given info.
	 */
	public static IAPOffer createConsumableOffer(String id, Lambda purchaseFunction) {
		return new IAPOffer(id, OfferType.CONSUMABLE, purchaseFunction);
	}
	
	/** Creates an IAP offer that is an entitlement. I.e. It's something that is purchased once
	 * and the user is entitled to this item forever afterwards, like premium app features.
	 * @param id SKU of the item.
	 * @param purchaseFunction The function that will execute when the offer is purchased
	 * or restored.
	 * @return An {@link IAPOffer} with the given info.
	 */
	public static IAPOffer createEntitlementOffer(String id, Lambda purchaseFunction) {
		return new IAPOffer(id, OfferType.ENTITLEMENT, purchaseFunction);
	}
	
	/** Creates an IAP offer that is an subscription. I.e. It's something that the user is entitled
	 * to for a set amount of time, at which point it automatically renews.
	 * @param id SKU of the item.
	 * @param purchaseFunction The function that will execute when the offer is purchased
	 * or restored.
	 * @return An {@link IAPOffer} with the given info.
	 */
	public static IAPOffer createSubscriptionOffer(String id, Lambda purchaseFunction) {
		return new IAPOffer(id, OfferType.SUBSCRIPTION, purchaseFunction);
	}


	/** Shows a Dialog with the given message.
	 * @param message
	 */
	public static void showDialog(final String title, final String message) {
		// Run this thing in as a runnable with a delay because it does some
		// weird stuff if you try to make a Dialog immediately after control returns
		// to the app.
		/*Timer timer = new Timer();

		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				Utilities.postNativeRunnable(new Thread() {
					@Override
					public void run() {		
						DialogHelper.messageBox(title, message).build().show();
					}					
				});
			}
		};
		
		timer.schedule(task, 1000);*/
		
		//DialogHelper.messageBox(title, message).build().show();
		DialogHelper.messageBox(title, message).build().show();
	}
	
	/** Shows a Dialog with the given error message.
	 * @param error
	 */
	public static void showErrorDialog(String error) {
		showDialog("Error", error);
	}
	
	/** Purchases the offer indicated by the id. There needs to be an offer
	 * with this id that has already been added to the purchase manager.
	 * @param id
	 */
	public static void purchase(String id) {
		PurchaseSystem.purchase(id);
	}
	
	/** Whether or not the {@link PurchaseSystem} has successfully installed.
	 * @return <code>true</code> if it has installed, <code>false</code> if there was an install error, 
	 * <code>null</code> if it is still working on the installation
	 * and hasn't finished yet.
	 */
	public static Boolean isInstalled() {
		return installed;
	}

	public static PurchaseManager getManager() {
		return manager;
	}

	public static void setManager(PurchaseManager manager) {
		IAPHelper.manager = manager;
	}
	
}
