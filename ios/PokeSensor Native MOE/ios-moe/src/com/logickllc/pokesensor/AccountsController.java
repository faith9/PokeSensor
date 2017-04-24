package com.logickllc.pokesensor;


import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

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
import apple.foundation.NSIndexPath;
import apple.foundation.NSString;
import apple.uikit.UIActivityViewController;
import apple.uikit.UIBarButtonItem;
import apple.uikit.UIImage;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.UITableViewController;
import apple.uikit.enums.UIBarButtonItemStyle;
import apple.uikit.enums.UIBarButtonSystemItem;
import apple.uikit.protocol.UITableViewDataSource;
import apple.uikit.protocol.UITableViewDelegate;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@Runtime(ObjCRuntime.class)
@ObjCClassName("AccountsController")
@RegisterOnStartup
public class AccountsController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {

	//@IBOutlet
	AccountTableView table;

	UIBarButtonItem exportButton;

	protected AccountsController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewDidLoad() {
		UIBarButtonItem addButton = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.Add, this, new SEL("addClicked:"));
		this.navigationItem().setRightBarButtonItem(addButton);

		UIBarButtonItem importButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("import.png"), UIBarButtonItemStyle.Plain, this, new SEL("importClicked:"));
		UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		exportButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("export.png"), UIBarButtonItemStyle.Plain, this, new SEL("exportClicked:"));
		UIBarButtonItem flexButton2 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		UIBarButtonItem deleteButton = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.Trash, this, new SEL("deleteClicked:"));
		//UIBarButtonItem flexButton3 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
		//UIBarButtonItem settingsButton = new UIBarButtonItem(UIImage.create("settings.png"), UIBarButtonItemStyle.Plain);

		//NSArray<UIBarButtonItem> items = customToolbar.getItems();
		NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(importButton, flexButton1, exportButton,
				flexButton2, deleteButton, null);

		this.setToolbarItems(items);

		this.navigationController().setToolbarHidden(false);

		table.setup(this);
	}

	@Selector("addClicked:")
	void addClicked(UIBarButtonItem sender) {
		AccountManager.showLoginScreen(null);
	}

	@Selector("importClicked:")
	void importClicked(UIBarButtonItem sender) {
		importAccounts();
	}

	@Selector("exportClicked:")
	void exportClicked(UIBarButtonItem sender) {
		exportAccounts();
	}

	@Selector("deleteClicked:")
	void deleteClicked(UIBarButtonItem sender) {
		Runnable positive = new Runnable() {
			@Override
			public void run() {
				AccountManager.deleteAllAccounts();
			}
		};
		DialogHelper.yesNoBox("Delete All?", "Are you sure you want to delete all accounts?", "Delete", positive, "Cancel", null).build().show();
	}

	@Override
	public void tableViewDidSelectRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
		UITableViewCell selected = tableView.cellForRowAtIndexPath(indexPath);
		tableView.deselectRowAtIndexPathAnimated(indexPath, true);
		int index = (int) indexPath.row();
		if (index >= AccountManager.accounts.size()) return;

		Account account = AccountManager.accounts.get(index);

		switch (account.getStatus()) {
			case CAPTCHA_REQUIRED:
				account.go.setHasChallenge(false);
				account.login(true);
				break;

			case WRONG_NAME_OR_PASSWORD:
				AccountManager.showLoginScreen(account);
				break;

			case NEEDS_EMAIL_VERIFICATION:
				MapController.features.longMessage("You need to verify your account using the activation email you received when you signed up.");
				break;

			case ERROR:
				account.login();
				break;

			case BANNED:
				MapController.features.longMessage("Niantic has banned your account from Pokemon Go. It will no longer work for scanning :(");
				break;
		}
	}

	@Override
	public void viewWillAppear(boolean b) {
		super.viewWillAppear(b);

		this.navigationController().setToolbarHidden(false);
		reloadData();
	}

	public static void reloadData() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (IOSLauncher.navigationController.topViewController() instanceof AccountsController) {
					AccountsController cont = (AccountsController) IOSLauncher.navigationController.topViewController();
					if (cont != null && cont.table != null) cont.table.reloadData();
				}
			}
		};
		MapController.features.runOnMainThread(runnable);
	}

	public void importAccounts() {
		ImportAccountsController cont = (ImportAccountsController) this.navigationController().storyboard().instantiateViewControllerWithIdentifier("ImportAccountsController");
		this.navigationController().showViewControllerSender(cont, null);
	}

	public void exportAccounts() {
		String accountString = "";
		if (accounts == null) {
			MapController.features.longMessage("Um...you don't have any accounts to export");
			return;
		}
		for (int n = 0; n < accounts.size(); n++) {
			Account account = accounts.get(n);
			accountString += account.getUsername() + "," + account.getPassword() + "\n";
		}
		shareText(accountString);
	}

	public void shareText(String text) {
		NSString nsText = NSString.stringWithString(text);
		NSArray<?> items = NSArray.arrayWithObject(nsText);

		UIActivityViewController controller = UIActivityViewController.alloc().initWithActivityItemsApplicationActivities(items, null);

		try {
			if (controller.popoverPresentationController() != null) {
				controller.popoverPresentationController().setBarButtonItem(exportButton);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
		}

		presentViewControllerAnimatedCompletion(controller, true, null);
	}

	@Override
	public void viewWillDisappear(boolean b) {
		super.viewWillDisappear(b);

		IOSLauncher.navigationController.setToolbarHidden(true);
	}

	@IBOutlet
	@Property
	@Selector("table")
	public AccountTableView getTable() { return table; }

	@IBOutlet
	@Property
	@Selector("setTable:")
	public void setTable(AccountTableView table) { this.table = table; }
}
