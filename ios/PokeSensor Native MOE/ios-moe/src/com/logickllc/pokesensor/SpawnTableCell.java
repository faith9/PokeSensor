package com.logickllc.pokesensor;


import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UIButton;
import apple.uikit.UIImageView;
import apple.uikit.UILabel;
import apple.uikit.UITableViewCell;

@Runtime(ObjCRuntime.class)
@ObjCClassName("SpawnTableCell")
@RegisterOnStartup
public class SpawnTableCell extends UITableViewCell {
	//@IBOutlet
    public UILabel name, location;

    //@IBOutlet
    public UIImageView history, history1, history2, history3;

    //@IBOutlet
    public UIButton move;

    //public UIControl.OnTouchUpInsideListener listener;

    protected SpawnTableCell(Pointer peer) {
        super(peer);
    }

    // TODO Add IBOutlets
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
    @Selector("location")
    public UILabel getLocation() { return location; }

    @IBOutlet
    @Property
    @Selector("setLocation:")
    public void setLocation(UILabel location) { this.location = location; }

    @IBOutlet
    @Property
    @Selector("history")
    public UIImageView getHistory() { return history; }

    @IBOutlet
    @Property
    @Selector("setHistory:")
    public void setHistory(UIImageView history) { this.history = history; }

    @IBOutlet
    @Property
    @Selector("history1")
    public UIImageView getHistory1() { return history1; }

    @IBOutlet
    @Property
    @Selector("setHistory1:")
    public void setHistory1(UIImageView history1) { this.history1 = history1; }

    @IBOutlet
    @Property
    @Selector("history2")
    public UIImageView getHistory2() { return history2; }

    @IBOutlet
    @Property
    @Selector("setHistory2:")
    public void setHistory2(UIImageView history2) { this.history2 = history2; }

    @IBOutlet
    @Property
    @Selector("history3")
    public UIImageView getHistory3() { return history3; }

    @IBOutlet
    @Property
    @Selector("setHistory3:")
    public void setHistory3(UIImageView history3) { this.history3 = history3; }

    @IBOutlet
    @Property
    @Selector("move")
    public UIButton getMove() { return move; }

    @IBOutlet
    @Property
    @Selector("setMove:")
    public void setMove(UIButton move) { this.move = move; }
}
