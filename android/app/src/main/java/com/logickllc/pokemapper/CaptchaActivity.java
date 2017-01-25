package com.logickllc.pokemapper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

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

    public void loadCaptcha(String url, PokemonGo go) {
        this.go = go;
        WebView webView = (WebView) findViewById(R.id.captchaWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        if (webView != null) webView.loadUrl(url);
        else PokeFinderActivity.features.longMessage("Error loading captcha...");
    }

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
    }
}
