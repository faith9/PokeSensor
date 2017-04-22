package com.logickllc.pokesensor.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.Circle;
import com.logickllc.pokemapper.AccountsActivity;
import com.logickllc.pokemapper.AndroidMapHelper;
import com.logickllc.pokemapper.NativePreferences;
import com.logickllc.pokemapper.PokeFinderActivity;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.listener.LoginListener;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleAutoCredentialProvider;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.request.CaptchaActiveException;
import com.pokegoapi.exceptions.request.HashException;
import com.pokegoapi.exceptions.request.HashLimitExceededException;
import com.pokegoapi.exceptions.request.LoginFailedException;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.main.RequestHandler;
import com.pokegoapi.util.hash.HashProvider;
import com.pokegoapi.util.hash.legacy.LegacyHashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashKey;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;
import com.twocaptcha.api.TwoCaptchaService;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class Account {
    public PokemonGo go;
    private Boolean serverAlive = null;
    private boolean loggingIn = false;
    private boolean scanning = false;
    private String token = "";
    private String authCode = "";
    private AccountType accountType;
    private int accountNumber;
    private AccountStatus status;
    private String username;
    private String password;
    private String captchaUrl;
    private final String TAG = "PokeFinder";
    private Context con;
    private final String PREF_TOKEN_PREFIX = "Token";
    public boolean captchaScreenVisible = false;
    public OkHttpClient httpClient;
    private CredentialProvider provider;
    public Circle circle;
    private boolean popupCaptchaImmediately = false;
    private boolean solvingCaptcha = false;
    public boolean wasError = false;
    public long lastScanTime = 0;
    public int scanErrorCount = 0;

    public static ExecutorService pool;
    public static final int MAX_POOL_THREADS = 5;

    public static ArrayList<RequestHandler> requestHandlers = null;
    public RequestHandler requestHandler = null;

    private static final String BANNED_STRING = "your account has been banned";
    private boolean updatedExpiration = false;

    public enum AccountType {
        PTC, GOOGLE;
    }

    public enum AccountStatus {
        GOOD, ERROR, CAPTCHA_REQUIRED, NEEDS_EMAIL_VERIFICATION, BANNED, WRONG_NAME_OR_PASSWORD, LOGGING_IN, SOLVING_CAPTCHA;
    }

    public Account(String username, String password, int accountNumber, Context con) {
        this.username = username;
        this.password = password;
        this.accountNumber = accountNumber;
        this.status = AccountStatus.LOGGING_IN;
        this.con = con;
    }

    public void changeCreds(String username, String password) {
        this.username = username;
        this.password = password;
        provider = null;
    }

    public static synchronized void saveCreds(int accountNumber, String username, String password, String token, Context con) {
        if (username.equals("") || password.equals("")) {
            try {
                throw new RuntimeException("Trying to save blank credentials!");
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
            return;
        }

        NativePreferences.lock("save creds");
        String suffix = accountNumber == 1 ? "" : (accountNumber + "");
        NativePreferences.putString(AccountManager.PREF_USERNAME_PREFIX + suffix, encode(username));
        NativePreferences.putString(AccountManager.PREF_PASSWORD_PREFIX + suffix, encode(password));
        NativePreferences.putString(AccountManager.PREF_TOKEN_PREFIX + suffix, token);
        NativePreferences.unlock();
    }

    public void login() {
        login(false);
    }

    public void login(final boolean popupCaptcha) {
        if (!loginLocked()) lockLogin();
        else {
            PokeFinderActivity.features.print(TAG, "Trying to login " + username + " but we were already trying to login!");
            return;
        }

        popupCaptchaImmediately = popupCaptcha;

        if (status == AccountStatus.ERROR) wasError = true;
        else wasError = false;

        setStatus(AccountStatus.LOGGING_IN);

        saveCreds(accountNumber, username, password, token, con);

        final Account me = this;

        Runnable loginThread = new Runnable() {
            public void run() {
                if (username.equals("") || password.equals("")) {
                    setStatus(AccountStatus.WRONG_NAME_OR_PASSWORD);
                    unlockLogin();
                    return;
                }

                AccountManager.assignRequestHandler(me);

                /*try {
                    String passwordUri = new URI("http://google.com/index.php?username=" + URLEncoder.encode(password, "UTF-8")).toASCIIString();
                    print(TAG, "Full passwordURI is " + passwordUri);
                    print(TAG, "After ? is " + passwordUri.substring(passwordUri.indexOf("?")));
                    print(TAG, "password replace all ? is " + password.replaceAll("\\?","%3F"));
                } catch (Exception e) {
                    e.printStackTrace();
                }*/

                boolean trying = true;
                int failCount = 0;
                final int MAX_TRIES = 5;
                if (httpClient == null) httpClient = new OkHttpClient();
                //if (requestHandler == null) requestHandler = new RequestHandler(httpClient);
                while (trying) {
                    PokeFinderActivity.features.print(TAG, "Still trying login for " + username + " after failing " + failCount + " times...");
                    try {
                        if (username.contains("@gmail.com")) accountType = AccountType.GOOGLE;
                        else accountType = AccountType.PTC;

                        if (provider == null) {
                            if (accountType == AccountType.GOOGLE) {
                                NativePreferences.lock();
                                token = NativePreferences.getString(PREF_TOKEN_PREFIX + (accountNumber == 1 ? "" : accountNumber + ""), token);
                                NativePreferences.unlock();

                                if (token.equals("")) {
                                    String authCode = authorizeGoogle();
                                    provider = new GoogleAutoCredentialProvider(httpClient, username, password);
                                    token = provider.getTokenId(false);
                                    NativePreferences.lock();
                                    NativePreferences.putString(PREF_TOKEN_PREFIX + (accountNumber == 1 ? "" : accountNumber + ""), token);
                                    NativePreferences.unlock();
                                    // TODO Add way to authorize Google account and get Google token
                                } else {
                                    provider = new GoogleUserCredentialProvider(httpClient, token);
                                }
                            }
                            else {
                                provider = new PtcCredentialProvider(httpClient, username, password);
                            }
                        }

                        /*OnCheckChallengeRequestListener challengeListener = new OnCheckChallengeRequestListener() {
                            @Override
                            public void onCheckChallenge(String s) {
                                    setStatus(AccountStatus.CAPTCHA_REQUIRED);
                                    captchaUrl = s;
                                if (popupCaptchaImmediately) {
                                    if (!captchaScreenVisible) {
                                        PokeFinderActivity.features.checkForCaptcha(me);
                                        popupCaptchaImmediately = false;
                                    }
                                }
                            }
                        };*/

                        print(TAG, "Logging in at coords: " + getMapHelper().getCurrentLat() + "," + getMapHelper().getCurrentLon());

                        final Lock lock = new Lock();

                        if (wasError || failCount == MAX_TRIES / 2) {
                            print(TAG, "Too many errors, trying a new PokemonGo object for " + username);
                            if (go != null) {
                                go.exit();
                            }
                            httpClient = new OkHttpClient();
                            go = new PokemonGo(httpClient, getMapHelper().getCurrentLat(), getMapHelper().getCurrentLon(), 0.0);
                            wasError = false;
                        }

                        if (go == null) {
                            go = new PokemonGo(httpClient, getMapHelper().getCurrentLat(), getMapHelper().getCurrentLon(), 0.0);
                            //go.setRequestHandler(requestHandler);
                            //requestHandler.api = go;
                        }
                        else {
                            go.setLocation(getMapHelper().getCurrentLat(), getMapHelper().getCurrentLon(), 0);
                        }

                        //go.setOnCheckChallengeRequestListener(challengeListener);
                        go.addListener(new LoginListener() {
                            @Override
                            public void onLogin(PokemonGo pokemonGo) {
                                if (status == AccountStatus.LOGGING_IN) {
                                    if (!go.hasChallenge()) {
                                        setStatus(AccountStatus.GOOD);
                                    } else {
                                        checkExceptionForCaptcha(new Exception("fake"));
                                    }
                                }
                                unlockLogin();
                            }

                            @Override
                            public void onChallenge(PokemonGo pokemonGo, String s) {
                                print(TAG, "onChallenge for " + username + " with URL " + s);
                                checkChallenge(s);
                                unlockLogin();
                            }
                        });

                        HashProvider hashProvider;
                        if (PokeFinderActivity.mapHelper.useNewApi && !PokeFinderActivity.mapHelper.newApiKey.equals("")) {
                            hashProvider = new PokeHashProvider(PokeHashKey.from(PokeFinderActivity.mapHelper.newApiKey), false);
                            PokeFinderActivity.features.print(TAG, "Using PokeHash API");
                        } else {
                            hashProvider = new LegacyHashProvider();
                            PokeFinderActivity.features.print(TAG, "Using Legacy API");
                        }

                        go.setHasChallenge(false);

                        if (username.equals("phract25")) {
                            username = username + "";
                        }

                        go.login(provider, hashProvider);

                        /*if (accountType == AccountType.GOOGLE) {
                            token = provider.getTokenId(true);

                            NativePreferences.lock();
                            NativePreferences.putString(PREF_TOKEN_PREFIX + (accountNumber == 1 ? "" : accountNumber + ""), token);
                            NativePreferences.unlock();
                        }*/

                        unlockLogin();

                        if (!updatedExpiration && PokeHashProvider.expiration != Long.MAX_VALUE) {
                            updatedExpiration = true;
                            NativePreferences.lock();
                            NativePreferences.putLong(PokeFinderActivity.mapHelper.newApiKey, PokeHashProvider.expiration);
                            NativePreferences.unlock();
                        }

                        return;
                    } catch (Throwable e) {
                        if (PokeFinderActivity.IS_AD_TESTING) print(TAG, getStackTraceString(e));

                        if (username.equals("phract1002")) {
                            username = username + "";
                        }

                        if (checkExceptionForCaptcha(e)) {
                            unlockLogin();
                            return;
                        }

                        if (go != null && go.hasChallenge()) {
                            print(TAG, "Secondary catch conditional for " + username + " with URL " + go.getChallengeURL());
                            print(TAG, "Has challenge " + go.getChallengeURL() + " but is still trying to login. This will always fail and end with Error. Resolving...");
                            checkChallenge(go.getChallengeURL());
                            unlockLogin();
                            return;
                        }

                        if (e.getMessage() != null && (e.getMessage().contains(BANNED_STRING) || getStackTraceString(e).contains(BANNED_STRING))) {
                        //if (go != null && go.getPlayerProfile().isBanned()) {
                            print(TAG, getStackTraceString(e));
                            setStatus(AccountStatus.BANNED);
                            unlockLogin();
                            return;
                        }
                        else if (e.getMessage() != null && e.getMessage().contains("Account is not yet active, please redirect.")) {
                            setStatus(AccountStatus.NEEDS_EMAIL_VERIFICATION);
                            unlockLogin();
                            return;
                        } else if ((e.getMessage() != null && (e.getMessage().contains("Your username or password is incorrect") || e.getMessage().contains("Please come back and try again")))) {
                            setStatus(AccountStatus.WRONG_NAME_OR_PASSWORD);
                            unlockLogin();
                            return;
                        }
                        else {
                            Throwable t = e;
                            while (t != null) {
                                if (t instanceof HashException) {
                                    if (t instanceof HashLimitExceededException) {
                                        PokeFinderActivity.mapHelper.waitForHashLimit();
                                        failCount--;
                                    } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unauthorized")) {
                                        PokeFinderActivity.mapHelper.tryGenericHashWarning("Something is wrong with your API key. Please make sure it is valid.");
                                        setStatus(AccountStatus.ERROR);
                                        unlockLogin();
                                        return;
                                    } else {
                                        PokeFinderActivity.mapHelper.tryGenericHashWarning(t.getMessage());
                                        setStatus(AccountStatus.ERROR);
                                        unlockLogin();
                                        return;
                                    }
                                    break;
                                } else {
                                    t = t.getCause();
                                }
                            }

                            if (++failCount < MAX_TRIES) {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                            } else {
                                e.printStackTrace(System.out);
                                //if (IOSLauncher.IS_AD_TESTING) PokeFinderActivity.features.superLongMessage(getStackTraceString(e));
                                //ErrorReporter.logExceptionThreaded(e);

                                if (e.getMessage() != null && (e.getMessage().contains(BANNED_STRING) || getStackTraceString(e).contains(BANNED_STRING)))
                                //if (go != null && go.getPlayerProfile().isBanned())
                                    setStatus(AccountStatus.BANNED);
                                else if (e.getMessage() != null && e.getMessage().contains("Account is not yet active, please redirect."))
                                    setStatus(AccountStatus.NEEDS_EMAIL_VERIFICATION);
                                else if (e.getMessage() != null && e.getMessage().contains("Please come back and try again"))
                                    setStatus(AccountStatus.WRONG_NAME_OR_PASSWORD);
                                else
                                    setStatus(AccountStatus.ERROR);

                                unlockLogin();
                                return;
                            }
                        }

                        if (e.getMessage() != null && e.getMessage().contains("Token error")) {
                            provider = null;
                        }

                        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("token not refreshed")) {
                            go.exit();
                            go = null;
                        }
                    }
                }
            }
        };
        AccountManager.run(loginThread);
    }

    public synchronized void setStatus(AccountStatus status) {
        this.status = status;
        print(TAG, "Account " + username + " status: " + status.name());
        PokeFinderActivity.instance.updateGoodAccountsLabelText();
        AccountsActivity.reloadData();
    }

    public void print(String tag, String message) {
        PokeFinderActivity.features.print(tag, message);
    }

    public static String encode(String string) {
        return PokeFinderActivity.features.encode(string);
    }

    public AndroidMapHelper getMapHelper() {
        return PokeFinderActivity.mapHelper;
    }

    public String getStackTraceString(Throwable t) {
        return PokeFinderActivity.features.getStackTraceString(t);
    }

    public double getVisibleScanDistance() throws LoginFailedException, RequestFailedException {
        return go.getSettings().getMapSettings().getPokemonVisibilityRange();
    }

    public double getMinScanRefresh() throws LoginFailedException, RequestFailedException {
        return go.getSettings().getMapSettings().getMinRefresh() / 1000;
    }

    public synchronized void tryTalkingToServer() {
        final Account me = this;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (status != AccountStatus.GOOD && status != AccountStatus.LOGGING_IN) {
                    serverAlive = false;
                    if (status == AccountStatus.ERROR) login();
                    return;
                }

                AccountManager.assignRequestHandler(me);

                serverAlive = null;
                setStatus(AccountStatus.LOGGING_IN);

                try {
            /*go.getPlayerProfile().updateProfile(new PokeCallback<PlayerProfile>() {
                @Override
                public void onResponse(PlayerProfile playerProfile) {
                    serverAlive = true;
                    setStatus(AccountStatus.GOOD);
                }

                @Override
                public void onError(Throwable error) {
                    serverAlive = false;
                    login();
                }
            });*/

                    go.getPlayerProfile().updateProfile();
                    serverAlive = true;
                    setStatus(AccountStatus.GOOD);
                } catch (Exception e) {
                    if (checkExceptionForCaptcha(e)) return;

                    serverAlive = false;
                    login();
                }
            }
        };
        AccountManager.run(runnable);
    }

    public void checkChallenge(final String s) {
        print(TAG, "Checking challenge for " + username + " with url " + s);
        if (status == AccountStatus.CAPTCHA_REQUIRED || status == AccountStatus.SOLVING_CAPTCHA) return; // We've already noted the captcha for this account
        if (scanning) PokeFinderActivity.mapHelper.showCaptchaCircle(this);
        setStatus(AccountStatus.CAPTCHA_REQUIRED);
        captchaUrl = s;
        if (PokeFinderActivity.mapHelper.use2Captcha) {
            solvingCaptcha = true;
            initHandlers();
            final Account me = this;
            Runnable thread = new Runnable() {
                public void run() {
                    //assignRequestHandler(me);
                    solveCaptcha(s);
                }
            };
            run(thread);
        } else {
            if (popupCaptchaImmediately) {
                if (!captchaScreenVisible) {
                    PokeFinderActivity.features.checkForCaptcha(this);
                    popupCaptchaImmediately = false;
                }
            }
        }
    }

    public boolean checkExceptionForCaptcha(Throwable e) {
        if (e instanceof CaptchaActiveException) {
            print(TAG, "checkExceptionForCaptcha primary conditional for " + username + " with URL " + ((CaptchaActiveException) e).getChallengeUrl());
            checkChallenge(((CaptchaActiveException) e).getChallengeUrl());
            return true;
        } else if (go != null && getStackTraceString(e).contains("com.pokegoapi.exceptions.CaptchaActiveException")) {
            print(TAG, "checkExceptionForCaptcha secondary conditional for " + username + " with URL " + go.getChallengeURL());
            print(TAG, "Not a captcha exception but it looks like we have a captcha so we'll mark it as such");
            checkChallenge(go.getChallengeURL());
            return  true;
        } else {
            return false;
        }
    }

    public String getCaptchaUrl() {
        return captchaUrl;
    }

    public void setCaptchaUrl(String captchaUrl) {
        this.captchaUrl = captchaUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isScanning() {
        return scanning;
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
    }

    public String getToken() {
        return token;
    }

    public Context getCon() {
        return con;
    }

    public void setCon(Context con) {
        this.con = con;
    }

    private synchronized void lockLogin() {
        if (!loginLocked()) loggingIn = true;
    }

    private synchronized void unlockLogin() {
        loggingIn = false;
    }

    public synchronized boolean loginLocked() {
        return loggingIn;
    }

    public synchronized boolean solveCaptcha(String url) {
        if (go == null) return false;
        if (!go.hasChallenge() || !solvingCaptcha) {
            return false;
        }
        setStatus(AccountStatus.SOLVING_CAPTCHA);
        TwoCaptchaService service = new TwoCaptchaService(PokeFinderActivity.mapHelper.captchaKey, "6LeeTScTAAAAADqvhqVMhPpr_vB9D364Ia-1dSgK", url);

        int failCount = 0;
        int MAX_FAILS = 1;
        boolean success = false;

        while (failCount < MAX_FAILS && !success) {
            try {
                print(TAG, "Starting 2captcha solve for " + username);
                String response = service.solveCaptcha();
                if (response.equals("")) {
                    solvingCaptcha = false;
                    setStatus(AccountStatus.CAPTCHA_REQUIRED);
                    return false;
                }
                PokeFinderActivity.features.print(TAG, "2Captcha returned this for " + username + "'s captcha: " + response);
                success = PokeFinderActivity.features.verifyChallenge(response, go);
                if (success) {
                    setStatus(Account.AccountStatus.GOOD);
                    setCaptchaUrl("");
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                failCount++;
            }
        }
        if (!success) setStatus(AccountStatus.CAPTCHA_REQUIRED);
        solvingCaptcha = false;
        return success;
    }

    public boolean isSolvingCaptcha() {
        return solvingCaptcha;
    }

    @SuppressLint("NewApi")
    public String authorizeGoogle() throws CaptchaActiveException, RequestFailedException, LoginFailedException {
        provider = new GoogleUserCredentialProvider(httpClient);
        authCode = "";

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                WebView webView = new WebView(PokeFinderActivity.instance);
                webView.setVisibility(View.GONE);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadUrl(GoogleUserCredentialProvider.LOGIN_URL);
                webView.setWebViewClient(new WebViewClient());
                webView.evaluateJavascript("document.getElementById('account-chooser-add-account').click();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value += " ";
                        // do nothing
                    }
                });
                webView.evaluateJavascript("document.getElementById('Email').value=\"" + username + "\"", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value += " ";
                        // do nothing
                    }
                });
                webView.evaluateJavascript("document.getElementById('next').click();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value += " ";
                        // do nothing
                    }
                });
                webView.evaluateJavascript("document.getElementById('Passwd').value=\"" + password + "\"", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value += " ";
                        // do nothing
                    }
                });
                webView.evaluateJavascript("document.getElementById('signIn').click();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value += " ";
                        // do nothing
                    }
                });
                webView.evaluateJavascript("document.getElementById('submit_approve_access').click();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value += " ";
                        // do nothing
                    }
                });
                webView.evaluateJavascript("document.getElementById('code').value;", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        value += " ";
                        authCode = value;
                    }
                });
            }
        };
        PokeFinderActivity.features.runOnMainThread(runnable);

        while (authCode.equals("")) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return authCode;
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
}