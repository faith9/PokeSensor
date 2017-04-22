package com.logickllc.pokesensor.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.aerserv.sdk.AerServBanner;
import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.controller.listener.AerServEventListenerLocator;
import com.aerserv.sdk.view.component.ASWebView;
import com.crashlytics.android.Crashlytics;
import com.logickllc.pokemapper.PokeFinderActivity;


public class FixedAerservBanner extends AerServBanner {
    private boolean clicked = false;
    private boolean listenerSet = false;
    private ASWebView latestWebview = null;

    public FixedAerservBanner(Context context) {
        super(context);
    }

    public FixedAerservBanner(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void addView(View view) {
        super.addView(view);

        if (view instanceof ASWebView) {
            ASWebView webview = (ASWebView) view;
            /*webview.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                    if (url.contains("play.google") && !clicked) {
                        PokeFinderActivity.features.print("PokeFinder", "Aerserv attempted redirect rejected by onJsBeforeUnload for " + url);
                        return false;
                    } else {
                        PokeFinderActivity.features.print("PokeFinder", "Aerserv attempted redirect allowed by onJsBeforeUnload for " + url);
                        clicked = false;
                        return true;
                    }
                }

                @Override
                public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                    if (url.contains("play.google") && !clicked) {
                        PokeFinderActivity.features.print("PokeFinder", "Aerserv attempted redirect rejected by onJsAlert for " + url);
                        return true;
                    } else {
                        PokeFinderActivity.features.print("PokeFinder", "Aerserv attempted redirect allowed by onJsAlert for " + url);
                        clicked = false;
                        return false;
                    }
                }
            });*/
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (!url.contains("play.google") && !url.contains("market://")) {
                        PokeFinderActivity.features.print("PokeFinder", "User click detected. Aerserv attempted redirect allowed by shouldOverrideUrlLoading for " + url);
                        clicked = false;
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        try {
                            PokeFinderActivity.instance.startActivity(i);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Crashlytics.logException(e);
                            return false;
                        }
                        return true;
                    } else {
                        PokeFinderActivity.features.print("PokeFinder", "Aerserv attempted redirect rejected by shouldOverrideUrlLoading for " + url);
                        return false;
                    }

                    /*if (!url.contains("aerserv") && !clicked) {
                        PokeFinderActivity.features.print("PokeFinder", "Aerserv attempted redirect rejected by shouldOverrideUrlLoading for " + url);
                        return true;
                    } else {
                        if (clicked) {
                            PokeFinderActivity.features.print("PokeFinder", "User click detected. Aerserv attempted redirect allowed by shouldOverrideUrlLoading for " + url);
                            clicked = false;
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            PokeFinderActivity.instance.startActivity(i);
                            return true;
                        } else {
                            PokeFinderActivity.features.print("PokeFinder", "User did not click. Aerserv attempted redirect allowed by shouldOverrideUrlLoading for " + url);
                            return false;
                        }
                    }*/
                }
            });

            if (webview != latestWebview) {
                latestWebview = webview;
                /*webview.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        PokeFinderActivity.features.print("PokeFinder", "User clicked Aerserv banner!");
                        clicked = true;
                    }
                });*/
            }
        }
    }

    public void setupClickListener() {
        if (listenerSet) return;

        this.setClickable(true);
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                PokeFinderActivity.features.print("PokeFinder", "User clicked Aerserv banner!");
                clicked = true;
                return false;
            }
        });

        listenerSet = true;
    }

    public void testRedirect(String url) {
        if (latestWebview == null) {
            System.out.println("No handle to an ASWebview yet...can't test url " + url);
            return;
        }

        System.out.println("Testing url " + url);
        latestWebview.loadUrl(url);
    }
}
