package com.logickllc.pokesensor;

import com.logickllc.pokesensor.api.Account;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.main.RequestHandler;

import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.foundation.NSURLRequest;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.apple.uikit.UIWebView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("CaptchaController")
public class CaptchaController extends UIViewController {
    private final String CAPTCHA_RESPONSE_ELEMENT = "g-recaptcha-response";
    public PokemonGo go;
    public Account account;
    public String name;
    public boolean disappearing = false;
    public UIViewController lastController;

    @IBOutlet
    public UIWebView webView;

    @IBOutlet
    public UILabel myLabel;

    @Override
    public void viewDidLoad() {
        super.viewDidLoad();
        go = account.go;
        loadCaptcha(account.getCaptchaUrl(), go);

        name = account.getUsername();

        MapController.features.print("PokeFinder", name);

        myLabel.setText("Please complete the captcha below to re-enable " + name + ". Click 'Verify' and then go back when you are done.");
    }

    @IBAction
    public void done() {
        /*if (webView.isLoading()) {
            MapController.features.longMessage("Please wait for the page to finish loading before clicking Done");
            return;
        }*/

        final String response = webView.evaluateJavaScript("document.getElementById('" + CAPTCHA_RESPONSE_ELEMENT + "').value");

        Thread thread = new Thread() {
            public void run() {
                boolean success = MapController.features.verifyChallenge(response, go);
                if (success) {
                    //MapController.features.longMessage("Captcha verified!");
                    account.setStatus(Account.AccountStatus.GOOD);
                    account.setCaptchaUrl("");

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (MapController.instance.getNavigationController().getTopViewController() instanceof CaptchaController && !disappearing) MapController.instance.getNavigationController().popViewController(true);
                        }
                    };
                    MapController.features.runOnMainThread(r);
                }
                else {
                    MapController.features.longMessage("Error verifying captcha.");
                }

                if (disappearing) {
                    MapController.features.captchaScreenVisible = false;
                    account.captchaScreenVisible = false;
                }
            }
        };
        thread.start();
    }

    public void loadCaptcha(String url, PokemonGo go) {
        this.go = go;
        webView.loadRequest(new NSURLRequest(new NSURL(url)));
    }

    @Override
    public void viewDidDisappear(boolean b) {
        super.viewDidDisappear(b);

        if (this.isMovingFromParentViewController()) {
            // If this disappeared due to another captcha controller, don't do this
            /*if (!MapController.instance.getNavigationController().getTopViewController().equals(this)
                && MapController.instance.getNavigationController().getTopViewController() instanceof CaptchaController
                && !MapController.instance.getNavigationController().getTopViewController().equals(lastController)) return;*/

            disappearing = true;
            if (account.getStatus() == Account.AccountStatus.CAPTCHA_REQUIRED) {
                // Try to send the verification here too
                done();
            } else {
                MapController.features.captchaScreenVisible = false;
                account.captchaScreenVisible = false;
            }
        }
    }

    @Override
    public void viewWillAppear(boolean animated) {
        super.viewWillAppear(animated);
        IOSLauncher.navigationController.setNavigationBarHidden(false);
        IOSLauncher.navigationController.setToolbarHidden(true);
        lastController = IOSLauncher.navigationController.getTopViewController();
    }

    @Override
    public void willRotate(UIInterfaceOrientation arg0, double arg1) {
        super.willRotate(arg0, arg1);
        IOSLauncher.instance.hideBanner();
    }

    @Override
    public void didRotate(UIInterfaceOrientation fromInterfaceOrientation) {
        super.didRotate(fromInterfaceOrientation);
        System.out.println("Did update to " + fromInterfaceOrientation);
        if (fromInterfaceOrientation == UIInterfaceOrientation.Portrait || fromInterfaceOrientation == UIInterfaceOrientation.PortraitUpsideDown)
            IOSLauncher.instance.changeOrientation(false);
        else
            IOSLauncher.instance.changeOrientation(true);
    }
}
