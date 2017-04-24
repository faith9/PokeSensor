package com.logickllc.pokesensor;


import com.badlogic.gdx.Gdx;
import com.logickllc.pokesensor.api.ExceptionCatchingRunnable;
import com.logickllc.pokesensor.api.ExceptionCatchingThreadFactory;
import com.logickllc.pokesensor.api.Features;

import org.apache.commons.io.FileUtils;
import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.IBOutlet;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Property;
import org.moe.natj.objc.ann.Selector;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import POGOProtos.Enums.PokemonIdOuterClass;
import apple.foundation.NSArray;
import apple.foundation.NSIndexPath;
import apple.foundation.NSString;
import apple.uikit.UIActivityViewController;
import apple.uikit.UIBarButtonItem;
import apple.uikit.UIImage;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.UITableViewController;
import apple.uikit.UIViewController;
import apple.uikit.enums.UIBarButtonItemStyle;
import apple.uikit.enums.UIBarButtonSystemItem;
import apple.uikit.protocol.UITableViewDataSource;
import apple.uikit.protocol.UITableViewDelegate;

@Runtime(ObjCRuntime.class)
@ObjCClassName("CustomImagesController")
@RegisterOnStartup
public class CustomImagesController extends UITableViewController implements UITableViewDataSource, UITableViewDelegate {

	//@IBOutlet
	CustomImagesTableView table;

	private int counter = 0;
	private int fetchingUrls = 0;
	private ThreadPoolExecutor pool;
	private final int MAX_POOL_THREADS = 5;
	private UIBarButtonItem exportButton;

	protected CustomImagesController(Pointer peer) {
		super(peer);
	}

	@Override
	public void viewDidLoad() {
		UIBarButtonItem importButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("import.png"), UIBarButtonItemStyle.Plain, this, new SEL("importClicked:"));
		UIBarButtonItem flexButton1 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		exportButton = UIBarButtonItem.alloc().initWithImageStyleTargetAction(UIImage.imageNamed("export.png"), UIBarButtonItemStyle.Plain, this, new SEL("exportClicked:"));
		UIBarButtonItem flexButton2 = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.FlexibleSpace, null, null);
		UIBarButtonItem deleteButton = UIBarButtonItem.alloc().initWithBarButtonSystemItemTargetAction(UIBarButtonSystemItem.Trash, this, new SEL("deleteClicked:"));

		NSArray<UIBarButtonItem> items = (NSArray<UIBarButtonItem>) NSArray.arrayWithObjects(importButton, flexButton1, exportButton,
				flexButton2, deleteButton, null);

		final UIViewController me = this;

		this.setToolbarItems(items);

		this.navigationController().setToolbarHidden(false);

		MapController.features.loadCustomImageUrls();
		table.setup(this);
	}

	@Selector("importClicked:")
	void importClicked(UIBarButtonItem sender) {
		ArrayList<String> options = new ArrayList<String>();
		options.add("Website URL");
		options.add("Import URLs");

		ArrayList<Lambda> functions = new ArrayList<Lambda>();
		functions.add(new Lambda() {
			@Override
			public void execute() {
				importCustomImages();
			}
		});

		functions.add(new Lambda() {
			@Override
			public void execute() {
				importUrls();
			}
		});

		IOSFeatures.showNativeOptionsList(options, functions, this);
	}

	@Selector("exportClicked:")
	void exportClicked(UIBarButtonItem sender) {
		exportCustomImages();
	}

	@Selector("deleteClicked:")
	void deleteClicked(UIBarButtonItem sender) {
		Runnable positive = new Runnable() {
			@Override
			public void run() {
				MapController.features.customImages.clear();
				for (int n = 0; n < Features.NUM_POKEMON; n++) {
					MapController.features.customImages.add(" ");
				}
				MapController.features.saveCustomImagesUrls();

				Gdx.files.local(IOSFeatures.CUSTOM_IMAGES_FOLDER).emptyDirectory();

				reloadData();
			}
		};
		DialogHelper.yesNoBox("Delete All?", "Are you sure you want to delete all custom images?", "Delete", positive, "Cancel", null).build().show();
	}

	@Override
	public void tableViewDidSelectRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
		UITableViewCell selected = tableView.cellForRowAtIndexPath(indexPath);
		tableView.deselectRowAtIndexPathAnimated(indexPath, true);
		final int row = (int) indexPath.row();

		if (fetchingUrls > 0) {
			MapController.features.longMessage("Already fetching images. Please wait until they finish before trying again");
			return;
		}

		Lambda positive = new Lambda() {
			@Override
			public void execute() {
				String text = (String) params.get("Text");
				text = text.trim();
				if (text != null && !text.equals("") && text.indexOf("http") == 0) {
					MapController.features.customImages.remove(row);
					MapController.features.customImages.add(row, text);
					MapController.features.saveCustomImagesUrls();

					Gdx.files.local(IOSFeatures.CUSTOM_IMAGES_FOLDER + (row + 1) + MapController.mapHelper.IMAGE_EXTENSION).delete();

					counter = 0;
					fetchingUrls = 1;
					fetchImageFromUrl(text, row + 1);
				} else {
					MapController.features.longMessage("Invalid image URL. Must start with http or https and be a valid image URL.");
				}
			}
		};

		String defaultText = MapController.features.customImages.get(row);
		if (defaultText.equals(" ")) defaultText = "";
		DialogHelper.textPrompt("Enter Image URL", "Enter the URL for your custom image. Don't use a copyrighted image unless you have the rights to use it.", defaultText, "Fetch", positive, null).setValue(defaultText).build().show();
	}

	@Override
	public void viewWillAppear(boolean b) {
		super.viewWillAppear(b);

		this.navigationController().setToolbarHidden(false);
		reloadData();
	}

	public static void reloadData() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (IOSLauncher.navigationController.topViewController() instanceof CustomImagesController) {
					CustomImagesController cont = (CustomImagesController) IOSLauncher.navigationController.topViewController();
					if (cont != null && cont.table != null) cont.table.reloadData();
				}
			}
		};
		MapController.features.runOnMainThread(runnable);
	}

	public void importUrls() {
		if (fetchingUrls > 0) {
			MapController.features.longMessage("Already fetching images. Please wait until they finish before trying again");
			return;
		}

		Lambda positive = new Lambda() {
			@Override
			public void execute() {
				int cursor = 0;
				String fullText = (String) params.get("Text");

				counter = 0;
				fetchingUrls = fullText.split(",").length;

				for (String text : fullText.split(",")) {
					if (cursor >= Features.NUM_POKEMON) break;
					text = text.trim();
					if (text != null && !text.equals("") && text.indexOf("http") == 0) {
						MapController.features.customImages.remove(cursor);
						MapController.features.customImages.add(cursor, text);
						MapController.features.saveCustomImagesUrls();

						Gdx.files.local(IOSFeatures.CUSTOM_IMAGES_FOLDER + (cursor + 1) + MapController.mapHelper.IMAGE_EXTENSION).delete();

						fetchImageFromUrl(text, cursor + 1);
					} else {
						//MapController.features.longMessage("Invalid image URL. Must start with http or https and be a valid image URL.");
					}
					cursor++;
				}
			}
		};

		DialogHelper.textPrompt("Enter Image URLs", "Enter the list of image URLs that you want to use. Each URL should be separated by a comma. Don't use copyrighted images unless you have the rights to use them.", "", "Fetch", positive, null).build().show();
	}

	public void importCustomImages() {
		if (fetchingUrls > 0) {
			MapController.features.longMessage("Already fetching images. Please wait until they finish before trying again");
			return;
		}

		Lambda positive = new Lambda() {
			@Override
			public void execute() {
				String text = (String) params.get("Text");
				text = text.trim();
				if (text != null && !text.equals("") && text.indexOf("http") == 0 && text.lastIndexOf("/") == text.length() - 1) {
					fetchAllImagesFromUrl(text);
				} else {
					MapController.features.longMessage("Invalid image URL. Must start with http or https and end with /");
				}
			}
		};

		DialogHelper.textPrompt("Enter Image URL", "Enter the base URL for your custom images. The app will do the rest. Don't use copyrighted images unless you have the rights to use them.", "", "Fetch", positive, null).build().show();
	}

	public void fetchImageFromUrl(final String url, final int pokedexNumber) {
		try {
			/*NSURL nsUrl = new NSURL(url);
			if (nsUrl == null) return;
			NSURLSessionDownloadTask task = NSURLSession.getSharedSession().newDownloadTask(nsUrl, new VoidBlock3<NSURL, NSURLResponse, NSError>() {
				@Override
				public void invoke(NSURL nsurl, NSURLResponse nsurlResponse, NSError nsError) {
					try {
						table.resetImage(pokedexNumber);
						if (nsError == null) {
							UIImage image = new UIImage(NSData.read(nsurl));
							Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER).mkdirs();
							image.toPNGData().write(Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER + pokedexNumber + MapController.mapHelper.IMAGE_EXTENSION).file(), false);
						} else {
							MapController.features.print("PokeFinder", nsError.toString());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					incCounter();
				}
			});
			task.resume();*/

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER).mkdirs();
						File dest = Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER + pokedexNumber + MapController.mapHelper.IMAGE_EXTENSION).file();

						FileUtils.copyURLToFile(new URL(url), dest);
					} catch (Exception e) {
						e.printStackTrace();
					}
					incCounter();
				}
			};
			run(runnable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void fetchAllImagesFromUrl(final String url) {
		counter = 0;
		fetchingUrls = Features.NUM_POKEMON;

		ArrayList<String> urls = new ArrayList<>();
		for (int n = 1; n <= Features.NUM_POKEMON; n++) {
			String name = PokemonIdOuterClass.PokemonId.valueOf(n).name();
			name = name.toLowerCase().replaceAll("\\.","").replaceAll("'","").replaceAll("_female","f").replaceAll("_male","m").replaceAll(" ","-").replaceAll("_","-").replaceAll("♂","m").replaceAll("♀","f");
			urls.add(url + name + MapController.mapHelper.IMAGE_EXTENSION);
		}

		MapController.features.customImages = urls;
		MapController.features.saveCustomImagesUrls();
		table.images.clear();
		Gdx.files.local(IOSFeatures.CUSTOM_IMAGES_FOLDER).emptyDirectory();
		reloadData();

		for (int n = 0; n < Features.NUM_POKEMON; n++) {
			fetchImageFromUrl(urls.get(n), n+1);
		}
	}

	public void exportCustomImages() {
		String bigString = "";

		for (int n = 0; n < Features.NUM_POKEMON; n++) {
			bigString += MapController.features.customImages.get(n) + (n == Features.NUM_POKEMON - 1 ? "" : ",");
		}

		shareText(bigString);
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

		MapController.features.saveCustomImagesUrls();
		this.navigationController().setToolbarHidden(true);
	}

	private synchronized void incCounter() {
		counter++;
		if (counter == fetchingUrls || counter % 10 == 0) reloadData();
		if (counter == fetchingUrls) {
			counter = 0;
			fetchingUrls = 0;
		}
	}

	public synchronized Future run(Runnable runnable) {
		if (pool == null) {
			MapController.features.print("PokeFinder", "Initializing a new thread pool");
			pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
			pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
		}
		Future future = pool.submit(new ExceptionCatchingRunnable(runnable));
		if (IOSLauncher.IS_AD_TESTING) MapController.features.print("PokeFinder", pool.getQueue().toString());
		return future;
	}

	@IBOutlet
	@Property
	@Selector("table")
	public CustomImagesTableView getTable() { return table; }

	@IBOutlet
	@Property
	@Selector("setTable:")
	public void setTable(CustomImagesTableView table) { this.table = table; }
}
