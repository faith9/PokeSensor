package com.logickllc.pokemapper;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.logickllc.pokesensor.api.Features;

public class IVFilterActivity extends AppCompatActivity {
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ivfilter);

        TextView minAttack = (TextView) findViewById(R.id.minAttack);
        TextView minDefense = (TextView) findViewById(R.id.minDefense);
        TextView minStamina = (TextView) findViewById(R.id.minStamina);
        TextView minPercent = (TextView) findViewById(R.id.minPercent);
        final TextView minOverride = (TextView) findViewById(R.id.minOverride);

        Switch minOverrideEnabled = (Switch) findViewById(R.id.minOverrideEnabled);

        minAttack.setText(PokeFinderActivity.mapHelper.getMinAttack() + "");
        minDefense.setText(PokeFinderActivity.mapHelper.getMinDefense() + "");
        minStamina.setText(PokeFinderActivity.mapHelper.getMinStamina() + "");
        minPercent.setText(PokeFinderActivity.mapHelper.getMinPercent() + "");
        minOverride.setText(PokeFinderActivity.mapHelper.getMinOverride() + "");

        minOverrideEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                minOverride.setEnabled(isChecked);
            }
        });

        minOverrideEnabled.setChecked(PokeFinderActivity.mapHelper.overrideEnabled);
        minOverride.setEnabled(minOverrideEnabled.isChecked());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_override_ivs:
                Intent intent = new Intent(this, IVOverrideActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.iv_filter_menu, menu);
        this.menu = menu;

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        TextView minAttack = (TextView) findViewById(R.id.minAttack);
        TextView minDefense = (TextView) findViewById(R.id.minDefense);
        TextView minStamina = (TextView) findViewById(R.id.minStamina);
        TextView minPercent = (TextView) findViewById(R.id.minPercent);
        TextView minOverride = (TextView) findViewById(R.id.minOverride);

        Switch minOverrideEnabled = (Switch) findViewById(R.id.minOverrideEnabled);

        PokeFinderActivity.mapHelper.setMinAttack(validateIV(minAttack, PokeFinderActivity.mapHelper.getMinAttack()));
        PokeFinderActivity.mapHelper.setMinDefense(validateIV(minDefense, PokeFinderActivity.mapHelper.getMinDefense()));
        PokeFinderActivity.mapHelper.setMinStamina(validateIV(minStamina, PokeFinderActivity.mapHelper.getMinStamina()));
        PokeFinderActivity.mapHelper.setMinPercent(validatePercent(minPercent, PokeFinderActivity.mapHelper.getMinPercent()));
        PokeFinderActivity.mapHelper.setMinOverride(validatePercent(minOverride, PokeFinderActivity.mapHelper.getMinOverride()));

        PokeFinderActivity.mapHelper.overrideEnabled = minOverrideEnabled.isChecked();

        PokeFinderActivity.mapHelper.saveIVFilters();
    }

    public int validatePercent(TextView field, int backup) {
        int result;
        try {
            result = Integer.parseInt(field.getText().toString());
        } catch (Exception e) {
            result = backup;
        }

        if (result < 0) result = 0;
        if (result > 100) result = 100;

        return result;
    }

    public int validateIV(TextView field, int backup) {
        int result;
        try {
            result = Integer.parseInt(field.getText().toString());
        } catch (Exception e) {
            result = backup;
        }

        if (result < 0) result = 0;
        if (result > 15) result = 15;

        return result;
    }
}
