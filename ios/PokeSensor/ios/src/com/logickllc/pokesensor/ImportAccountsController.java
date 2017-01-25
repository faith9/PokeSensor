package com.logickllc.pokesensor;


import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UITextField;
import org.robovm.apple.uikit.UITextView;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@CustomClass("ImportAccountsController")
public class ImportAccountsController extends UIViewController {
    @IBOutlet
    UITextView importText;

    @IBAction
    public void importAccounts() {
        String csvText = importText.getText().trim();

        if (csvText.equals("")) {
            DialogHelper.messageBox("Um...What?", "You have to type at least 1 account/password combo before you can import...").build().show();
            return;
        }

        try {
            if (!csvText.equals("")) {
                CSVParser parser = CSVParser.parse(csvText, CSVFormat.DEFAULT.withRecordSeparator("\n"));
                for (CSVRecord record : parser) {
                    try {
                        String username = record.get(0).trim();
                        String password = record.get(1).trim();

                        if (username.equals("") || password.equals("")) continue;

                        // Adding a new account
                        Account newAccount = new Account(username, password, AccountManager.incNumAccounts());

                        boolean dupe = false;

                        for (int n = 0; n < accounts.size(); n++) {
                            Account tempAccount = accounts.get(n);
                            if (tempAccount.getUsername().equals(newAccount.getUsername())) {
                                AccountManager.decNumAccounts();
                                dupe = true;
                                break;
                            }
                        }

                        if (dupe) continue;

                        newAccount.login();
                        accounts.add(newAccount);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.getNavigationController().popViewController(true);
        AccountsController.reloadData();
    }

    @Override
    public void viewDidLoad() {
        super.viewDidLoad();

        importText.getLayer().setBorderColor(UIColor.black().getCGColor());
        importText.getLayer().setBorderWidth(1);
    }

    @Override
    public void viewWillDisappear(boolean b) {
        super.viewWillDisappear(b);
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
}
