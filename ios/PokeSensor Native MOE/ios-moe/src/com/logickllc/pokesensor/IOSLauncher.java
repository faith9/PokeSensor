package com.logickllc.pokesensor;

import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.backends.iosmoe.IOSApplication;
import com.badlogic.gdx.backends.iosmoe.IOSApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;

import org.moe.natj.general.Pointer;

import apple.avfoundation.AVAudioSession;
import apple.c.Globals;
import apple.foundation.NSDictionary;
import apple.uikit.UIApplication;
import apple.uikit.UIWindow;
import apple.uikit.c.UIKit;

public class IOSLauncher extends IOSApplication.Delegate {
	public static NavigationController navigationController;
	public static final boolean IS_AD_TESTING = true;
	public boolean isPhone = true;
	private boolean isPortrait = true;
	public static IOSLauncher instance;
	private IOSApplication iosApp;

	public Vector2 size = Vector2.Zero;

	// Needed for storyboard
	private UIWindow window;

	// Needed for storyboard
	@Override
	public void setWindow(UIWindow value) {
		window = value;
	}

	// Needed for storyboard
	@Override
	public UIWindow window() {
		return window;
	}

	protected IOSLauncher(Pointer peer) {
		super(peer);
	}

	@Override
	protected IOSApplication createApplication() {
		try {
			instance = this;
			IOSApplicationConfiguration config = new IOSApplicationConfiguration();
			config.allowIpod = true;
			iosApp = new IOSApplication(new PokeSensor(), config);
			NativePreferences.init();

			iosApp.addLifecycleListener(new LifecycleListener() {

				@Override
				public void pause() {
					if (MapController.instance != null) MapController.instance.appPause();
				}

				@Override
				public void resume() {
					MapController.instance.appResume();
					MapController.instance.tryTalkingToServer();
				}

				@Override
				public void dispose() {
				}
			});

			return iosApp;
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logException(t);
			return iosApp;
		}
	}

	@Override
	public boolean applicationDidFinishLaunchingWithOptions(UIApplication application, NSDictionary<?, ?> launchOptions) {
		super.applicationDidFinishLaunchingWithOptions(application, launchOptions);
		try {
			Globals.sigignore(13); // Ignore SIGPIPE because it's not a big deal

			AVAudioSession.sharedInstance().setCategoryError("AVAudioSessionCategoryAmbient", null);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logException(e);
		}

		return true;
	}

	private void print(String message) {
		if (IS_AD_TESTING) System.out.println(message);
	}

	public static void main(String[] argv) {
		UIKit.UIApplicationMain(0, null, null, IOSLauncher.class.getName());
	}

	public void postNativeRunnable(Runnable runnable) {
		Globals.dispatch_async(Globals.dispatch_get_main_queue(), runnable::run);
	}
}
