package com.logickllc.pokesensor;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.Spawn;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.NInt;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

import POGOProtos.Enums.PokemonIdOuterClass;
import apple.foundation.NSIndexPath;
import apple.uikit.UIButton;
import apple.uikit.UIImage;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.UITableViewController;
import apple.uikit.UIView;
import apple.uikit.enums.UIControlEvents;
import apple.uikit.enums.UITableViewCellEditingStyle;
import apple.uikit.protocol.UITableViewDataSource;

@Runtime(ObjCRuntime.class)
@ObjCClassName("SpawnTableView")
@RegisterOnStartup
public class SpawnTableView extends UITableView {
    ArrayList<Spawn> spawns = new ArrayList<>(MapController.mapHelper.spawns.values());
    Hashtable<String, UIImage> images = new Hashtable<String, UIImage>();
    UITableViewController controller;
    UIView ogBackground;
    @NInt long ogSeparatorStyle;

    protected SpawnTableView(Pointer peer) {
        super(peer);
    }

    public void setup(UITableViewController myController) {
        this.controller = myController;
        ogBackground = this.backgroundView();
        ogSeparatorStyle = this.separatorStyle();
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
        } else {
            showEmptySpawnMessage();
        }

        //registerReusableCellClass(SpawnTableCell.class, "cell");

        setDataSource(new UITableViewDataSource() {
            @Override
            public UITableViewCell tableViewCellForRowAtIndexPath(UITableView table, NSIndexPath path) {
                System.out.println("getCellForRow " + path.row());

                SpawnTableCell cell = (SpawnTableCell) table.dequeueReusableCellWithIdentifierForIndexPath("cell", path);
                final Spawn spawn = spawns.get((int) path.row());

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

                cell.move.setTag(path.row());
                cell.move.addTargetActionForControlEvents(SpawnTableView.this, new SEL("moveClicked:"), UIControlEvents.TouchUpInside);

                return cell;
            }

            @Override
            public long numberOfSectionsInTableView(UITableView tableView) {
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
            public long tableViewNumberOfRowsInSection(UITableView tableView, @NInt long section) {
                // Return the local copy of the array in case the MapHelper version changes without changing this one
                return spawns.size();
            }

            @Override
            public boolean tableViewCanEditRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
                return true;
            }

            @Override
            public void tableViewCommitEditingStyleForRowAtIndexPath(UITableView tableView, @NInt long editingStyle, NSIndexPath indexPath) {
                if (editingStyle == UITableViewCellEditingStyle.Delete) {
                    Spawn spawn = spawns.remove((int) indexPath.row());
                    MapController.mapHelper.spawns.remove(spawn.id);
                    tableView.reloadData();
                }
            }
        });
    }

    @Selector("moveClicked:")
    void moveClicked(UIButton sender) {
        moveToSpawn(spawns.get((int) sender.tag()));
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
                MapController.mapHelper.spawnScan(mySpawnList, false);
            }
        };

        MapController.instance.dontRefreshAccounts = true;
        MapController.instance.navigationController().popToRootViewControllerAnimated(true);
        MapController.features.runOnMainThread(r);
    }

    public UIImage getImageByNum(int num) {
        if (IOSMapHelper.CAN_SHOW_IMAGES) {
            String name = PokemonIdOuterClass.PokemonId.forNumber(num).name().toLowerCase();
            String filename = MapController.mapHelper.POKEMON_FOLDER + name + MapController.mapHelper.IMAGE_EXTENSION;
            if (!images.containsKey(name)) images.put(name.toLowerCase(), UIImage.imageNamed(filename));
            return images.get(name);
        } else {
            String filename = num + MapController.mapHelper.IMAGE_EXTENSION;
            try {

                FileHandle handle = Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER + filename);
                if (handle.exists()) {
                    if (!images.containsKey(filename))
                        images.put(filename, UIImage.imageWithContentsOfFile(handle.file().getAbsolutePath()));
                    return images.get(filename);
                } else {
                    if (!images.containsKey(filename)) images.put(filename.toLowerCase(), UIImage.imageNamed(IOSMapHelper.NUMBER_MARKER_FOLDER + filename));
                    return images.get(filename);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!images.containsKey(filename)) images.put(filename.toLowerCase(), UIImage.imageNamed(IOSMapHelper.NUMBER_MARKER_FOLDER + filename));
                return images.get(filename);
            }
        }
    }

    public void showEmptySpawnMessage() {

    }
}
