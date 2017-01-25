package com.logickllc.pokesensor;


import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UISwitch;
import org.robovm.apple.uikit.UITextField;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("IVFilterController")
public class IVFilterController extends UIViewController {
    @IBOutlet
    UITextField minAttack, minDefense, minStamina, minPercent, minOverride;

    @IBOutlet
    UISwitch overrideSwitch;

    @IBAction
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

        overrideSwitch.addOnValueChangedListener(new UIControl.OnValueChangedListener() {
            @Override
            public void onValueChanged(UIControl uiControl) {
                togglePokemonOverride();
            }
        });
    }

    public int validatePercent(UITextField field, int backup) {
        int result;
        try {
            result = Integer.parseInt(field.getText());
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
            result = Integer.parseInt(field.getText());
        } catch (Exception e) {
            result = backup;
        }

        if (result < 0) result = 0;
        if (result > 15) result = 15;

        return result;
    }

    @Override
    public void willRotate(UIInterfaceOrientation arg0, double arg1) {
        super.willRotate(arg0, arg1);
        IOSLauncher.instance.hideBanner();
    }

    @Override
    public void didRotate(UIInterfaceOrientation fromInterfaceOrientation) {
        super.didRotate(fromInterfaceOrientation);
        System.out.println("Did update to " + fromInterfaceOrientation);
        if (fromInterfaceOrientation == UIInterfaceOrientation.Portrait
                || fromInterfaceOrientation == UIInterfaceOrientation.PortraitUpsideDown)
            IOSLauncher.instance.changeOrientation(false);
        else
            IOSLauncher.instance.changeOrientation(true);
    }

    public void togglePokemonOverride() {
        minOverride.setEnabled(overrideSwitch.isOn());
    }
}
