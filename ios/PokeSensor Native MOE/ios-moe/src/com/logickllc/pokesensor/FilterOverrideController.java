package com.logickllc.pokesensor;


import com.logickllc.pokesensor.api.Features;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.foundation.NSArray;
import apple.uikit.UIBarButtonItem;
import apple.uikit.UIImage;
import apple.uikit.UITableViewController;
import apple.uikit.enums.UIBarButtonItemStyle;
import apple.uikit.enums.UIBarButtonSystemItem;
import apple.uikit.protocol.UITableViewDataSource;
import apple.uikit.protocol.UITableViewDelegate;

@Runtime(ObjCRuntime.class)
@ObjCClassName("FilterOverrideController")
@RegisterOnStartup
public class FilterOverrideController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {

	//@IBOutlet
    FilterOverrideTableView table;

	protected FilterOverrideController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewDidLoad() {
		UIBarButtonItem checkAllButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("check_all.png"), UIBarButtonItemStyle.Plain, this, new SEL("checkAllClicked:"));
		UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		UIBarButtonItem uncheckAllButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("uncheck_all.png"), UIBarButtonItemStyle.Plain, this, new SEL("uncheckAllClicked:"));

		//NSArray<UIBarButtonItem> items = customToolbar.getItems();
		NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(checkAllButton, flexButton1, uncheckAllButton, null);

		this.setToolbarItems(items);

		this.navigationController().setToolbarHidden(false);

		table.setup();
	}

	@Selector("checkAllClicked:")
	void checkAllClicked(UIBarButtonItem sender) {
		for (int n = 1; n <= Features.NUM_POKEMON; n++) {
			MapController.features.filterOverrides.put(n, true);
		}
		table.reloadData();
	}

	@Selector("uncheckAllClicked:")
	void uncheckAllClicked(UIBarButtonItem sender) {
		for (int n = 1; n <= Features.NUM_POKEMON; n++) {
			MapController.features.filterOverrides.put(n, false);
		}
		table.reloadData();
	}

	@Override
	public void viewWillDisappear(boolean b) {
		super.viewWillDisappear(b);

		this.navigationController().setToolbarHidden(true);
		MapController.features.saveFilterOverrides();
	}

	@Override
	public void viewWillAppear(boolean b) {
		super.viewWillAppear(b);

		this.navigationController().setToolbarHidden(false);
	}

	@IBOutlet
	@Property
	@Selector("table")
	public FilterOverrideTableView getTable() { return table; }

	@IBOutlet
	@Property
	@Selector("setTable:")
	public void setTable(FilterOverrideTableView table) { this.table = table; }
}
