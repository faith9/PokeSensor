package com.logickllc.pokemapper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.logickllc.pokesensor.api.Features;

public class NotificationFilterActivity extends AppCompatActivity {
    ListView list;
    NotificationFilterAdapter adapter;
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_filter);

        list = (ListView) findViewById(R.id.filterList);

        adapter = new NotificationFilterAdapter(this);

        list.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_select_all:
                for (int n = 1; n <= Features.NUM_POKEMON; n++) {
                    PokeFinderActivity.features.notificationFilter.put(n, true);
                }
                adapter.notifyDataSetChanged();
                return true;

            case R.id.action_deselect_all:
                for (int n = 1; n <= Features.NUM_POKEMON; n++) {
                    PokeFinderActivity.features.notificationFilter.put(n, false);
                }
                adapter.notifyDataSetChanged();
                return true;

            case R.id.action_copy_pokemon_filter:
                for (int n = 1; n <= Features.NUM_POKEMON; n++) {
                    PokeFinderActivity.features.notificationFilter.put(n, PokeFinderActivity.features.filter.get(n));
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
        inflater.inflate(R.menu.notification_filter_menu, menu);
        this.menu = menu;

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PokeFinderActivity.features.saveNotificationFilter();
    }
}
