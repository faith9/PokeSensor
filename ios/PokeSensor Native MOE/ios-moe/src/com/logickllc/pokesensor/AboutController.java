package com.logickllc.pokesensor;


import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UILabel;
import apple.uikit.UIViewController;

@Runtime(ObjCRuntime.class)
@ObjCClassName("AboutController")
@RegisterOnStartup
public class AboutController extends UIViewController {
	//@IBOutlet
	private UILabel label;

	protected AboutController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewWillAppear(boolean animated) {
		super.viewWillAppear(animated);
		IOSLauncher.navigationController.setNavigationBarHidden(false);
		IOSLauncher.navigationController.setToolbarHidden(true);
		String text = R.string.aboutMessage;
		label.setNumberOfLines(0);
		label.setText(text);
	}

	@IBOutlet
	@Property
	@Selector("label")
	public UILabel getLabel() { return label; }

	@IBOutlet
	@Property
	@Selector("setLabel:")
	public void setLabel(UILabel label) { this.label = label; }
}
