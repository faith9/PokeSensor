package com.logickllc.pokemapper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SeekBar;
import android.widget.TextView;

import com.logickllc.pokesensor.api.MapHelper;
import com.pokegoapi.api.map.Map;

public class TunerActivity extends AppCompatActivity {
    final String PREF_SCAN_DISTANCE = "ScanDistance";
    final String PREF_SCAN_TIME = "ScanTime";
    final String PREF_SCAN_SPEED = "ScanSpeed";

    final int DEFAULT_SCAN_DISTANCE = MapHelper.DEFAULT_SCAN_DISTANCE;
    final int DEFAULT_SCAN_SPEED = MapHelper.DEFAULT_SCAN_SPEED;

    final int DISTANCE_STEP = 10;
    final int SPEED_STEP = 1;

    int scanDistance, scanSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tuner);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        scanDistance = prefs.getInt(PREF_SCAN_DISTANCE, DEFAULT_SCAN_DISTANCE);
        scanSpeed = prefs.getInt(PREF_SCAN_TIME, DEFAULT_SCAN_SPEED);

        PokeFinderActivity.mapHelper.updateScanSettings();

        if (scanDistance > MapHelper.MAX_SCAN_DISTANCE) scanDistance = MapHelper.MAX_SCAN_DISTANCE;
        if (scanSpeed > MapHelper.maxScanSpeed) scanSpeed = (int) MapHelper.maxScanSpeed;

        final TextView distance = (TextView) findViewById(R.id.distance);
        final TextView speed = (TextView) findViewById(R.id.speed);

        SeekBar seekDistance = (SeekBar) findViewById(R.id.seekbarDistance);
        seekDistance.setMax(MapHelper.MAX_SCAN_DISTANCE / DISTANCE_STEP);

        SeekBar seekSpeed = (SeekBar) findViewById(R.id.seekbarTime);
        seekSpeed.setMax(MapHelper.maxScanSpeed / SPEED_STEP);

        final TextView time = (TextView) findViewById(R.id.time);

        seekDistance.setProgress(scanDistance / DISTANCE_STEP);
        seekSpeed.setProgress(scanSpeed / SPEED_STEP);
        distance.setText(scanDistance + "m");

        int scanSpeedMeters = Math.round(scanSpeed * 3.6f);
        int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

        speed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");
        String timeString = PokeFinderActivity.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed) + "s";
        time.setText(timeString.replace(":", "m"));

        seekDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress * DISTANCE_STEP < 10) {
                    seekBar.setProgress(10 / DISTANCE_STEP);
                    return;
                }
                scanDistance = progress * DISTANCE_STEP;
                distance.setText(scanDistance + "m");

                String timeString = PokeFinderActivity.mapHelper.getTimeString(Math.round(getDistanceTraveled(scanDistance) / (float) scanSpeed)) + "s";
                time.setText(timeString.replace(":", "m"));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress * SPEED_STEP < 1) {
                    seekBar.setProgress(1 / SPEED_STEP);
                    return;
                }

                scanSpeed = progress * SPEED_STEP;
                int scanSpeedMeters = Math.round(scanSpeed * 3.6f);
                int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

                speed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");
                String timeString = PokeFinderActivity.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed) + "s";
                time.setText(timeString.replace(":", "m"));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public int getDistanceTraveled(int radius) {
        final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
        final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
        int sectors = BOXES_PER_ROW * BOXES_PER_ROW;

        return MINI_SQUARE_SIZE * (sectors - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.empty_menu, menu);
        return true;
    }

    @Override
    protected void onPause() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_SCAN_DISTANCE, scanDistance);
        editor.putInt(PREF_SCAN_SPEED, scanSpeed);
        editor.commit();

        super.onPause();
    }
}
