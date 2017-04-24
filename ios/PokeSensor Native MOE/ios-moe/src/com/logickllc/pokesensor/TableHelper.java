package com.logickllc.pokesensor;

// Ported from answer at http://stackoverflow.com/questions/15746745/handling-an-empty-uitableview-print-a-friendly-message


import apple.coregraphics.struct.CGPoint;
import apple.coregraphics.struct.CGRect;
import apple.coregraphics.struct.CGSize;
import apple.uikit.UIColor;
import apple.uikit.UIFont;
import apple.uikit.UILabel;
import apple.uikit.UITableViewController;
import apple.uikit.enums.NSTextAlignment;
import apple.uikit.enums.UITableViewCellSeparatorStyle;

public class TableHelper {
    public static void showEmptyTableMessage(UITableViewController tableController, String message) {
        UILabel messageLabel = UILabel.alloc().initWithFrame(new CGRect(new CGPoint(0, 0), new CGSize(tableController.view().bounds().size().width(), tableController.view().bounds().size().height())));
        messageLabel.setText(message);
        messageLabel.setTextColor(UIColor.grayColor());
        messageLabel.setNumberOfLines(0);
        messageLabel.setTextAlignment(NSTextAlignment.Center);
        messageLabel.setFont(UIFont.fontWithNameSize("TrebuchetMS", 15));
        messageLabel.sizeToFit();

        tableController.tableView().setBackgroundView(messageLabel);
        tableController.tableView().setSeparatorStyle(UITableViewCellSeparatorStyle.None);
    }
}
