package com.logickllc.pokesensor;


import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.NInt;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCClassName;

import java.util.ArrayList;

import apple.foundation.NSError;
import apple.foundation.NSIndexPath;
import apple.mapkit.MKLocalSearch;
import apple.mapkit.MKLocalSearchRequest;
import apple.mapkit.MKLocalSearchResponse;
import apple.mapkit.MKMapItem;
import apple.mapkit.MKPlacemark;
import apple.mapkit.struct.MKCoordinateRegion;
import apple.mapkit.struct.MKCoordinateSpan;
import apple.uikit.UISearchController;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.UITableViewController;
import apple.uikit.protocol.UISearchResultsUpdating;

@Runtime(ObjCRuntime.class)
@ObjCClassName("SearchTableController")
@RegisterOnStartup
public class SearchTableController extends UITableViewController implements UISearchResultsUpdating {
	public ArrayList<MKMapItem> searchResults = new ArrayList<MKMapItem>();

	protected SearchTableController(Pointer peer) {
		super(peer);
	}

	@Override
	public long tableViewNumberOfRowsInSection(UITableView tableView, @NInt long section) {
		return searchResults.size();
	}

	@Override
	public UITableViewCell tableViewCellForRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
		try {
			UITableViewCell cell = tableView.dequeueReusableCellWithIdentifier("cell");
			MKPlacemark placemark = searchResults.get((int) indexPath.row()).placemark();
			cell.textLabel().setText(placemark.name());

			String address = placemark.toString();
			if (address.contains("@")) address = address.substring(0, address.indexOf("@"));
			address = address.trim();
			cell.detailTextLabel().setText(address);
			return cell;
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logExceptionThreaded(t);
		}

		return null;
	}

	@Override
	public void tableViewDidSelectRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
		try {
			final MKPlacemark placemark = searchResults.get((int) indexPath.row()).placemark();

			Block_dismissViewControllerAnimatedCompletion completionHandler = new Block_dismissViewControllerAnimatedCompletion() {
				@Override
				public void call_dismissViewControllerAnimatedCompletion() {
					MapController.mapHelper.setLocationOverride(true);
					MapController.mapHelper.moveMe(placemark.coordinate().latitude(), placemark.coordinate().longitude(), placemark.location().altitude(), true, true);
				}
			};

			this.dismissViewControllerAnimatedCompletion(true, completionHandler);
		} catch (Throwable t) {
			t.printStackTrace();
			ErrorReporter.logExceptionThreaded(t);

			Block_dismissViewControllerAnimatedCompletion completionHandler = new Block_dismissViewControllerAnimatedCompletion() {
				@Override
				public void call_dismissViewControllerAnimatedCompletion() {
					MapController.features.longMessage("Search error. Please try again.");
				}
			};

			this.dismissViewControllerAnimatedCompletion(true, completionHandler);
		}
	}

	@Override
	public void updateSearchResultsForSearchController(UISearchController searchController) {
		String searchText = searchController.searchBar().text();
		if (searchText.equals("")) return;
		MKLocalSearchRequest request = MKLocalSearchRequest.alloc().init();
		request.setNaturalLanguageQuery(searchText);

		MKCoordinateRegion region = new MKCoordinateRegion(MapController.instance.map.centerCoordinate(), new MKCoordinateSpan(0.2f, 0.2f));
		request.setRegion(region);

		MKLocalSearch localSearch = MKLocalSearch.alloc().initWithRequest(request);
		localSearch.startWithCompletionHandler(new MKLocalSearch.Block_startWithCompletionHandler() {

			@Override
			public void call_startWithCompletionHandler(MKLocalSearchResponse a, NSError arg1) {
				try {
					if (a == null) return;
					searchResults = new ArrayList<MKMapItem>(a.mapItems());
					//MapController.features.print("Search", "Results: " + searchResults.toString());
					tableView().reloadData();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
