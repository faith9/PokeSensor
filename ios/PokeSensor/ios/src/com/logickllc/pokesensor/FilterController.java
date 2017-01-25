package com.logickllc.pokesensor;


import com.badlogic.gdx.maps.Map;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.Spawn;

import org.robovm.apple.corelocation.CLGeocoder;
import org.robovm.apple.corelocation.CLLocation;
import org.robovm.apple.corelocation.CLPlacemark;
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
import org.robovm.objc.block.VoidBlock2;

@CustomClass("FilterController")
public class FilterController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {

	@IBOutlet
	FilterTableView table;

	@Override
	public void viewDidLoad() {
		UIBarButtonItem ivButton = new UIBarButtonItem("IVs", UIBarButtonItemStyle.Plain);
		ivButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {
			@Override
			public void onClick(UIBarButtonItem uiBarButtonItem) {
				UINavigationController nav = MapController.instance.getNavigationController();
				IVFilterController filterCont = (IVFilterController) nav.getStoryboard().instantiateViewController("IVFilterController");
				nav.showViewController(filterCont, null);
			}
		});
		this.getNavigationItem().setRightBarButtonItem(ivButton);


		UIBarButtonItem checkAllButton = new UIBarButtonItem(UIImage.create("check_all.png"), UIBarButtonItemStyle.Plain);
		UIBarButtonItem flexButton1 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
		final UIBarButtonItem uncheckAllButton = new UIBarButtonItem(UIImage.create("uncheck_all.png"), UIBarButtonItemStyle.Plain);

		//NSArray<UIBarButtonItem> items = customToolbar.getItems();
		NSArray<UIBarButtonItem> items = new NSArray<UIBarButtonItem>(checkAllButton, flexButton1, uncheckAllButton);

		checkAllButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {

			@Override
			public void onClick(UIBarButtonItem arg0) {
				for (int n = 1; n <= Features.NUM_POKEMON; n++) {
					MapController.features.filter.put(n, true);
				}
				table.reloadData();
			}

		});
		uncheckAllButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {

			@Override
			public void onClick(UIBarButtonItem arg0) {
				for (int n = 1; n <= Features.NUM_POKEMON; n++) {
					MapController.features.filter.put(n, false);
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
		MapController.features.saveFilter();
	}

	@Override
	public void viewWillAppear(boolean b) {
		super.viewWillAppear(b);

		this.getNavigationController().setToolbarHidden(false);
	}
}
