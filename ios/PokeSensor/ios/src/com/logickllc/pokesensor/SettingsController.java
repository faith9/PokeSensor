package com.logickllc.pokesensor;

import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UIStatusBarStyle;
import org.robovm.apple.uikit.UISwitch;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.apple.uikit.UITableViewRowAnimation;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

import com.badlogic.gdx.Gdx;

@CustomClass("SettingsController")
public class SettingsController extends UITableViewController {
	
	@IBOutlet
	UITableViewCell contactButton, logoutButton, twitterButton, facebookButton, moreAppsButton, aboutButton;

	@Override
	public UIStatusBarStyle getPreferredStatusBarStyle() {
		return UIStatusBarStyle.Default;
	}

	@Override
	public void viewDidLoad() {
		super.viewDidLoad();
		this.setNeedsStatusBarAppearanceUpdate();
		this.getNavigationController().setToolbarHidden(true, true);
	}

	@Override
	public void didSelectRow(UITableView tableView, NSIndexPath indexPath) {
		UITableViewCell selected = tableView.getCellForRow(indexPath);
		if (selected == contactButton) {
			tableView.deselectRow(indexPath, true);
			Gdx.net.openURI("mailto:logickllc@gmail.com");
		}
		else if (selected == aboutButton) {
			tableView.deselectRow(indexPath, true);
		}
		else if (selected == logoutButton) {
			tableView.deselectRow(indexPath, true);
			MapController.features.logout();
		} 
		else if (selected == twitterButton) {
			tableView.deselectRow(indexPath, true);
			Gdx.net.openURI("https://twitter.com/LogickLLC");
		}
		else if (selected == facebookButton) {
			tableView.deselectRow(indexPath, true);
			Gdx.net.openURI("https://www.facebook.com/Logick-LLC-984234335029611/");
			
		} else if (selected == moreAppsButton) {
			tableView.deselectRow(indexPath, true);
			Gdx.net.openURI("https://itunes.apple.com/us/developer/patrick-ballard/id1026470545");			
		}
	}

	@Override
	public void viewWillDisappear(boolean animated) {
		//this.getNavigationController().setNavigationBarHidden(true);
	}

	@Override
	public boolean isMovingFromParentViewController() {
		return super.isMovingFromParentViewController();
	}

	@Override
	public boolean prefersStatusBarHidden() {
		return false;
	}	
	
	@Override
	public void willRotate(UIInterfaceOrientation arg0, double arg1) {
		super.willRotate(arg0, arg1);
		IOSLauncher.instance.hide();
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
