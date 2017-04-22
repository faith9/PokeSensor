package com.logickllc.pokemapper;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.logickllc.pokesensor.api.AccountManager;
import com.pokegoapi.util.PokeDictionary;
import com.pokegoapi.util.Signature;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import java.util.Timer;
import java.util.TimerTask;

public class PreferencesActivity extends AppCompatActivity {
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
            addPreferencesFromResource(R.xml.preferences);

            SwitchPreference showIvs = (SwitchPreference) findPreference("ShowIvs");

            toggleIvsAlwaysVisible(showIvs.isChecked());

            showIvs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    toggleIvsAlwaysVisible((boolean) newValue);
                    return true;
                }
            });

            /*SwitchPreference useNewApi = (SwitchPreference) findPreference("UseNewApi");

            useNewApi.setTitle("Use Paid API (" + PokeHashProvider.VERSION_STRING + ")");

            //toggleFallbackApi(useNewApi.isChecked());

            useNewApi.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    //toggleFallbackApi((boolean) newValue);

                    if ((boolean) newValue) {
                        if (!PokeFinderActivity.mapHelper.newApiKey.equals("")) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(act);
                            builder.setTitle("Reuse Key?")
                                    .setMessage("Would you like to use the last key you entered?\nKey: " + PokeFinderActivity.mapHelper.newApiKey)
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // nothing
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            askForApiKey();
                                        }
                                    });
                            builder.create().show();
                        } else {
                            askForApiKey();
                        }
                    }

                    return true;
                }
            });*/

            SwitchPreference use2Captcha = (SwitchPreference) findPreference("Use2Captcha");

            use2Captcha.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean) newValue) {
                        if (!PokeFinderActivity.mapHelper.captchaKey.equals("")) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(act);
                            builder.setTitle("Reuse Key?")
                                    .setMessage("Would you like to use the last key you entered?\nKey: " + PokeFinderActivity.mapHelper.captchaKey)
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // nothing
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            askForCaptchaKey();
                                        }
                                    });
                            builder.create().show();
                        } else {
                            askForCaptchaKey();
                        }
                    }

                    return true;
                }
            });

            ListPreference imageSize = (ListPreference) findPreference("ImageSizeFake");
            imageSize.setValueIndex((int) PokeFinderActivity.mapHelper.imageSize);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            SwitchPreference showSpawns = (SwitchPreference) findPreference("ShowSpawns");
            if (showSpawns.isChecked()) PokeFinderActivity.mapHelper.showSpawnsOnMap();
            else PokeFinderActivity.mapHelper.hideSpawnsOnMap();

            AccountManager.switchHashProvider();
            //Signature.fallbackApi = PokeFinderActivity.mapHelper.fallbackApi;
            Signature.fallbackApi = false;

            ListPreference imageSize = (ListPreference) findPreference("ImageSizeFake");
            PokeFinderActivity.mapHelper.imageSize = Integer.parseInt(imageSize.getValue());

            NativePreferences.lock("saving prefs");
            NativePreferences.putLong(AndroidMapHelper.PREF_IMAGE_SIZE, PokeFinderActivity.mapHelper.imageSize);
            NativePreferences.unlock();

            //PokeFinderActivity.instance.refreshGpsPermissions();
        }

        public void toggleIvsAlwaysVisible(boolean isChecked) {
            SwitchPreference ivsAlwaysVisible = (SwitchPreference) findPreference("IvsAlwaysVisible");
            ivsAlwaysVisible.setEnabled(isChecked);
            SwitchPreference showMovesets = (SwitchPreference) findPreference("ShowMovesets");
            showMovesets.setEnabled(isChecked);
            SwitchPreference showHeightWeight = (SwitchPreference) findPreference("ShowHeightWeight");
            showHeightWeight.setEnabled(isChecked);
        }

        /*public void toggleFallbackApi(boolean isChecked) {
            SwitchPreference fallbackApi = (SwitchPreference) findPreference("FallbackApi");
            fallbackApi.setEnabled(false);
        }*/

        public void askForApiKey() {
            final SwitchPreference useNewApi = (SwitchPreference) findPreference("UseNewApi");

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                    View view = act.getLayoutInflater().inflate(R.layout.input_box, null);

                    TextView message = (TextView) view.findViewById(R.id.message);
                    final EditText input = (EditText) view.findViewById(R.id.input);

                    message.setText("Enter your PokeHash API key. This will let you use the latest reversed API provided by the PokeFarmer devs for a fee. Note this is not my system. It can be buggy/unavailable at times and I can't do anything about it.");
                    input.setText(PokeFinderActivity.mapHelper.newApiKey);
                    //input.selectAll();

                    builder.setTitle("Enter API Key")
                            .setView(view)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String text = input.getText().toString();

                                    if (text == null || text.equals("")) {
                                        useNewApi.setChecked(false);
                                        //toggleFallbackApi(false);
                                        return;
                                    }

                                    PokeFinderActivity.mapHelper.newApiKey = text;
                                    Signature.validApiKey = null;
                                    NativePreferences.lock();
                                    NativePreferences.putString(AndroidMapHelper.PREF_NEW_API_KEY, PokeFinderActivity.mapHelper.newApiKey);
                                    NativePreferences.unlock();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    useNewApi.setChecked(false);
                                    //toggleFallbackApi(false);
                                }
                            });
                    builder.create().show();
                }
            };

            PokeFinderActivity.features.runOnMainThread(runnable);
        }

        public void askForCaptchaKey() {
            final SwitchPreference use2Captcha = (SwitchPreference) findPreference("Use2Captcha");

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                    View view = act.getLayoutInflater().inflate(R.layout.input_box, null);

                    TextView message = (TextView) view.findViewById(R.id.message);
                    final EditText input = (EditText) view.findViewById(R.id.input);

                    message.setText("Enter your 2Captcha API key. 2Captcha is a paid service that will automatically solve captchas for you.");
                    input.setText(PokeFinderActivity.mapHelper.captchaKey);
                    //input.selectAll();

                    builder.setTitle("Enter API Key")
                            .setView(view)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String text = input.getText().toString();

                                    if (text == null || text.equals("")) {
                                        use2Captcha.setChecked(false);
                                        return;
                                    }

                                    PokeFinderActivity.mapHelper.captchaKey = text;
                                    NativePreferences.lock();
                                    NativePreferences.putString(AndroidMapHelper.PREF_2CAPTCHA_KEY, PokeFinderActivity.mapHelper.captchaKey);
                                    NativePreferences.unlock();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    use2Captcha.setChecked(false);
                                }
                            });
                    builder.create().show();
                }
            };

            PokeFinderActivity.features.runOnMainThread(runnable);
        }
    }
}
