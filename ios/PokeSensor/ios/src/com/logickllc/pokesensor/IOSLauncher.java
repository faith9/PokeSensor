package com.logickllc.pokesensor;

import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;

import org.robovm.apple.avfoundation.AVAudioSession;
import org.robovm.apple.avfoundation.AVAudioSessionCategory;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.dispatch.DispatchQueue;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSException;
import org.robovm.apple.foundation.NSPropertyList;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIApplicationLaunchOptions;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.bindings.aerserv.ASAdView;
import org.robovm.bindings.aerserv.ASConstants;
import org.robovm.bindings.amazon.AmazonAdError;
import org.robovm.bindings.amazon.AmazonAdOptions;
import org.robovm.bindings.amazon.AmazonAdOptions.AmazonAdHorizontalAlignment;
import org.robovm.bindings.amazon.AmazonAdOptions.AmazonAdVerticalAlignment;
import org.robovm.bindings.amazon.AmazonAdRegistration;
import org.robovm.bindings.amazon.AmazonAdView;
import org.robovm.bindings.amazon.AmazonAdView.AmazonAdViewDelegate;
import org.robovm.bindings.appirater.Appirater;
import org.robovm.bindings.appirater.AppiraterDelegateAdapter;
import org.robovm.bindings.firebase.FIRApp;
import org.robovm.objc.block.VoidBlock1;
import org.robovm.pods.facebook.core.FBSDKAppEvents;
import org.robovm.pods.facebook.core.FBSDKApplicationDelegate;
import org.robovm.pods.google.mobileads.GADAdSize;
import org.robovm.pods.google.mobileads.GADBannerView;
import org.robovm.pods.google.mobileads.GADBannerViewDelegateAdapter;
import org.robovm.pods.google.mobileads.GADGender;
import org.robovm.pods.google.mobileads.GADRequest;
import org.robovm.pods.google.mobileads.GADRequestError;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class IOSLauncher extends IOSApplication.Delegate implements AdController, AmazonAdViewDelegate {
	public static NavigationController navigationController;
	public static final boolean IS_AD_TESTING = true;
	private static final String TEST_IPHONESE_ID = ""; // My iPhone SE
	private static final String TEST_IPAD_ID = ""; // iPad 3
	private static final String BANNER_ID = "";
	private static final String AMAZON_KEY = "";
	public final String PHONE_PLC = "";
	public final String TABLET_PLC = "";
	public boolean isPhone = true;
	private final long ADMOB_REFRESH_RATE = 30000; // 0 means the ad network will push ads (auto refresh)
	private final long AMAZON_REFRESH_RATE = 30000;
	private final long AERSERV_REFRESH_RATE = 30000;
	private final long AMAZON_EXTRA_FAIL_TIME = 5;
	private final int AMAZON_BANNER_MEDIATION_ORDER = 1;
	private final int ADMOB_BANNER_MEDIATION_ORDER = 2;
	private final int AERSERV_BANNER_MEDIATION_ORDER = 3;
	private static ArrayList<String> testDevices;
	private ArrayList<String> keywords;
	private GADBannerView admobBanner;
	public ASAdView aerservBanner;
	private boolean isPortrait = true;
	public static IOSLauncher instance;
	private AmazonAdView amazonBanner;
	private IOSApplication iosApp;
	private int amazonAdded = 1, aerservAdded = 1;

	private boolean admobLoaded = false;
	private boolean amazonLoaded = false;
	private boolean amazonFailed = false;
	public boolean aerservFailed = false;
	public boolean aerservLoaded = false;

	public AdMediator mediator;
	private AdMediationBanner amazonMediationBanner;
	private AdMediationBanner admobMediationBanner;
	private AdMediationBanner aerservMediationBanner;

	private boolean canShowAerserv = true;
	private boolean canShowAmazon = true;
	private final String PREF_CAN_SHOW_AERSERV = "CanShowAerserv";
	private final String PREF_CAN_SHOW_AMAZON = "CanShowAmazon";
	private final String PREF_LAST_AERSERV_CRASH = "LastAerservCrash";
	private final String PREF_LAST_AMAZON_CRASH = "LastAmazonCrash";
	private final long RECURRING_CRASH_THRESHOLD = 600;

	public Vector2 size = Vector2.Zero;

	@Override
	protected IOSApplication createApplication() {
		try {
			instance = this;
			IOSApplicationConfiguration config = new IOSApplicationConfiguration();
			config.allowIpod = true;
			iosApp = new IOSApplication(new PokeSensor(), config);
			NativePreferences.init();

			NativePreferences.lock();
			canShowAerserv = NativePreferences.getBoolean(PREF_CAN_SHOW_AERSERV, true);
			canShowAmazon = NativePreferences.getBoolean(PREF_CAN_SHOW_AMAZON, true);
			NativePreferences.unlock();

			iosApp.addLifecycleListener(new LifecycleListener() {

				@Override
				public void pause() {
					pauseMediation();
				}

				@Override
				public void resume() {
					if (navigationController.getVisibleViewController() instanceof MapController)
						resumeMediation();
					MapController.instance.tryTalkingToServer();
				}

				@Override
				public void dispose() {
				}
			});

			return iosApp;
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
			return iosApp;
		}
	}

	@Override
	public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions launchOptions) {
		super.didFinishLaunching(application, launchOptions);

		try {
			NSException.setUncaughtExceptionHandler(new VoidBlock1<NSException>() {

				@Override
				public void invoke(NSException e) {
					try {
						ErrorReporter.logException(e);
					} catch (Exception ex) {
						ex.printStackTrace(System.out);
					}
					System.out.println("Uncaught exception handler says: " + e.getName() + "\n" + e.getReason());
					// See if it's a recurring problem with Aerserv or Amazon and disable them if so
					if (e.getName().toLowerCase().contains("aerserv") || e.getReason().toLowerCase().contains("aerserv")) {
						print("The Aerserv library caused this crash. Check if it's a recurring problem.");
						NativePreferences.lock();
						long lastTime = NativePreferences.getLong(PREF_LAST_AERSERV_CRASH, 0);
						if (lastTime == 0) {
							print("This is the first Aerserv crash. If it happens again too soon Aerserv will be blocked.");
							NativePreferences.putLong(PREF_LAST_AERSERV_CRASH, Calendar.getInstance().getTime().getTime() / 1000);
						} else if (Calendar.getInstance().getTime().getTime() / 1000 - lastTime < RECURRING_CRASH_THRESHOLD) {
							print("This is a recurring Aerserv crash. Aerserv is now disabled for this device.");
							NativePreferences.putBoolean(PREF_CAN_SHOW_AERSERV, false);
						} else {
							print("It's been a while since the last Aerserv crash. Not a recurring crash.");
							NativePreferences.putLong(PREF_LAST_AERSERV_CRASH, Calendar.getInstance().getTime().getTime() / 1000);
						}
						NativePreferences.unlock();
					}

					if (e.getName().toLowerCase().contains("amazon") || e.getReason().toLowerCase().contains("amazon")) {
						print("The Amazon library caused this crash. Check if it's a recurring problem.");
						NativePreferences.lock();
						long lastTime = NativePreferences.getLong(PREF_LAST_AMAZON_CRASH, 0);
						if (lastTime == 0) {
							print("This is the first Amazon crash. If it happens again too soon Aerserv will be blocked.");
							NativePreferences.putLong(PREF_LAST_AMAZON_CRASH, Calendar.getInstance().getTime().getTime() / 1000);
						} else if (Calendar.getInstance().getTime().getTime() / 1000 - lastTime < RECURRING_CRASH_THRESHOLD) {
							print("This is a recurring Amazon crash. Amazon is now disabled for this device.");
							NativePreferences.putBoolean(PREF_CAN_SHOW_AMAZON, false);
						} else {
							print("It's been a while since the last Amazon crash. Not a recurring crash.");
							NativePreferences.putLong(PREF_LAST_AMAZON_CRASH, Calendar.getInstance().getTime().getTime() / 1000);
						}
						NativePreferences.unlock();
					}
				}

			});

			NSException.registerDefaultJavaUncaughtExceptionHandler();
			final Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
			Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable ex) {
					try {
						ErrorReporter.logException(ex);
						handler.uncaughtException(thread, ex);
					} catch (Exception e) {
						e.printStackTrace(System.out);
					}
				}
			});

			AVAudioSession.getSharedInstance().setCategory(AVAudioSessionCategory.Ambient);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}

		try {
			FBSDKApplicationDelegate.getSharedInstance().didFinishLaunching(application, launchOptions);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}

		/*try {
			//Flurry.setCrashReportingEnabled(true);
			Flurry.setEventLoggingEnabled(true);
			Flurry.setBackgroundSessionEnabled(false);
			Flurry.setSessionReportsOnCloseEnabled(true);
			Flurry.setSessionReportsOnPauseEnabled(true);
			//Flurry.setDebugLogEnabled(true);
			Flurry.startSession("K7CND8BXF3F5Z3Z6C467");
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		try {
			FIRApp.configure();
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}

		try {
			Appirater.setDebug(false);
			Appirater.setAppId("");
			Appirater.setDaysUntilPrompt(2);
			Appirater.setUsesUntilPrompt(7);
			Appirater.setDelegate(new AppiraterDelegateAdapter() {
				@Override
				public void didOptToRate(Appirater appirater) {
					System.out.println("User decided to rate!");
				}

				@Override
				public void didDeclineToRate(Appirater appirater) {
					System.out.println("User doesn't want to rate!");
				}
			});

			Appirater.appLaunched(true);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}

		return true;
	}

	private void print(String message) {
		if (IS_AD_TESTING) System.out.println(message);
	}

	@Override
	public boolean openURL(UIApplication application, NSURL url, String sourceApplication, NSPropertyList annotation) {
		super.openURL(application, url, sourceApplication, annotation);
		try {
			return FBSDKApplicationDelegate.getSharedInstance().openURL(application, url, sourceApplication, annotation);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
			return false;
		}
	}

	@Override
	public void didBecomeActive(UIApplication application) {
		super.didBecomeActive(application);
		try {
			FBSDKAppEvents.activateApp();
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public static void main(String[] argv) {
		NSAutoreleasePool pool = new NSAutoreleasePool();
		UIApplication.main(argv, null, IOSLauncher.class);
		pool.close();
	}

	public void initAds() {
		String simID = GADRequest.getSimulatorID();
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
		keywords.add("Hearthstone");
		keywords.add("World of Warcraft");
		mediator = new AdMediator();
		initBanners();
		//initAmazonBanner();
		//initAdmobBanner();
	}

	public void initBanners() {
		if (canShowAmazon) addAmazonBanner();
		if (canShowAerserv) addAerservBanner();
		addAdmobBanner();
	}

	public void addAdmobBanner() {
		try {
			// Initialize the Admob banner
			admobMediationBanner = new AdMediationBanner() {

				@Override
				public void init() {
					try {
						if (isPortrait)
							admobBanner = new GADBannerView(GADAdSize.SmartBannerPortrait());
						else admobBanner = new GADBannerView(GADAdSize.SmartBannerLandscape());
						admobBanner.setAdUnitID(BANNER_ID);
						// Notify the mediator when the banner loads or fails
						admobBanner.setDelegate(new GADBannerViewDelegateAdapter() {

							@Override
							public void didReceiveAd(GADBannerView view) {
								try {
									MapController.instance.getView().bringSubviewToFront(admobBanner);
									mediator.onBannerLoad();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							@Override
							public void didFailToReceiveAd(GADBannerView view,
														   GADRequestError error) {
								try {
									if (admobMediationBanner == mediator.getCurrentBanner())
										mediator.onBannerFail();
									else System.out.println("Admob banner failed off-screen");
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

						});
						// Add it to the screen
						admobBanner.setRootViewController(MapController.instance);
						MapController.instance.getView().addSubview(admobBanner);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void load() {
					try {
						if (true) {
							// Make it start requesting ads
							System.out.println("Mediator trying to load Admob banner");
							GADRequest request = new GADRequest();
							request.setGender(GADGender.Male);
							//TODO Fix the birthday thing
							//request.setBirthday(1, 1, Calendar.getInstance().get(Calendar.YEAR) - 1900 - 18);
							request.setKeywords(keywords);
							request.setTestDevices(testDevices);
							admobBanner.loadRequest(request);
							admobLoaded = true;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void show() {
					try {
						if (admobMediationBanner.isShowing()) return;
						else admobMediationBanner.setShowing(true);
						CGSize screenSize = UIScreen.getMainScreen().getBounds().getSize();
						double screenWidth = screenSize.getWidth();
						double screenHeight = screenSize.getHeight();

						CGSize adSize = admobBanner.getBounds().getSize();
						double adHeight = adSize.getHeight();
						double adWidth = adSize.getWidth();

						float bannerWidth = (float) screenWidth;
						float bannerHeight = (float) (bannerWidth / adWidth * adHeight);

						// Set the coords and size of the admobBanner
						admobBanner.setFrame(new CGRect(screenWidth / 2 - adWidth / 2, 0, bannerWidth, bannerHeight));
						System.out.println("Showing Admob banner");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void hide() {
					try {
						if (!admobMediationBanner.isShowing()) return;
						else admobMediationBanner.setShowing(false);
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
						//admobBanner.setFrame(new CGRect(0, (-1)*bannerHeight, 0, 0));
						System.out.println("Hiding Admob banner");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public Vector2 getSize() {
					return Vector2.Zero;
				}

				@Override
				public void changeOrientation(boolean portrait) {
					try {
						isPortrait = portrait;
						if (admobBanner == null)
							return;

						if (portrait)
							admobBanner.setAdSize(GADAdSize.SmartBannerPortrait());
						else
							admobBanner.setAdSize(GADAdSize.SmartBannerLandscape());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			};
			admobMediationBanner.setMediationOrder(mediator.banners.size() + 1);
			admobMediationBanner.setRefreshRate(ADMOB_REFRESH_RATE);
			admobMediationBanner.setName("Admob banner");

			mediator.addBanner(admobMediationBanner);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public void addAmazonBanner() {
		try {
			// Notify the mediator when the banner loads or fails
			final AmazonAdViewDelegate me = this;

			// Initialize the Admob banner
			amazonMediationBanner = new AdMediationBanner() {

				@Override
				public void init() {
					try {
						AmazonAdRegistration.getSharedRegistration().setAppKey(AMAZON_KEY);

						/*if (IS_AD_TESTING) {
						Random random = new Random();
						if (random.nextBoolean()) 
							amazonBanner = new AmazonAdView(new CGRect(0, 0, 10, 10));
						else 
							amazonBanner = new AmazonAdView(new CGRect(0, 0, UIScreen.getMainScreen().getBounds().getSize().getWidth(), 90));
					} else {
						amazonBanner = new AmazonAdView(new CGRect(0, 0, UIScreen.getMainScreen().getBounds().getSize().getWidth(), 90));
					}*/

						amazonBanner = new AmazonAdView(new CGRect(0, 0, UIScreen.getMainScreen().getBounds().getSize().getWidth(), 90));

						//amazonBanner = new AmazonAdView(new CGSize(320, 50));
						amazonBanner.setHorizontalAlignment(AmazonAdHorizontalAlignment.AmazonAdHorizontalAlignmentCenter);
						amazonBanner.setVerticalAlignment(AmazonAdVerticalAlignment.AmazonAdVerticalAlignmentFitToContent);

						amazonBanner.setDelegate(me);
						amazonBanner.addStrongRef(me);

						// Add it to the screen
						//amazonBanner.setRootViewController(MapController.instance);
						MapController.instance.getView().addSubview(amazonBanner);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void load() {
					Runnable r = new Runnable() {
						public void run() {
							try {
								amazonBanner.removeFromSuperview();
								init();

								AmazonAdOptions options = new AmazonAdOptions();
								options.setTestRequest(IS_AD_TESTING);

								amazonFailed = false;
								amazonLoaded = false;

								amazonBanner.loadAd(options);

								Timer timer = new Timer();
								TimerTask task = new TimerTask() {
									@Override
									public void run() {
										if (!amazonLoaded && !amazonFailed) {
											Runnable r = new Runnable() {
												public void run() {
													try {
														amazonBanner.getDelegate().adViewDidFailToLoad(null, null);
													} catch (Exception e) {
														e.printStackTrace();
													}
												}
											};
											postNativeRunnable(r);
										}
									}
								};

								timer.schedule(task, (Math.round(options.timeout()) + AMAZON_EXTRA_FAIL_TIME) * 1000);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					postNativeRunnable(r);
				}

				@Override
				public void show() {
					try {
						if (amazonMediationBanner.isShowing()) return;
						else amazonMediationBanner.setShowing(true);
						CGSize screenSize = UIScreen.getMainScreen().getBounds().getSize();
						double screenWidth = screenSize.getWidth();
						double screenHeight = screenSize.getHeight();

						CGSize adSize = amazonBanner.getBounds().getSize();
						double adHeight = adSize.getHeight();
						double adWidth = adSize.getWidth();

						float bannerWidth = (float) screenWidth;
						float bannerHeight = (float) (bannerWidth / adWidth * adHeight);

						// Set the coords and size of the admobBanner
						amazonBanner.setFrame(new CGRect(screenWidth / 2 - adWidth / 2, 0, bannerWidth, bannerHeight));
						System.out.println("Showing Amazon banner");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void hide() {
					try {
						if (!amazonMediationBanner.isShowing()) return;
						else amazonMediationBanner.setShowing(false);
						CGSize screenSize = UIScreen.getMainScreen().getBounds().getSize();
						double screenWidth = screenSize.getWidth();
						double screenHeight = screenSize.getHeight();

						CGSize adSize = amazonBanner.getBounds().getSize();
						double adHeight = adSize.getHeight();
						double adWidth = adSize.getWidth();

						float bannerWidth = (float) screenWidth;
						float bannerHeight = (float) (bannerWidth / adWidth * adHeight);

						// Set the coords and size of the amazonBanner
						amazonBanner.setFrame(new CGRect(0, (-5)*bannerHeight, bannerWidth, bannerHeight));
						//amazonBanner.setFrame(new CGRect(0, (-1)*bannerHeight, 0, 0));
						System.out.println("Hiding Amazon banner");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public Vector2 getSize() {
					return Vector2.Zero;
				}

				@Override
				public void changeOrientation(boolean portrait) {
					//System.out.println("Screen width after orientation change: " + UIScreen.getMainScreen().getBounds().getSize().getWidth());
					//System.out.println("Screen height after orientation change: " + UIScreen.getMainScreen().getBounds().getSize().getHeight());
				}

			};
			amazonMediationBanner.setMediationOrder(mediator.banners.size()+1);
			amazonMediationBanner.setRefreshRate(AMAZON_REFRESH_RATE);
			amazonMediationBanner.setName("Amazon banner");

			mediator.addBanner(amazonMediationBanner);
			amazonAdded = 0;
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}
	}

	public void addAerservBanner() {
		try {
			// Initialize the Aerserv banner
			aerservMediationBanner = new AdMediationBanner() {

				@Override
				public void init() {
					try {
						System.out.println("Trying to instantiate Aerserv banner");

						// Set the app key
						double width = UIScreen.getMainScreen().getBounds().getWidth();
						if (width >= 728) {
							aerservBanner = new ASAdView(TABLET_PLC, new CGSize(728, 90));
							isPhone = false;
						}
						else {
							aerservBanner = new ASAdView(PHONE_PLC, new CGSize(320, 50));
							isPhone = true;
						}
						if (!IS_AD_TESTING) aerservBanner.setEnv(ASConstants.ASEnvironmentType.kASEnvProduction);
						else aerservBanner.setEnv(ASConstants.ASEnvironmentType.kASEnvStaging);

						aerservBanner.setBackgroundColor(UIColor.clear());

						// Set the delegate and implement the basic needs
						// Use the MapController as the delegate because we are already using this class
						// as the Amazon delegate and we can't have 2 selectors named viewControllerForPresentingModalView
						aerservBanner.setDelegate(MapController.instance);
						aerservBanner.addStrongRef(MapController.instance);

						// Add it to the screen
						MapController.instance.getView().addSubview(aerservBanner);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void load() {
					Runnable r = new Runnable() {
						public void run() {
							try {
								aerservFailed = false;
								aerservLoaded = false;

								aerservBanner.loadAd();

								Timer timer = new Timer();
								TimerTask task = new TimerTask() {
									@Override
									public void run() {
										if (!aerservLoaded && !aerservFailed) {
											Runnable r = new Runnable() {
												public void run() {
													try {
														aerservBanner.getDelegate().adViewDidFailToLoadAd(null, null);
													} catch (Exception e) {
														e.printStackTrace();
													}
												}
											};
											postNativeRunnable(r);
										}
									}
								};

								timer.schedule(task, (Math.round(aerservBanner.getTimeoutInterval()) + AMAZON_EXTRA_FAIL_TIME) * 1000);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
					postNativeRunnable(r);
				}

				@Override
				public void show() {
					try {
						CGSize screenSize = UIScreen.getMainScreen().getBounds().getSize();
						double screenWidth = screenSize.getWidth();
						double screenHeight = screenSize.getHeight();

						CGSize adSize = aerservBanner.getBounds().getSize();
						double adHeight = isPhone ? 50 : 90;
						double adWidth = isPhone ? 320 : 728;

						if (adHeight <= 0 || adWidth <= 0) System.out.println("Invalid banner dimensions (" + adWidth + ", " + adHeight + ")");

						float bannerWidth = (float) screenWidth;
						float bannerHeight = (float) (bannerWidth / adWidth * adHeight);
						CGRect frame = new CGRect(screenWidth / 2 - adWidth / 2, 0, adWidth, adHeight);
						aerservBanner.setFrame(frame);

						System.out.println("Banner frame is now (" + frame.getX() + ", " + frame.getY() + ", " + frame.getWidth() + ", " + frame.getHeight() + ")");

						System.out.println("Showing Aerserv banner");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void hide() {
					try {
						if (!aerservMediationBanner.isShowing()) return;
						else aerservMediationBanner.setShowing(false);
						CGSize screenSize = UIScreen.getMainScreen().getBounds().getSize();
						double screenWidth = screenSize.getWidth();
						double screenHeight = screenSize.getHeight();

						CGSize adSize = aerservBanner.getBounds().getSize();
						double adHeight = isPhone ? 50 : 90;
						double adWidth = isPhone ? 320 : 728;

						float bannerWidth = (float) screenWidth;
						float bannerHeight = (float) (bannerWidth / adWidth * adHeight);

						// Set the coords and size of the aerservBanner
						aerservBanner.setFrame(new CGRect(0, (-5)*adHeight, adWidth, adHeight));
						//aerservBanner.setFrame(new CGRect(0, (-1)*bannerHeight, 0, 0));
						System.out.println("Hiding Aerserv banner");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public Vector2 getSize() {
					return Vector2.Zero;
				}

				@Override
				public void changeOrientation(boolean portrait) {
					try {
						aerservBanner.removeFromSuperview();
						init();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			};
			aerservMediationBanner.setMediationOrder(mediator.banners.size()+1);
			aerservMediationBanner.setRefreshRate(AERSERV_REFRESH_RATE);
			aerservMediationBanner.setName("Aerserv banner");

			mediator.addBanner(aerservMediationBanner);
			aerservAdded = 0;
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	@Override
	public void startMediation() {
		try {
			mediator.startBannerMediation();
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}
	}

	@Override
	public void showBanner() {
		try {
			mediator.showBanner();
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}
	}

	@Override
	public void hideBanner() {
		try {
			if (mediator != null) mediator.hideBanner();
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}
	}

	@Override
	public Vector2 getBannerSize() {
		try {
			return mediator.getBannerSize();
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
			return Vector2.Zero;
		}
	}

	@Override
	public void resumeMediation() {
		try {
			if (mediator != null) mediator.resumeMediation();
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}
	}

	@Override
	public void pauseMediation() {
		try {
			if (mediator != null) mediator.pauseMediation();
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}
	}

	@Override
	public void stopMediation() {
		try {
			if (mediator != null) {
				mediator.stopMediation();
				//admobBanner.removeFromSuperview();
			}
		} catch (Throwable e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}
	}

	public void removeAds() {
		try {
			if (mediator != null) mediator.stopMediation();
			mediator = null;
			admobBanner.removeFromSuperview();
			amazonBanner.removeFromSuperview();
			aerservBanner.removeFromSuperview();
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	/*public void initAmazonBanner() {
		AmazonAdRegistration.getSharedRegistration().setAppKey(AMAZON_KEY);
		AmazonAdOptions options = new AmazonAdOptions();
		options.setTestRequest(true);
		//amazonBanner = new AmazonAdView(new CGRect(0, 0, UIScreen.getMainScreen().getBounds().getSize().getWidth(), 60));
		amazonBanner = AmazonAdView.amazonAdViewWithAdSize(new CGSize(320, 50));
		amazonBanner.setHorizontalAlignment(AmazonAdHorizontalAlignment.AmazonAdHorizontalAlignmentCenter);
		amazonBanner.setVerticalAlignment(AmazonAdVerticalAlignment.AmazonAdVerticalAlignmentFitToContent);
		amazonBanner.setDelegate(new AmazonAdViewDelegateAdapter() {

			@Override
			public void adViewDidFailToLoad(AmazonAdView view, AmazonAdError error) {
				super.adViewDidFailToLoad(view, error);
				if (IS_AD_TESTING) System.out.println("Amazon banner failed to load");
			}

			@Override
			public void adViewDidLoad(AmazonAdView view) {
				super.adViewDidLoad(view);
				showAmazon();
				if (IS_AD_TESTING) System.out.println("Amazon banner loaded");
			}

			@Override
			public UIViewController getViewControllerForPresentingModalView() {
				super.getViewControllerForPresentingModalView();
				return MapController.instance;
			}



		});

		// Add it to the screen
		//amazonBanner.setRootViewController(MapController.instance);
		MapController.instance.getView().addSubview(amazonBanner);

		//showAmazon();
		amazonBanner.loadAd(options);
	}

	public void showAmazon() {
		try {
			CGSize screenSize = UIScreen.getMainScreen().getBounds().getSize();
			double screenWidth = screenSize.getWidth();
			double screenHeight = screenSize.getHeight();

			CGSize adSize = amazonBanner.getBounds().getSize();
			double adHeight = 50;
			double adWidth = 320;

			float bannerWidth = (float) screenWidth;
			float bannerHeight = (float) (bannerWidth / adWidth * adHeight);
			amazonBanner.setFrame(new CGRect(screenWidth / 2 - adWidth / 2, 0, bannerWidth, bannerHeight));

			System.out.println("Showing Amazon banner");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initAdmobBanner() {
		if (isPortrait)
			admobBanner = new GADBannerView(GADAdSize.SmartBannerPortrait());
		else
			admobBanner = new GADBannerView(GADAdSize.SmartBannerLandscape());
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
		//TODO Figure out birthday
		//request.setBirthday(1, 1, Calendar.getInstance().get(Calendar.YEAR) - 1900 - 18);
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
	}*/

	public void changeOrientation(boolean portrait) {
		if (mediator == null) return;
		isPortrait = portrait;
		mediator.changeBannerOrientation(portrait);
	}

	public Vector2 getSize() {
		if (AdHelper.isShowAds()) {
			// Sometimes the iPhone scales the app's display to account for retina graphics. This is how I account for that.
			if (isPortrait)
				return new Vector2((float) (GADAdSize.SmartBannerPortrait().toCGSize().getWidth()), (float) (Math.max(GADAdSize.SmartBannerPortrait().toCGSize().getHeight(), size.y)));
			else
				return new Vector2((float) (GADAdSize.SmartBannerLandscape().toCGSize().getWidth()), (float) (Math.max(GADAdSize.SmartBannerLandscape().toCGSize().getHeight() * 1.2, size.y)));
		} else {
			return Vector2.Zero;
		}
	}

	@Override
	public float getAccurateScreenHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void initInterstitial() {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadInterstitial() {
		// TODO Auto-generated method stub

	}

	@Override
	public void showInterstitial() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isInterstitialLoaded() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInterstitialFailed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUIViewLoaded() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isInterstitialShowing() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void delayBannerRefresh() {
		try {
			if (mediator != null) mediator.resumeMediation();
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	public void postNativeRunnable(Runnable runnable) {
		DispatchQueue.getMainQueue().async(runnable);
	}

	// START Amazon delegate callbacks

	@Override
	public UIViewController getViewControllerForPresentingModalView() {
		return MapController.instance;
	}

	@Override
	public void adViewWillExpand(AmazonAdView view) {
		// TODO Auto-generated method stub

	}

	@Override
	public void adViewDidCollapse(AmazonAdView view) {
		// TODO Auto-generated method stub

	}

	@Override
	public void adViewWillResize(AmazonAdView view, CGRect frame) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean willHandleAdViewResize(AmazonAdView view, CGRect frame) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void adViewDidFailToLoad(AmazonAdView view, AmazonAdError error) {
		try {
			amazonFailed = true;
			mediator.onBannerFail();
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	@Override
	public void adViewDidLoad(AmazonAdView view) {
		try {
			amazonLoaded = true;
			MapController.instance.getView().bringSubviewToFront(amazonBanner);
			mediator.onBannerLoad();
			//size = new Vector2((float) amazonBanner.getBounds().getWidth(), (float) amazonBanner.getBounds().getHeight());
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					MapController.instance.refreshMessageContainerHeight();
				}
			};
			postNativeRunnable(runnable);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
		}
	}

	// END Amazon delegate callbacks
}
