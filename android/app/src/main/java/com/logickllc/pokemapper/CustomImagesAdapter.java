package com.logickllc.pokemapper;


import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.logickllc.pokesensor.api.Features;

import java.io.File;

import POGOProtos.Enums.PokemonIdOuterClass;

public class CustomImagesAdapter extends BaseAdapter {
    Activity act;
    LayoutInflater inflater;

    public CustomImagesAdapter(Activity act) {
        this.act = act;
        inflater = act.getLayoutInflater();
    }

    @Override
    public int getCount() {
        return Features.NUM_POKEMON;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.custom_image_list_item, parent, false);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.pic = (ImageView) convertView.findViewById(R.id.pic);
            holder.url = (TextView) convertView.findViewById(R.id.url);
            holder.switcher = (ViewSwitcher) convertView.findViewById(R.id.switcher);
            holder.number = (TextView) convertView.findViewById(R.id.number);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.pokedexNumber = position + 1;
        holder.name.setText(PokeFinderActivity.mapHelper.getLocalName(holder.pokedexNumber));

        if (PokeFinderActivity.features.customImages.get(position).equals(" ")) {
            holder.url.setText("NONE");
        } else {
            holder.url.setText(PokeFinderActivity.features.customImages.get(position));
        }

        setPic(holder);

        PokeFinderActivity.features.print("PokeFinder", "Called getView()");

        return convertView;
    }

    private static class ViewHolder {
        public TextView name;
        public ImageView pic;
        public TextView url;
        public int pokedexNumber;
        public TextView number;
        public ViewSwitcher switcher;
    }

    private void setPic(ViewHolder holder) {
        String name = PokemonIdOuterClass.PokemonId.forNumber(holder.pokedexNumber).name();
        String filename = holder.pokedexNumber + PokeFinderActivity.mapHelper.IMAGE_EXTENSION;
        String baseFolder = PokeFinderActivity.instance.getFilesDir().getAbsolutePath();
        String customImagesFolder = baseFolder + Features.CUSTOM_IMAGES_FOLDER;
        File file = new File(customImagesFolder + filename);
        if (file.exists()) {
            holder.pic.setImageDrawable(Drawable.createFromPath(file.getAbsolutePath()));
            holder.switcher.setDisplayedChild(1);
        } else {
            holder.number.setText(holder.pokedexNumber + ".");
            holder.switcher.setDisplayedChild(0);
            //int markerResourceID = act.getResources().getIdentifier(Features.NUMBER_MARKER_PREFIX + holder.pokedexNumber + "", "drawable", act.getPackageName());
            //holder.pic.setImageResource(markerResourceID);
        }
    }
}
