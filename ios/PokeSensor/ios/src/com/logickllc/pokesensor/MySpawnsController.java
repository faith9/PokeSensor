package com.logickllc.pokesensor;


import com.logickllc.pokesensor.api.ExceptionCatchingRunnable;
import com.logickllc.pokesensor.api.ExceptionCatchingThreadFactory;
import com.logickllc.pokesensor.api.Spawn;

import org.robovm.apple.corelocation.CLGeocoder;
import org.robovm.apple.corelocation.CLLocation;
import org.robovm.apple.corelocation.CLPlacemark;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.uikit.UIBarButtonItem;
import org.robovm.apple.uikit.UIBarButtonSystemItem;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.apple.uikit.UITableViewDataSource;
import org.robovm.apple.uikit.UITableViewDelegate;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBOutlet;
import org.robovm.objc.block.VoidBlock2;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.logickllc.pokesensor.MapController.features;

@CustomClass("MySpawnsController")
public class MySpawnsController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {
	@IBOutlet
	SpawnTableView table;

	public static ThreadPoolExecutor pool;
	public static final int MAX_POOL_THREADS = 3;

	@Override
	public void viewDidLoad() {
		UIBarButtonItem addButton = new UIBarButtonItem(UIBarButtonSystemItem.Trash);
		addButton.setOnClickListener(new UIBarButtonItem.OnClickListener() {
			@Override
			public void onClick(UIBarButtonItem uiBarButtonItem) {
				Runnable positive = new Runnable() {
					@Override
					public void run() {
						MapController.mapHelper.deleteAllSpawns();
						if (table != null) {
							table.spawns.clear();

							Runnable runnable = new Runnable() {
								@Override
								public void run() {
									table.reloadData();
								}
							};
							features.runOnMainThread(runnable);
						}
					}
				};
				DialogHelper.yesNoBox("Delete All Spawns?", "Are you sure you want to delete all spawn point data?", positive, null).build().show();
			}
		});
		this.getNavigationItem().setRightBarButtonItem(addButton);

		for (final Spawn spawn : MapController.mapHelper.spawns.values()) {
			if (spawn.location.equals("Unknown")) {
				Runnable geocoderRunnable = new Runnable() {
					@Override
					public void run() {
						CLGeocoder geocoder = new CLGeocoder();
						geocoder.reverseGeocodeLocation(new CLLocation(spawn.lat, spawn.lon), new VoidBlock2() {

							@Override
							public void invoke(Object a, Object b) {
								try {
									if (a == null) return;
									NSArray placemarks = (NSArray) a;
									if (placemarks.size() > 0) {
										CLPlacemark placemark = (CLPlacemark) placemarks.get(0);
										spawn.location = placemark.getLocality() + ", " + placemark.getAdministrativeArea() + ", " + placemark.getISOcountryCode();
										features.print("PokeFinder", spawn.nickname + " is located at " + spawn.location);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

						});
					}
				};
				run(geocoderRunnable);
			}
		}

		table.setup(this);
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

	public synchronized Future run(Runnable runnable) {
		if (pool == null) {
			MapController.features.print("MySpawnsController", "Initializing a new thread pool");
			pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
			pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
		}
		Future future = pool.submit(new ExceptionCatchingRunnable(runnable));
		return future;
	}
}
