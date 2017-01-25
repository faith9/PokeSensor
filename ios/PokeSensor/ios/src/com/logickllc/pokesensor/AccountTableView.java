package com.logickllc.pokesensor;

import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewCellEditingStyle;
import org.robovm.apple.uikit.UITableViewCellSeparatorStyle;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.apple.uikit.UITableViewDataSourceAdapter;
import org.robovm.apple.uikit.UIView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.rt.bro.annotation.MachineSizedSInt;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@CustomClass("AccountTableView")
public class AccountTableView extends UITableView {
    Hashtable<String, UIImage> images = new Hashtable<String, UIImage>();
    UITableViewController controller;
    UIView ogBackground;
    UITableViewCellSeparatorStyle ogSeparatorStyle;

    public void setup(UITableViewController myController) {
        this.controller = myController;
        ogBackground = this.getBackgroundView();
        ogSeparatorStyle = this.getSeparatorStyle();
        setDataSource(new UITableViewDataSourceAdapter() {
            @Override
            public UITableViewCell getCellForRow(UITableView table, final NSIndexPath path) {
                System.out.println("getCellForRow " + path.getRow());
                int position = path.getRow();

                AccountTableCell cell = (AccountTableCell) table.dequeueReusableCell("cell", path);

                Account account = accounts.get(position);
                cell.name.setText(account.getUsername());

                cell.statusLabel.setText(account.getStatus().name().replaceAll("_"," "));

                switch (account.getStatus()) {
                    case GOOD:
                        cell.status.setImage(getImageByName("status_good"));
                        break;

                    case CAPTCHA_REQUIRED:
                        cell.status.setImage(getImageByName("status_warning"));
                        break;

                    case BANNED:
                        cell.status.setImage(getImageByName("status_banned"));
                        break;

                    case INVALID_CREDENTIALS:
                        cell.status.setImage(getImageByName("status_error"));
                        break;

                    case NEEDS_EMAIL_VERIFICATION:
                        cell.status.setImage(getImageByName("status_warning"));
                        break;

                    case LOGGING_IN:
                        cell.status.setImage(new UIImage());
                        break;

                    case SOLVING_CAPTCHA:
                        cell.status.setImage(new UIImage());
                        break;

                    default:
                        cell.status.setImage(getImageByName("status_error"));
                        break;
                }

                return cell;
            }

            @Override
            public long getNumberOfSections(UITableView tableView) {
                if (accounts != null && accounts.size() > 0) {
                    tableView.setBackgroundView(ogBackground);
                    tableView.setSeparatorStyle(ogSeparatorStyle);
                    return 1;
                }
                else {
                    TableHelper.showEmptyTableMessage(controller, "You don't have any accounts!\nClick + to add an account.");
                    return 0;
                }
            }

            @Override
            public long getNumberOfRowsInSection(UITableView tableView, @MachineSizedSInt long section) {
                return accounts.size();
            }

            @Override
            public boolean canEditRow(UITableView uiTableView, NSIndexPath nsIndexPath) {
                return true;
            }

            @Override
            public void commitEditingStyleForRow(UITableView uiTableView, UITableViewCellEditingStyle uiTableViewCellEditingStyle, NSIndexPath nsIndexPath) {
                if (uiTableViewCellEditingStyle == UITableViewCellEditingStyle.Delete) {
                    AccountManager.removeAccount(nsIndexPath.getRow());
                    uiTableView.reloadData();
                }
            }
        });
    }

    public UIImage getImageByName(String name) {
        try {
            String filename = name + MapController.mapHelper.IMAGE_EXTENSION;
            if (!images.containsKey(name)) images.put(name, UIImage.create(filename));

            return images.get(name);
        } catch (Exception e) {
            e.printStackTrace();

            ConcurrentHashMap<String, String> extras = new ConcurrentHashMap<>();
            extras.put("Fatal", "false");
            ErrorReporter.logExceptionThreaded(e, extras);
        }

        return new UIImage();
    }
}
