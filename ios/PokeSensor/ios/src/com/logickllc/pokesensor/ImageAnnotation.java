package com.logickllc.pokesensor;

import org.robovm.apple.mapkit.MKAnnotationView;
import org.robovm.apple.mapkit.MKPointAnnotation;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UILabel;

public class ImageAnnotation extends MKPointAnnotation {
	public String imagePath;
	public MKAnnotationView view;
	public UIControl control;
	public UILabel callout;
	public String ivs = "";
	public boolean isCustom = false;
	public String name;
	public int pokedexNumber;
	
	public ImageAnnotation(String imagePath) {
		super();
		this.imagePath = imagePath;
	}
	
	public void initCallout() {
		callout = new UILabel();
		view.setDetailCalloutAccessoryView(callout);
	}
	
	public ImageAnnotation(ImageAnnotation copy) {
		super();
		this.imagePath = copy.imagePath;
		this.view = copy.view;
		this.control = copy.control;
		//this.setTitle(copy.getTitle());
		//this.setSubtitle(copy.getSubtitle());
	}
}
