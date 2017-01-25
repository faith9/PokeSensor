package com.logickllc.pokesensor;


import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.logickllc.pokesensor.api.CustomLock;

import org.robovm.apple.foundation.NSDictionary;
import org.robovm.apple.foundation.NSString;
import org.robovm.apple.foundation.NSUserDefaults;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NativePreferences {
    private static NSUserDefaults prefs;
    private static CustomLock lock;
    private static final boolean LOCKABLE = true;
    private static String location = "";

    public static void init() {
        prefs = NSUserDefaults.getStandardUserDefaults();
        lock = new CustomLock();
        System.out.println("Initializing native preferences...");
    }

    /*public static void lock(String location) {
        if (lock.isHeldByCurrentThread()) return;
        getLock(location);
    }*/

    public static void lock() {
        lock("Unknown");
    }

    public static void lock(String location) {
        if (lock.isLocked() && false) throw new RuntimeException("Trying to lock a locked lock!!!");
        if (lock.isLocked() && true) System.out.println("Trying to lock a locked lock!!!");
        /*try {
            lock.tryLock(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        lock.lock();
        NativePreferences.location = location;
        printLockStatus("Locking " + location);
    }

    public static void unlock() {
        //if (!LOCKABLE) return;
        prefs.synchronize();
        lock.unlock();
        printLockStatus("Unlocking " + location);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        checkLock();

        if (keyExists(key)) return defaultValue;
        else return prefs.getBoolean(key);
    }

    public static String getString(String key, String defaultValue) {
        checkLock();

        if (keyExists(key)) return defaultValue;
        else return prefs.getString(key);
    }

    public static int getInteger(String key, int defaultValue) {
        checkLock();

        if (keyExists(key)) return defaultValue;
        else return prefs.getInt(key);
    }

    public static float getFloat(String key, float defaultValue) {
        checkLock();

        if (keyExists(key)) return defaultValue;
        else return prefs.getFloat(key);
    }

    public static long getLong(String key, long defaultValue) {
        checkLock();

        if (keyExists(key)) return defaultValue;
        else return prefs.getLong(key);
    }

    public static double getDouble(String key, double defaultValue) {
        checkLock();

        if (keyExists(key)) return defaultValue;
        else return prefs.getDouble(key);
    }

    public static void putBoolean(String key, boolean value) {
        checkLock();

        prefs.put(key, value);
    }

    public static void putString(String key, String value) {
        checkLock();

        prefs.put(key, value);
    }

    public static void putInteger(String key, int value) {
        checkLock();

        prefs.put(key, value);
    }

    public static void putFloat(String key, float value) {
        checkLock();

        prefs.put(key, value);
    }

    public static void putLong(String key, long value) {
        checkLock();

        prefs.put(key, value);
    }

    public static void putDouble(String key, double value) {
        checkLock();

        prefs.put(key, value);
    }

    public static void remove(String key) {
        checkLock();

        prefs.remove(key);
    }

    private synchronized static void checkLock() {
        //if (!LOCKABLE) return;
        if (!lock.isLocked()) throw new ConcurrentModificationException("Must lock preferences before reading/writing.");
    }

    private static boolean keyExists(String key) {
        checkLock();

        if (prefs.asDictionary().containsKey(key)) return false;
        else return true;
    }

    public static void copyPrefs(Preferences preferences) {
        checkLock();

        Map<String, ?> objects = preferences.get();
        for (String key : preferences.get().keySet()) {
            Object obj = objects.get(key);
            if (obj instanceof String) prefs.put(key, (String) obj);
            else if (obj instanceof Integer) prefs.put(key, (Integer) obj);
            else if (obj instanceof Float) prefs.put(key, (Float) obj);
            else if (obj instanceof Long) prefs.put(key, (Long) obj);
            else if (obj instanceof Double) prefs.put(key, (Double) obj);
            else if (obj instanceof Boolean) prefs.put(key, (Boolean) obj);
            else System.out.println("Couldn't convert preference " + key);
        }
    }

    public static void printPrefs() {
        System.out.println("\nPrefs List\n------------");
        for (NSString key : prefs.asDictionary().keySet()) {
            System.out.println("Key: " + key.toString() + "\nValue: " + prefs.asDictionary().get(key).toString());
        }
    }

    public synchronized static void printLockStatus(String location) {
        System.out.println("Location: " + location + "\nNativePreferences is locked? " + lock.isLocked() + "\nQueue size: " + lock.getQueueLength());
        if (lock.isLocked()) System.out.println("Lock is held by current thread? " + lock.isHeldByCurrentThread());
    }
}
