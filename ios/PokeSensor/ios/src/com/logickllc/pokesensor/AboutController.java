package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("AboutController")
public class AboutController extends UIViewController {
	@IBOutlet
	private UILabel label;

	@Override
	public void viewWillAppear(boolean animated) {
		super.viewWillAppear(animated);
		IOSLauncher.navigationController.setNavigationBarHidden(false);
		IOSLauncher.navigationController.setToolbarHidden(true);
		String text = R.string.aboutMessage;
		label.setNumberOfLines(0);
		label.setText(text);
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
