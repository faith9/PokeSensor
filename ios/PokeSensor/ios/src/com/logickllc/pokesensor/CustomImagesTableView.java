package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.Spawn;
import com.pokegoapi.util.PokeDictionary;

import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewCellEditingStyle;
import org.robovm.apple.uikit.UITableViewCellSeparatorStyle;
import org.robovm.apple.uikit.UITableViewController;
import org.robovm.apple.uikit.UITableViewDataSourceAdapter;
import org.robovm.apple.uikit.UIView;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.rt.bro.annotation.MachineSizedSInt;

import java.util.Hashtable;
import java.util.Locale;

import static com.logickllc.pokesensor.api.AccountManager.PREF_NUM_ACCOUNTS;
import static com.logickllc.pokesensor.api.AccountManager.accounts;

@CustomClass("CustomImagesTableView")
public class CustomImagesTableView extends UITableView {
    public Hashtable<String, UIImage> images = new Hashtable<String, UIImage>();
    UITableViewController controller;
    UIView ogBackground;
    UITableViewCellSeparatorStyle ogSeparatorStyle;
    UIImage blank;

    public void setup(UITableViewController myController) {
        this.controller = myController;
        ogBackground = this.getBackgroundView();
        ogSeparatorStyle = this.getSeparatorStyle();
        blank = UIImage.create("blank.png");
        setDataSource(new UITableViewDataSourceAdapter() {
            @Override
            public UITableViewCell getCellForRow(UITableView table, final NSIndexPath path) {
                System.out.println("getCellForRow " + path.getRow());
                int position = path.getRow();

                CustomImagesTableCell cell = (CustomImagesTableCell) table.dequeueReusableCell("cell", path);

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
            public long getNumberOfSections(UITableView tableView) {
                return 1;
            }

            @Override
            public long getNumberOfRowsInSection(UITableView tableView, @MachineSizedSInt long section) {
                return Features.NUM_POKEMON;
            }

            @Override
            public boolean canEditRow(UITableView uiTableView, NSIndexPath nsIndexPath) {
                return true;
            }

            @Override
            public void commitEditingStyleForRow(UITableView uiTableView, UITableViewCellEditingStyle uiTableViewCellEditingStyle, NSIndexPath nsIndexPath) {
                if (uiTableViewCellEditingStyle == UITableViewCellEditingStyle.Delete) {
                    MapController.features.customImages.remove(nsIndexPath.getRow());
                    MapController.features.customImages.add(nsIndexPath.getRow(), " ");
                    MapController.features.saveCustomImagesUrls();

                    Gdx.files.local(IOSFeatures.CUSTOM_IMAGES_FOLDER + (nsIndexPath.getRow() + 1) + MapController.mapHelper.IMAGE_EXTENSION).delete();
                    resetImage(nsIndexPath.getRow() + 1);

                    uiTableView.reloadData();
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
                    images.put(filename, UIImage.getImage(handle.file()));
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
