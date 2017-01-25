package com.logickllc.pokemapper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.logickllc.pokesensor.api.Features;

public class IVOverrideActivity extends AppCompatActivity {
    ListView list;
    IVOverrideAdapter adapter;
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        list = (ListView) findViewById(R.id.filterList);

        adapter = new IVOverrideAdapter(this);

        list.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_select_all:
                for (int n = 1; n <= Features.NUM_POKEMON; n++) {
                    PokeFinderActivity.features.filterOverrides.put(n, true);
                }
                adapter.notifyDataSetChanged();
                return true;

            case R.id.action_deselect_all:
                for (int n = 1; n <= Features.NUM_POKEMON; n++) {
                    PokeFinderActivity.features.filterOverrides.put(n, false);
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
        inflater.inflate(R.menu.iv_override_menu, menu);
        this.menu = menu;

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PokeFinderActivity.features.saveFilterOverrides();
    }
}
