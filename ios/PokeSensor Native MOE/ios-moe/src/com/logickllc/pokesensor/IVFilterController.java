package com.logickllc.pokesensor;


import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBAction;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UISwitch;
import apple.uikit.UITextField;
import apple.uikit.UIViewController;
import apple.uikit.enums.UIControlEvents;

@Runtime(ObjCRuntime.class)
@ObjCClassName("IVFilterController")
@RegisterOnStartup
public class IVFilterController extends UIViewController {
    //@IBOutlet
    UITextField minAttack, minDefense, minStamina, minPercent, minOverride;

    //@IBOutlet
    UISwitch overrideSwitch;

    protected IVFilterController(Pointer peer) {
        super(peer);
    }

    @IBAction
    @Selector("override")
    public void override() {

    }

    @Override
    public void viewDidLoad() {
        super.viewDidLoad();

        minAttack.setText(MapController.mapHelper.getMinAttack() + "");
        minDefense.setText(MapController.mapHelper.getMinDefense() + "");
        minStamina.setText(MapController.mapHelper.getMinStamina() + "");
        minPercent.setText(MapController.mapHelper.getMinPercent() + "");
        minOverride.setText(MapController.mapHelper.getMinOverride() + "");

        MapController.addDoneButtonToKeyboard(minAttack);
        MapController.addDoneButtonToKeyboard(minDefense);
        MapController.addDoneButtonToKeyboard(minStamina);
        MapController.addDoneButtonToKeyboard(minPercent);
        MapController.addDoneButtonToKeyboard(minOverride);
    }

    @Override
    public void viewWillDisappear(boolean b) {
        super.viewWillDisappear(b);

        MapController.mapHelper.setMinAttack(validateIV(minAttack, MapController.mapHelper.getMinAttack()));
        MapController.mapHelper.setMinDefense(validateIV(minDefense, MapController.mapHelper.getMinDefense()));
        MapController.mapHelper.setMinStamina(validateIV(minStamina, MapController.mapHelper.getMinStamina()));
        MapController.mapHelper.setMinPercent(validatePercent(minPercent, MapController.mapHelper.getMinPercent()));
        MapController.mapHelper.setMinOverride(validatePercent(minOverride, MapController.mapHelper.getMinOverride()));
        MapController.mapHelper.overrideEnabled = overrideSwitch.isOn();

        MapController.mapHelper.saveIVFilters();
    }

    @Override
    public void viewWillAppear(boolean b) {
        super.viewWillAppear(b);

        overrideSwitch.setOn(MapController.mapHelper.overrideEnabled);

        togglePokemonOverride();

        overrideSwitch.addTargetActionForControlEvents(this, new SEL("overrideSwitched:"), UIControlEvents.ValueChanged);
    }

    @Selector("overrideSwitched:")
    void overrideSwitched(UISwitch sender) {
        togglePokemonOverride();
    }

    public int validatePercent(UITextField field, int backup) {
        int result;
        try {
            result = Integer.parseInt(field.text());
        } catch (Exception e) {
            result = backup;
        }

        if (result < 0) result = 0;
        if (result > 100) result = 100;

        return result;
    }

    public int validateIV(UITextField field, int backup) {
        int result;
        try {
            result = Integer.parseInt(field.text());
        } catch (Exception e) {
            result = backup;
        }

        if (result < 0) result = 0;
        if (result > 15) result = 15;

        return result;
    }

    public void togglePokemonOverride() {
        minOverride.setEnabled(overrideSwitch.isOn());
    }

    @IBOutlet
    @Property
    @Selector("minAttack")
    public UITextField getMinAttack() { return minAttack; }

    @IBOutlet
    @Property
    @Selector("setMinAttack:")
    public void setMinAttack(UITextField minAttack) { this.minAttack = minAttack; }

    @IBOutlet
    @Property
    @Selector("minDefense")
    public UITextField getMinDefense() { return minDefense; }

    @IBOutlet
    @Property
    @Selector("setMinDefense:")
    public void setMinDefense(UITextField minDefense) { this.minDefense = minDefense; }

    @IBOutlet
    @Property
    @Selector("minStamina")
    public UITextField getMinStamina() { return minStamina; }

    @IBOutlet
    @Property
    @Selector("setMinStamina:")
    public void setMinStamina(UITextField minStamina) { this.minStamina = minStamina; }

    @IBOutlet
    @Property
    @Selector("minPercent")
    public UITextField getMinPercent() { return minPercent; }

    @IBOutlet
    @Property
    @Selector("setMinPercent:")
    public void setMinPercent(UITextField minPercent) { this.minPercent = minPercent; }

    @IBOutlet
    @Property
    @Selector("minOverride")
    public UITextField getMinOverride() { return minOverride; }

    @IBOutlet
    @Property
    @Selector("setMinOverride:")
    public void setMinOverride(UITextField minOverride) { this.minOverride = minOverride; }

    @IBOutlet
    @Property
    @Selector("overrideSwitch")
    public UISwitch getOverrideSwitch() { return overrideSwitch; }

    @IBOutlet
    @Property
    @Selector("setOverrideSwitch:")
    public void setOverrideSwitch(UISwitch overrideSwitch) { this.overrideSwitch = overrideSwitch; }
}
