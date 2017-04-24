package com.logickllc.pokesensor;

import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.NInt;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCClassName;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import apple.foundation.NSIndexPath;
import apple.uikit.UIImage;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.UITableViewController;
import apple.uikit.UIView;
import apple.uikit.enums.UITableViewCellEditingStyle;
import apple.uikit.protocol.UITableViewDataSource;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@Runtime(ObjCRuntime.class)
@ObjCClassName("AccountTableView")
@RegisterOnStartup
public class AccountTableView extends UITableView {
    Hashtable<String, UIImage> images = new Hashtable<String, UIImage>();
    UITableViewController controller;
    UIView ogBackground;
    @NInt long ogSeparatorStyle;

    protected AccountTableView(Pointer peer) {
        super(peer);
    }

    public void setup(UITableViewController myController) {
        this.controller = myController;
        ogBackground = this.backgroundView();
        ogSeparatorStyle = this.separatorStyle();
        setDataSource(new UITableViewDataSource() {
            @Override
            public UITableViewCell tableViewCellForRowAtIndexPath(UITableView table, NSIndexPath path) {
                System.out.println("getCellForRow " + path.row());
                int position = (int) path.row();

                AccountTableCell cell = (AccountTableCell) table.dequeueReusableCellWithIdentifierForIndexPath("cell", path);

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

                    case WRONG_NAME_OR_PASSWORD:
                        cell.status.setImage(getImageByName("status_error"));
                        break;

                    case NEEDS_EMAIL_VERIFICATION:
                        cell.status.setImage(getImageByName("status_warning"));
                        break;

                    case LOGGING_IN:
                        cell.status.setImage(UIImage.alloc().init());
                        break;

                    case SOLVING_CAPTCHA:
                        cell.status.setImage(UIImage.alloc().init());
                        break;

                    default:
                        cell.status.setImage(getImageByName("status_error"));
                        break;
                }

                return cell;
            }

            @Override
            public long numberOfSectionsInTableView(UITableView tableView) {
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
            public long tableViewNumberOfRowsInSection(UITableView tableView, @NInt long section) {
                return accounts.size();
            }

            @Override
            public boolean tableViewCanEditRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
                return true;
            }

            @Override
            public void tableViewCommitEditingStyleForRowAtIndexPath(UITableView tableView, @NInt long editingStyle, NSIndexPath indexPath) {
                if (editingStyle == UITableViewCellEditingStyle.Delete) {
                    AccountManager.removeAccount((int) indexPath.row());
                    tableView.reloadData();
                }
            }
        });
    }

    public UIImage getImageByName(String name) {
        try {
            String filename = name + MapController.mapHelper.IMAGE_EXTENSION;
            if (!images.containsKey(name)) images.put(name, UIImage.imageNamed(filename));

            return images.get(name);
        } catch (Exception e) {
            e.printStackTrace();

            ConcurrentHashMap<String, String> extras = new ConcurrentHashMap<>();
            extras.put("Fatal", "false");
            ErrorReporter.logExceptionThreaded(e, extras);
        }

        return UIImage.alloc().init();
    }
}
