package com.logickllc.pokesensor;


import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UILabel;
import apple.uikit.UIView;

@Runtime(ObjCRuntime.class)
@ObjCClassName("RpmCountView")
@RegisterOnStartup
public class RpmCountView extends UIView {
    //@IBOutlet
    UILabel rpmCountLabel;

    protected RpmCountView(Pointer peer) {
        super(peer);
    }

    public synchronized void setText(String text) {
        rpmCountLabel.setText(text);
    }

    @IBOutlet
    @Property
    @Selector("rpmCountLabel")
    public UILabel getRpmCountLabel() { return rpmCountLabel; }

    @IBOutlet
    @Property
    @Selector("setRpmCountLabel:")
    public void setRpmCountLabel(UILabel rpmCountLabel) { this.rpmCountLabel = rpmCountLabel; }
}
