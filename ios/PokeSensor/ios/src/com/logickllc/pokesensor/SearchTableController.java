package com.logickllc.pokesensor;

import java.util.ArrayList;

import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.mapkit.MKCoordinateRegion;
import org.robovm.apple.mapkit.MKCoordinateSpan;
import org.robovm.apple.mapkit.MKLocalSearch;
import org.robovm.apple.mapkit.MKLocalSearchRequest;
import org.robovm.apple.mapkit.MKLocalSearchResponse;
import org.robovm.apple.mapkit.MKMapItem;
import org.robovm.apple.mapkit.MKPlacemark;
import org.robovm.apple.uikit.UISearchController;
import org.robovm.apple.uikit.UISearchResultsUpdating;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.block.VoidBlock2;

@CustomClass("SearchTableController")
public class SearchTableController extends UITableViewController implements UISearchResultsUpdating {
	public ArrayList<MKMapItem> searchResults = new ArrayList<MKMapItem>();

	@Override
	public long getNumberOfRowsInSection(UITableView tableView, long section) {
		return searchResults.size();
	}

	@Override
	public UITableViewCell getCellForRow(UITableView tableView, NSIndexPath indexPath) {
		UITableViewCell cell = tableView.dequeueReusableCell("cell");
		MKPlacemark placemark = searchResults.get(indexPath.getRow()).getPlacemark();
		cell.getTextLabel().setText(placemark.getName());
		
		String address = placemark.toString();
		if (address.contains("@")) address = address.substring(0, address.indexOf("@"));
		address = address.trim();
		cell.getDetailTextLabel().setText(address);
		
		return cell;
	}

	@Override
	public void didSelectRow(UITableView tableView, NSIndexPath indexPath) {
		final MKPlacemark placemark = searchResults.get(indexPath.getRow()).getPlacemark();
		Runnable runnable = new Runnable() {
			public void run() {
				MapController.mapHelper.setLocationOverride(true);
				MapController.mapHelper.moveMe(placemark.getCoordinate().getLatitude(), placemark.getCoordinate().getLongitude(), true, true);
			}
		};
		this.dismissViewController(true, runnable);
	}
	
	@Override
	public void updateSearchResults(UISearchController searchController) {
		String searchText = searchController.getSearchBar().getText();
		if (searchText.isEmpty()) return;
		MKLocalSearchRequest request = new MKLocalSearchRequest();
		request.setNaturalLanguageQuery(searchText);
		
		MKCoordinateRegion region = new MKCoordinateRegion(MapController.instance.map.getCenterCoordinate(), new MKCoordinateSpan(0.2f, 0.2f));
		request.setRegion(region);
		
		MKLocalSearch localSearch = new MKLocalSearch(request);
		localSearch.start(new VoidBlock2<MKLocalSearchResponse, NSError>() {
			@Override
			public void invoke(MKLocalSearchResponse a, NSError b) {
				searchResults = new ArrayList<MKMapItem>(a.getMapItems());
				MapController.features.print("Search", "Results: " + searchResults.toString());
				getTableView().reloadData();
			}
		});
	}

	
}
