package com.logickllc.pokemapper;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.logickllc.pokesensor.api.Spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MySpawnsActivity extends AppCompatActivity {
    ListView list;
    MySpawnsAdapter adapter;
    ArrayList<Spawn> spawns;
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_spawns);

        list = (ListView) findViewById(R.id.spawnsList);

        if (!PokeFinderActivity.mapHelper.spawns.isEmpty()) {
            spawns = new ArrayList<>(PokeFinderActivity.mapHelper.spawns.values());
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
            adapter = new MySpawnsAdapter(spawns, this);

            list.setAdapter(adapter);
        } else {
            showEmptySpawnMessage();
        }
        final Activity act = this;
        list.setLongClickable(true);
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                // Show login screen
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle("Delete Account?");
                //builder.setMessage(R.string.loginMessage);
                builder.setMessage("Are you sure you want to delete " + spawns.get(position).nickname + "?");

                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Spawn spawn = spawns.remove(position);
                        PokeFinderActivity.mapHelper.spawns.remove(spawn.id);
                        adapter.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing
                    }
                });

                try {
                    builder.create().show();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_delete_spawns:
                deleteAll();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.spawn_menu, menu);
        this.menu = menu;

        return true;
    }

    public void deleteAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete All Spawns?").setMessage("Are you sure you want to delete all collected spawn points?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PokeFinderActivity.mapHelper.deleteAllSpawns();
                        showEmptySpawnMessage();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });

        builder.create().show();
    }

    public void showEmptySpawnMessage() {
        list.setVisibility(View.GONE);

        TextView emptyMessage = (TextView) findViewById(R.id.emptyMessage);
        emptyMessage.setVisibility(View.VISIBLE);

        if (PokeFinderActivity.mapHelper.collectSpawns) {
            emptyMessage.setText("You haven't collected any spawns yet. Spawn points will be automatically collected when you scan, and then they will show up here.");
        } else {
            emptyMessage.setText("You haven't collected any spawns and you have disabled spawn collecting. You can enable automatic spawn point collecting via the Scan Settings menu");
        }
    }
}
