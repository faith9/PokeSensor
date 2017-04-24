package com.logickllc.pokesensor.api;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.logickllc.pokesensor.AccountsController;
import com.logickllc.pokesensor.DialogHelper;
import com.logickllc.pokesensor.ErrorReporter;
import com.logickllc.pokesensor.IOSLauncher;
import com.logickllc.pokesensor.LoginController;
import com.logickllc.pokesensor.MapController;
import com.logickllc.pokesensor.NativePreferences;
import com.pokegoapi.exceptions.request.LoginFailedException;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.util.hash.legacy.LegacyHashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashKey;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import apple.uikit.enums.UIModalPresentationStyle;

public class AccountManager {
    public static ArrayList<Account> accounts = null;
    private static int numAccounts = 0;
    public static final String PREF_USERNAME_PREFIX = "ProfileName";
    public static final String PREF_PASSWORD_PREFIX = "Nickname";
    public static final String PREF_TOKEN_PREFIX = "Token";
    public static final String PREF_NUM_ACCOUNTS = "NumAccounts";
    public static final String PREF_POKEHASH_KEY = "PokehashKey";

    public static ThreadPoolExecutor pool;
    public static final int MAX_POOL_THREADS = 10;

    public static synchronized void login() {
        accounts = new ArrayList<>();
        NativePreferences.lock("account manager login()");
        numAccounts = NativePreferences.getInteger(PREF_NUM_ACCOUNTS, 0);
        MapController.instance.canDecode = NativePreferences.getBoolean(MapController.instance.PREF_FIRST_DECODE_LOAD, true);
        PokeHashProvider.expiration = NativePreferences.getLong(MapController.mapHelper.newApiKey, Long.MAX_VALUE);
        NativePreferences.unlock();

        MapController.instance.updateGoodAccountsLabelText();

        boolean canLogin = MapController.mapHelper.promptForApiKey();

        if (numAccounts == 0 && canLogin) showLoginScreen(null);
        else {
            for (int n = 1; n <= numAccounts; n++) {
                String suffix = n == 1 ? "" : n + "";

                NativePreferences.printLockStatus("");
                NativePreferences.lock("loading account creds " + n);
                String username = decode(NativePreferences.getString(PREF_USERNAME_PREFIX + suffix, ""));
                String password = decode(NativePreferences.getString(PREF_PASSWORD_PREFIX + suffix, ""));
                NativePreferences.unlock();
                //NativePreferences.printLockStatus("After creds");

                Account account = new Account(username, password, n);
                //NativePreferences.printLockStatus("account login");
                if (canLogin) account.login();
                //NativePreferences.printLockStatus("After account login");
                accounts.add(account);
            }
        }

        NativePreferences.printLockStatus("After account manager login");

        NativePreferences.lock("deactivate decode");
        NativePreferences.putBoolean(MapController.instance.PREF_FIRST_DECODE_LOAD, false);
        NativePreferences.unlock();

        NativePreferences.printLockStatus("After deactivating decode");
    }

    public static void refreshAccounts() {
        if (!MapController.mapHelper.promptForApiKey()) return;

        if (accounts == null) {
            login();
            return;
        }

        for (int n = 0; n < accounts.size(); n++) {
            Account account = accounts.get(n);
            account.login();
        }
    }

    public static void showLoginScreen(final Account account) {
        Runnable r = new Runnable() {
            public void run() {
                LoginController loginController = (LoginController) IOSLauncher.navigationController.storyboard().instantiateViewControllerWithIdentifier("LoginController");
                IOSLauncher.navigationController.setModalPresentationStyle(UIModalPresentationStyle.OverCurrentContext);
                IOSLauncher.navigationController.setProvidesPresentationContextTransitionStyle(true);
                IOSLauncher.navigationController.setDefinesPresentationContext(true);

                loginController.account = account;

                IOSLauncher.navigationController.presentViewControllerAnimatedCompletion(loginController, true, null);
            }
        };
        MapController.features.runOnMainThread(r);
    }

    public static String decode(String string) {
        return MapController.features.decode(string);
    }

    public static double getVisibleScanDistance() throws LoginFailedException, RequestFailedException {
        try {
            for (int n = 0; n < accounts.size(); n++) {
                Account account = accounts.get(n);
                if (account.getStatus() == Account.AccountStatus.GOOD) {
                    return account.getVisibleScanDistance();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new RequestFailedException("No valid accounts to get scan distance");
    }

    public static double getMinScanRefresh() throws LoginFailedException, RequestFailedException {
        try {
            for (int n = 0; n < accounts.size(); n++) {
                Account account = accounts.get(n);
                if (account.getStatus() == Account.AccountStatus.GOOD) {
                    return account.getMinScanRefresh();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new RequestFailedException("No valid accounts to get min refresh");
    }

    public static synchronized void tryTalkingToServer() {
        if (accounts == null) {
            login();
            return;
        }

        MapController.instance.resetGoodAccountsLabelText();
        try {
            for (int n = 0; n < accounts.size(); n++) {
                Account account = accounts.get(n);
                account.tryTalkingToServer();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Account> getGoodAccounts() {
        ArrayList<Account> goodAccounts = new ArrayList<>();

        try {
            int size = accounts.size();
            for (int n = 0; n < size; n++) {
                Account account = accounts.get(n);
                if (account.getStatus() == Account.AccountStatus.GOOD) goodAccounts.add(account);
            }
        } catch (Exception e) {
            // This segment is known for concurrent modification exceptions so just block it off
            e.printStackTrace();
        }

        return goodAccounts;
    }

    public static ArrayList<Account> getLoggingInAccounts() {
        ArrayList<Account> unknownAccounts = new ArrayList<>();

        try {
            for (int n = 0; n < accounts.size(); n++) {
                Account account = accounts.get(n);
                if (account.getStatus() == Account.AccountStatus.LOGGING_IN) unknownAccounts.add(account);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return unknownAccounts;
    }

    public synchronized static boolean isScanning() {
        for (int n = 0; n < accounts.size(); n++) {
            Account account = accounts.get(n);
            if (account.isScanning()) return true;
        }

        return false;
    }

    private synchronized static int changeNumAccounts(int change) {
        numAccounts += change;
        NativePreferences.lock("change num accounts");
        NativePreferences.putInteger(PREF_NUM_ACCOUNTS, numAccounts);
        NativePreferences.unlock();
        return numAccounts;
    }

    public synchronized static int incNumAccounts() {
        return changeNumAccounts(1);
    }

    public synchronized static int decNumAccounts() {
        return changeNumAccounts(-1);
    }

    public synchronized static void removeAccount(int index) {
        for (int n = 0; n < accounts.size(); n++) {
            Account account = accounts.get(n);
            removePrefs(account.getAccountNumber());
        }

        accounts.remove(index);
        decNumAccounts();

        for (int n = 1; n <= numAccounts; n++) {
            Account account = accounts.get(n-1);
            account.setAccountNumber(n);
            Account.saveCreds(account.getAccountNumber(), account.getUsername(), account.getPassword(), account.getToken());
        }

        MapController.instance.updateGoodAccountsLabelText();
    }

    public static void removePrefs(int accountNum) {
        String suffix = accountNum == 1 ? "" : (accountNum + "");

        NativePreferences.lock("remove prefs");
        NativePreferences.remove(PREF_USERNAME_PREFIX + suffix);
        NativePreferences.remove(PREF_PASSWORD_PREFIX + suffix);
        NativePreferences.remove(PREF_TOKEN_PREFIX + suffix);
        NativePreferences.unlock();
    }

    public static void deleteAllAccounts() {
        while (numAccounts > 0) {
            removeAccount(0);
            AccountsController.reloadData();
        }
    }

    public synchronized static Future run(Runnable runnable) {
        if (pool == null) {
            MapController.features.print("PokeFinder", "Initializing a new thread pool");
            pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
        }
        Future future = pool.submit(new ExceptionCatchingRunnable(runnable));
        //if (IOSLauncher.IS_AD_TESTING) MapController.features.print("PokeFinder", pool.getQueue().toString());
        return future;
        //DispatchQueue.getGlobalQueue(DispatchQueue.PRIORITY_BACKGROUND, 0).async(runnable);
        //return null;
    }

    public static void purgePool() {
        // Testing purposes only
        if (pool != null) {
            pool.shutdownNow();
            MapController.features.print("PokeFinder", "Initializing a new thread pool");
            pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
        }
    }

    public static void switchHashProvider() {
        for (int n = 1; n <= numAccounts; n++) {
            Account account = accounts.get(n-1);
            if (MapController.mapHelper.useNewApi) {
                try {
                    MapController.features.print("PokeFinder", "Hash provider for " + account.getUsername() + " is PokeHash");
                    if (account.go != null)
                        account.go.setHashProvider(new PokeHashProvider(PokeHashKey.from(MapController.mapHelper.newApiKey), false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                MapController.features.print("PokeFinder","Hash provider for " + account.getUsername() + " is legacy");
                if (account.go != null) account.go.setHashProvider(new LegacyHashProvider());
            }
        }
    }

    public static int getNumAccounts() {
        return numAccounts;
    }

    public static void loginErrorAccounts() {
        if (accounts == null) {
            return;
        }

        try {
            for (int n = 0; n < accounts.size(); n++) {
                Account account = accounts.get(n);
                if (account.getStatus() == Account.AccountStatus.ERROR) {
                    account.login();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String urlToStringOneLine(String url2) throws IOException {
        /*String baseFolder = PokeFinderActivity.instance.getFilesDir().getAbsolutePath();
        File file = new File(baseFolder + "/tempurlfile.txt");
        FileUtils.copyURLToFile(new URL(url), file);
        String message = FileUtils.readFileToString(file);*/
        URL url = new URL(url2);

        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str = in.readLine();
        in.close();
        return str;
    }
}
