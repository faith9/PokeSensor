package com.logickllc.pokesensor;

import org.robovm.apple.corelocation.CLLocationCoordinate2D;
import org.robovm.apple.mapkit.MKCircle;
import org.robovm.apple.uikit.UIColor;

public class CustomCircle extends MKCircle {
	UIColor strokeColor = UIColor.black();
	UIColor fillColor = UIColor.clear();
	double lineWidth = 1;
	
	public CustomCircle(CLLocationCoordinate2D coord, double radius) {
		super(coord, radius);
	}
}
