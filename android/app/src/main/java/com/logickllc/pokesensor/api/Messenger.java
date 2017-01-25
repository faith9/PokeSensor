package com.logickllc.pokesensor.api;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import com.google.gson.Gson;
import com.logickllc.pokemapper.NativePreferences;
import com.logickllc.pokemapper.PokeFinderActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

public class Messenger {
    public static final String PREF_LAST_MESSAGE_ID = "LastMessageId";
    public static int lastMessageId = -1;
    public static final String MESSAGES_URL = "YOUR_MESSAGE_URL";
    public static final String MESSAGES_FILE = "messages.txt";
    public static Messages messages = null;

    public static void fetchMessages() {
        try {
            String baseFolder = PokeFinderActivity.instance.getFilesDir().getAbsolutePath();
            File file = new File(baseFolder + "/" + MESSAGES_FILE);
            FileUtils.copyURLToFile(new URL(MESSAGES_URL), file);
            String messagesJson = FileUtils.readFileToString(file);
            Gson gson = new Gson();
            messages = gson.fromJson(messagesJson, Messages.class);

            NativePreferences.lock("load last message id");
            lastMessageId = NativePreferences.getInt(PREF_LAST_MESSAGE_ID, -1);
            NativePreferences.unlock();

            for (final Message message : messages.messages.values()) {
                if (message.id > lastMessageId && message.minVersionAndroid <= PokeFinderActivity.instance.getAppVersion()
                        && message.maxVersionAndroid >= PokeFinderActivity.instance.getAppVersion()) {
                    lastMessageId = message.id;
                    String button1 = null, button2 = null;
                    for (String key : message.buttons.keySet()) {
                        if (button1 == null) {
                            button1 = key;
                            continue;
                        }
                        if (button1 != null && button2 == null) {
                            button2 = key;
                            break;
                        }
                    }

                    final String finalButton1 = button1, finalButton2 = button2;
                    Runnable positive = null, negative = null;
                    if (!message.buttons.get(button1).equals("")) {
                        positive = new Runnable() {
                            @Override
                            public void run() {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            PokeFinderActivity.instance.startActivity(new Intent(Intent.ACTION_VIEW,
                                                    Uri.parse(message.buttons.get(finalButton1))));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };
                                PokeFinderActivity.features.runOnMainThread(runnable);
                            }
                        };
                    }

                    final Runnable finalPositive = positive;

                    if (message.buttons.size() >= 2) {
                        if (!message.buttons.get(button2).equals("")) {
                            negative = new Runnable() {
                                @Override
                                public void run() {
                                    Runnable runnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                PokeFinderActivity.instance.startActivity(new Intent(Intent.ACTION_VIEW,
                                                        Uri.parse(message.buttons.get(finalButton2))));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    };
                                    PokeFinderActivity.features.runOnMainThread(runnable);
                                }
                            };
                        }

                        final Runnable finalNegative = negative;

                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(PokeFinderActivity.instance);
                                builder.setTitle(message.title);
                                builder.setMessage(message.message);
                                builder.setPositiveButton(finalButton1, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (finalPositive != null) finalPositive.run();
                                    }
                                });
                                builder.setNegativeButton(finalButton2, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (finalNegative != null) finalNegative.run();
                                    }
                                });
                                try {
                                    builder.create().show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        PokeFinderActivity.features.runOnMainThread(r);
                    } else {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(PokeFinderActivity.instance);
                                builder.setTitle(message.title);
                                builder.setMessage(message.message);
                                builder.setPositiveButton(finalButton1, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (finalPositive != null) finalPositive.run();
                                    }
                                });
                                try {
                                    builder.create().show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        PokeFinderActivity.features.runOnMainThread(r);
                    }

                    NativePreferences.lock("save last message id");
                    NativePreferences.putInt(PREF_LAST_MESSAGE_ID, lastMessageId);
                    NativePreferences.unlock();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
