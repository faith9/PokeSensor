package com.logickllc.pokesensor.api;

import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.Circle;
import com.logickllc.pokemapper.AccountsActivity;
import com.logickllc.pokemapper.AndroidMapHelper;
import com.logickllc.pokemapper.NativePreferences;
import com.logickllc.pokemapper.PokeFinderActivity;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.listener.LoginListener;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.exceptions.hash.HashException;
import com.pokegoapi.exceptions.hash.HashLimitExceededException;
import com.pokegoapi.util.hash.HashProvider;
import com.pokegoapi.util.hash.legacy.LegacyHashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;
import com.twocaptcha.api.TwoCaptchaService;

import okhttp3.OkHttpClient;

public class Account {
    public PokemonGo go;
    private Boolean serverAlive = null;
    private boolean loggingIn = false;
    private boolean scanning = false;
    private String token = "";
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
    private OkHttpClient httpClient;
    private CredentialProvider provider;
    public Circle circle;
    private boolean popupCaptchaImmediately = false;
    private boolean solvingCaptcha = false;
    public long lastScanTime = 0;

    public enum AccountType {
        PTC, GOOGLE;
    }

    public enum AccountStatus {
        GOOD, ERROR, CAPTCHA_REQUIRED, NEEDS_EMAIL_VERIFICATION, BANNED, INVALID_CREDENTIALS, LOGGING_IN, SOLVING_CAPTCHA;
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

        setStatus(AccountStatus.LOGGING_IN);

        saveCreds(accountNumber, username, password, token, con);

        final Account me = this;

        Runnable loginThread = new Runnable() {
            public void run() {
                if (username.equals("") || password.equals("")) {
                    setStatus(AccountStatus.INVALID_CREDENTIALS);
                    unlockLogin();
                    return;
                }

                boolean trying = true;
                int failCount = 0;
                final int MAX_TRIES = 3;
                if (httpClient == null) httpClient = new OkHttpClient();
                while (trying) {
                    PokeFinderActivity.features.print(TAG, "Still trying login for " + username + " after failing " + failCount + " times...");
                    try {
                        if (username.contains("@gmail.com")) accountType = AccountType.GOOGLE;
                        else accountType = AccountType.PTC;

                        if (provider == null) {
                            if (accountType == AccountType.GOOGLE)
                                provider = new GoogleUserCredentialProvider(httpClient);
                            else
                                provider = new PtcCredentialProvider(httpClient, username, password);
                        }

                        if (accountType == AccountType.GOOGLE) {
                            NativePreferences.lock();
                            token = NativePreferences.getString(PREF_TOKEN_PREFIX + (accountNumber == 1 ? "" : accountNumber + ""), token);
                            NativePreferences.unlock();

                            if (token.equals("")) {
                                // TODO Add way to authorize Google account and get Google token
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

                        if (go == null) go = new PokemonGo(httpClient);
                        else {
                            go.setLocation(getMapHelper().getCurrentLat(), getMapHelper().getCurrentLon(), 0);
                        }

                        //go.setOnCheckChallengeRequestListener(challengeListener);
                        go.addListener(new LoginListener() {
                            @Override
                            public void onLogin(PokemonGo pokemonGo) {
                                if (status == AccountStatus.LOGGING_IN) setStatus(AccountStatus.GOOD);
                                unlockLogin();
                            }

                            @Override
                            public void onChallenge(PokemonGo pokemonGo, String s) {
                                checkChallenge(s);
                                unlockLogin();
                            }
                        });

                        HashProvider hashProvider;
                        if (PokeFinderActivity.mapHelper.useNewApi && !PokeFinderActivity.mapHelper.newApiKey.equals("")) {
                            hashProvider = new PokeHashProvider(PokeFinderActivity.mapHelper.newApiKey);
                            PokeFinderActivity.features.print(TAG, "Using PokeHash API");
                        } else {
                            hashProvider = new LegacyHashProvider();
                            PokeFinderActivity.features.print(TAG, "Using Legacy API");
                        }
                        go.login(provider, hashProvider);

                        if (accountType == AccountType.GOOGLE) {
                            token = provider.getTokenId();

                            NativePreferences.lock();
                            NativePreferences.putString(PREF_TOKEN_PREFIX + (accountNumber == 1 ? "" : accountNumber + ""), token);
                            NativePreferences.unlock();
                        }

                        unlockLogin();
                        return;
                    } catch (Throwable e) {
                        if (checkExceptionForCaptcha(e)) {
                            unlockLogin();
                            return;
                        }

                        if (e.getMessage() != null && e.getMessage().contains("banned")) {
                            print(TAG, getStackTraceString(e));
                            setStatus(AccountStatus.BANNED);
                            unlockLogin();
                            return;
                        }
                        else if (e.getMessage() != null && e.getMessage().contains("Account is not yet active, please redirect.")) {
                            setStatus(AccountStatus.NEEDS_EMAIL_VERIFICATION);
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

                                if (e.getMessage() != null && e.getMessage().contains("banned"))
                                    setStatus(AccountStatus.BANNED);
                                else if (e.getMessage() != null && e.getMessage().contains("Account is not yet active, please redirect."))
                                    setStatus(AccountStatus.NEEDS_EMAIL_VERIFICATION);
                                else if (e.getMessage() != null && e.getMessage().contains("Please come back and try again"))
                                    setStatus(AccountStatus.INVALID_CREDENTIALS);
                                else
                                    setStatus(AccountStatus.ERROR);

                                unlockLogin();
                                return;
                            }
                        }

                        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("token not refreshed")) {
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

    public double getVisibleScanDistance() throws LoginFailedException, RemoteServerException {
        return go.getSettings().getMapSettings().getPokemonVisibilityRange();
    }

    public double getMinScanRefresh() throws LoginFailedException, RemoteServerException {
        return go.getSettings().getMapSettings().getMinRefresh() / 1000;
    }

    public synchronized void tryTalkingToServer() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (status != AccountStatus.GOOD && status != AccountStatus.LOGGING_IN) {
                    serverAlive = false;
                    if (status == AccountStatus.ERROR) login();
                    return;
                }

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
            Thread thread = new Thread() {
                public void run() {
                    solveCaptcha(s);
                }
            };
            thread.start();
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
            checkChallenge(((CaptchaActiveException) e).getCaptcha());
            return true;
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
        if (!go.hasChallenge() || !solvingCaptcha) {
            return false;
        }
        setStatus(AccountStatus.SOLVING_CAPTCHA);
        TwoCaptchaService service = new TwoCaptchaService(PokeFinderActivity.mapHelper.captchaKey, "6LeeTScTAAAAADqvhqVMhPpr_vB9D364Ia-1dSgK", url);

        int failCount = 0;
        int MAX_FAILS = 3;
        boolean success = false;

        while (failCount < MAX_FAILS && !success) {
            try {
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
}