package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("RpmView")
public class RpmView extends UIView {
    @IBOutlet
    UILabel rpmLabel;

    public synchronized void setText(String text) {
        rpmLabel.setText(text);
    }
}
