package com.logickllc.pokemapper;


import android.content.SharedPreferences;

import com.logickllc.pokesensor.api.CustomLock;

import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.ReentrantLock;

public class NativePreferences {
    private static SharedPreferences prefs;
    private static SharedPreferences.Editor editor;
    private static CustomLock lock;
    private static String location = "";

    public static void init(SharedPreferences myPrefs) {
        prefs = myPrefs;
        lock = new CustomLock();
        System.out.println("Initializing native preferences...");
    }

    public static void lock() {
        lock("Unknown");
    }

    public static void lock(String location) {
        if (lock.isLocked() && true) System.out.println("Trying to lock a locked lock!!!");
        editor = prefs.edit();
        lock.lock();
        NativePreferences.location = location;
        printLockStatus("Locking " + location);
    }

    public static void unlock() {
        editor.apply();
        lock.unlock();
        printLockStatus("Unlocking " + location);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        checkLock();

        return prefs.getBoolean(key, defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        checkLock();

        return prefs.getString(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        checkLock();

        return prefs.getInt(key, defaultValue);
    }

    public static float getFloat(String key, float defaultValue) {
        checkLock();

        return prefs.getFloat(key, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        checkLock();

        return prefs.getLong(key, defaultValue);
    }

    public static void putBoolean(String key, boolean value) {
        checkLock();

        editor.putBoolean(key, value);
    }

    public static void putString(String key, String value) {
        checkLock();

        editor.putString(key, value);
    }

    public static void putInt(String key, int value) {
        checkLock();

        editor.putInt(key, value);
    }

    public static void putFloat(String key, float value) {
        checkLock();

        editor.putFloat(key, value);
    }

    public static void putLong(String key, long value) {
        checkLock();

        editor.putLong(key, value);
    }

    public static void remove(String key) {
        checkLock();

        editor.remove(key);
    }

    private static void checkLock() {
        if (!lock.isLocked()) throw new ConcurrentModificationException("Must lock preferences before reading/writing.");
    }

    public synchronized static void printLockStatus(String location) {
        System.out.println("Location: " + location + "\nNativePreferences is locked? " + lock.isLocked() + "\nQueue size: " + lock.getQueueLength());
        if (lock.isLocked()) System.out.println("Lock is held by current thread? " + lock.isHeldByCurrentThread());
    }
}
