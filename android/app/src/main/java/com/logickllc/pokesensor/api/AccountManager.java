package com.logickllc.pokesensor.api;


import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.logickllc.pokemapper.AccountsActivity;
import com.logickllc.pokemapper.NativePreferences;
import com.logickllc.pokemapper.PokeFinderActivity;
import com.logickllc.pokemapper.R;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.util.hash.legacy.LegacyHashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    public static ExecutorService pool;
    public static final int MAX_POOL_THREADS = 10;

    public static void login(Activity activity) {
        act = activity;
        accounts = new ArrayList<>();
        NativePreferences.lock("account manager login()");
        numAccounts = NativePreferences.getInt(PREF_NUM_ACCOUNTS, 0);
        PokeFinderActivity.instance.canDecode = NativePreferences.getBoolean(PokeFinderActivity.instance.PREF_FIRST_DECODE_LOAD, true);
        NativePreferences.unlock();

        PokeFinderActivity.instance.updateGoodAccountsLabelText();

        if (numAccounts == 0) showLoginScreen(null, act);
        else {
            for (int n = 1; n <= numAccounts; n++) {
                String suffix = n == 1 ? "" : n + "";

                NativePreferences.lock("loading account creds " + n);
                String username = decode(NativePreferences.getString(PREF_USERNAME_PREFIX + suffix, ""));
                String password = decode(NativePreferences.getString(PREF_PASSWORD_PREFIX + suffix, ""));
                NativePreferences.unlock();

                Account account = new Account(username, password, n, act);
                account.login();
                accounts.add(account);
            }
        }

        NativePreferences.printLockStatus("After account manager login");

        NativePreferences.lock("deactivate decode");
        NativePreferences.putBoolean(PokeFinderActivity.instance.PREF_FIRST_DECODE_LOAD, false);
        NativePreferences.unlock();

        NativePreferences.printLockStatus("After deactivating decode");
    }

    public static void refreshAccounts(Activity act) {
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

    public static double getVisibleScanDistance() throws LoginFailedException, RemoteServerException {
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

        throw new RemoteServerException("No valid accounts to get scan distance");
    }

    public static double getMinScanRefresh() throws LoginFailedException, RemoteServerException {
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

        throw new RemoteServerException("No valid accounts to get min refresh");
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
                PokeFinderActivity.features.print("PokeFinder","Hash provider for " + account.getUsername() + " is PokeHash");
                if (account.go != null) account.go.setHashProvider(new PokeHashProvider(PokeFinderActivity.mapHelper.newApiKey));
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
}
