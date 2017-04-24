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
import apple.uikit.UINavigationController;
import apple.uikit.UITableViewController;
import apple.uikit.enums.UIBarButtonItemStyle;
import apple.uikit.enums.UIBarButtonSystemItem;
import apple.uikit.protocol.UITableViewDataSource;
import apple.uikit.protocol.UITableViewDelegate;

@Runtime(ObjCRuntime.class)
@ObjCClassName("FilterController")
@RegisterOnStartup
public class FilterController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {

	//@IBOutlet
    FilterTableView table;

	protected FilterController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewDidLoad() {
		UIBarButtonItem ivButton = UIBarButtonItem.alloc().initWithTitleStyleTargetAction("IVs", UIBarButtonItemStyle.Plain, this, new SEL("ivsClicked:"));

		this.navigationItem().setRightBarButtonItem(ivButton);


		UIBarButtonItem checkAllButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("check_all.png"), UIBarButtonItemStyle.Plain, this, new SEL("checkAllClicked:"));
		UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		UIBarButtonItem uncheckAllButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("uncheck_all.png"), UIBarButtonItemStyle.Plain, this, new SEL("uncheckAllClicked:"));

		//NSArray<UIBarButtonItem> items = customToolbar.getItems();
		NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(checkAllButton, flexButton1, uncheckAllButton, null);

		this.setToolbarItems(items);

		this.navigationController().setToolbarHidden(false);

		table.setup();
	}

	@Selector("ivsClicked:")
	void ivsClicked(UIBarButtonItem sender) {
		UINavigationController nav = MapController.instance.navigationController();
		IVFilterController filterCont = (IVFilterController) nav.storyboard().instantiateViewControllerWithIdentifier("IVFilterController");
		nav.showViewControllerSender(filterCont, null);
	}

	@Selector("checkAllClicked:")
	void checkAllClicked(UIBarButtonItem sender) {
		for (int n = 1; n <= Features.NUM_POKEMON; n++) {
			MapController.features.filter.put(n, true);
		}
		table.reloadData();
	}

	@Selector("uncheckAllClicked:")
	void uncheckAllClicked(UIBarButtonItem sender) {
		for (int n = 1; n <= Features.NUM_POKEMON; n++) {
			MapController.features.filter.put(n, false);
		}
		table.reloadData();
	}

	@Override
	public void viewWillDisappear(boolean b) {
		super.viewWillDisappear(b);

		this.navigationController().setToolbarHidden(true);
		MapController.features.saveFilter();
	}

	@Override
	public void viewWillAppear(boolean b) {
		super.viewWillAppear(b);

		this.navigationController().setToolbarHidden(false);
	}

	@IBOutlet
	@Property
	@Selector("table")
	public FilterTableView getTable() { return table; }

	@IBOutlet
	@Property
	@Selector("setTable:")
	public void setTable(FilterTableView table) { this.table = table; }
}
