package com.logickllc.pokesensor;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBAction;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UIImageView;
import apple.uikit.UILabel;
import apple.uikit.UISwitch;
import apple.uikit.UITableViewCell;

@Runtime(ObjCRuntime.class)
@ObjCClassName("FilterTableCell")
@RegisterOnStartup
public class FilterTableCell extends UITableViewCell {
    public int pokedexNumber;

	//@IBOutlet
    public UILabel name;

    //@IBOutlet
    public UIImageView pic;

    //@IBOutlet
    public UISwitch toggle;

    protected FilterTableCell(Pointer peer) {
        super(peer);
    }

    @IBAction
    @Selector("toggleFilter")
    public void toggleFilter() {
        MapController.features.filter.put(pokedexNumber, toggle.isOn());
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
    @Selector("pic")
    public UIImageView getPic() { return pic; }

    @IBOutlet
    @Property
    @Selector("setPic:")
    public void setPic(UIImageView pic) { this.pic = pic; }

    @IBOutlet
    @Property
    @Selector("toggle")
    public UISwitch getToggle() { return toggle; }

    @IBOutlet
    @Property
    @Selector("setToggle:")
    public void setToggle(UISwitch toggle) { this.toggle = toggle; }
}
