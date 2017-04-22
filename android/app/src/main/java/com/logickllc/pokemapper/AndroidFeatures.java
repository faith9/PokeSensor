package com.logickllc.pokemapper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.Features;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

public class AndroidFeatures extends Features {
    private Activity act;
    public static final String PREF_TOKEN = "Token";
    public static final String PREF_USERNAME = "ProfileName";
    public static final String PREF_PASSWORD = "Nickname";

    public static final String PREF_TOKEN2 = "Token2";
    public static final String PREF_USERNAME2 = "ProfileName2";
    public static final String PREF_PASSWORD2 = "Nickname2";

    private ProgressDialog progressDialog;

    public AndroidFeatures(Activity act) {
        this.act = act;
    }

    public void dismissProgressDialog() {
        try {
            if (progressDialog != null) progressDialog.dismiss();
            print(TAG, "Somehow the progress dialog is null! How is this possible???");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login() {
        AccountManager.login(act);
    }

    /*public void login(final boolean isSecond) {
        if (!loginLocked()) lockLogin();
        else return;
        Thread loginThread = new Thread() {
            public void run() {
                print(TAG, "Attempting to login...");
                try {
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                    if (!isSecond) token = preferences.getString(PREF_TOKEN, "");
                    else token2 = preferences.getString(PREF_TOKEN2, "");
                    *//*if (token != "") {
                        final ProgressDialog tryingDialog = showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                        boolean trying = true;
                        int failCount = 0;
                        final int MAX_TRIES = 3;
                        while (trying) {
                            try {
                                print(TAG, "Attempting to login with token: " + token);

                                OkHttpClient httpClient = new OkHttpClient();
                                go = new PokemonGo(auth, httpClient);
                                tryTalkingToServer(); // This will error if we can't reach the server
                                shortMessage(R.string.loginSuccessfulMessage);
                                unlockLogin();
                                dismissProgressDialog();
                                return;
                            } catch (Exception e) {
                                if (++failCount < MAX_TRIES) {
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e1) {
                                        e1.printStackTrace();
                                    }
                                } else {
                                    e.printStackTrace();
                                    token = "";
                                    print(TAG, "Erasing token because it seems to be expired.");
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString(PREF_TOKEN, token);
                                    editor.commit();
                                    //longMessage(R.string.loginFailedMessage);
                                    unlockLogin();
                                    dismissProgressDialog();
                                    login();
                                    return;
                                }
                            }
                        }
                    } else {*//*

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            String pastUsername;
                            String pastPassword;

                            if (!isSecond) {
                                pastUsername = preferences.getString(PREF_USERNAME, "");
                                pastPassword = preferences.getString(PREF_PASSWORD, "");
                            } else {
                                pastUsername = preferences.getString(PREF_USERNAME2, "");
                                pastPassword = preferences.getString(PREF_PASSWORD2, "");
                            }

                            if (!pastUsername.equals("") && !pastPassword.equals("")) {
                                final String username = decode(pastUsername);
                                final String password = decode(pastPassword);

                                if (username.equals("") || password.equals("")) {
                                    // Erase username and pass and prompt for login again
                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    if (!isSecond) {
                                        editor.putString(PREF_USERNAME, "");
                                        editor.putString(PREF_PASSWORD, "");
                                    } else {
                                        editor.putString(PREF_USERNAME2, "");
                                        editor.putString(PREF_PASSWORD2, "");
                                    }
                                    editor.commit();
                                    unlockLogin();
                                    login(isSecond);
                                    return;
                                }

                                final Thread thread = new Thread() {
                                    @Override
                                    public void run() {
                                        showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                                        boolean trying = true;
                                        int failCount = 0;
                                        final int MAX_TRIES = 10;
                                        while (trying) {
                                            OkHttpClient httpClient = new OkHttpClient();
                                            //RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth = null;
                                            try {
                                                //print(TAG, "Attempting to login with Username: " + username + " and password: " + password);

                                                //if (true) throw(new Exception("Answers test successful!"));

                                                PtcCredentialProvider provider = new PtcCredentialProvider(httpClient, username, password);
                                                final Lock lock = new Lock();
                                                OnCheckChallengeRequestListener challengeListener = new OnCheckChallengeRequestListener() {
                                                    @Override
                                                    public void onCheckChallenge(String s) {
                                                        RequestHandler.captchaRequired = true;
                                                        RequestHandler.captchaUrl = s;
                                                    }
                                                };

                                                print(TAG, "Logging in at coords: " + getMapHelper().getCurrentLat() + "," + getMapHelper().getCurrentLon());
                                                if (!isSecond) {
                                                    go = new PokemonGo(httpClient, getMapHelper().getCurrentLat(), getMapHelper().getCurrentLon(), 0);
                                                    go.setOnCheckChallengeRequestListener(challengeListener);
                                                    go.login(provider, new PokeCallback<Void>() {
                                                        @Override
                                                        public void onError(Throwable error) {
                                                            lock.object = error;
                                                            lock.unlock();
                                                        }

                                                        @Override
                                                        public void onResponse(Void aVoid) {
                                                            lock.unlock();
                                                        }
                                                    });
                                                    lock.waitForUnlock();
                                                    username1 = username;
                                                } else {
                                                    go2 = new PokemonGo(httpClient, getMapHelper().getCurrentLat(), getMapHelper().getCurrentLon(), 0);
                                                    go.setOnCheckChallengeRequestListener(challengeListener);
                                                    go2.login(provider, new PokeCallback<Void>() {
                                                        @Override
                                                        public void onError(Throwable error) {
                                                            lock.object = error;
                                                            lock.unlock();
                                                        }

                                                        @Override
                                                        public void onResponse(Void aVoid) {
                                                            lock.unlock();
                                                        }
                                                    });
                                                    lock.waitForUnlock();
                                                    username2 = username;
                                                }

                                                if (lock.object != null && lock.object instanceof Throwable) throw (Throwable) lock.object;

                                                shortMessage(username + " " + act.getResources().getString(R.string.loginSuccessfulMessage));
                                                if (!isSecond) token = provider.getTokenId();
                                                else token2 = provider.getTokenId();
                                                //print(TAG, "Token: " + token);
                                                SharedPreferences.Editor editor = preferences.edit();
                                                if (!isSecond) editor.putString(PREF_TOKEN, token);
                                                else editor.putString(PREF_TOKEN2, token2);
                                                editor.commit();
                                                unlockLogin();
                                                if (isSecond) useDualAccounts();
                                                if (progressDialog != null) dismissProgressDialog();
                                                if (refreshDualAccounts() && !isSecond) login(true);
                                                return;
                                            } catch (Throwable e) {
                                                if (isSecond) go2 = null;
                                                else go = null;

                                                if (!(e instanceof LoginFailedException) && !(e instanceof RemoteServerException)) {
                                                    // TODO Report this to answers without crashing the app
                                                    try {
                                                        if (PokeFinderActivity.IS_AD_TESTING) e.printStackTrace();
                                                        if (e.getCause().toString().contains("Captcha:")) {
                                                            String text = e.getCause().toString();
                                                            final String url = text.substring(text.indexOf("Captcha: ") + "Captcha: ".length());
                                                            print("PokeFinder", "You need to do the captcha at " + url);
                                                            longMessage("Captcha required for " + username + ". Please login to Pokemon GO with " + username + " to complete the captcha. Until then your scans won't work.");
                                                            unlockLogin();
                                                            if (progressDialog != null) dismissProgressDialog();
                                                            else print(TAG, "Somehow the progress dialog is null at this point. This shouldn't be possible...");
                                                            //This will show the captcha but it doesn't go away yet. Gotta figure out
                                                            // how to send verification back to Niantic somehow
                                                            *//*Timer timer = new Timer();
                                                            TimerTask task = new TimerTask() {

                                                                @Override
                                                                public void run() {
                                                                    Runnable captchaThread = new Runnable() {
                                                                        public void run() {
                                                                            try {
                                                                                act.startActivity(new Intent(Intent.ACTION_VIEW,
                                                                                        Uri.parse(url)));
                                                                            } catch (Exception e1) {
                                                                                e1.printStackTrace();
                                                                            }
                                                                        }
                                                                    };
                                                                    act.runOnUiThread(captchaThread);
                                                                }
                                                            };
                                                            timer.schedule(task, 3000);*//*
                                                            return;
                                                        } else {
                                                            Answers.getInstance().logCustom(new CustomEvent("Strange post login exception")
                                                                    .putCustomAttribute("Message", e.getMessage())
                                                                    .putCustomAttribute("Stack Trace", Log.getStackTraceString(e)));
                                                        }
                                                    } catch (Exception f) {
                                                        f.printStackTrace();
                                                    }
                                                }

                                                if (++failCount < MAX_TRIES) {
                                                    try {
                                                        Thread.sleep(3000);
                                                    } catch (InterruptedException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                } else {
                                                    e.printStackTrace();
                                                    // TODO Add some more advanced logic to give the user a reason why it failed
                                                    //if (e.getMessage().toLowerCase().contains("banned")) longMessage(username + " " + act.getResources().getString(R.string.loginBanned));
                                                    //else longMessage(username + " " + act.getResources().getString(R.string.loginFailedMessage));

                                                    if (getStackTraceString(e).contains("banned")) longMessage(username + " " + act.getResources().getString(R.string.loginBanned));
                                                    else if (e.getMessage().contains("Account is not yet active, please redirect.")) longMessage(username + " " + act.getResources().getString(R.string.accountInactive));
                                                    else longMessage(username + " " + act.getResources().getString(R.string.loginFailedMessage));

                                                    unlockLogin();
                                                    if (progressDialog != null) dismissProgressDialog();
                                                    else print(TAG, "Somehow the progress dialog is null at this point. This shouldn't be possible...");
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                };
                                thread.start();
                            } else {

                                // Show login screen
                                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                                builder.setTitle(R.string.loginTitle);
                                //builder.setMessage(R.string.loginMessage);
                                View view = act.getLayoutInflater().inflate(R.layout.login, null);
                                builder.setView(view);

                                final EditText username = (EditText) view.findViewById(R.id.username);
                                final EditText password = (EditText) view.findViewById(R.id.password);
                                final CheckBox rememberLogin = (CheckBox) view.findViewById(R.id.rememberLogin);
                                final TextView createAccount = (TextView) view.findViewById(R.id.createAccountLink);
                                createAccount.setText(act.getResources().getText(R.string.createAccountMessage));
                                createAccount.setMovementMethod(LinkMovementMethod.getInstance());

                                builder.setPositiveButton(R.string.loginButton, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Thread thread = new Thread() {
                                            @Override
                                            public void run() {
                                                if (rememberLogin.isChecked()) {
                                                    // Boss gave us permission to store the credentials
                                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                                                    SharedPreferences.Editor editor = preferences.edit();
                                                    if (!isSecond) {
                                                        editor.putString(PREF_USERNAME, encode(username.getText().toString()));
                                                        editor.putString(PREF_PASSWORD, encode(password.getText().toString()));
                                                    } else {
                                                        editor.putString(PREF_USERNAME2, encode(username.getText().toString()));
                                                        editor.putString(PREF_PASSWORD2, encode(password.getText().toString()));
                                                    }
                                                    editor.commit();
                                                }
                                                showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                                                boolean trying = true;
                                                int failCount = 0;
                                                final int MAX_TRIES = 10;
                                                while (trying) {
                                                    OkHttpClient httpClient = new OkHttpClient();
                                                    try {
                                                        //print(TAG, "Attempting to login with Username: " + username.getText().toString() + " and password: " + password.getText().toString());

                                                        PtcCredentialProvider provider = new PtcCredentialProvider(httpClient, username.getText().toString(), password.getText().toString());
                                                        final Lock lock = new Lock();
                                                        OnCheckChallengeRequestListener challengeListener = new OnCheckChallengeRequestListener() {
                                                            @Override
                                                            public void onCheckChallenge(String s) {
                                                                RequestHandler.captchaRequired = true;
                                                                RequestHandler.captchaUrl = s;
                                                            }
                                                        };
                                                        print(TAG, "Logging in at coords: " + getMapHelper().getCurrentLat() + "," + getMapHelper().getCurrentLon());
                                                        if (!isSecond) {
                                                            go = new PokemonGo(httpClient, getMapHelper().getCurrentLat(), getMapHelper().getCurrentLon(), 0);
                                                            go.setOnCheckChallengeRequestListener(challengeListener);
                                                            go.login(provider, new PokeCallback<Void>() {
                                                                @Override
                                                                public void onError(Throwable error) {
                                                                    lock.object = error;
                                                                    lock.unlock();
                                                                }

                                                                @Override
                                                                public void onResponse(Void aVoid) {
                                                                    lock.unlock();
                                                                }
                                                            });
                                                            lock.waitForUnlock();
                                                            username1 = username.getText().toString();
                                                        } else {
                                                            go2 = new PokemonGo(httpClient, getMapHelper().getCurrentLat(), getMapHelper().getCurrentLon(), 0);
                                                            go.setOnCheckChallengeRequestListener(challengeListener);
                                                            go2.login(provider, new PokeCallback<Void>() {
                                                                @Override
                                                                public void onError(Throwable error) {
                                                                    lock.object = error;
                                                                    lock.unlock();
                                                                }

                                                                @Override
                                                                public void onResponse(Void aVoid) {
                                                                    lock.unlock();
                                                                }
                                                            });
                                                            lock.waitForUnlock();
                                                            username2 = username.getText().toString();
                                                        }

                                                        if (lock.object != null && lock.object instanceof Throwable) throw (Throwable) lock.object;

                                                        shortMessage(username.getText().toString() + " " + act.getResources().getString(R.string.loginSuccessfulMessage));
                                                        //if (!isSecond) token = provider.getTokenId();
                                                        //else token2 = provider.getTokenId();
                                                        //print(TAG, "Token: " + token);
                                                        //SharedPreferences.Editor editor = preferences.edit();
                                                        //if (!isSecond) editor.putString(PREF_TOKEN, token);
                                                        //else editor.putString(PREF_TOKEN2, token2);
                                                        //editor.commit();
                                                        unlockLogin();
                                                        if (isSecond) useDualAccounts();
                                                        if (progressDialog != null) dismissProgressDialog();
                                                        if (refreshDualAccounts() && !isSecond) login(true);
                                                        return;
                                                    } catch (Throwable e) {
                                                        if (isSecond) go2 = null;
                                                        else go = null;

                                                        if (!(e instanceof LoginFailedException) && !(e instanceof RemoteServerException)) {
                                                            // TODO Report this to answers without crashing the app
                                                            try {
                                                                Answers.getInstance().logCustom(new CustomEvent("Strange first login exception")
                                                                        .putCustomAttribute("Message", e.getMessage())
                                                                        .putCustomAttribute("Stack Trace", Log.getStackTraceString(e)));
                                                            } catch (Exception f) {
                                                                f.printStackTrace();
                                                            }
                                                        }

                                                        if (++failCount < MAX_TRIES) {
                                                            try {
                                                                Thread.sleep(3000);
                                                            } catch (InterruptedException e1) {
                                                                e1.printStackTrace();
                                                            }
                                                        } else {
                                                            e.printStackTrace();
                                                            if (e.getMessage().toLowerCase().contains("banned")) longMessage(username.getText().toString() + " " + act.getResources().getString(R.string.loginBanned));
                                                            else longMessage(username.getText().toString() + " " + act.getResources().getString(R.string.loginFailedMessage));

                                                            unlockLogin();
                                                            if (progressDialog != null) dismissProgressDialog();

                                                            return;
                                                        }
                                                    }
                                                }
                                            }
                                        };
                                        thread.start();
                                    }
                                });
                                builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        unlockLogin();
                                    }
                                });

                                try {
                                    builder.create().show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    unlockLogin();
                                }
                            }
                        }
                    };
                    runOnMainThread(runnable);

                } catch (Exception e) {
                    print(TAG, "Login failed...");
                    e.printStackTrace();
                    unlockLogin();
                }
            }
        };
        loginThread.start();
    }*/

    /*public void logout(final boolean isSecond) {
        final Context con = act;

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(R.string.logoutTitle);
        builder.setMessage(R.string.logoutMessage);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Erase login creds so we can try again
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(con);
                SharedPreferences.Editor editor = prefs.edit();
                if (!isSecond) {
                    editor.putString(PREF_TOKEN, "");
                    editor.putString(PREF_USERNAME, "");
                    editor.putString(PREF_PASSWORD, "");
                } else {
                    editor.putString(PREF_TOKEN2, "");
                    editor.putString(PREF_USERNAME2, "");
                    editor.putString(PREF_PASSWORD2, "");
                    dontUseDualAccounts();
                }
                editor.apply();

                if (!isSecond) login(isSecond);
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        try {
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
            longMessage(R.string.logoutError);
        }
    }*/

    @Override
    public void print(String tag, String message) {
        if (PokeFinderActivity.IS_AD_TESTING) Log.d(tag, message);
    }

    @Override
    public void runOnMainThread(Runnable r) {
        act.runOnUiThread(r);
    }

    public void showProgressDialog(int titleid, int messageid) {
        showProgressDialog(act.getResources().getString(titleid), act.getResources().getString(messageid));
    }

    public void showProgressDialog(final String title, final String message) {
        final Context con = act;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    progressDialog = new ProgressDialog(con);
                    progressDialog.setTitle(title);
                    progressDialog.setMessage(message);
                    progressDialog.setIndeterminate(true);
                    progressDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        runOnMainThread(runnable);
    }

    public void shortMessage(int resid) {
        shortMessage(act.getResources().getString(resid));
    }

    public void shortMessage(final String message) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, message, Toast.LENGTH_SHORT).show();
            }
        };
        runOnMainThread(r);
    }

    public void longMessage(int resid) {
        longMessage(act.getResources().getString(resid));
    }

    public void longMessage(final String message) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, message, Toast.LENGTH_LONG).show();
            }
        };
        runOnMainThread(r);
    }

    public void superLongMessage(int resid) {
        superLongMessage(act.getResources().getString(resid));
    }

    public void superLongMessage(final String message) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, message, Toast.LENGTH_LONG).show();
            }
        };
        runOnMainThread(r);
    }

    public String getStackTraceString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public void loadCaptcha(final Account account) {
        final String url = account.getCaptchaUrl();

        if (Build.VERSION.SDK_INT >= 19) {
            captchaScreenVisible = true;
            account.captchaScreenVisible = true;
            Runnable r = new Runnable() {
                public void run() {
                    Intent intent = new Intent(act, CaptchaActivity.class);
                    int index = AccountManager.accounts.indexOf(account);
                    if (index >= 0 && index < AccountManager.accounts.size()) {
                        intent.putExtra("account_index", index);
                        act.startActivity(intent);
                    } else {
                        captchaScreenVisible = false;
                        account.captchaScreenVisible = false;
                    }
                }
            };

            runOnMainThread(r);
        } else {
            captchaScreenVisible = true;
            account.captchaScreenVisible = true;
            final String name = account.getUsername();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                    builder.setTitle("Account Disabled");
                    builder.setMessage("Please login to Pokemon Go with " + name + " and complete the captcha to re-enable this account. If you upgrade to Android KitKat or higher, you can re-enable your account within PokeSensor.");
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                        }
                    });
                    try {
                        builder.create().show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            runOnMainThread(r);
        }
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
            AccountManager.refreshAccounts(act);
        }
    }

    public static void showNativeOptionsList(ArrayList<String> options, final ArrayList<Lambda> functions, Activity act) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(act);
        builder.setItems(options.toArray(new String[options.size()]), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, final int which) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        functions.get(which).execute();
                    }
                };
                PokeFinderActivity.features.runOnMainThread(runnable);
            }
        });

        android.app.AlertDialog dialog = builder.create();

        // Hide the title so the popup list feels more natural. This won't work unless we create the Dialog without
        // using a builder, so may want to try that sometime
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }
}
