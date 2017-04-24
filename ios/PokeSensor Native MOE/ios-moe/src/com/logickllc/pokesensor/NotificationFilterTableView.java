package com.logickllc.pokesensor;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.logickllc.pokesensor.api.Features;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.NInt;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCClassName;

import java.util.Hashtable;

import POGOProtos.Enums.PokemonIdOuterClass;
import apple.foundation.NSIndexPath;
import apple.uikit.UIImage;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.protocol.UITableViewDataSource;

@Runtime(ObjCRuntime.class)
@ObjCClassName("NotificationFilterTableView")
@RegisterOnStartup
public class NotificationFilterTableView extends UITableView {
    Hashtable<String, UIImage> images = new Hashtable<String, UIImage>();

    protected NotificationFilterTableView(Pointer peer) {
        super(peer);
    }

    public void setup() {
        setDataSource(new UITableViewDataSource() {
            @Override
            public UITableViewCell tableViewCellForRowAtIndexPath(UITableView table, NSIndexPath path) {
                System.out.println("getCellForRow " + path.row());
                int pokedexNumber = (int) path.row() + 1;

                NotificationFilterTableCell cell = (NotificationFilterTableCell) table.dequeueReusableCellWithIdentifierForIndexPath("cell", path);

                cell.pokedexNumber = pokedexNumber;
                cell.name.setText(MapController.mapHelper.getLocalName(pokedexNumber));
                cell.pic.setImage(getImageByNum(pokedexNumber));
                cell.toggle.setOn(MapController.features.notificationFilter.get(pokedexNumber));

                return cell;
            }

            @Override
            public long numberOfSectionsInTableView(UITableView tableView) {
                return 1;
            }

            @Override
            public long tableViewNumberOfRowsInSection(UITableView tableView, @NInt long section) {
                return Features.NUM_POKEMON;
            }
        });
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
}
