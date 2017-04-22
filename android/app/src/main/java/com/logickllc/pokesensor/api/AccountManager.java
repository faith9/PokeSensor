package com.logickllc.pokesensor.api;


import android.app.Activity;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.logickllc.pokemapper.AccountsActivity;
import com.logickllc.pokemapper.NativePreferences;
import com.logickllc.pokemapper.PokeFinderActivity;
import com.logickllc.pokemapper.R;
import com.pokegoapi.exceptions.request.LoginFailedException;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.main.RequestHandler;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AccountManager {
    public static ArrayList<Account> accounts = null;
    private static int numAccounts = 0;
    public static final String PREF_USERNAME_PREFIX = "ProfileName";
    public static final String PREF_PASSWORD_PREFIX = "Nickname";
    public static final String PREF_TOKEN_PREFIX = "Token";
    public static final String PREF_NUM_ACCOUNTS = "NumAccounts";
    public static Activity act;

    public static final String PREF_POKEHASH_URL = "PokehashUrl";
    public static final String PREF_POKEHASH_VERSION = "PokehashVersion";
    public static final String PREF_POKEHASH_UNK = "PokehashUnk";
    public static final String PREF_POKEHASH_VERSION_STRING = "PokehashVersionString";

    public static final String POKEHASH_URL_URL = "https://pokehash.buddyauth.com/api/hash/versions";
    public static final String POKEHASH_VERSION_STRING_URL = "https://raw.githubusercontent.com/MrPat/CardDragTest/master/language_en.txt";
    public static final String POKEHASH_VERSION_INT_URL = "https://raw.githubusercontent.com/MrPat/CardDragTest/master/language_es.txt";
    public static final String LEGACY_UNK_URL = "https://raw.githubusercontent.com/MrPat/CardDragTest/master/sample.txt";
    public static final String POKEHASH_UNK_URL = "https://raw.githubusercontent.com/MrPat/CardDragTest/master/extraneous.txt";
    public static final String USE_OTHER_UNK_URL = "https://raw.githubusercontent.com/MrPat/CardDragTest/master/mem_dump.txt";
    public static final String POKEHASH_STATUS_MESSAGE_URL = "https://raw.githubusercontent.com/MrPat/CardDragTest/master/extern_cache.txt";

    public static ExecutorService pool;
    public static final int MAX_POOL_THREADS = 10;

    public static ArrayList<RequestHandler> requestHandlers = null;

    public static void login(final Activity activity) {
        Thread thread = new Thread() {
            public void run() {
                act = activity;
                getHashInfo();

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        initHandlers();
                        accounts = new ArrayList<>();
                        NativePreferences.lock("account manager login()");
                        numAccounts = NativePreferences.getInt(PREF_NUM_ACCOUNTS, 0);
                        PokeFinderActivity.instance.canDecode = NativePreferences.getBoolean(PokeFinderActivity.instance.PREF_FIRST_DECODE_LOAD, true);
                        PokeHashProvider.expiration = NativePreferences.getLong(PokeFinderActivity.mapHelper.newApiKey, Long.MAX_VALUE);
                        NativePreferences.unlock();

                        PokeFinderActivity.instance.updateGoodAccountsLabelText();

                        boolean canLogin = PokeFinderActivity.mapHelper.promptForApiKey(activity);

                        if (numAccounts == 0 && canLogin) showLoginScreen(null, act);
                        else {
                            for (int n = 1; n <= numAccounts; n++) {
                                String suffix = n == 1 ? "" : n + "";

                                NativePreferences.lock("loading account creds " + n);
                                String username = decode(NativePreferences.getString(PREF_USERNAME_PREFIX + suffix, ""));
                                String password = decode(NativePreferences.getString(PREF_PASSWORD_PREFIX + suffix, ""));
                                NativePreferences.unlock();

                                Account account = new Account(username, password, n, act);
                                if (canLogin) account.login();
                                accounts.add(account);
                            }
                        }

                        NativePreferences.printLockStatus("After account manager login");

                        NativePreferences.lock("deactivate decode");
                        NativePreferences.putBoolean(PokeFinderActivity.instance.PREF_FIRST_DECODE_LOAD, false);
                        NativePreferences.unlock();

                        NativePreferences.printLockStatus("After deactivating decode");
                    }
                };

                PokeFinderActivity.features.runOnMainThread(runnable);
            }
        };

        thread.start();
    }

    public static void refreshAccounts(Activity act) {
        if (!PokeFinderActivity.mapHelper.promptForApiKey(act)) return;
        for (int n = 0; n < accounts.size(); n++) {
            Account account = accounts.get(n);
            account.setCon(act);
            account.login();
        }
    }

    public static void showLoginScreen(final Account account, final Activity act) {
        // Show login screen
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(R.string.loginTitle);
        //builder.setMessage(R.string.loginMessage);
        View view = act.getLayoutInflater().inflate(R.layout.login, null);
        builder.setView(view);

        final EditText username = (EditText) view.findViewById(R.id.username);
        final EditText password = (EditText) view.findViewById(R.id.password);
        final TextView createAccount = (TextView) view.findViewById(R.id.createAccountLink);
        createAccount.setText(act.getResources().getText(R.string.createAccountMessage));
        createAccount.setMovementMethod(LinkMovementMethod.getInstance());

        if (account != null) {
            username.setText(account.getUsername());
            password.setText(account.getPassword());
        }

        builder.setPositiveButton(R.string.loginButton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (username.getText().toString().equals("") || password.getText().toString().equals("")) {
                    PokeFinderActivity.features.longMessage("Username and password must not be blank");
                    return;
                }

                if (username.getText().toString().contains("@gmail")) {
                    PokeFinderActivity.features.longMessage("Google logins are not yet supported, coming in a future update.");
                    return;
                }

                if (Build.VERSION.SDK_INT < 19 && username.getText().toString().contains("@gmail")) {
                    PokeFinderActivity.features.longMessage("Google logins are only supported for Android API 19 or higher. Please update your Android OS to use a Pokemon Trainer Club account.");
                    return;
                }

                if (account == null) {
                    // Adding a new account
                    Account newAccount = new Account(username.getText().toString(), password.getText().toString(), incNumAccounts(), act);
                    boolean dupe = false;

                    for (Account tempAccount : accounts) {
                        if (tempAccount.getUsername().equals(newAccount.getUsername())) {
                            AccountManager.decNumAccounts();
                            dupe = true;
                            break;
                        }
                    }

                    if (dupe) {
                        PokeFinderActivity.features.longMessage("You already have an account named " + username.getText().toString() + "!");
                        return;
                    }

                    newAccount.login();
                    accounts.add(newAccount);

                    if (act instanceof AccountsActivity) {
                        if (((AccountsActivity) act).adapter == null) ((AccountsActivity) act).initAdapter();
                        ((AccountsActivity) act).adapter.notifyDataSetChanged();
                    }
                } else {
                    // Changing credentials for an old account
                    account.changeCreds(username.getText().toString(), password.getText().toString());
                    account.login();
                }
                AccountsActivity.reloadData();
            }
        });
        builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing
            }
        });

        try {
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String decode(String string) {
        return PokeFinderActivity.features.decode(string);
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
            login(PokeFinderActivity.instance);
            return;
        }

        PokeFinderActivity.instance.resetGoodAccountsLabelText();
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
        NativePreferences.putInt(PREF_NUM_ACCOUNTS, numAccounts);
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
            account.saveCreds(account.getAccountNumber(), account.getUsername(), account.getPassword(), account.getToken(), account.getCon());
        }

        PokeFinderActivity.instance.updateGoodAccountsLabelText();
    }

    public synchronized static void removePrefs(int accountNum) {
        String suffix = accountNum == 1 ? "" : (accountNum + "");

        NativePreferences.lock("remove prefs");
        NativePreferences.remove(PREF_USERNAME_PREFIX + suffix);
        NativePreferences.remove(PREF_PASSWORD_PREFIX + suffix);
        NativePreferences.remove(PREF_TOKEN_PREFIX + suffix);
        NativePreferences.unlock();
    }

    public synchronized static void deleteAllAccounts() {
        while (numAccounts > 0) {
            removeAccount(0);
            AccountsActivity.reloadData();
        }
    }

    public synchronized static Future run(Runnable runnable) {
        if (pool == null) {
            PokeFinderActivity.features.print("PokeFinder", "Initializing a new thread pool");
            pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            //pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
        }
        Future future = pool.submit(new ExceptionCatchingRunnable(runnable));
        //if (IOSLauncher.IS_AD_TESTING) PokeFinderActivity.features.print("PokeFinder", pool.getQueue().toString());
        return future;
        //DispatchQueue.getGlobalQueue(DispatchQueue.PRIORITY_BACKGROUND, 0).async(runnable);
        //return null;
    }

    public static void purgePool() {
        // Testing purposes only
        if (pool != null) {
            pool.shutdownNow();
            PokeFinderActivity.features.print("PokeFinder", "Initializing a new thread pool");
            pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            //pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
        }
    }

    public static void switchHashProvider() {
        for (int n = 1; n <= numAccounts; n++) {
            Account account = accounts.get(n-1);
            if (PokeFinderActivity.mapHelper.useNewApi) {
                try {
                    PokeFinderActivity.features.print("PokeFinder", "Hash provider for " + account.getUsername() + " is PokeHash");
                    if (account.go != null)
                        account.go.setHashProvider(new PokeHashProvider(PokeHashKey.from(PokeFinderActivity.mapHelper.newApiKey), false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                PokeFinderActivity.features.print("PokeFinder","Hash provider for " + account.getUsername() + " is legacy");
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

    public static void getHashInfo() {
        // Make sure legacy UNK25 is the current one
        /*try {
            String useOtherUnk = urlToStringOneLine(USE_OTHER_UNK_URL).replaceAll("\\\n","").trim();
            if (useOtherUnk.equals("1")) {
                String otherUnk = urlToStringOneLine(LEGACY_UNK_URL).replaceAll("\\\n","").trim();
                if (!otherUnk.equals("")) {
                    long otherUnkLong = 0;
                    try {
                        otherUnkLong = Long.parseLong(otherUnk);
                        PokeFinderActivity.features.print("PokeFinder", "Old UNK25: " + LegacyHashProvider.UNK25);
                        LegacyHashProvider.UNK25 = otherUnkLong;
                        PokeFinderActivity.features.print("PokeFinder", "New UNK25: " + otherUnk);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Crashlytics.logException(t);
                    }
                }
            } else {
                PokeFinderActivity.features.print("PokeFinder", "Using native UNK25. useOtherUnk is " + useOtherUnk);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Crashlytics.logException(t);
        }*/

        // Make sure Pokehash is up-to-date. Only covers changes to version and UK25 and endpoint
        NativePreferences.lock();

        int oldVersionInt = NativePreferences.getInt(PREF_POKEHASH_VERSION, Integer.MAX_VALUE);
        oldVersionInt = Math.max(oldVersionInt, PokeHashProvider.VERSION);

        //PokeHashProvider.VERSION = oldVersionInt = 5702;

        try {
            String versionIntString = urlToStringOneLine(POKEHASH_VERSION_INT_URL).trim();
            int versionInt = Integer.parseInt(versionIntString);

            if (versionInt > PokeHashProvider.VERSION) {
                String versionsJson = urlToStringOneLine(POKEHASH_URL_URL);
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> entries = gson.fromJson(versionsJson, type);

                String version = urlToStringOneLine(POKEHASH_VERSION_STRING_URL).trim();
                String hashUrlExtension = entries.get(version);
                String hashEndpoint = "https://pokehash.buddyauth.com/" + hashUrlExtension;

                String unkString = urlToStringOneLine(POKEHASH_UNK_URL).trim();
                long unk = Long.parseLong(unkString);

                PokeHashProvider.DEFAULT_ENDPOINT = hashEndpoint;
                PokeHashProvider.VERSION = versionInt;
                PokeHashProvider.UNK25 = unk;
                PokeHashProvider.VERSION_STRING = version;

                NativePreferences.putString(PREF_POKEHASH_URL, hashEndpoint);
                NativePreferences.putInt(PREF_POKEHASH_VERSION, versionInt);
                NativePreferences.putLong(PREF_POKEHASH_UNK, unk);
                NativePreferences.putString(PREF_POKEHASH_VERSION_STRING, version);

                PokeFinderActivity.features.print("PokeFinder", "Successfully retrieved hash info from server!");
            } else {
                PokeFinderActivity.features.print("PokeFinder", "Pokehash info is up-to-date already.");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            PokeFinderActivity.features.print("PokeFinder", "Something failed trying to fetch PokeHash stuff. Reverting to last known values.");

            try {
                int version = NativePreferences.getInt(PREF_POKEHASH_VERSION, Integer.MIN_VALUE);

                if (version > PokeHashProvider.VERSION) {
                    String url = NativePreferences.getString(PREF_POKEHASH_URL, "");
                    long unk = NativePreferences.getLong(PREF_POKEHASH_UNK, 1337);
                    String versionString = NativePreferences.getString(PREF_POKEHASH_VERSION_STRING, "ERROR");

                    if (!url.equals("") && version != Integer.MIN_VALUE && unk != 1337) {
                        PokeHashProvider.DEFAULT_ENDPOINT = url;
                        PokeHashProvider.VERSION = version;
                        PokeHashProvider.UNK25 = unk;
                        PokeHashProvider.VERSION_STRING = versionString;
                        PokeFinderActivity.features.print("PokeFinder", "Successfully retrieved hash info from prefs!");
                    }
                    PokeFinderActivity.features.print("PokeFinder", "Failed to get hash info from prefs. Reverting to originals");
                } else {
                    PokeFinderActivity.features.print("PokeFinder", "Pokehash info is up-to-date already. Canceling revert.");
                }
            } catch (Throwable t) {
                t.printStackTrace();
                Crashlytics.logException(t);
                PokeFinderActivity.features.print("PokeFinder", "Failed to get hash info. Reverting to originals");
            }
        }

        try {
            int newVersionInt = NativePreferences.getInt(PREF_POKEHASH_VERSION, Integer.MIN_VALUE);
            if (newVersionInt > oldVersionInt) {
                // We changed Pokehash config so let the user know what and why
                final String newVersionString = NativePreferences.getString(PREF_POKEHASH_VERSION_STRING, "ERROR");
                final String statusMessage = urlToStringOneLine(POKEHASH_STATUS_MESSAGE_URL);

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(act);
                        builder.setTitle("Paid API Updated")
                                .setMessage("PokeSensor has updated its configuration to support version " + newVersionString + " of the paid API. Status: " + statusMessage)
                                .setPositiveButton("Got it", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Do nothing
                                    }
                                }).setCancelable(false);

                        builder.create().show();
                    }
                };
                act.runOnUiThread(runnable);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }

        NativePreferences.unlock();

        PokeFinderActivity.features.print("PokeFinder", "Using hash endpoint " + PokeHashProvider.DEFAULT_ENDPOINT + " with version " + PokeHashProvider.VERSION + " and unk " + PokeHashProvider.UNK25);
    }

    private static void initHandlers() {
        /*if (requestHandlers == null) {
            requestHandlers = new ArrayList<>();
            for (int n = 0; n < MAX_POOL_THREADS; n++) {
                requestHandlers.add(new RequestHandler(new OkHttpClient()));
            }
        }*/
    }

    public static void assignRequestHandler(Account account) {
        /*String threadName = Thread.currentThread().getName();
        int start = threadName.lastIndexOf("-");
        //int end = threadName.lastIndexOf("-");
        int index = Integer.parseInt(threadName.substring(start+1)) - 1;
        RequestHandler handler = requestHandlers.get(index);
        if (account.go != null) {
            handler.setApi(account.go);
            account.go.setRequestHandler(handler);
        }
        account.requestHandler = handler;
        account.httpClient = handler.client;
        PokeFinderActivity.features.print("PokeFinder",account.getUsername() + " was assigned to request handler " + index + " and is on the thread " + threadName);*/
    }
}
