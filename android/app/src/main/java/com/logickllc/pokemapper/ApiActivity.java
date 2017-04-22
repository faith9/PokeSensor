package com.logickllc.pokemapper;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.logickllc.pokesensor.api.AccountManager;
import com.logickllc.pokesensor.api.Features;
import com.logickllc.pokesensor.api.MapHelper;
import com.pokegoapi.util.Signature;

public class ApiActivity extends AppCompatActivity {
    public static final String PREF_EXPECTED_POKEMON = "ExpectedPokemon";
    public static final int REQUESTS_PER_LOGIN = 6;
    public static final int REQUESTS_PER_SECTOR_SCAN = 1;
    public static final int REQUESTS_PER_IV_SCAN = 1;
    public static final int MIN_SCAN_TIME = 10;
    public static final double SCANS_PER_MINUTE = 60 / MIN_SCAN_TIME;

    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);

        EditText apiKey = (EditText) findViewById(R.id.apiKey);
        apiKey.setText(PokeFinderActivity.mapHelper.newApiKey);

        EditText numAccounts = (EditText) findViewById(R.id.numAccounts);
        numAccounts.setText(AccountManager.getNumAccounts() + "");

        NativePreferences.lock();
        int numPokemonInt = NativePreferences.getInt(PREF_EXPECTED_POKEMON, 25);
        NativePreferences.unlock();
        EditText numPokemon = (EditText) findViewById(R.id.numPokemon);
        numPokemon.setText(numPokemonInt + "");

        EditText scanRadius = (EditText) findViewById(R.id.scanRadius);
        scanRadius.setText(PokeFinderActivity.mapHelper.getScanDistance() + "");

        calculate();

        Button calculate = (Button) findViewById(R.id.calculateRpm);
        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_buy_api:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://hashing.pogodev.org")));
                return true;

            case R.id.action_api_help:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(AndroidMapHelper.PAID_API_HELP_PAGE_URL)));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.api_menu, menu);
        this.menu = menu;

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        EditText numPokemon = (EditText) findViewById(R.id.numPokemon);
        NativePreferences.lock();
        try {
            int numPokemonInt = Integer.parseInt(numPokemon.getText().toString());
            NativePreferences.putInt(PREF_EXPECTED_POKEMON, numPokemonInt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        NativePreferences.unlock();

        EditText apiKey = (EditText) findViewById(R.id.apiKey);
        if (!apiKey.getText().toString().equals("") && !apiKey.getText().toString().equals(PokeFinderActivity.mapHelper.newApiKey)) {
            NativePreferences.lock();
            NativePreferences.putString(AndroidMapHelper.PREF_NEW_API_KEY, apiKey.getText().toString());
            NativePreferences.unlock();

            PokeFinderActivity.mapHelper.newApiKey = apiKey.getText().toString();
            Signature.validApiKey = null;

            AccountManager.tryTalkingToServer();
        }
    }

    public void calculate() {
        TextView loginRpm = (TextView) findViewById(R.id.loginRpm);
        TextView scanRpm = (TextView) findViewById(R.id.scanRpm);
        TextView ivRpm = (TextView) findViewById(R.id.ivRpm);

        EditText numAccounts = (EditText) findViewById(R.id.numAccounts);
        EditText numPokemon = (EditText) findViewById(R.id.numPokemon);
        EditText scanRadius = (EditText) findViewById(R.id.scanRadius);

        int numAccountsInt, numPokemonInt, scanRadiusInt;

        try {
            numAccountsInt = Math.abs(Integer.parseInt(numAccounts.getText().toString()));
        } catch (Exception e) {
            e.printStackTrace();
            PokeFinderActivity.features.shortMessage("Accounts must be a valid number!");
            return;
        }

        try {
            numPokemonInt = Math.abs(Integer.parseInt(numPokemon.getText().toString()));
        } catch (Exception e) {
            e.printStackTrace();
            PokeFinderActivity.features.shortMessage("Expected Pokemon must be a valid number!");
            return;
        }

        try {
            scanRadiusInt = Math.abs(Integer.parseInt(scanRadius.getText().toString()));
        } catch (Exception e) {
            e.printStackTrace();
            PokeFinderActivity.features.shortMessage("Scan radius must be a valid number!");
            return;
        }

        if (scanRadiusInt > MapHelper.MAX_SCAN_DISTANCE) {
            PokeFinderActivity.features.shortMessage("Scan radius must be less than the max radius (" + MapHelper.MAX_SCAN_DISTANCE + " m)");
            return;
        }

        loginRpm.setText((numAccountsInt * REQUESTS_PER_LOGIN) + "");

        int scanRpmInt = ((int) Math.min(REQUESTS_PER_SECTOR_SCAN * SCANS_PER_MINUTE * numAccountsInt, getExpectedSectors(scanRadiusInt) * REQUESTS_PER_SECTOR_SCAN));
        scanRpm.setText(scanRpmInt + "");

        double temp = numPokemonInt * REQUESTS_PER_IV_SCAN / (getExpectedTime(scanRadiusInt) / numAccountsInt);
        int ivScanRpmInt = (int) Math.ceil(temp);
        ivRpm.setText(ivScanRpmInt + scanRpmInt + "");
    }

    private int getExpectedSectors(int radius) {
        final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MapHelper.MAX_SCAN_RADIUS);
        final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
        final float ITERATIONS = MapHelper.MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

        int hexSectors = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);

        final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
        final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
        int sectors = BOXES_PER_ROW * BOXES_PER_ROW;

        int squareSectors = sectors;

        int squareSize = MINI_SQUARE_SIZE;
        int hexSize = (int) HEX_DISTANCE;

        double squareSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(MINI_SQUARE_SIZE / MapHelper.minScanTime, (double) PokeFinderActivity.mapHelper.getScanSpeed()));
        double hexSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(HEX_DISTANCE / MapHelper.minScanTime, (double) PokeFinderActivity.mapHelper.getScanSpeed()));

        if (hexSectors * hexSpeed <= squareSectors * squareSpeed) return hexSectors;
        else return squareSectors;
    }

    private double getExpectedTime(int radius) {
        final float HEX_DISTANCE = (float) (int) (Math.sqrt(3)*MapHelper.MAX_SCAN_RADIUS);
        final float BIG_HEX_SIZE = 2*radius / (float) Math.sqrt(3);
        final float ITERATIONS = MapHelper.MAX_SCAN_RADIUS < radius ? (float) Math.ceil(BIG_HEX_SIZE / HEX_DISTANCE) + 1 : 1;

        int hexSectors = (int) (3*Math.pow(ITERATIONS - 1, 2) + 3*(ITERATIONS - 1) + 1);

        final int MINI_SQUARE_SIZE = (int) Math.sqrt(Math.pow(MapHelper.MAX_SCAN_RADIUS * 2, 2) / 2);
        final int BOXES_PER_ROW = (int) Math.ceil(2 * radius / (float) MINI_SQUARE_SIZE);
        int sectors = BOXES_PER_ROW * BOXES_PER_ROW;

        int squareSectors = sectors;

        int squareSize = MINI_SQUARE_SIZE;
        int hexSize = (int) HEX_DISTANCE;

        double squareSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(MINI_SQUARE_SIZE / MapHelper.minScanTime, (double) PokeFinderActivity.mapHelper.getScanSpeed()));
        double hexSpeed = Math.min((double) MapHelper.SPEED_CAP, Math.min(HEX_DISTANCE / MapHelper.minScanTime, (double) PokeFinderActivity.mapHelper.getScanSpeed()));

        double seconds = 0;

        if (hexSectors * hexSpeed <= squareSectors * squareSpeed) seconds = hexSectors * hexSpeed;
        else seconds = squareSectors * squareSpeed;

        int minutes = (int) Math.ceil(seconds / 60);
        return minutes;
    }
}
