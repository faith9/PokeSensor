package com.logickllc.pokesensor;

import com.logickllc.pokesensor.api.Account;
import com.pokegoapi.api.PokemonGo;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBAction;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.foundation.NSURL;
import apple.foundation.NSURLRequest;
import apple.uikit.UILabel;
import apple.uikit.UIViewController;
import apple.uikit.UIWebView;

@Runtime(ObjCRuntime.class)
@ObjCClassName("CaptchaController")
@RegisterOnStartup
public class CaptchaController extends UIViewController {
    private final String CAPTCHA_RESPONSE_ELEMENT = "g-recaptcha-response";
    public PokemonGo go;
    public Account account;
    public String name;
    public boolean disappearing = false;
    public UIViewController lastController;

    //@IBOutlet
    public UIWebView webView;

    //@IBOutlet
    public UILabel myLabel;

    protected CaptchaController(Pointer peer) {
        super(peer);
    }

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
    @Selector("done")
    public void done() {
        /*if (webView.isLoading()) {
            MapController.features.longMessage("Please wait for the page to finish loading before clicking Done");
            return;
        }*/

        final String response = webView.stringByEvaluatingJavaScriptFromString("document.getElementById('" + CAPTCHA_RESPONSE_ELEMENT + "').value");

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
                            if (MapController.instance.navigationController().topViewController() instanceof CaptchaController && !disappearing) MapController.instance.navigationController().popViewControllerAnimated(true);
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
        webView.loadRequest(NSURLRequest.alloc().initWithURL(NSURL.URLWithString(url)));
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
        lastController = IOSLauncher.navigationController.topViewController();
    }

    @IBOutlet
    @Property
    @Selector("webView")
    public UIWebView getWebView() { return webView; }

    @IBOutlet
    @Property
    @Selector("setWebView:")
    public void setWebView(UIWebView webView) { this.webView = webView; }

    @IBOutlet
    @Property
    @Selector("myLabel")
    public UILabel getMyLabel() { return myLabel; }

    @IBOutlet
    @Property
    @Selector("setMyLabel:")
    public void setMyLabel(UILabel myLabel) { this.myLabel = myLabel; }
}
