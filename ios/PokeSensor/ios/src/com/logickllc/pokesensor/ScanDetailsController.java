package com.logickllc.pokesensor;

import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UISlider;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.MapHelper;

@CustomClass("ScanDetailsController")
public class ScanDetailsController extends UIViewController {
	final String PREF_SCAN_DISTANCE = "ScanDistance";
    final String PREF_SCAN_TIME = "ScanTime";
    final String PREF_SCAN_SPEED = "ScanSpeed";
	final String PREF_COLLECT_SPAWNS = "CollectSpawns";
	final String PREF_SHOW_IVS = "ShowIvs";

    final int DEFAULT_SCAN_DISTANCE = MapHelper.DEFAULT_SCAN_DISTANCE;
    final int DEFAULT_SCAN_SPEED = MapHelper.DEFAULT_SCAN_SPEED;

    final int DISTANCE_STEP = 10;
    final int SPEED_STEP = 1;

    int scanDistance, scanTime, scanSpeed, timeFactor = 1;
	
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

		int oldMaxSpeed = MapHelper.maxScanSpeed;
        MapHelper.getSpeed(scanDistance);
		seekSpeed.setMaximumValue(MapHelper.maxScanSpeed / SPEED_STEP);

		if (oldMaxSpeed != MapHelper.maxScanSpeed) {
			if (scanSpeed == oldMaxSpeed) {
				scanSpeed = MapHelper.maxScanSpeed;
				seekSpeed.setValue(scanSpeed);
			}
		}
		
		int scanSpeedMeters = Math.round(scanSpeed * 3.6f);
        int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

        speed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");

        String timeString = MapController.mapHelper.getTimeString(Math.round(getDistanceTraveled(scanDistance) / (float) scanSpeed) / timeFactor) + "s";
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
        String timeString = MapController.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed / timeFactor) + "s";
        time.setText(timeString.replace(":", "m"));
	}

	@Override
	public void viewWillAppear(boolean animated) {
		IOSLauncher.navigationController.setNavigationBarHidden(false, true);
		this.getNavigationController().setToolbarHidden(true, true);

		NativePreferences.lock();
        scanDistance = NativePreferences.getInteger(PREF_SCAN_DISTANCE, DEFAULT_SCAN_DISTANCE);
        scanSpeed = NativePreferences.getInteger(PREF_SCAN_SPEED, DEFAULT_SCAN_SPEED);
		NativePreferences.unlock();

        MapController.mapHelper.updateScanSettings();
        
        timeFactor = AccountManager.getGoodAccounts().size();
		if (timeFactor == 0) timeFactor = 1;

        if (scanDistance > MapHelper.MAX_SCAN_DISTANCE) scanDistance = MapHelper.MAX_SCAN_DISTANCE;
        
        MapHelper.getSpeed(scanDistance);
        
        if (scanSpeed > MapHelper.maxScanSpeed) scanSpeed = (int) MapHelper.maxScanSpeed;

        seekDistance.setMaximumValue(MapHelper.MAX_SCAN_DISTANCE / DISTANCE_STEP);
        seekSpeed.setMaximumValue(MapHelper.maxScanSpeed / SPEED_STEP);

        seekDistance.setValue(scanDistance / DISTANCE_STEP);
        seekSpeed.setValue(scanSpeed / SPEED_STEP);
        distance.setText(scanDistance + "m");

        int scanSpeedMeters = Math.round(scanSpeed * 3.6f);
        int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

        speed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");
        String timeString = MapController.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed / timeFactor) + "s";
        time.setText(timeString.replace(":", "m"));
	}

	@IBAction
	public void close() {
		MapController.features.print("ScanDetails","Pressed close button");
		seekDistance.setValue(seekDistance.getValue() - 1, true);
		distanceChanged();
	}

	@IBAction
	public void far() {
		MapController.features.print("ScanDetails","Pressed far button");
		seekDistance.setValue(seekDistance.getValue() + 1, true);
		distanceChanged();
	}

	@IBAction
	public void slow() {
		MapController.features.print("ScanDetails","Pressed slow button");
		seekSpeed.setValue(seekSpeed.getValue() - 1, true);
		speedChanged();
	}

	@IBAction
	public void fast() {
		MapController.features.print("ScanDetails","Pressed fast button");
		seekSpeed.setValue(seekSpeed.getValue() + 1, true);
		speedChanged();
	}
	
	public int getDistanceTraveled(int radius) {
		final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MapHelper.MAX_SCAN_RADIUS);
		final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
		final float ITERATIONS = MapHelper.MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

		int hexSectors = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);
		int hexDist = (int) (HEX_DISTANCE * (hexSectors - 1));
		
		final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
		final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
		int sectors = BOXES_PER_ROW * BOXES_PER_ROW;
		
		int squareSectors = sectors;
		int squareDist = MINI_SQUARE_SIZE * (squareSectors - 1);

		double squareSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(MINI_SQUARE_SIZE / MapHelper.minScanTime, (double) scanSpeed));
		double hexSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(HEX_DISTANCE / MapHelper.minScanTime, (double) scanSpeed));

		if (hexSectors * hexSpeed <= squareSectors * squareSpeed) return hexDist;
		else return squareDist;
	}

	@Override
	public void viewWillDisappear(boolean animated) {
		//IOSLauncher.navigationController.setNavigationBarHidden(true);
		NativePreferences.lock();
		NativePreferences.putInteger(PREF_SCAN_DISTANCE, scanDistance);
		NativePreferences.putInteger(PREF_SCAN_SPEED, scanSpeed);
		NativePreferences.unlock();
		
		MapController.mapHelper.refreshTempScanCircle();
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
		if (fromInterfaceOrientation == UIInterfaceOrientation.Portrait || fromInterfaceOrientation == UIInterfaceOrientation.PortraitUpsideDown) 
			IOSLauncher.instance.changeOrientation(false);
		else 
			IOSLauncher.instance.changeOrientation(true);
	}
}
