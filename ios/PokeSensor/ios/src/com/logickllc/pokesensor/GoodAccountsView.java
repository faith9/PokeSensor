package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("GoodAccountsView")
public class GoodAccountsView extends UIView {
	@IBOutlet
    UILabel goodAccountsLabel;

    public synchronized void setText(String text) {
        goodAccountsLabel.setText(text);
    }
}
