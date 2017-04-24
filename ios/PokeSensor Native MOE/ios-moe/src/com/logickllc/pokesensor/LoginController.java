package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBAction;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UITextField;
import apple.uikit.UIViewController;
import apple.uikit.protocol.UITextFieldDelegate;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@Runtime(ObjCRuntime.class)
@ObjCClassName("LoginController")
@RegisterOnStartup
public class LoginController extends UIViewController {
	//@IBOutlet
    UITextField username, password;

	public Account account = null;

	protected LoginController(Pointer peer) {
		super(peer);
	}

	@IBAction
	@Selector("cancel")
	public void cancel() {
		this.dismissViewControllerAnimatedCompletion(true, null);
	}
	
	@IBAction
	@Selector("createAccount")
	public void createAccount() {
		Gdx.net.openURI("https://www.pokemon.com/us/pokemon-trainer-club/sign-up/");
	}
	
	@IBAction
	@Selector("login")
	public void login() {
		if (username.text().equals("") || password.text().equals("")) {
			DialogHelper.messageBox("Um...what?","Username and password can't be blank. I may only be an app, but even I knew that...").build().show();
			return;
		}
		if (!username.text().contains("@gmail.com")) {
			if (username.text().contains("@")) {
				DialogHelper.messageBox("Invalid Username", "Please login with your account name instead of your account email address.").build().show();
			} else {
				if (account == null) {
					// Adding a new account
					Account newAccount = new Account(username.text(), password.text(), AccountManager.incNumAccounts());

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
							accounts.add(newAccount);
						} else {
							MapController.features.longMessage("You already have an account named " + username.text() + "!");
						}
					}
				} else {
					// Changing credentials for an old account
					account.changeCreds(username.text(), password.text());
					account.login();
				}
			}
		} else {
			DialogHelper.messageBox("Google Not Supported", "Sorry, Google accounts are not supported at this time. You can expect Google support in a future update.").build().show();
		}

		Block_dismissViewControllerAnimatedCompletion completionHandler = new Block_dismissViewControllerAnimatedCompletion() {
			@Override
			public void call_dismissViewControllerAnimatedCompletion() {
				AccountsController.reloadData();
			}
		};

		this.dismissViewControllerAnimatedCompletion(true, completionHandler);
	}

	@Override
	public void viewWillAppear(boolean animated) {
		if (account != null) {
			username.setText(account.getUsername());
			password.setText(account.getPassword());
		}

		username.setDelegate(new UITextFieldDelegate() {

			@Override
			public boolean textFieldShouldReturn(UITextField textField) {
				password.becomeFirstResponder();
				return true;
			}
			
		});
		
		password.setDelegate(new UITextFieldDelegate() {

			@Override
			public boolean textFieldShouldReturn(UITextField textField) {
				password.resignFirstResponder();
				return true;
			}
			
		});
	}

	@IBOutlet
	@Property
	@Selector("username")
	public UITextField getUsername() { return username; }

	@IBOutlet
	@Property
	@Selector("setUsername:")
	public void setUsername(UITextField username) { this.username = username; }

	@IBOutlet
	@Property
	@Selector("password")
	public UITextField getPassword() { return password; }

	@IBOutlet
	@Property
	@Selector("setPassword:")
	public void setPassword(UITextField password) { this.password = password; }
}
