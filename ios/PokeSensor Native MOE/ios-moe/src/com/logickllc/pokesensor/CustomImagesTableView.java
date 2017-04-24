package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.logickllc.pokesensor.api.Features;
import com.pokegoapi.util.PokeDictionary;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.NInt;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCClassName;

import java.util.Hashtable;
import java.util.Locale;

import apple.foundation.NSIndexPath;
import apple.uikit.UIImage;
import apple.uikit.UITableView;
import apple.uikit.UITableViewCell;
import apple.uikit.UITableViewController;
import apple.uikit.UIView;
import apple.uikit.enums.UITableViewCellEditingStyle;
import apple.uikit.protocol.UITableViewDataSource;

@Runtime(ObjCRuntime.class)
@ObjCClassName("CustomImagesTableView")
@RegisterOnStartup
public class CustomImagesTableView extends UITableView {
    public Hashtable<String, UIImage> images = new Hashtable<String, UIImage>();
    UITableViewController controller;
    UIView ogBackground;
    @NInt long ogSeparatorStyle;
    UIImage blank;

    protected CustomImagesTableView(Pointer peer) {
        super(peer);
    }

    public void setup(UITableViewController myController) {
        this.controller = myController;
        ogBackground = this.backgroundView();
        ogSeparatorStyle = this.separatorStyle();
        blank = UIImage.imageNamed("blank.png");
        setDataSource(new UITableViewDataSource() {
            @Override
            public UITableViewCell tableViewCellForRowAtIndexPath(UITableView tableView, NSIndexPath path) {
                System.out.println("getCellForRow " + path.row());
                int position = (int) path.row();

                CustomImagesTableCell cell = (CustomImagesTableCell) tableView.dequeueReusableCellWithIdentifierForIndexPath("cell", path);

                String url = MapController.features.customImages.get(position);
                cell.name.setText(PokeDictionary.getDisplayName(position+1, Locale.getDefault()));

                if (!url.equals(" ")) cell.statusLabel.setText(url);
                else cell.statusLabel.setText("NONE");

                try {
                    UIImage image = getImageByNum(position + 1);
                    if (image != null) cell.status.setImage(image);
                } catch (Exception e) {
                    e.printStackTrace();
                }

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

            @Override
            public boolean tableViewCanEditRowAtIndexPath(UITableView tableView, NSIndexPath indexPath) {
                return true;
            }

            @Override
            public void tableViewCommitEditingStyleForRowAtIndexPath(UITableView tableView, @NInt long editingStyle, NSIndexPath indexPath) {
                if (editingStyle == UITableViewCellEditingStyle.Delete) {
                    MapController.features.customImages.remove((int) indexPath.row());
                    MapController.features.customImages.add((int) indexPath.row(), " ");
                    MapController.features.saveCustomImagesUrls();

                    Gdx.files.local(IOSFeatures.CUSTOM_IMAGES_FOLDER + (indexPath.row() + 1) + MapController.mapHelper.IMAGE_EXTENSION).delete();
                    resetImage((int) indexPath.row() + 1);

                    tableView.reloadData();
                }
            }
        });
    }

    public UIImage getImageByNum(int num) {
        try {
            String filename = num + MapController.mapHelper.IMAGE_EXTENSION;
            FileHandle handle = Gdx.files.local(Features.CUSTOM_IMAGES_FOLDER + filename);
            if (handle.exists()) {
                if (!images.containsKey(filename))
                    images.put(filename, UIImage.imageWithContentsOfFile(handle.file().getAbsolutePath()));
                return images.get(filename);
            } else {
                return blank;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return blank;
        }
    }

    public void resetImage(int num) {
        String filename = num + MapController.mapHelper.IMAGE_EXTENSION;
        images.remove(filename);
    }
}
