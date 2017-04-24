package com.logickllc.pokesensor;


import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.MapHelper;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.IBAction;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import apple.uikit.UILabel;
import apple.uikit.UISlider;
import apple.uikit.UIViewController;

@Runtime(ObjCRuntime.class)
@ObjCClassName("ScanDetailsController")
@RegisterOnStartup
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
	
	//@IBOutlet
	UISlider seekDistance, seekSpeed;
	
	//@IBOutlet
	UILabel distance, speed, time;

	protected ScanDetailsController(Pointer peer) {
		super(peer);
	}

	@IBAction
	@Selector("distanceChanged")
	public void distanceChanged() {
		int progress = (int) seekDistance.value();
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
	@Selector("speedChanged")
	public void speedChanged() {
		int progress = (int) seekSpeed.value();
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
		IOSLauncher.navigationController.setNavigationBarHiddenAnimated(false, true);
		this.navigationController().setToolbarHiddenAnimated(true, true);

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
	@Selector("close")
	public void close() {
		MapController.features.print("ScanDetails","Pressed close button");
		seekDistance.setValueAnimated(seekDistance.value() - 1, true);
		distanceChanged();
	}

	@IBAction
	@Selector("far")
	public void far() {
		MapController.features.print("ScanDetails","Pressed far button");
		seekDistance.setValueAnimated(seekDistance.value() + 1, true);
		distanceChanged();
	}

	@IBAction
	@Selector("slow")
	public void slow() {
		MapController.features.print("ScanDetails","Pressed slow button");
		seekSpeed.setValueAnimated(seekSpeed.value() - 1, true);
		speedChanged();
	}

	@IBAction
	@Selector("fast")
	public void fast() {
		MapController.features.print("ScanDetails","Pressed fast button");
		seekSpeed.setValueAnimated(seekSpeed.value() + 1, true);
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

	@IBOutlet
	@Property
	@Selector("seekDistance")
	public UISlider getSeekDistance() { return seekDistance; }

	@IBOutlet
	@Property
	@Selector("setSeekDistance:")
	public void setSeekDistance(UISlider seekDistance) { this.seekDistance = seekDistance; }

	@IBOutlet
	@Property
	@Selector("seekSpeed")
	public UISlider getSeekSpeed() { return seekSpeed; }

	@IBOutlet
	@Property
	@Selector("setSeekSpeed:")
	public void setSeekSpeed(UISlider seekSpeed) { this.seekSpeed = seekSpeed; }

	@IBOutlet
	@Property
	@Selector("distance")
	public UILabel getDistance() { return distance; }

	@IBOutlet
	@Property
	@Selector("setDistance:")
	public void setDistance(UILabel distance) { this.distance = distance; }

	@IBOutlet
	@Property
	@Selector("speed")
	public UILabel getSpeed() { return speed; }

	@IBOutlet
	@Property
	@Selector("setSpeed:")
	public void setSpeed(UILabel speed) { this.speed = speed; }

	@IBOutlet
	@Property
	@Selector("time")
	public UILabel getTime() { return time; }

	@IBOutlet
	@Property
	@Selector("setTime:")
	public void setTime(UILabel time) { this.time = time; }
}
