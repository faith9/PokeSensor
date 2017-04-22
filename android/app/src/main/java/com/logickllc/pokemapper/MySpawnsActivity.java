package com.logickllc.pokemapper;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.logickllc.pokesensor.api.Spawn;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

            case R.id.action_import_spawns:
                importSpawns();
                return true;

            case R.id.action_export_spawns:
                exportSpawns();
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

    public void importSpawns() {
        // Show login screen
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Import Spawns");
        final View view = getLayoutInflater().inflate(R.layout.import_accounts, null);

        TextView textView = (TextView) view.findViewById(R.id.importMessage);
        textView.setText("Enter the spawn data EXACTLY as it was exported from PokeSensor.");

        builder.setView(view);
        final Activity act = this;

        builder.setPositiveButton("Import", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                EditText csv = (EditText) view.findViewById(R.id.csv);
                String csvText = csv.getText().toString().trim();

                if (csvText.equals("")) {
                    PokeFinderActivity.features.longMessage("Um...that can't be blank");
                    return;
                }

                try {
                    if (!csvText.equals("")) {
                        ArrayList<Spawn> spawnList;
                        try {
                            Type type = new TypeToken<List<Spawn>>(){}.getType();
                            Gson gson = new Gson();
                            spawnList = gson.fromJson(csvText, type);
                            if (spawnList == null) {
                                return;
                            }

                            for (int n = 0; n < spawnList.size(); n++) {
                                Spawn spawn = spawnList.get(n);
                                if (!PokeFinderActivity.mapHelper.spawns.contains(spawn)) PokeFinderActivity.mapHelper.showSpawnOnMap(spawn);
                                PokeFinderActivity.mapHelper.spawns.put(spawn.id, spawn);
                            }

                            PokeFinderActivity.mapHelper.saveSpawns();
                            spawns = new ArrayList<>(PokeFinderActivity.mapHelper.spawns.values());
                            adapter.spawns = spawns;
                        } catch (Exception e) {
                            e.printStackTrace();
                            Crashlytics.logException(e);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
    }

    public void exportSpawns() {
        ArrayList<Spawn> spawnList = new ArrayList<Spawn>(PokeFinderActivity.mapHelper.spawns.values());
        Type type = new TypeToken<List<Spawn>>(){}.getType();
        Gson gson = new Gson();
        String text = gson.toJson(spawnList, type);
        shareText(text);
    }

    public void shareText(String text) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);

        this.startActivity(Intent.createChooser(shareIntent, "Choose how to share"));
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
