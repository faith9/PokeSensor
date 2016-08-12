package com.logickllc.pokesensor;

import org.robovm.apple.corelocation.CLAuthorizationStatus;
import org.robovm.apple.corelocation.CLLocationManager;
import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.uikit.UIActionSheet;
import org.robovm.apple.uikit.UIActionSheetDelegateAdapter;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIBarButtonItem;
import org.robovm.apple.uikit.UIBarButtonItem.OnClickListener;
import org.robovm.apple.uikit.UIBarButtonItemStyle;
import org.robovm.apple.uikit.UIBarButtonSystemItem;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UINavigationController;
import org.robovm.apple.uikit.UIToolbar;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("NavigationController")
public class NavigationController extends UINavigationController {
	@IBOutlet
	private UIToolbar customToolbar;

	private final long UNDO_TAG = 1;
	private final long NEW_GAME_TAG = 2;
	private final long SETTINGS_TAG = 3;
	private boolean isLoaded = false;

	private final String SETTINGS_VIEW_CONTROLLER = "Settings";
	private final String SCAN_DETAILS_VIEW_CONTROLLER = "ScanDetails";

	@Override
	public void viewDidAppear(boolean arg) {
		if (!isLoaded) {
			this.setNavigationBarHidden(false);
			IOSLauncher.navigationController = this;
			//Utilities.debug("Got a handle to the Navigation Controller");
			//DefaultIOSLauncher.myApp.getUIWindow().setRootViewController(this);
			//this.showViewController(DefaultIOSLauncher.myApp.getUIViewController(), null);
			//DefaultIOSLauncher.showViewController(this, DefaultIOSLauncher.myApp.getUIViewController());
			//showViewInNavigationController(DefaultIOSLauncher.myApp.getUIViewController());

			MapController mapController = (MapController) this.getStoryboard().instantiateViewController("MapController");
			this.showViewController(mapController, null);

			UIBarButtonItem scanDetailsButton = new UIBarButtonItem(UIImage.create("scan_details.png"), UIBarButtonItemStyle.Plain);
			UIBarButtonItem flexButton1 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
			final UIBarButtonItem locateButton = new UIBarButtonItem(UIImage.create("locate.png"), UIBarButtonItemStyle.Plain);
			UIBarButtonItem flexButton2 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
			UIBarButtonItem scanButton = new UIBarButtonItem(UIImage.create("scan.png"), UIBarButtonItemStyle.Plain);
			UIBarButtonItem flexButton3 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
			UIBarButtonItem settingsButton = new UIBarButtonItem(UIImage.create("settings.png"), UIBarButtonItemStyle.Plain);

			//NSArray<UIBarButtonItem> items = customToolbar.getItems();
			NSArray<UIBarButtonItem> items = new NSArray<UIBarButtonItem>(scanDetailsButton, flexButton1, locateButton,
					flexButton2, scanButton, flexButton3, settingsButton);

			scanDetailsButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(UIBarButtonItem arg0) {
					ScanDetailsController scanDetailsController = (ScanDetailsController) IOSLauncher.navigationController.getStoryboard().instantiateViewController(SCAN_DETAILS_VIEW_CONTROLLER);
					showViewInNavigationController(scanDetailsController);
					scanDetailsController.getNavigationController().setNavigationBarHidden(false);
				}

			});
			locateButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(UIBarButtonItem arg0) {
					if (CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Denied
							|| CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Restricted) {
						MapController.instance.deniedLocationPermission();
					} else { 
						MapController.instance.initLocation();
					}
				}

			});

			scanButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(UIBarButtonItem arg0) {
					MapController.mapHelper.wideScan();
				}

			});

			settingsButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(UIBarButtonItem arg0) {
					//FreeCell.instance.playSound("settings_placeholder.wav");
					SettingsController settingsController = (SettingsController) IOSLauncher.navigationController.getStoryboard().instantiateViewController(SETTINGS_VIEW_CONTROLLER);
					//DefaultIOSLauncher.navigationController.showViewController(settingsController, null);
					showViewInNavigationController(settingsController);
					settingsController.getNavigationController().setNavigationBarHidden(false);
				}

			});

			//customToolbar.setItems(items);

			//DefaultIOSLauncher.myApp.getUIViewController().setToolbarItems(customToolbar.getItems());
			mapController.setToolbarItems(items);
			isLoaded = true;
			DialogHelper.initialize();
		}
	}	

	public void showViewInNavigationController(UIViewController controller) {
		if (Foundation.getMajorSystemVersion() >= 8) {
			this.showViewController(controller, null);
		} else {
			this.pushViewController(controller, true);
		}
	}

	@Override
	public void viewWillAppear(boolean animated) {
		super.viewWillAppear(animated);
	}
}
