package com.logickllc.pokesensor;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.Owned;
import org.moe.natj.objc.ann.Selector;

import apple.mapkit.MKAnnotationView;
import apple.mapkit.MKPointAnnotation;
import apple.uikit.UIControl;
import apple.uikit.UILabel;

public class ImageAnnotation extends MKPointAnnotation {
	public String imagePath;
	public MKAnnotationView view = null;
	public UIControl control;
	public UILabel callout;
	public String ivs = "";
	public String moves = "";
	public String heightWeight = "";
	public boolean isCustom = false;
	public String name;
	public int pokedexNumber;

	protected ImageAnnotation(Pointer peer) {
		super(peer);
	}

	@Owned
	@Selector("alloc")
	public static native ImageAnnotation alloc();

	public ImageAnnotation init(String imagePath) {
		super.init();
		this.imagePath = imagePath;
		return this;
	}

	public void initCallout() {
		callout = UILabel.alloc().init();
		view.setDetailCalloutAccessoryView(callout);
	}
	
	public ImageAnnotation initByCopy(ImageAnnotation copy) {
		super.init();
		this.imagePath = copy.imagePath;
		this.view = copy.view;
		this.control = copy.control;
		return this;
	}
}
