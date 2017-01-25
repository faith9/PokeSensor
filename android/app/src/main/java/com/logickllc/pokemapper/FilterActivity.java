package com.logickllc.pokemapper;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.Spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FilterActivity extends AppCompatActivity {
    ListView list;
    FilterAdapter adapter;
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        list = (ListView) findViewById(R.id.filterList);

        adapter = new FilterAdapter(this);

        list.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_select_all:
                for (int n = 1; n <= Features.NUM_POKEMON; n++) {
                    PokeFinderActivity.features.filter.put(n, true);
                }
                adapter.notifyDataSetChanged();
                return true;

            case R.id.action_deselect_all:
                for (int n = 1; n <= Features.NUM_POKEMON; n++) {
                    PokeFinderActivity.features.filter.put(n, false);
                }
                adapter.notifyDataSetChanged();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filter_menu, menu);
        this.menu = menu;

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PokeFinderActivity.features.saveFilter();
    }
}
