package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UIImageView;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UISwitch;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("AccountTableCell")
public class AccountTableCell extends UITableViewCell {
	@IBOutlet
    public UILabel name, statusLabel;

    @IBOutlet
    public UIImageView status;
}
