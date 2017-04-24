package com.logickllc.pokesensor;


import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.Owned;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Selector;

import apple.corelocation.struct.CLLocationCoordinate2D;
import apple.mapkit.MKCircle;
import apple.mapkit.MKCircleRenderer;
import apple.mapkit.MKShape;
import apple.mapkit.protocol.MKOverlay;
import apple.mapkit.struct.MKMapRect;
import apple.uikit.UIColor;

@Runtime(ObjCRuntime.class)
@ObjCClassName("CustomCircle")
@RegisterOnStartup
public class CustomCircle extends MKShape implements MKOverlay {
	UIColor strokeColor = UIColor.blackColor();
	UIColor fillColor = UIColor.clearColor();
	double lineWidth = 1;
	MKCircle circle;
	MKCircleRenderer renderer = null;

	protected CustomCircle(Pointer peer) {
		super(peer);
	}

	@Owned
	@Selector("alloc")
	public static native CustomCircle alloc();

	@Owned
	@Selector("init")
	public native CustomCircle init();

	public CustomCircle initWithCenterCoordinateRadius(CLLocationCoordinate2D coord, double radius) {
		init();
		circle = MKCircle.circleWithCenterCoordinateRadius(coord, radius);
		return this;
	}

	@Override
	public MKMapRect boundingMapRect() {
		return circle.boundingMapRect();
	}
}
