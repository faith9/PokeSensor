package com.logickllc.pokesensor;


import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.foundation.NSObject;
import org.robovm.apple.foundation.NSString;
import org.robovm.apple.uikit.UIActivityViewController;
import org.robovm.apple.uikit.UIBarButtonItem;
import org.robovm.apple.uikit.UIBarButtonItemStyle;
import org.robovm.apple.uikit.UIBarButtonSystemItem;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewCellEditingStyle;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.apple.uikit.UITableViewDataSource;
import org.robovm.apple.uikit.UITableViewDelegate;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@CustomClass("AccountsController")
public class AccountsController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {

	@IBOutlet
	AccountTableView table;

	UIBarButtonItem exportButton;

	@Override
	public void viewDidLoad() {
		UIBarButtonItem addButton = new UIBarButtonItem(UIBarButtonSystemItem.Add);
		addButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {
			@Override
			public void onClick(UIBarButtonItem uiBarButtonItem) {
				AccountManager.showLoginScreen(null);
			}
		});
		this.getNavigationItem().setRightBarButtonItem(addButton);

		UIBarButtonItem importButton = new UIBarButtonItem(UIImage.create("import.png"), UIBarButtonItemStyle.Plain);
		UIBarButtonItem flexButton1 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
		exportButton = new UIBarButtonItem(UIImage.create("export.png"), UIBarButtonItemStyle.Plain);
		UIBarButtonItem flexButton2 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
		UIBarButtonItem deleteButton = new UIBarButtonItem(UIBarButtonSystemItem.Trash);
		//UIBarButtonItem flexButton3 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
		//UIBarButtonItem settingsButton = new UIBarButtonItem(UIImage.create("settings.png"), UIBarButtonItemStyle.Plain);

		//NSArray<UIBarButtonItem> items = customToolbar.getItems();
		NSArray<UIBarButtonItem> items = new NSArray<UIBarButtonItem>(importButton, flexButton1, exportButton,
				flexButton2, deleteButton);

		importButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {

			@Override
			public void onClick(UIBarButtonItem arg0) {
				importAccounts();
			}

		});
		exportButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {

			@Override
			public void onClick(UIBarButtonItem arg0) {
				exportAccounts();
			}

		});

		deleteButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {

			@Override
			public void onClick(UIBarButtonItem arg0) {
				Runnable positive = new Runnable() {
					@Override
					public void run() {
						AccountManager.deleteAllAccounts();
					}
				};
				DialogHelper.yesNoBox("Delete All?", "Are you sure you want to delete all accounts?", "Delete", positive, "Cancel", null).build().show();
			}

		});

		this.setToolbarItems(items);

		this.getNavigationController().setToolbarHidden(false);

		table.setup(this);
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
	public void didSelectRow(UITableView tableView, NSIndexPath indexPath) {
		UITableViewCell selected = tableView.getCellForRow(indexPath);
		tableView.deselectRow(indexPath, true);
		int index = indexPath.getRow();
		if (index >= AccountManager.accounts.size()) return;

		Account account = AccountManager.accounts.get(index);

		switch (account.getStatus()) {
			case CAPTCHA_REQUIRED:
				account.go.setHasChallenge(false);
				account.login(true);
				break;

			case INVALID_CREDENTIALS:
				AccountManager.showLoginScreen(account);
				break;

			case NEEDS_EMAIL_VERIFICATION:
				MapController.features.longMessage("You need to verify your account using the activation email you received when you signed up.");
				break;

			case ERROR:
				account.login();
				break;
		}
	}

	@Override
	public void viewWillAppear(boolean b) {
		super.viewWillAppear(b);

		this.getNavigationController().setToolbarHidden(false);
		reloadData();
	}

	public static void reloadData() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (IOSLauncher.navigationController.getTopViewController() instanceof AccountsController) {
					AccountsController cont = (AccountsController) IOSLauncher.navigationController.getTopViewController();
					if (cont != null && cont.table != null) cont.table.reloadData();
				}
			}
		};
		MapController.features.runOnMainThread(runnable);
	}

	public void importAccounts() {
		ImportAccountsController cont = (ImportAccountsController) this.getNavigationController().getStoryboard().instantiateViewController("ImportAccountsController");
		this.getNavigationController().showViewController(cont, null);
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
		NSString nsText = new NSString(text);
		NSArray<?> items = new NSArray<NSObject>(nsText);

		UIActivityViewController controller = new UIActivityViewController(items, null);

		try {
			if (controller.getPopoverPresentationController() != null) {
				controller.getPopoverPresentationController().setBarButtonItem(exportButton);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
		}

		presentViewController(controller, true, null);
	}

	@Override
	public void viewWillDisappear(boolean b) {
		super.viewWillDisappear(b);

		IOSLauncher.navigationController.setToolbarHidden(true);
	}
}
