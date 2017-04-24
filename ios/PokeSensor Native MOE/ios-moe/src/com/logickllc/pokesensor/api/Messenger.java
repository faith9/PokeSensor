package com.logickllc.pokesensor.api;


import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.logickllc.pokesensor.DialogHelper;
import com.logickllc.pokesensor.MapController;
import com.logickllc.pokesensor.NativePreferences;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

public class Messenger {
    public static final String PREF_LAST_MESSAGE_ID = "LastMessageId";
    public static int lastMessageId = -1;
    public static final String MESSAGES_URL = "https://raw.githubusercontent.com/MrPat/CardDragTest/master/error_log.txt";
    public static final String MESSAGES_FILE = "messages.txt";
    public static Messages messages = null;

    public static void fetchMessages() {
        try {
            File file = Gdx.files.local(MESSAGES_FILE).file();
            FileUtils.copyURLToFile(new URL(MESSAGES_URL), file);
            String messagesJson = FileUtils.readFileToString(file);
            Gson gson = new Gson();
            messages = gson.fromJson(messagesJson, Messages.class);

            NativePreferences.lock("load last message id");
            lastMessageId = NativePreferences.getInteger(PREF_LAST_MESSAGE_ID, -1);
            NativePreferences.unlock();

            for (final Message message : messages.messages.values()) {
                if (message.id > lastMessageId && message.minVersionIOS <= MapController.instance.getAppVersion()
                        && message.maxVersionIOS >= MapController.instance.getAppVersion()) {
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
                                            Gdx.net.openURI(message.buttons.get(finalButton1));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };
                                Gdx.app.postRunnable(runnable);
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
                                                Gdx.net.openURI(message.buttons.get(finalButton2));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    };
                                    Gdx.app.postRunnable(runnable);
                                }
                            };
                        }

                        final Runnable finalNegative = negative;

                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                DialogHelper.yesNoBox(message.title, message.message, finalButton1, finalPositive, finalButton2, finalNegative).build().show();
                            }
                        };
                        MapController.features.runOnMainThread(runnable);
                    } else {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                DialogHelper.messageBox(message.title, message.message, finalButton1, finalPositive).build().show();
                            }
                        };
                        MapController.features.runOnMainThread(runnable);
                    }

                    NativePreferences.lock("save last message id");
                    NativePreferences.putInteger(PREF_LAST_MESSAGE_ID, lastMessageId);
                    NativePreferences.unlock();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
