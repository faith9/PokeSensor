package com.logickllc.pokesensor;


import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UIImageView;
import apple.uikit.UILabel;
import apple.uikit.UITableViewCell;

@Runtime(ObjCRuntime.class)
@ObjCClassName("AccountTableCell")
@RegisterOnStartup
public class AccountTableCell extends UITableViewCell {
	//@IBOutlet
    public UILabel name, statusLabel;

    //@IBOutlet
    public UIImageView status;

    protected AccountTableCell(Pointer peer) {
        super(peer);
    }

    @IBOutlet
    @Property
    @Selector("name")
    public UILabel getName() { return name; }

    @IBOutlet
    @Property
    @Selector("setName:")
    public void setName(UILabel name) { this.name = name; }

    @IBOutlet
    @Property
    @Selector("statusLabel")
    public UILabel getStatusLabel() { return statusLabel; }

    @IBOutlet
    @Property
    @Selector("setStatusLabel:")
    public void setStatusLabel(UILabel statusLabel) { this.statusLabel = statusLabel; }

    @IBOutlet
    @Property
    @Selector("status")
    public UIImageView getStatus() { return status; }

    @IBOutlet
    @Property
    @Selector("setStatus:")
    public void setStatus(UIImageView status) { this.status = status; }
}
