package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UITextField;
import org.robovm.apple.uikit.UITextFieldDelegateAdapter;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@CustomClass("LoginController")
public class LoginController extends UIViewController {
	@IBOutlet
	UITextField username, password;

	public Account account = null;

	@IBAction
	public void cancel() {
		this.dismissViewController(true, null);
	}
	
	@IBAction
	public void createAccount() {
		Gdx.net.openURI("https://www.pokemon.com/us/pokemon-trainer-club/sign-up/");
	}
	
	@IBAction
	public void login() {
		if (username.getText().equals("") || password.getText().equals("")) {
			DialogHelper.messageBox("Um...what?","Username and password can't be blank. I may only be an app, but even I knew that...").build().show();
			return;
		}
		if (!username.getText().contains("@gmail.com")) {
			if (username.getText().contains("@")) {
				DialogHelper.messageBox("Invalid Username", "Please login with your account name instead of your account email address.").build().show();
			} else {
				if (account == null) {
					// Adding a new account
					Account newAccount = new Account(username.getText(), password.getText(), AccountManager.incNumAccounts());

					boolean dupe = false;

					if (accounts != null) {
						for (Account tempAccount : accounts) {
							if (tempAccount.getUsername().equals(newAccount.getUsername())) {
								AccountManager.decNumAccounts();
								dupe = true;
								break;
							}
						}

						if (!dupe) {
							newAccount.login();
							AccountManager.accounts.add(newAccount);
						} else {
							MapController.features.longMessage("You already have an account named " + username.getText() + "!");
						}
					}
				} else {
					// Changing credentials for an old account
					account.changeCreds(username.getText(), password.getText());
					account.login();
				}
			}
		} else {
			DialogHelper.messageBox("Google Not Supported", "Sorry, Google accounts are not supported at this time. You can expect Google support in a future update.").build().show();
		}

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				AccountsController.reloadData();
			}
		};
		this.dismissViewController(true, runnable);
	}

	@Override
	public void viewWillAppear(boolean animated) {
		if (account != null) {
			username.setText(account.getUsername());
			password.setText(account.getPassword());
		}

		username.setDelegate(new UITextFieldDelegateAdapter() {

			@Override
			public boolean shouldReturn(UITextField textField) {
				password.becomeFirstResponder();
				return true;
			}
			
		});
		
		password.setDelegate(new UITextFieldDelegateAdapter() {

			@Override
			public boolean shouldReturn(UITextField textField) {
				password.resignFirstResponder();
				return true;
			}
			
		});
	}
	
	
}
