package com.logickllc.pokemapper;


import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.Spawn;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import POGOProtos.Enums.PokemonIdOuterClass;

public class MySpawnsAdapter extends BaseAdapter {
    ArrayList<Spawn> spawns;
    Activity act;
    LayoutInflater inflater;
    Drawable edit;
    Drawable check;
    Drawable history;
    Drawable move;

    public MySpawnsAdapter(ArrayList<Spawn> spawns, Activity act) {
        this.spawns = spawns;
        this.act = act;
        inflater = act.getLayoutInflater();
        edit = act.getResources().getDrawable(R.drawable.ic_edit);
        check = act.getResources().getDrawable(R.drawable.check);
    }

    @Override
    public int getCount() {
        return spawns.size();
    }

    @Override
    public Object getItem(int position) {
        return spawns.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.spawn_list_item, parent, false);

            holder = new ViewHolder();
            holder.titleView = (TextView) convertView.findViewById(R.id.titleView);
            holder.location = (TextView) convertView.findViewById(R.id.location);
            holder.edit = (ImageView) convertView.findViewById(R.id.edit);
            holder.history = (ImageView) convertView.findViewById(R.id.history);
            holder.history1 = (ImageView) convertView.findViewById(R.id.history1);
            holder.history2 = (ImageView) convertView.findViewById(R.id.history2);
            holder.history3 = (ImageView) convertView.findViewById(R.id.history3);
            holder.move = (ImageView) convertView.findViewById(R.id.move);
            holder.switcher = (ViewSwitcher) convertView.findViewById(R.id.switcher);
            holder.titleEdit = (EditText) convertView.findViewById(R.id.editTitle);

            holder.switcher1 = (ViewSwitcher) convertView.findViewById(R.id.switcher1);
            holder.switcher2 = (ViewSwitcher) convertView.findViewById(R.id.switcher2);
            holder.switcher3 = (ViewSwitcher) convertView.findViewById(R.id.switcher3);

            holder.number1 = (TextView) convertView.findViewById(R.id.number1);
            holder.number2 = (TextView) convertView.findViewById(R.id.number2);
            holder.number3 = (TextView) convertView.findViewById(R.id.number3);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Spawn spawn = spawns.get(position);
        holder.titleView.setText(spawn.nickname);
        holder.location.setText(spawn.location);

        holder.history1.setVisibility(View.INVISIBLE);
        holder.history2.setVisibility(View.INVISIBLE);
        holder.history3.setVisibility(View.INVISIBLE);

        holder.switcher1.setDisplayedChild(1);
        holder.switcher2.setDisplayedChild(1);
        holder.switcher3.setDisplayedChild(1);

        int n = 0;
        for (int num : spawn.history) {
            n++;
            if (n > 3) break;
            String name = PokemonIdOuterClass.PokemonId.forNumber(num).name();
            //int resourceID = act.getResources().getIdentifier(name.toLowerCase(), "drawable", act.getPackageName());
            switch(n) {
                case 1:
                    setPic(holder.history1, num, holder.switcher1, holder.number1);
                    holder.history1.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    setPic(holder.history2, num, holder.switcher2, holder.number2);
                    holder.history2.setVisibility(View.VISIBLE);
                    break;
                case 3:
                    setPic(holder.history2, num, holder.switcher3, holder.number3);
                    holder.history3.setVisibility(View.VISIBLE);
                    break;
            }
        }

        holder.move.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToSpawn(position);
            }
        });

        /*holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.switcher.showNext();
                if (!holder.editing) {
                    holder.titleEdit.setText(spawn.nickname);
                    holder.titleEdit.requestFocus();

                    *//*View view = act.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(holder.titleEdit, InputMethodManager.SHOW_IMPLICIT);
                        holder.titleEdit.requestFocus();
                    }*//*

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            holder.titleEdit.requestFocus();
                        }
                    };
                    act.runOnUiThread(r);

                    //holder.titleEdit.selectAll();

                    holder.edit.setImageDrawable(check);

                    //holder.titleEdit.performClick();
                    *//*holder.titleEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_DONE && event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                                spawn.nickname = holder.titleEdit.getText().toString();
                                PokeFinderActivity.mapHelper.spawns.get(spawn.id).nickname = holder.titleEdit.getText().toString();

                                holder.titleView.setText(spawn.nickname);
                                holder.edit.setImageDrawable(edit);
                                holder.editing = !holder.editing;
                                holder.switcher.showNext();

                                View view = act.getCurrentFocus();
                                if (view != null) {
                                    InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                                }

                                return true;
                            }
                            return false;
                        }
                    });*//*
                } else {
                    spawn.nickname = holder.titleEdit.getText().toString();
                    PokeFinderActivity.mapHelper.spawns.get(spawn.id).nickname = holder.titleEdit.getText().toString();

                    holder.titleView.setText(spawn.nickname);
                    holder.edit.setImageDrawable(edit);

                    View view = act.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }

                PokeFinderActivity.features.print("PokeFinder", "Clicked edit button");
                holder.editing = !holder.editing;
            }
        });*/

        PokeFinderActivity.features.print("PokeFinder", "Called getView()");

        return convertView;
    }

    private void moveToSpawn(int position) {
        final ConcurrentHashMap<String, Spawn> mySpawnList = new ConcurrentHashMap<>();
        final Spawn mySpawn = spawns.get(position);
        mySpawnList.put(mySpawn.id, mySpawn);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                PokeFinderActivity.mapHelper.moveMe(mySpawn.lat, mySpawn.lon, true, true);
                PokeFinderActivity.mapHelper.spawnScan(mySpawnList);
            }
        };

        PokeFinderActivity.instance.dontRefreshAccounts = true;
        act.finish();
        PokeFinderActivity.features.runOnMainThread(r);
    }

    private static class ViewHolder {
        public CheckBox checkBox;
        public TextView titleView;
        public TextView location;
        public ImageView edit;
        public ImageView history;
        public ImageView history1;
        public ImageView history2;
        public ImageView history3;
        public ImageView move;
        public ViewSwitcher switcher;
        public EditText titleEdit;

        public ViewSwitcher switcher1;
        public ViewSwitcher switcher2;
        public ViewSwitcher switcher3;

        public TextView number1;
        public TextView number2;
        public TextView number3;

        public boolean editing = false;
    }

    private void setPic(ImageView image, int pokedexNumber, ViewSwitcher switcher, TextView number) {
        String name = PokemonIdOuterClass.PokemonId.forNumber(pokedexNumber).name();
        String filename = pokedexNumber + PokeFinderActivity.mapHelper.IMAGE_EXTENSION;
        String baseFolder = PokeFinderActivity.instance.getFilesDir().getAbsolutePath();
        String customImagesFolder = baseFolder + Features.CUSTOM_IMAGES_FOLDER;
        File file = new File(customImagesFolder + filename);
        if (file.exists()) {
            image.setImageDrawable(Drawable.createFromPath(file.getAbsolutePath()));
            //switcher.setDisplayedChild(1);
        } else {
            //number.setText("#" + pokedexNumber);
            //switcher.setDisplayedChild(0);
            int markerResourceID = act.getResources().getIdentifier(Features.NUMBER_MARKER_PREFIX + pokedexNumber + "", "drawable", act.getPackageName());
            image.setImageResource(markerResourceID);
        }
    }
}
