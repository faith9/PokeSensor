package com.logickllc.pokesensor;

import com.badlogic.gdx.Gdx;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class Stopwatch {
    private static boolean enabled = false;
    public static String times = "";
    private static ConcurrentHashMap<String, Long> stopwatches = new ConcurrentHashMap<>();
    public static String filter = "";
    public static long minTime = 0;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        Stopwatch.enabled = enabled;
    }

    public static void click(String tag) {
        if (!enabled) return;
        tag += "|" + Thread.currentThread().getName();
        if (stopwatches.containsKey(tag)) {
            printTime(tag);
            stopwatches.remove(tag);
        } else {
            stopwatches.put(tag, System.currentTimeMillis());
        }
    }

    private static void printTime(String tag) {
        if (!enabled) return;
        long start = stopwatches.get(tag);
        long end = System.currentTimeMillis();
        long timeMs = end - start;

        if (!tag.contains(filter) || timeMs < minTime) return;

        String output = tag + " took " + timeMs + "ms to complete";
        if (IOSLauncher.IS_AD_TESTING) System.out.println(output);

        times += output + "\n";
    }

    public static void printAllTimes() {
        if (!enabled) return;
        if (IOSLauncher.IS_AD_TESTING) {
            File file = Gdx.files.local("times.txt").file();
            try {
                FileUtils.writeStringToFile(file, times);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void readPreviousTimes() {
        if (!enabled) return;
        if (IOSLauncher.IS_AD_TESTING) {
            File file = Gdx.files.local("times.txt").file();
            try {
                String output = FileUtils.readFileToString(file);
                System.out.println("\nPrevious stopwatch times:\n\n" + output + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
