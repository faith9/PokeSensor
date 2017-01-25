package com.logickllc.pokesensor;


import com.logickllc.pokesensor.api.Features;

import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.uikit.UIBarButtonItem;
import org.robovm.apple.uikit.UIBarButtonItemStyle;
import org.robovm.apple.uikit.UIBarButtonSystemItem;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UINavigationController;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.apple.uikit.UITableViewDataSource;
import org.robovm.apple.uikit.UITableViewDelegate;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("FilterOverrideController")
public class FilterOverrideController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {

	@IBOutlet
	FilterOverrideTableView table;

	@Override
	public void viewDidLoad() {
		UIBarButtonItem checkAllButton = new UIBarButtonItem(UIImage.create("check_all.png"), UIBarButtonItemStyle.Plain);
		UIBarButtonItem flexButton1 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
		final UIBarButtonItem uncheckAllButton = new UIBarButtonItem(UIImage.create("uncheck_all.png"), UIBarButtonItemStyle.Plain);

		//NSArray<UIBarButtonItem> items = customToolbar.getItems();
		NSArray<UIBarButtonItem> items = new NSArray<UIBarButtonItem>(checkAllButton, flexButton1, uncheckAllButton);

		checkAllButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {

			@Override
			public void onClick(UIBarButtonItem arg0) {
				for (int n = 1; n <= Features.NUM_POKEMON; n++) {
					MapController.features.filterOverrides.put(n, true);
				}
				table.reloadData();
			}

		});
		uncheckAllButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {

			@Override
			public void onClick(UIBarButtonItem arg0) {
				for (int n = 1; n <= Features.NUM_POKEMON; n++) {
					MapController.features.filterOverrides.put(n, false);
				}
				table.reloadData();
			}

		});

		this.setToolbarItems(items);

		this.getNavigationController().setToolbarHidden(false);

		table.setup();
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
		if (fromInterfaceOrientation == UIInterfaceOrientation.Portrait
				|| fromInterfaceOrientation == UIInterfaceOrientation.PortraitUpsideDown)
			IOSLauncher.instance.changeOrientation(false);
		else
			IOSLauncher.instance.changeOrientation(true);
	}

	@Override
	public void viewWillDisappear(boolean b) {
		super.viewWillDisappear(b);

		this.getNavigationController().setToolbarHidden(true);
		MapController.features.saveFilterOverrides();
	}

	@Override
	public void viewWillAppear(boolean b) {
		super.viewWillAppear(b);

		this.getNavigationController().setToolbarHidden(false);
	}
}
