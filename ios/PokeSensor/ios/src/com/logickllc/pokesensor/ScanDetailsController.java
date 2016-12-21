package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UISlider;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.MapHelper;

@CustomClass("ScanDetailsController")
public class ScanDetailsController extends UIViewController {
	final String PREF_SCAN_DISTANCE = "ScanDistance";
    final String PREF_SCAN_TIME = "ScanTime";
    final String PREF_SCAN_SPEED = "ScanSpeed";

    final int DEFAULT_SCAN_DISTANCE = MapHelper.DEFAULT_SCAN_DISTANCE;
    final int DEFAULT_SCAN_SPEED = MapHelper.DEFAULT_SCAN_SPEED;

    final int DISTANCE_STEP = 10;
    final int SPEED_STEP = 1;

    int scanDistance, scanTime, scanSpeed;
	
	@IBOutlet
	UISlider seekDistance, seekSpeed;
	
	@IBOutlet
	UILabel distance, speed, time;

	@IBAction
	public void distanceChanged() {
		int progress = (int) seekDistance.getValue();
		if (progress * DISTANCE_STEP < 10) {
            seekDistance.setValue(10 / DISTANCE_STEP);
            return;
        }
        scanDistance = progress * DISTANCE_STEP;
        distance.setText(scanDistance + "m");

        String timeString = MapController.mapHelper.getTimeString(Math.round(getDistanceTraveled(scanDistance) / (float) scanSpeed)) + "s";
        time.setText(timeString.replace(":", "m"));
	}
	
	@IBAction
	public void speedChanged() {
		int progress = (int) seekSpeed.getValue();
		if (progress * SPEED_STEP < 1) {
            seekSpeed.setValue(1 / SPEED_STEP);
            return;
        }
        
		scanSpeed = progress * SPEED_STEP;
		int scanSpeedMeters = Math.round(scanSpeed * 3.6f);
        int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

        speed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");
        String timeString = MapController.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed) + "s";
        time.setText(timeString.replace(":", "m"));
	}

	@Override
	public void viewWillAppear(boolean animated) {
		IOSLauncher.navigationController.setNavigationBarHidden(false, true);
		this.getNavigationController().setToolbarHidden(true, true);
		Preferences prefs = Gdx.app.getPreferences(IOSFeatures.PREFS_NAME);
        scanDistance = prefs.getInteger(PREF_SCAN_DISTANCE, DEFAULT_SCAN_DISTANCE);
        scanSpeed = prefs.getInteger(PREF_SCAN_SPEED, DEFAULT_SCAN_SPEED);

        MapController.mapHelper.updateScanSettings();

        if (scanDistance > MapHelper.MAX_SCAN_DISTANCE) scanDistance = MapHelper.MAX_SCAN_DISTANCE;
        if (scanSpeed > MapHelper.maxScanSpeed) scanSpeed = (int) MapHelper.maxScanSpeed;

        seekDistance.setMaximumValue(MapHelper.MAX_SCAN_DISTANCE / DISTANCE_STEP);
        seekSpeed.setMaximumValue(MapHelper.maxScanSpeed / SPEED_STEP);

        seekDistance.setValue(scanDistance / DISTANCE_STEP);
        seekSpeed.setValue(scanSpeed / SPEED_STEP);
        distance.setText(scanDistance + "m");

        int scanSpeedMeters = Math.round(scanSpeed * 3.6f);
        int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

        speed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");
        String timeString = MapController.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed) + "s";
        time.setText(timeString.replace(":", "m"));
	}
	
	public int getDistanceTraveled(int radius) {
		final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
		final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
		int sectors = BOXES_PER_ROW * BOXES_PER_ROW;
		
		return MINI_SQUARE_SIZE * (sectors - 1);
	}

	@Override
	public void viewWillDisappear(boolean animated) {
		//IOSLauncher.navigationController.setNavigationBarHidden(true);
		Preferences prefs = Gdx.app.getPreferences(IOSFeatures.PREFS_NAME);
		prefs.putInteger(PREF_SCAN_DISTANCE, scanDistance);
		prefs.putInteger(PREF_SCAN_SPEED, scanSpeed);
		prefs.flush();
	}
	
	@Override
	public void willRotate(UIInterfaceOrientation arg0, double arg1) {
		super.willRotate(arg0, arg1);
		IOSLauncher.instance.hide();
	}

	@Override
	public void didRotate(UIInterfaceOrientation fromInterfaceOrientation) {
		super.didRotate(fromInterfaceOrientation);
		System.out.println("Did update to " + fromInterfaceOrientation);
		if (fromInterfaceOrientation == UIInterfaceOrientation.Portrait || fromInterfaceOrientation == UIInterfaceOrientation.PortraitUpsideDown) 
			IOSLauncher.instance.changeOrientation(false);
		else 
			IOSLauncher.instance.changeOrientation(true);
	}
}
