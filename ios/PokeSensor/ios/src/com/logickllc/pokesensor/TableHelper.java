package com.logickllc.pokesensor;

// Ported from answer at http://stackoverflow.com/questions/15746745/handling-an-empty-uitableview-print-a-friendly-message

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UITableViewCellSeparatorStyle;
import org.robovm.apple.uikit.UITableViewController;

public class TableHelper {
    public static void showEmptyTableMessage(UITableViewController tableController, String message) {
        UILabel messageLabel = new UILabel(new CGRect(0, 0, tableController.getView().getBounds().getWidth(), tableController.getView().getBounds().getWidth()));
        messageLabel.setText(message);
        messageLabel.setTextColor(UIColor.gray());
        messageLabel.setNumberOfLines(0);
        messageLabel.setTextAlignment(NSTextAlignment.Center);
        messageLabel.setFont(UIFont.getFont("TrebuchetMS", 15));
        messageLabel.sizeToFit();

        tableController.getTableView().setBackgroundView(messageLabel);
        tableController.getTableView().setSeparatorStyle(UITableViewCellSeparatorStyle.None);
    }
}
