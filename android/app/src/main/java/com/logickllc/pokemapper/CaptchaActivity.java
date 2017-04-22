package com.logickllc.pokemapper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.internal.models.ThreadData;
import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.main.RequestHandler;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class CaptchaActivity extends AppCompatActivity {
    private final String CAPTCHA_RESPONSE_ELEMENT = "g-recaptcha-response";
    public PokemonGo go;
    public String name;
    public boolean disappearing = false;
    public Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha);
        if (PokeFinderActivity.features.provoking) throw new RuntimeException("Got a captcha!");
        Intent intent = getIntent();

        account = AccountManager.accounts.get(intent.getIntExtra("account_index", 0));
        go = account.go;
        name = account.getUsername();

        loadCaptcha(account.getCaptchaUrl(), go);

        TextView label = (TextView) findViewById(R.id.captchaLabel);
        if (label != null) label.setText("Please complete the captcha below to re-enable " + name + ". Click 'Verify' and then go back when you are done.");

        Button done = (Button) findViewById(R.id.captchaDone);
        if (done != null) done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done();
            }
        });
        done.setVisibility(View.GONE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void loadCaptcha(String url, final PokemonGo go) {
        final Activity act = this;
        this.go = go;
        WebView webView = (WebView) findViewById(R.id.captchaWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                try {
                    throw new RuntimeException(description);
                } catch (Throwable e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                try {
                    throw new RuntimeException(error.toString());
                } catch (Throwable e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                try {
                    throw new RuntimeException(errorResponse.toString());
                } catch (Throwable e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                try {
                    handler.proceed();
                    throw new RuntimeException(error.toString());
                } catch (Throwable e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("unity:")) {
                    PokeFinderActivity.features.print("PokeFinder", url);
                    final String response = url.replace("unity:", "");
                    disappearing = true;
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            act.finish();
                        }
                    };
                    PokeFinderActivity.features.runOnMainThread(runnable);

                    Thread thread = new Thread() {
                        public void run() {
                            boolean success = PokeFinderActivity.features.verifyChallenge(response, go);
                            if (success) {
                                PokeFinderActivity.features.longMessage("Captcha verified!");
                                account.setStatus(Account.AccountStatus.GOOD);
                                account.setCaptchaUrl("");

                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!act.isDestroyed() && !act.isFinishing() && !disappearing) act.finish();
                                    }
                                };
                                PokeFinderActivity.features.runOnMainThread(r);
                            }
                            else {
                                PokeFinderActivity.features.longMessage("Error verifying captcha.");
                            }

                            if (disappearing) {
                                PokeFinderActivity.features.captchaScreenVisible = false;
                                account.captchaScreenVisible = false;
                            }
                        }
                    };
                    thread.start();
                    return true;
                } else {
                    return false;
                }


            }
        });
        if (webView != null) webView.loadUrl(url);
        else PokeFinderActivity.features.longMessage("Error loading captcha...");
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void done() {
        final Activity act = this;
        WebView webView = (WebView) findViewById(R.id.captchaWebView);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.evaluateJavascript("document.getElementById('" + CAPTCHA_RESPONSE_ELEMENT + "').value", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                final String response = value.replace("\"", "");
                Thread thread = new Thread() {
                    public void run() {
                        boolean success = PokeFinderActivity.features.verifyChallenge(response, go);
                        if (success) {
                            PokeFinderActivity.features.longMessage("Captcha verified!");
                            account.setStatus(Account.AccountStatus.GOOD);
                            account.setCaptchaUrl("");

                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    if (!act.isDestroyed() && !act.isFinishing() && !disappearing) act.finish();
                                }
                            };
                            PokeFinderActivity.features.runOnMainThread(r);
                        }
                        else {
                            PokeFinderActivity.features.longMessage("Error verifying captcha.");
                        }

                        if (disappearing) {
                            PokeFinderActivity.features.captchaScreenVisible = false;
                            account.captchaScreenVisible = false;
                        }
                    }
                };
                thread.start();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        disappearing = true;
        if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
            // Try to send the verification here too
            done();
        } else {
            PokeFinderActivity.features.captchaScreenVisible = false;
            account.captchaScreenVisible = false;
        }

        if (PokeFinderActivity.IS_AD_TESTING) getAllHtml();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void getAllHtml() {
        final Activity act = this;
        WebView webView = (WebView) findViewById(R.id.captchaWebView);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.evaluateJavascript("document.getElementsByTagName('html')[0].innerHTML", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                System.out.println("\nFull HTML for captcha page:\n\n" + value + "\n");
            }
        });
    }
}
