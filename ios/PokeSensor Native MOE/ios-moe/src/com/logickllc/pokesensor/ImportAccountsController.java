package com.logickllc.pokesensor;


import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBAction;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UIColor;
import apple.uikit.UITextView;
import apple.uikit.UIViewController;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@Runtime(ObjCRuntime.class)
@ObjCClassName("ImportAccountsController")
@RegisterOnStartup
public class ImportAccountsController extends UIViewController {
    //@IBOutlet
    UITextView importText;

    protected ImportAccountsController(Pointer peer) {
        super(peer);
    }

    @IBAction
    @Selector("importAccounts")
    public void importAccounts() {
        String csvText = importText.text().trim();

        if (csvText.equals("")) {
            DialogHelper.messageBox("Um...What?", "You have to type at least 1 account/password combo before you can import...").build().show();
            return;
        }

        try {
            if (!csvText.equals("")) {
                CSVParser parser;
                if (csvText.contains(",")) parser = CSVParser.parse(csvText, CSVFormat.DEFAULT.withRecordSeparator("\n"));
                else parser = CSVParser.parse(csvText, CSVFormat.DEFAULT.withRecordSeparator("\n").withDelimiter(' '));
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
        } catch (Throwable e) {
            e.printStackTrace();
        }
        this.navigationController().popViewControllerAnimated(true);
        AccountsController.reloadData();
    }

    @Override
    public void viewDidLoad() {
        super.viewDidLoad();

        importText.layer().setBorderColor(UIColor.blackColor().CGColor());
        importText.layer().setBorderWidth(1);
        MapController.addDoneButtonToKeyboard(importText);
    }

    @Override
    public void viewWillDisappear(boolean b) {
        super.viewWillDisappear(b);
    }

    @IBOutlet
    @Property
    @Selector("importText")
    public UITextView getImportText() { return importText; }

    @IBOutlet
    @Property
    @Selector("setImportText:")
    public void setImportText(UITextView importText) { this.importText = importText; }
}
