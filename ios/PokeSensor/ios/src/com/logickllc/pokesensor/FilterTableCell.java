package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIImageView;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UISwitch;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("FilterTableCell")
public class FilterTableCell extends UITableViewCell {
    public int pokedexNumber;

	@IBOutlet
    public UILabel name;

    @IBOutlet
    public UIImageView pic;

    @IBOutlet
    public UISwitch toggle;

    @IBAction
    public void toggleFilter() {
        MapController.features.filter.put(pokedexNumber, toggle.isOn());
    }
}
