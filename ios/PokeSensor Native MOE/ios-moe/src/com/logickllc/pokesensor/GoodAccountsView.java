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
@ObjCClassName("GoodAccountsView")
@RegisterOnStartup
public class GoodAccountsView extends UIView {
	//@IBOutlet
    UILabel goodAccountsLabel;

    protected GoodAccountsView(Pointer peer) {
        super(peer);
    }

    public synchronized void setText(String text) {
        goodAccountsLabel.setText(text);
    }

    @IBOutlet
    @Property
    @Selector("goodAccountsLabel")
    public UILabel getGoodAccountsLabel() { return goodAccountsLabel; }

    @IBOutlet
    @Property
    @Selector("setGoodAccountsLabel:")
    public void setGoodAccountsLabel(UILabel goodAccountsLabel) { this.goodAccountsLabel = goodAccountsLabel; }
}
