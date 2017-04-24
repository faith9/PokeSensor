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
@ObjCClassName("RpmView")
@RegisterOnStartup
public class RpmView extends UIView {
    //@IBOutlet
    UILabel rpmLabel;

    protected RpmView(Pointer peer) {
        super(peer);
    }

    public synchronized void setText(String text) {
        rpmLabel.setText(text);
    }

    @IBOutlet
    @Property
    @Selector("rpmLabel")
    public UILabel getRpmLabel() { return rpmLabel; }

    @IBOutlet
    @Property
    @Selector("setRpmLabel:")
    public void setRpmLabel(UILabel rpmLabel) { this.rpmLabel = rpmLabel; }
}
