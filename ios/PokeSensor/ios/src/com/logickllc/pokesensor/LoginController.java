package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UITextField;
import org.robovm.apple.uikit.UITextFieldDelegateAdapter;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

import com.badlogic.gdx.Gdx;

@CustomClass("LoginController")
public class LoginController extends UIViewController {
	@IBOutlet
	UITextField username, password;

	@IBAction
	public void cancel() {
		this.dismissViewController(true, null);
		MapController.features.unlockLogin();
	}
	
	@IBAction
	public void createAccount() {
		Gdx.net.openURI("https://www.pokemon.com/us/pokemon-trainer-club/sign-up/");
	}
	
	@IBAction
	public void login() {
		this.dismissViewController(true, null);
		MapController.features.tryLogin(username.getText(), password.getText());
	}

	@Override
	public void viewWillAppear(boolean animated) {
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
