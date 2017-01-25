package com.logickllc.pokemapper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.MapHelper;
import com.pokegoapi.api.map.Map;

import org.w3c.dom.Text;

public class TunerActivity extends AppCompatActivity {
    final String PREF_SCAN_DISTANCE = "ScanDistance";
    final String PREF_SCAN_TIME = "ScanTime";
    final String PREF_SCAN_SPEED = "ScanSpeed";
    final String PREF_COLLECT_SPAWNS = "CollectSpawns";
    final String PREF_SHOW_IVS = "ShowIvs";

    final int DEFAULT_SCAN_DISTANCE = MapHelper.DEFAULT_SCAN_DISTANCE;
    final int DEFAULT_SCAN_SPEED = MapHelper.DEFAULT_SCAN_SPEED;

    final int DISTANCE_STEP = 10;
    final int SPEED_STEP = 1;

    int scanDistance, scanSpeed, timeFactor = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tuner);

        NativePreferences.lock();
        scanDistance = NativePreferences.getInt(PREF_SCAN_DISTANCE, DEFAULT_SCAN_DISTANCE);
        scanSpeed = NativePreferences.getInt(PREF_SCAN_SPEED, DEFAULT_SCAN_SPEED);
        NativePreferences.unlock();

        PokeFinderActivity.mapHelper.updateScanSettings();

        timeFactor = AccountManager.getGoodAccounts().size();
        if (timeFactor == 0) timeFactor = 1;

        if (scanDistance > MapHelper.MAX_SCAN_DISTANCE) scanDistance = MapHelper.MAX_SCAN_DISTANCE;

        MapHelper.getSpeed(scanDistance);

        if (scanSpeed > MapHelper.maxScanSpeed) scanSpeed = (int) MapHelper.maxScanSpeed;

        final TextView distance = (TextView) findViewById(R.id.distance);
        final TextView speed = (TextView) findViewById(R.id.speed);

        final SeekBar seekDistance = (SeekBar) findViewById(R.id.seekbarDistance);
        seekDistance.setMax(MapHelper.MAX_SCAN_DISTANCE / DISTANCE_STEP);

        final SeekBar seekSpeed = (SeekBar) findViewById(R.id.seekbarTime);
        seekSpeed.setMax(MapHelper.maxScanSpeed / SPEED_STEP);

        final TextView time = (TextView) findViewById(R.id.time);

        seekDistance.setProgress(scanDistance / DISTANCE_STEP);
        seekSpeed.setProgress(scanSpeed / SPEED_STEP);
        distance.setText(scanDistance + "m");

        int scanSpeedMeters = Math.round(scanSpeed * 3.6f);
        int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

        speed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");
        String timeString = PokeFinderActivity.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed / timeFactor) + "s";
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

                //
                int oldMaxSpeed = MapHelper.maxScanSpeed;
                MapHelper.getSpeed(scanDistance);

                seekSpeed.setMax(MapHelper.maxScanSpeed / SPEED_STEP);
                if (oldMaxSpeed != MapHelper.maxScanSpeed) {
                    if (scanSpeed == oldMaxSpeed) {
                        scanSpeed = MapHelper.maxScanSpeed;
                        seekSpeed.setProgress(scanSpeed);
                    }
                }
                //

                int scanSpeedMeters = Math.round(scanSpeed * 3.6f);
                int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

                speed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");

                String timeString = PokeFinderActivity.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed / timeFactor) + "s";
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
                String timeString = PokeFinderActivity.mapHelper.getTimeString(getDistanceTraveled(scanDistance) / scanSpeed / timeFactor) + "s";
                time.setText(timeString.replace(":", "m"));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        TextView close = (TextView) findViewById(R.id.close);
        TextView far = (TextView) findViewById(R.id.far);
        TextView slow = (TextView) findViewById(R.id.slow);
        TextView fast = (TextView) findViewById(R.id.fast);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekDistance.setProgress(seekDistance.getProgress() - 1);
            }
        });

        far.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekDistance.setProgress(seekDistance.getProgress() + 1);
            }
        });

        slow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekSpeed.setProgress(seekSpeed.getProgress() - 1);
            }
        });

        fast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekSpeed.setProgress(seekSpeed.getProgress() + 1);
            }
        });
    }

    public int getDistanceTraveled(int radius) {
        final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MapHelper.MAX_SCAN_RADIUS);
        final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
        final float ITERATIONS = MapHelper.MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

        int hexSectors = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);
        int hexDist = (int) (HEX_DISTANCE * (hexSectors - 1));

        final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
        final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
        int sectors = BOXES_PER_ROW * BOXES_PER_ROW;

        int squareSectors = sectors;
        int squareDist = MINI_SQUARE_SIZE * (squareSectors - 1);

        double squareSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(MINI_SQUARE_SIZE / MapHelper.minScanTime, (double) scanSpeed));
        double hexSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(HEX_DISTANCE / MapHelper.minScanTime, (double) scanSpeed));

        if (hexSectors * hexSpeed <= squareSectors * squareSpeed) return hexDist;
        else return squareDist;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.empty_menu, menu);
        return true;
    }

    @Override
    protected void onPause() {
        NativePreferences.lock();

        NativePreferences.putInt(PREF_SCAN_DISTANCE, scanDistance);
        NativePreferences.putInt(PREF_SCAN_SPEED, scanSpeed);

        NativePreferences.unlock();

        super.onPause();
    }
}
