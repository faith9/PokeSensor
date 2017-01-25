package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("RpmCountView")
public class RpmCountView extends UIView {
    @IBOutlet
    UILabel rpmCountLabel;

    public synchronized void setText(String text) {
        rpmCountLabel.setText(text);
    }
}
