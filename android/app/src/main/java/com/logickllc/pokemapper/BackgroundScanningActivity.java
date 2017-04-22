package com.logickllc.pokemapper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

public class BackgroundScanningActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        Activity act;

        public SettingsFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            act = this.getActivity();

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.background_scanning);

            Preference notificationFilter = findPreference("FakeNotificationFilter");
            notificationFilter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(act, NotificationFilterActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
        }

        @Override
        public void onStop() {
            super.onStop();
            PokeFinderActivity.mapHelper.refreshPrefs();
        }
    }
}
