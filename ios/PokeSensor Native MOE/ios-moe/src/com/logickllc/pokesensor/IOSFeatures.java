package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.Features;

import org.moe.natj.general.ann.NInt;

import java.util.ArrayList;

import apple.c.Globals;
import apple.uikit.UIActionSheet;
import apple.uikit.UIViewController;
import apple.uikit.protocol.UIActionSheetDelegate;
import de.tomgrill.gdxdialogs.core.dialogs.GDXProgressDialog;

public class IOSFeatures extends Features {
	public static final String PREFS_NAME = "PokeSensor";
	public static final String PREF_TOKEN = "Token";
	public static final String PREF_USERNAME = "ProfileName";
	public static final String PREF_PASSWORD = "Nickname";
	public static final String PREF_FIRST_LOAD = "FirstLoad";

	public static final String PREF_TOKEN2 = "Token2";
	public static final String PREF_USERNAME2 = "ProfileName2";
	public static final String PREF_PASSWORD2 = "Nickname2";

	public static final int SUPER_LONG_MESSAGE_TIME = 5000;
	public static final int LONG_MESSAGE_TIME = 3000;
	public static final int SHORT_MESSAGE_TIME = 1500;

	private GDXProgressDialog progressDialog;

	public static final String REMOVE_ADS_IAP_PRODUCT_ID = "com.logickllc.pokesensor.removeads";

	public void login() {
		Thread loginThread = new Thread() {
			public void run() {
				try {
					((IOSMapHelper) getMapHelper()).refreshNewApiLabels();

                    try {
						MapController.instance.tryFetchingMessages();
					} catch (Exception e) {
						e.printStackTrace();
						ErrorReporter.logExceptionThreaded(e);
					}
					AccountManager.login();
				} catch (Throwable t) {
					t.printStackTrace();
					ErrorReporter.logException(t);
				}
			}
		};
		loginThread.start();
	}

	@Override
	public void print(String tag, String message) {
		if (IOSLauncher.IS_AD_TESTING) {
			//System.out.println(tag + ": " + message);
			Gdx.app.log(tag, message);
		}
	}

	@Override
	public void runOnMainThread(Runnable r) {
		Globals.dispatch_async(Globals.dispatch_get_main_queue(), r::run);
	}

	public Object showProgressDialog(int titleid, int messageid) {
		return null;
	}

	public Object showProgressDialog(final String title, final String message) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				progressDialog = DialogHelper.progressDialog(title, message);
			}
		};
		runOnMainThread(runnable);
		return progressDialog;
	}

	public void shortMessage(int resid) {

	}

	public void shortMessage(final String message) {
		MapController.instance.showMessageFor(message, SHORT_MESSAGE_TIME);
	}

	public void longMessage(int resid) {

	}

	public void longMessage(final String message) {
		boolean showDialog = false;
		try {
			if (!(IOSLauncher.navigationController.visibleViewController() instanceof MapController)) showDialog = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (!showDialog) {
			MapController.instance.showMessageFor(message, LONG_MESSAGE_TIME);
		} else {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					DialogHelper.messageBox("Message", message).build().show();
				}
			};
			runOnMainThread(r);
		}
	}

	public void longMessage(final String message, boolean showDialog) {
		longMessage(message);
	}

	public void superLongMessage(final String message) {
		boolean showDialog = false;
		try {
			if (!(IOSLauncher.navigationController.visibleViewController() instanceof MapController)) showDialog = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!showDialog) {
			MapController.instance.showMessageFor(message, SUPER_LONG_MESSAGE_TIME);
		} else {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					DialogHelper.messageBox("Message", message).build().show();
				}
			};
			runOnMainThread(r);
		}
	}

	public void saveUsernamePassword(String username, String password, boolean isSecond) {
		NativePreferences.lock("legacy save username password");
		if (!isSecond) {
			NativePreferences.putString(PREF_USERNAME, encode(username));
			NativePreferences.putString(PREF_PASSWORD, encode(password));
		} else {
			NativePreferences.putString(PREF_USERNAME2, encode(username));
			NativePreferences.putString(PREF_PASSWORD2, encode(password));
		}
		NativePreferences.unlock();
	}

	public void saveToken(String token, boolean isSecond) {
		NativePreferences.lock("legacy save token");
		if (!isSecond) NativePreferences.putString(PREF_TOKEN, token);
		else NativePreferences.putString(PREF_TOKEN2, token);
		NativePreferences.unlock();
	}

	public void loadCaptcha(final Account account) {
		final String url = account.getCaptchaUrl();

		captchaScreenVisible = true;
		Runnable r = new Runnable() {
			public void run() {
				CaptchaController cont = (CaptchaController) MapController.instance.storyboard().instantiateViewControllerWithIdentifier("CaptchaController");
				cont.account = account;
				account.captchaScreenVisible = true;
				MapController.instance.navigationController().showViewControllerSender(cont, null);
			}
		};

		runOnMainThread(r);
	}

	@Override
	public void loadFilter() {
		String defaultString = "";
		for (int n = 0; n < NUM_POKEMON; n++) {
			defaultString += "1";
		}

		NativePreferences.lock("load filter");
		String filterString = NativePreferences.getString(PREF_FILTER_STRING, defaultString);
		NativePreferences.unlock();

		loadFilterFromString(filterString);
	}

	@Override
	public void saveFilter() {
		String filterString = "";
		for (int n = 0; n < NUM_POKEMON; n++) {
			filterString += filter.get(n+1) ? "1" : "0";
		}

		NativePreferences.lock("save filter");
		NativePreferences.putString(PREF_FILTER_STRING, filterString);
		NativePreferences.unlock();
	}

	@Override
	public void loadFilterOverrides() {
		String defaultString = "";
		for (int n = 0; n < NUM_POKEMON; n++) {
			defaultString += "0";
		}

		NativePreferences.lock("load filter overrides");
		String filterString = NativePreferences.getString(PREF_FILTER_OVERRIDES_STRING, defaultString);
		NativePreferences.unlock();

		loadFilterOverridesFromString(filterString);
	}

	@Override
	public void saveFilterOverrides() {
		String filterString = "";
		for (int n = 0; n < NUM_POKEMON; n++) {
			filterString += filterOverrides.get(n+1) ? "1" : "0";
		}

		NativePreferences.lock("save filter overrides");
		NativePreferences.putString(PREF_FILTER_OVERRIDES_STRING, filterString);
		NativePreferences.unlock();
	}

	@Override
	public void loadNotificationFilter() {
		String defaultString = "";
		for (int n = 0; n < NUM_POKEMON; n++) {
			defaultString += "1";
		}

		NativePreferences.lock("load filter");
		String filterString = NativePreferences.getString(PREF_NOTIFICATION_FILTER_STRING, defaultString);
		NativePreferences.unlock();

		loadNotificationFilterFromString(filterString);
	}

	@Override
	public void saveNotificationFilter() {
		String filterString = "";
		for (int n = 0; n < NUM_POKEMON; n++) {
			filterString += notificationFilter.get(n+1) ? "1" : "0";
		}

		NativePreferences.lock("save filter");
		NativePreferences.putString(PREF_NOTIFICATION_FILTER_STRING, filterString);
		NativePreferences.unlock();
	}

	public void refreshAccounts() {
		if (getMapHelper().scanning) {
			longMessage("Can't refresh accounts while scanning.");
		} else if (AccountManager.getLoggingInAccounts().size() > 0) {
			longMessage("Can't refresh while an account is still logging in.");
		} else {
			Thread thread = new Thread() {
				public void run() {
					AccountManager.refreshAccounts();
				}
			};
			thread.start();
		}
	}

	@SuppressWarnings("deprecation")
	public static void showNativeOptionsList(final ArrayList<String> options, final ArrayList<Lambda> functions, Object viewController) {
		if (!(viewController instanceof UIViewController)) System.out.println("Trying to show options list on a non-UIViewController object!");
		UIActionSheet actionSheet = UIActionSheet.alloc().init();
		for (String option : options) {
			actionSheet.addButtonWithTitle(option);
		}
		actionSheet.addButtonWithTitle("Cancel");

		actionSheet.setCancelButtonIndex(options.size());

		actionSheet.setDelegate(new UIActionSheetDelegate() {
			@Override
			public void actionSheetClickedButtonAtIndex(UIActionSheet actionSheet, @NInt long buttonIndex) {
				if (buttonIndex != options.size()) functions.get((int) buttonIndex).execute();
			}
		});

		actionSheet.showInView(((UIViewController) viewController).view());
		//actionSheet.showFrom(DefaultIOSLauncher.navigationController.getToolbar());
	}
}
