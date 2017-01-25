package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIImageView;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("SpawnTableCell")
public class SpawnTableCell extends UITableViewCell {
	@IBOutlet
    public UILabel name, location;

    @IBOutlet
    public UIImageView history, history1, history2, history3;

    @IBOutlet
    public UIButton move;

    public UIControl.OnTouchUpInsideListener listener;
}
