package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.Spawn;

import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIEvent;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewCellEditingStyle;
import org.robovm.apple.uikit.UITableViewCellSeparatorStyle;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.apple.uikit.UITableViewDataSourceAdapter;
import org.robovm.apple.uikit.UITableViewDelegateAdapter;
import org.robovm.apple.uikit.UIView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.rt.bro.annotation.MachineSizedSInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import POGOProtos.Enums.PokemonIdOuterClass;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

@CustomClass("SpawnTableView")
public class SpawnTableView extends UITableView {
    ArrayList<Spawn> spawns = new ArrayList<>(MapController.mapHelper.spawns.values());
    Hashtable<String, UIImage> images = new Hashtable<String, UIImage>();
    UITableViewController controller;
    UIView ogBackground;
    UITableViewCellSeparatorStyle ogSeparatorStyle;

    public void setup(UITableViewController myController) {
        this.controller = myController;
        ogBackground = this.getBackgroundView();
        ogSeparatorStyle = this.getSeparatorStyle();
        spawns = new ArrayList<>(MapController.mapHelper.spawns.values());
        if (!MapController.mapHelper.spawns.isEmpty()) {
            //spawns = new ArrayList<>(MapController.mapHelper.spawns.values());
            Collections.sort(spawns, new Comparator<Spawn>() {
                @Override
                public int compare(Spawn lhs, Spawn rhs) {
                    return lhs.nickname.compareTo(rhs.nickname);
                }
            });
            Collections.sort(spawns, new Comparator<Spawn>() {
                @Override
                public int compare(Spawn lhs, Spawn rhs) {
                    return Long.valueOf(lhs.timeFound).compareTo(rhs.timeFound);
                }
            });

			/*for (Spawn spawn : spawns) {
            		for (int num : spawn.history) {
            			String name = PokemonIdOuterClass.PokemonId.forNumber(num).name().toLowerCase();
            			String filename = MapController.mapHelper.POKEMON_FOLDER + name + MapController.mapHelper.IMAGE_EXTENSION;
            			if (!images.containsKey(name)) images.put(name.toLowerCase(), UIImage.create(filename));
            		}
            }*/
        } else {
            showEmptySpawnMessage();
        }

        //registerReusableCellClass(SpawnTableCell.class, "cell");

        setDataSource(new UITableViewDataSourceAdapter() {
            @Override
            public UITableViewCell getCellForRow(UITableView table, final NSIndexPath path) {
                System.out.println("getCellForRow " + path.getRow());

                SpawnTableCell cell = (SpawnTableCell) table.dequeueReusableCell("cell", path);
                final Spawn spawn = spawns.get(path.getRow());

                cell.name.setText(spawn.nickname);
                cell.location.setText(spawn.location);
                cell.history1.setHidden(true);
                cell.history2.setHidden(true);
                cell.history3.setHidden(true);

                int n = 0;
                for (int i = spawn.history.size() - 1; i >= 0; i--) {
                    int num = spawn.history.get(i);
                    n++;
                    if (n > 3) break;
                    switch(n) {
                        case 1:
                            cell.history1.setImage(getImageByNum(num));
                            cell.history1.setHidden(false);
                            break;
                        case 2:
                            cell.history2.setImage(getImageByNum(num));
                            cell.history2.setHidden(false);
                            break;
                        case 3:
                            cell.history3.setImage(getImageByNum(num));
                            cell.history3.setHidden(false);
                            break;
                    }
                }

                if (cell.listener != null) cell.move.removeListener(cell.listener);

                UIControl.OnTouchUpInsideListener listener = new UIControl.OnTouchUpInsideListener() {
                    @Override
                    public void onTouchUpInside(UIControl uiControl, UIEvent uiEvent) {
                        moveToSpawn(spawn);
                    }
                };

                cell.move.addOnTouchUpInsideListener(listener);
                cell.listener = listener;

                return cell;
            }

            @Override
            public long getNumberOfSections(UITableView tableView) {
                if (spawns.size() > 0) {
                    tableView.setBackgroundView(ogBackground);
                    tableView.setSeparatorStyle(ogSeparatorStyle);
                    return 1;
                }
                else {
                    TableHelper.showEmptyTableMessage(controller, "You don't have any spawn data yet.\nScan some more to find some spawn points.");
                    return 0;
                }
            }

            @Override
            public long getNumberOfRowsInSection(UITableView tableView, @MachineSizedSInt long section) {
                // Return the local copy of the array in case the MapHelper version changes without changing this one
                return spawns.size();
            }

            @Override
            public boolean canEditRow(UITableView uiTableView, NSIndexPath nsIndexPath) {
                return true;
            }

            @Override
            public void commitEditingStyleForRow(UITableView uiTableView, UITableViewCellEditingStyle uiTableViewCellEditingStyle, NSIndexPath nsIndexPath) {
                if (uiTableViewCellEditingStyle == UITableViewCellEditingStyle.Delete) {
                    Spawn spawn = spawns.remove(nsIndexPath.getRow());
                    MapController.mapHelper.spawns.remove(spawn.id);
                    uiTableView.reloadData();
                }
            }
        });
    }



    private void moveToSpawn(Spawn spawn) {
        final ConcurrentHashMap<String, Spawn> mySpawnList = new ConcurrentHashMap<>();
        final Spawn mySpawn = spawn;
        mySpawnList.put(mySpawn.id, mySpawn);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                MapController.mapHelper.setLocationOverride(true);
                MapController.features.print("PokeFinder", "Moving to " + mySpawn.nickname);
                MapController.mapHelper.moveMe(mySpawn.lat, mySpawn.lon, MapController.mapHelper.altitude, true, true);
                MapController.mapHelper.spawnScan(mySpawnList);
            }
        };

        MapController.instance.dontRefreshAccounts = true;
        MapController.instance.getNavigationController().popToRootViewController(true);
        MapController.features.runOnMainThread(r);
    }

    public UIImage getImageByNum(int num) {
        if (IOSMapHelper.CAN_SHOW_IMAGES) {
            String name = PokemonIdOuterClass.PokemonId.forNumber(num).name().toLowerCase();
            String filename = MapController.mapHelper.POKEMON_FOLDER + name + MapController.mapHelper.IMAGE_EXTENSION;
            if (!images.containsKey(name)) images.put(name.toLowerCase(), UIImage.create(filename));
            return images.get(name);
        } else {
            String filename = num + MapController.mapHelper.IMAGE_EXTENSION;
            try {

                FileHandle handle = Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER + filename);
                if (handle.exists()) {
                    if (!images.containsKey(filename))
                        images.put(filename, UIImage.getImage(handle.file()));
                    return images.get(filename);
                } else {
                    if (!images.containsKey(filename)) images.put(filename.toLowerCase(), UIImage.create(IOSMapHelper.NUMBER_MARKER_FOLDER + filename));
                    return images.get(filename);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!images.containsKey(filename)) images.put(filename.toLowerCase(), UIImage.create(IOSMapHelper.NUMBER_MARKER_FOLDER + filename));
                return images.get(filename);
            }
        }
    }

    public void showEmptySpawnMessage() {

    }
}
