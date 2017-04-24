package com.logickllc.pokesensor;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.logickllc.pokesensor.api.ExceptionCatchingRunnable;
import com.logickllc.pokesensor.api.ExceptionCatchingThreadFactory;
import com.logickllc.pokesensor.api.Spawn;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import apple.foundation.NSArray;
import apple.foundation.NSString;
import apple.uikit.UIActivityViewController;
import apple.uikit.UIBarButtonItem;
import apple.uikit.UIImage;
import apple.uikit.UITableViewController;
import apple.uikit.UIViewController;
import apple.uikit.enums.UIBarButtonItemStyle;
import apple.uikit.enums.UIBarButtonSystemItem;
import apple.uikit.protocol.UITableViewDataSource;
import apple.uikit.protocol.UITableViewDelegate;

import static com.logickllc.pokesensor.MapController.features;

@Runtime(ObjCRuntime.class)
@ObjCClassName("MySpawnsController")
@RegisterOnStartup
public class MySpawnsController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {
	//@IBOutlet
    SpawnTableView table;

	public static ThreadPoolExecutor pool;
	public static final int MAX_POOL_THREADS = 3;
	private UIBarButtonItem exportButton;

	protected MySpawnsController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewDidLoad() {
		UIBarButtonItem deleteButton = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.Trash, this, new SEL("deleteClicked:"));
		this.navigationItem().setRightBarButtonItem(deleteButton);

		UIBarButtonItem importButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("import.png"), UIBarButtonItemStyle.Plain, this, new SEL("importClicked:"));
		UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		exportButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("export.png"), UIBarButtonItemStyle.Plain, this, new SEL("exportClicked:"));
		//UIBarButtonItem flexButton3 = new UIBarButtonItem(UIBarButtonSystemItem.FlexibleSpace);
		//UIBarButtonItem settingsButton = new UIBarButtonItem(UIImage.create("settings.png"), UIBarButtonItemStyle.Plain);

		//NSArray<UIBarButtonItem> items = customToolbar.getItems();
		NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(importButton, flexButton1, exportButton, null);

		final UIViewController me = this;

		this.setToolbarItems(items);

		this.navigationController().setToolbarHidden(false);

		for (final Spawn spawn : MapController.mapHelper.spawns.values()) {
			if (spawn.location.equals("Unknown")) {
				/*Runnable geocoderRunnable = new Runnable() {
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
				run(geocoderRunnable);*/
			}
		}

		table.setup(this);
	}

	@Selector("deleteClicked:")
	void deleteClicked(UIBarButtonItem sender) {
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

	@Selector("importClicked:")
	void importClicked(UIBarButtonItem sender) {
		importSpawns();
	}

	@Selector("exportClicked:")
	void exportClicked(UIBarButtonItem sender) {
		exportSpawns();
	}

	public void importSpawns() {
		Lambda positive = new Lambda() {
			@Override
			public void execute() {
				String csvText = (String) params.get("Text");

				if (csvText.equals("")) {
					features.longMessage("Um...that can't be blank");
					return;
				}

				try {
					if (!csvText.equals("")) {
						ArrayList<Spawn> spawnList;
						try {
							Type type = new TypeToken<List<Spawn>>(){}.getType();
							Gson gson = new Gson();
							spawnList = gson.fromJson(csvText, type);
							if (spawnList == null) {
								return;
							}

							for (int n = 0; n < spawnList.size(); n++) {
								Spawn spawn = spawnList.get(n);
								if (!MapController.mapHelper.spawns.containsKey(spawn.id)) MapController.mapHelper.showSpawnOnMap(spawn);
								MapController.mapHelper.spawns.put(spawn.id, spawn);
							}

							MapController.mapHelper.saveSpawns();
						} catch (Exception e) {
							e.printStackTrace();
							ErrorReporter.logExceptionThreaded(e);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						table.setup(MySpawnsController.this);
					}
				};
				features.runOnMainThread(runnable);
			}
		};

		DialogHelper.textPrompt("Import Spawns", "Enter the spawn data EXACTLY as it was exported from PokeSensor.", "", "Import", positive, null).build().show();
	}

	public void exportSpawns() {
		ArrayList<Spawn> spawnList = new ArrayList<Spawn>(MapController.mapHelper.spawns.values());
		Type type = new TypeToken<List<Spawn>>(){}.getType();
		Gson gson = new Gson();
		String text = gson.toJson(spawnList, type);
		shareText(text);
	}

	public void shareText(String text) {
		NSString nsText = NSString.stringWithString(text);
		NSArray<?> items = NSArray.arrayWithObject(nsText);

		UIActivityViewController controller = UIActivityViewController.alloc().initWithActivityItemsApplicationActivities(items, null);

		try {
			if (controller.popoverPresentationController() != null) {
				controller.popoverPresentationController().setBarButtonItem(exportButton);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.logExceptionThreaded(e);
		}

		presentViewControllerAnimatedCompletion(controller, true, null);
	}

	@Override
	public void viewWillDisappear(boolean b) {
		super.viewWillDisappear(b);
		IOSLauncher.navigationController.setToolbarHidden(true);
	}

	public synchronized Future run(Runnable runnable) {
		if (pool == null) {
			features.print("MySpawnsController", "Initializing a new thread pool");
			pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
			pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
		}
		Future future = pool.submit(new ExceptionCatchingRunnable(runnable));
		return future;
	}

	@IBOutlet
	@Property
	@Selector("table")
	public SpawnTableView getTable() { return table; }

	@IBOutlet
	@Property
	@Selector("setTable:")
	public void setTable(SpawnTableView table) { this.table = table; }
}
