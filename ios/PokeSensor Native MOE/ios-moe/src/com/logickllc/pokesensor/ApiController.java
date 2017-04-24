package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.MapHelper;
import com.pokegoapi.util.Signature;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.ByValue;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBAction;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.foundation.NSArray;
import apple.foundation.struct.NSRange;
import apple.uikit.UIBarButtonItem;
import apple.uikit.UIImage;
import apple.uikit.UILabel;
import apple.uikit.UITextField;
import apple.uikit.UIViewController;
import apple.uikit.enums.UIBarButtonItemStyle;
import apple.uikit.enums.UIBarButtonSystemItem;
import apple.uikit.protocol.UITextFieldDelegate;

@Runtime(ObjCRuntime.class)
@ObjCClassName("ApiController")
@RegisterOnStartup
public class ApiController extends UIViewController implements UITextFieldDelegate {
	public static final String PREF_EXPECTED_POKEMON = "ExpectedPokemon";
	public static final int REQUESTS_PER_LOGIN = 6;
	public static final int REQUESTS_PER_SECTOR_SCAN = 1;
	public static final int REQUESTS_PER_IV_SCAN = 1;
	public static final int MIN_SCAN_TIME = 10;
	public static final double SCANS_PER_MINUTE = 60 / MIN_SCAN_TIME;

	//@IBOutlet
	private UILabel loginRpm, scanRpm, ivRpm;

	//@IBOutlet
	private UITextField apiKey, numAccounts, numPokemon, scanRadius;

	protected ApiController(Pointer peer) {
		super(peer);
	}

	@IBAction
	@Selector("calculate")
	private void calculate() {
		int numAccountsInt, numPokemonInt, scanRadiusInt;

		try {
			numAccountsInt = Math.abs(Integer.parseInt(numAccounts.text()));
		} catch (Exception e) {
			e.printStackTrace();
			MapController.features.shortMessage("Accounts must be a valid number!");
			return;
		}

		try {
			numPokemonInt = Math.abs(Integer.parseInt(numPokemon.text()));
		} catch (Exception e) {
			e.printStackTrace();
			MapController.features.shortMessage("Expected Pokemon must be a valid number!");
			return;
		}

		try {
			scanRadiusInt = Math.abs(Integer.parseInt(scanRadius.text()));
		} catch (Exception e) {
			e.printStackTrace();
			MapController.features.shortMessage("Scan radius must be a valid number!");
			return;
		}

		if (scanRadiusInt > MapHelper.MAX_SCAN_DISTANCE) {
			MapController.features.shortMessage("Scan radius must be less than the max radius (" + MapHelper.MAX_SCAN_DISTANCE + " m)");
			return;
		}

		loginRpm.setText((numAccountsInt * REQUESTS_PER_LOGIN) + "");

		int scanRpmInt = ((int) Math.min(REQUESTS_PER_SECTOR_SCAN * SCANS_PER_MINUTE * numAccountsInt, getExpectedSectors(scanRadiusInt) * REQUESTS_PER_SECTOR_SCAN));
		scanRpm.setText(scanRpmInt + "");

		double temp = numPokemonInt * REQUESTS_PER_IV_SCAN / (getExpectedTime(scanRadiusInt) / numAccountsInt);
		int ivScanRpmInt = (int) Math.ceil(temp);
		ivScanRpmInt = Math.min(ivScanRpmInt, numPokemonInt * REQUESTS_PER_IV_SCAN);
		ivRpm.setText(ivScanRpmInt + scanRpmInt + "");
	}

	@Override
	public void viewWillAppear(boolean animated) {
		super.viewWillAppear(animated);

		apiKey.setText(MapController.mapHelper.newApiKey);

		numAccounts.setText(AccountManager.getNumAccounts() + "");

		NativePreferences.lock();
		int numPokemonInt = NativePreferences.getInteger(PREF_EXPECTED_POKEMON, 25);
		NativePreferences.unlock();
		numPokemon.setText(numPokemonInt + "");

		scanRadius.setText(MapController.mapHelper.getScanDistance() + "");

		calculate();

		UIBarButtonItem helpButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("help.png"), UIBarButtonItemStyle.Plain, this, new SEL("helpClicked:"));
		UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		UIBarButtonItem buyButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("buy.png"), UIBarButtonItemStyle.Plain, this, new SEL("buyClicked:"));

		NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(helpButton, flexButton1, buyButton, null);

		final UIViewController me = this;

		this.setToolbarItems(items);

		this.navigationController().setToolbarHidden(false);
	}

	@Selector("helpClicked:")
	void helpClicked(UIBarButtonItem sender) {
		Gdx.net.openURI(IOSMapHelper.PAID_API_HELP_PAGE_URL);
	}

	@Selector("buyClicked:")
	void buyClicked(UIBarButtonItem sender) {
		Gdx.net.openURI("https://hashing.pogodev.org");
	}

	@Override
	public void viewWillDisappear(boolean b) {
		NativePreferences.lock();
		try {
			int numPokemonInt = Integer.parseInt(numPokemon.text());
			NativePreferences.putInteger(PREF_EXPECTED_POKEMON, numPokemonInt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		NativePreferences.unlock();

		if (!apiKey.text().equals("") && !apiKey.text().equals(MapController.mapHelper.newApiKey)) {
			NativePreferences.lock();
			NativePreferences.putString(IOSMapHelper.PREF_NEW_API_KEY, apiKey.text());
			NativePreferences.unlock();

			MapController.mapHelper.newApiKey = apiKey.text();
			Signature.validApiKey = null;

			AccountManager.tryTalkingToServer();
		}

		IOSLauncher.navigationController.setToolbarHidden(true);
	}

	private int getExpectedSectors(int radius) {
		final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MapHelper.MAX_SCAN_RADIUS);
		final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
		final float ITERATIONS = MapHelper.MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

		int hexSectors = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);

		final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
		final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
		int sectors = BOXES_PER_ROW * BOXES_PER_ROW;

		int squareSectors = sectors;

		int squareSize = MINI_SQUARE_SIZE;
		int hexSize = (int) HEX_DISTANCE;

		double squareSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(MINI_SQUARE_SIZE / MapHelper.minScanTime, (double) MapController.mapHelper.getScanSpeed()));
		double hexSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(HEX_DISTANCE / MapHelper.minScanTime, (double) MapController.mapHelper.getScanSpeed()));

		if (hexSectors * hexSpeed <= squareSectors * squareSpeed) return hexSectors;
		else return squareSectors;
	}

	private double getExpectedTime(int radius) {
		final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MapHelper.MAX_SCAN_RADIUS);
		final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
		final float ITERATIONS = MapHelper.MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

		int hexSectors = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);

		final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
		final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
		int sectors = BOXES_PER_ROW * BOXES_PER_ROW;

		int squareSectors = sectors;

		int squareSize = MINI_SQUARE_SIZE;
		int hexSize = (int) HEX_DISTANCE;

		double squareSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(MINI_SQUARE_SIZE / MapHelper.minScanTime, (double) MapController.mapHelper.getScanSpeed()));
		double hexSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(HEX_DISTANCE / MapHelper.minScanTime, (double) MapController.mapHelper.getScanSpeed()));

		double seconds = 0;

		if (hexSectors * hexSpeed <= squareSectors * squareSpeed) seconds = hexSectors * hexSpeed;
		else seconds = squareSectors * squareSpeed;

		int minutes = (int) Math.ceil(seconds / 60);
		return minutes;
	}

	@Override
	public void viewDidLoad() {
		apiKey.setDelegate(this);
		numAccounts.setDelegate(this);
		numPokemon.setDelegate(this);
		scanRadius.setDelegate(this);

		MapController.addDoneButtonToKeyboard(numAccounts);
		MapController.addDoneButtonToKeyboard(numPokemon);
		MapController.addDoneButtonToKeyboard(scanRadius);
		MapController.addDoneButtonToKeyboard(apiKey);
	}

	@Override
	public boolean textFieldShouldBeginEditing(UITextField textField) {
		return true;
	}

	@Override
	public boolean textFieldShouldEndEditing(UITextField textField) {
		return true;
	}

	@Override
	public boolean textFieldShouldChangeCharactersInRangeReplacementString(UITextField textField, @ByValue NSRange range, String string) {
		return true;
	}

	@Override
	public boolean textFieldShouldClear(UITextField textField) {
		return true;
	}

	@Override
	public boolean textFieldShouldReturn(UITextField textField) {
		textField.resignFirstResponder();
		return false;
	}

	@IBOutlet
	@Property
	@Selector("loginRpm")
	public UILabel getLoginRpm() { return loginRpm; }

	@IBOutlet
	@Property
	@Selector("setLoginRpm:")
	public void setLoginRpm(UILabel loginRpm) { this.loginRpm = loginRpm; }

	@IBOutlet
	@Property
	@Selector("scanRpm")
	public UILabel getScanRpm() { return scanRpm; }

	@IBOutlet
	@Property
	@Selector("setScanRpm:")
	public void setScanRpm(UILabel scanRpm) { this.scanRpm = scanRpm; }

	@IBOutlet
	@Property
	@Selector("ivRpm")
	public UILabel getIvRpm() { return ivRpm; }

	@IBOutlet
	@Property
	@Selector("setIvRpm:")
	public void setIvRpm(UILabel ivRpm) { this.ivRpm = ivRpm; }

	@IBOutlet
	@Property
	@Selector("apiKey")
	public UITextField getApiKey() { return apiKey; }

	@IBOutlet
	@Property
	@Selector("setApiKey:")
	public void setApiKey(UITextField apiKey) { this.apiKey = apiKey; }

	@IBOutlet
	@Property
	@Selector("numAccounts")
	public UITextField getNumAccounts() { return numAccounts; }

	@IBOutlet
	@Property
	@Selector("setNumAccounts:")
	public void setNumAccounts(UITextField numAccounts) { this.numAccounts = numAccounts; }

	@IBOutlet
	@Property
	@Selector("numPokemon")
	public UITextField getNumPokemon() { return numPokemon; }

	@IBOutlet
	@Property
	@Selector("setNumPokemon:")
	public void setNumPokemon(UITextField numPokemon) { this.numPokemon = numPokemon; }

	@IBOutlet
	@Property
	@Selector("scanRadius")
	public UITextField getScanRadius() { return scanRadius; }

	@IBOutlet
	@Property
	@Selector("setScanRadius:")
	public void setScanRadius(UITextField scanRadius) { this.scanRadius = scanRadius; }
}
