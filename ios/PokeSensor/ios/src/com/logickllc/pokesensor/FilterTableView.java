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
import org.robovm.apple.uikit.UITableViewDataSourceAdapter;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.rt.bro.annotation.MachineSizedSInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import POGOProtos.Enums.PokemonIdOuterClass;

@CustomClass("FilterTableView")
public class FilterTableView extends UITableView {
    Hashtable<String, UIImage> images = new Hashtable<String, UIImage>();

    public void setup() {
        setDataSource(new UITableViewDataSourceAdapter() {
            @Override
            public UITableViewCell getCellForRow(UITableView table, final NSIndexPath path) {
                System.out.println("getCellForRow " + path.getRow());
                int pokedexNumber = path.getRow() + 1;

                FilterTableCell cell = (FilterTableCell) table.dequeueReusableCell("cell", path);

                cell.pokedexNumber = pokedexNumber;
                cell.name.setText(MapController.mapHelper.getLocalName(pokedexNumber));
                cell.pic.setImage(getImageByNum(pokedexNumber));
                cell.toggle.setOn(MapController.features.filter.get(pokedexNumber));

                return cell;
            }

            @Override
            public long getNumberOfSections(UITableView tableView) {
                return 1;
            }

            @Override
            public long getNumberOfRowsInSection(UITableView tableView, @MachineSizedSInt long section) {
                return Features.NUM_POKEMON;
            }

        });
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
}
