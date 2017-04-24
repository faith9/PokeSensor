package com.logickllc.pokesensor.api;

import com.logickllc.pokesensor.ErrorReporter;
import com.logickllc.pokesensor.MapController;
import com.logickllc.pokesensor.NativePreferences;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;
import com.pokegoapi.exceptions.request.LoginFailedException;
import com.pokegoapi.exceptions.request.RequestFailedException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Features {
	private com.logickllc.pokesensor.api.MapHelper mapHelper;
	protected final String TAG = "PokeFinder";

	public ConcurrentHashMap<Account, MapObjects> objects;

	public boolean captchaScreenVisible = false;
	public ConcurrentHashMap<Integer, Boolean> filter;
	public ConcurrentHashMap<Integer, Boolean> filterOverrides;
	public ConcurrentHashMap<Integer, Boolean> notificationFilter;
	public static int NUM_POKEMON = 251;
	public static final String PREF_FILTER_STRING = "Filter";
	public static final String PREF_FILTER_OVERRIDES_STRING = "FilterOverrides";
	public static final String PREF_NOTIFICATION_FILTER_STRING = "NotificationFilter";
	public static final String PREF_CUSTOM_IMAGES_STRING = "CustomImages";
	public static final String CUSTOM_IMAGES_FOLDER = "custom_pokemon/";
	public ArrayList<String> customImages = new ArrayList<>();

	public abstract void runOnMainThread(Runnable r);
	public abstract Object showProgressDialog(int titleid, int messageid);
	public abstract Object showProgressDialog(final String title, final String message);
	public abstract void shortMessage(int resid);
	public abstract void shortMessage(final String message);
	public abstract void longMessage(int resid);
	public abstract void longMessage(final String message);
	public abstract void login();
	public abstract void print(String tag, String message);
	public abstract void loadCaptcha(Account account);
	public abstract void loadFilter();
	public abstract void saveFilter();
	public abstract void loadFilterOverrides();
	public abstract void saveFilterOverrides();
	public abstract void loadNotificationFilter();
	public abstract void saveNotificationFilter();

	public MapHelper getMapHelper() {
		return mapHelper;
	}

	public void setMapHelper(MapHelper mapHelper) {
		this.mapHelper = mapHelper;
	}

	/*public boolean loggedIn(boolean isSecond) {
		PokemonGo temp;
		if (!isSecond) temp = go;
		else temp = go2;
		if (temp != null) {
			try {
				tryTalkingToServer(temp);
				return true;
			} catch (RequestFailedException e) {
				// Looks like we're logged in but the server is cranky
				e.printStackTrace();
				return true;
			} catch (LoginFailedException e) {
				// Not logged in. Try it now
				e.printStackTrace();
				login(isSecond);
				return false;
			}
		} else {
			print("PokeFinder", "go is null");
			login(isSecond);
			return false;
		}
	}*/

	public String encode(String value) {
		/*int length = value.length();
		String result = "";
		for (int n = length - 1; n >= 0; n--) {
			result += String.format("%010d", (int) value.charAt(n));
		}

		//print(TAG, "Encoded \"" + value + "\" as \"" + result + "\"");
		return result;*/
		return value;
	}

	public String decode(String value) {
		if (MapController.instance.canDecode) {
			try {
				String result = "";
				int digits = 10;

				for (int n = value.length() / digits - 1; n >= 0; n--) {
					result += Character.valueOf((char) Integer.parseInt(value.substring(n * digits, (n + 1) * digits))).toString();
				}

				//print(TAG, "Decoded \"" + value + "\" as \"" + result + "\"");
				return result;
			} catch (Exception e) {
				e.printStackTrace();
				ErrorReporter.logExceptionThreaded(e);
				return "";
			}
		} else {
			return value;
		}
	}

	public void resetMapObjects() {
		if (objects == null) objects = new ConcurrentHashMap<>();
		objects.clear();
	}

	public void refreshMapObjects(Account account) throws Throwable {
		/*final Lock lock = new Lock();
		account.go.getMap().getMapObjects(new PokeCallback<MapObjects>() {
			@Override
			public void onError(Throwable error) {
				lock.object = error;
				lock.unlock();
			}

			@Override
			public void onResponse(MapObjects mapObjects) {
				lock.object = mapObjects;
				lock.unlock();
			}
		});

		lock.waitForUnlock();

		if (lock.object != null && lock.object instanceof Throwable) throw (Throwable) lock.object;*/
		account.go.getMap().update();
		objects.put(account, (MapObjects) account.go.getMap().getMapObjects());
	}

	public List<CatchablePokemon> getCatchablePokemon(Account account, int cells) throws LoginFailedException, RequestFailedException {
		List<CatchablePokemon> catchablePokemons = new ArrayList<CatchablePokemon>();
		//MapObjects objects = go.getMap().getMapObjects(cells);

		for (CatchablePokemon pokemon : objects.get(account).getPokemon()) {
			catchablePokemons.add(pokemon);
		}

		return catchablePokemons;
	}

	public List<NearbyPokemon> getNearbyPokemon(Account account, int cells) throws LoginFailedException, RequestFailedException {
		List<NearbyPokemon> nearbyPokemons = new ArrayList<>();
		//MapObjects objects = go.getMap().getMapObjects(cells);

		for (NearbyPokemon mapPokemon : objects.get(account).getNearby()) {
			nearbyPokemons.add(mapPokemon);
		}

		return nearbyPokemons;
	}

	/*public List<WildPokemonOuterClass.WildPokemon> getWildPokemon(Account account, int cells) throws LoginFailedException, RequestFailedException {
		List<WildPokemonOuterClass.WildPokemon> wildPokemons = new ArrayList<WildPokemonOuterClass.WildPokemon>();
		//MapObjects objects = go.getMap().getMapObjects(cells);

		for (WildPokemonOuterClass.WildPokemon mapPokemon : objects.get(account).getWildPokemons()) {
			wildPokemons.add(mapPokemon);
		}

		return wildPokemons;
	}*/

	/*public synchronized void tryTalkingToServer(final PokemonGo go) throws LoginFailedException, RequestFailedException {
		serverAlive = null;

		Thread thread = new Thread() {
			public void run() {
				try {
					//go.getMap().getCatchablePokemon();
					//if (go2 != null) go2.getMap().getCatchablePokemon();
					go.getPlayerProfile().getStats();
					serverAlive = true;
				} catch (Exception e) {
					if (e instanceof LoginFailedException) serverAlive = false;
					else if (e instanceof RequestFailedException) serverAlive = true;
					else serverAlive = false;
					e.printStackTrace();
				}
			}
		};

		thread.start();

		while (serverAlive == null) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (!serverAlive) throw new LoginFailedException("You are not logged in!");
	}*/

	public double getVisibleScanDistance() throws LoginFailedException, RequestFailedException {
		return AccountManager.getVisibleScanDistance();
	}

	public double getMinScanRefresh() throws LoginFailedException, RequestFailedException {
		return AccountManager.getMinScanRefresh();
	}
	
	public String getStackTraceString(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	public void skipTOS(boolean isSecond) {
		/*try {
			//print(TAG, "Skipping TOS...");
			if (!isSecond) go.setTutorialState();
			else go2.setTutorialState();
			login(isSecond);
			//print("Tutorial?", "" + go.getPlayerProfile().getPlayerData().getTutorialStateValue(TutorialStateOuterClass.TutorialState.LEGAL_SCREEN_VALUE));
		} catch (Exception e) {
			e.printStackTrace();
			print(TAG, "Failed to skip TOS");
		}*/
	}

	public boolean verifyChallenge(String captchaToken, PokemonGo go) {
		/*final Lock lock = new Lock();
		go.getPlayerProfile().sendChallenge(captchaToken, new PokeCallback<Boolean>() {
			@Override
			public void onError(Throwable error) {
				// Set success = false because something weird happened
				lock.object = false;
				error.printStackTrace();
				lock.unlock();
			}

			@Override
			public void onResponse(Boolean success) {
				lock.object = success;
				lock.unlock();
			}
		});

		lock.waitForUnlock();*/
		try {
			return go.verifyChallenge(captchaToken);
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean checkForCaptcha(Account account) {
		if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
			account.captchaScreenVisible = true;
			print("PokeFinder", "You need to do the captcha at " + account.getCaptchaUrl());
			//superLongMessage("Captcha required for " + username + ". Please login to Pokemon GO with " + username + " to complete the captcha. Until then your scans won't work.");

			loadCaptcha(account);
		}

		return account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED;
	}

	public void loadFilterFromString(String filterString) {
		filter = new ConcurrentHashMap<>();

		// Assume string is a binary string ordered by Pokemon #
		for (int n = 0; n < NUM_POKEMON; n++) {
			if (filterString.length() > n) {
				String bit = Character.toString(filterString.charAt(n));
				boolean showPokemon = bit.equals("1");
				filter.put(n + 1, showPokemon);
			} else {
				filter.put(n + 1, true);
			}
		}
	}

	public void loadFilterOverridesFromString(String filterString) {
		filterOverrides = new ConcurrentHashMap<>();

		// Assume string is a binary string ordered by Pokemon #
		for (int n = 0; n < NUM_POKEMON; n++) {
			if (filterString.length() > n) {
				String bit = Character.toString(filterString.charAt(n));
				boolean showPokemon = bit.equals("1");
				filterOverrides.put(n + 1, showPokemon);
			} else {
				filterOverrides.put(n + 1, false);
			}
		}
	}

	public void loadNotificationFilterFromString(String filterString) {
		notificationFilter = new ConcurrentHashMap<>();

		// Assume string is a binary string ordered by Pokemon #
		for (int n = 0; n < NUM_POKEMON; n++) {
			if (filterString.length() > n) {
				String bit = Character.toString(filterString.charAt(n));
				boolean showPokemon = bit.equals("1");
				notificationFilter.put(n + 1, showPokemon);
			} else {
				notificationFilter.put(n + 1, true);
			}
		}
	}

	public ArrayList<String> loadCustomImageUrls() {
		NativePreferences.lock("load custom image urls");
		String customImagesString = NativePreferences.getString(PREF_CUSTOM_IMAGES_STRING, "");
		NativePreferences.unlock();

		customImages = new ArrayList<>();
		for (String url : customImagesString.split(",")) {
			customImages.add(url);
		}

		if (customImages.size() < NUM_POKEMON) {
			for (int n = customImages.size(); n < NUM_POKEMON; n++) {
				customImages.add(" ");
			}
		}

		return customImages;
	}

	public void saveCustomImagesUrls() {
		String bigString = "";

		for (int n = 0; n < Features.NUM_POKEMON; n++) {
			bigString += customImages.get(n) + (n == Features.NUM_POKEMON - 1 ? "" : ",");
		}

		NativePreferences.lock("save custom image urls");
		NativePreferences.putString(PREF_CUSTOM_IMAGES_STRING, bigString);
		NativePreferences.unlock();
	}
}
