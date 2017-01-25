package com.logickllc.pokesensor;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.custom.HWMachine;
import com.twocaptcha.http.HttpWrapper;

import org.apache.commons.io.FileUtils;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSException;
import org.robovm.apple.foundation.NSString;
import org.robovm.apple.uikit.UIDevice;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ErrorReporter {
    private static final String SERVER = "YOUR_SERVER_HERE";
    private static final String LEFTOVERS = "leftovers.txt";
    private static final int MAX_LEFTOVERS_PER_SEND = 10;

    public static void logExceptionThreaded(final Throwable t, final Map<String, String> extras) {
        Thread thread = new Thread() {
            public void run() {
                logException(t, extras);
            }
        };
        thread.start();
    }

    public static void logException(final Throwable t, final Map<String, String> extras) {
        try {
            MapController.features.print("ErrorReporter", "Preparing Java error report for " + t.getClass().getName());
            Map<String, String> params = new HashMap<String, String>();
            String ios = UIDevice.getCurrentDevice().getSystemVersion();
            String device = getDeviceModel();
            String version = MapController.instance.getAppVersion() + "";
            String stacktrace = "";

            ArrayList<StackTraceElement> stack = new ArrayList<>();
            Throwable temp = t;
            while (temp != null) {
                StackTraceElement[] elements = temp.getStackTrace();
                for (int n = 0; n < elements.length; n++) {
                    stack.add(elements[n]);
                    //stacktrace += elements[n].toString() + "\n";
                }
                stacktrace += MapController.features.getStackTraceString(temp) + "\n";
                temp = temp.getCause();
                MapController.features.print("ErrorReporter", "Making another iteration in the stack trace...");
            }

            int counter = 0;
            double usableParams = (extras == null) ? 10.0 : 9.0;
            int stacksPerParam = (int) Math.ceil(stack.size() / usableParams);
            for (int n = 0; n < stack.size(); n++) {
                for (int x = 0; x < stacksPerParam; x++) {
                    if (n + x >= stack.size()) break;
                    params.put(counter + "", stack.get(n + x).toString());
                    if (x < stacksPerParam - 1)
                        params.put(counter + "", params.get(counter + "") + "\n");
                }
                n += stacksPerParam - 1;
                counter++;
            }

            if (extras != null) {
                String extraString = "---Extras---\n";
                for (String key : extras.keySet()) {
                    extraString += key + ": " + extras.get(key) + "\n";
                    stacktrace += key + ": " + extras.get(key) + "\n";
                }
                params.put("Extras", extraString);
            }

            /*int failCount = 0;
            while (!Flurry.activeSessionExists()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (failCount++ > 10) return;
            }*/
            String name = t.getClass().getName();
            if (name == null) name = "No name...";
            String message = t.getMessage();
            if (message == null) message = "No message...";
            MapController.features.print("ErrorReporter", "Submitting Java error report for " + t.getClass().getName());
            //Flurry.logEvent(name + ": " + message, params);

            // Log to my custom server
            logToMyServer(version, device, ios, name + ": " + message, stacktrace);

            MapController.features.print("ErrorReporter", "Successfully submitted Java error report for " + t.getClass().getName());
            //Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public static void logExceptionThreaded(final NSException t, final Map<String, String> extras) {
        Thread thread = new Thread() {
            public void run() {
                logException(t, extras);
            }
        };
        thread.start();
    }

    public static void logException(final NSException t, final Map<String, String> extras) {
        try {
            MapController.features.print("ErrorReporter", "Preparing native iOS error report for " + t.getName());
            Map<String, String> params = new HashMap<String, String>();

            String ios = UIDevice.getCurrentDevice().getSystemVersion();
            String device = getDeviceModel();
            String version = MapController.instance.getAppVersion() + "";
            String stacktrace = "";

            int counter = 0;
            NSArray<NSString> stack = t.getCallStackSymbols();

            double usableParams;
            if (extras == null) {
                usableParams = 10.0;
            } else {
                usableParams = 9.0;
            }
            int stacksPerParam = (int) Math.ceil(stack.size() / usableParams);
            for (int n = 0; n < stack.size(); n++) {
                for (int x = 0; x < stacksPerParam; x++) {
                    if (n+x >= stack.size()) break;
                    stacktrace += stack.get(n+x).toString() + "\n";
                    params.put(counter + "", stack.get(n+x).toString());
                    if (x < stacksPerParam - 1) params.put(counter + "", params.get(counter + "") + "\n");
                }
                n += stacksPerParam - 1;
                counter++;
            }

            if (extras != null) {
                String extraString = "---Extras---\n";
                for (String key : extras.keySet()) {
                    extraString += key + ": " + extras.get(key) + "\n";
                    stacktrace += key + ": " + extras.get(key) + "\n";
                }
                params.put("Extras", extraString);
            }

            int failCount = 0;
            /*while (!Flurry.activeSessionExists()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (failCount++ > 10) return;
            }*/
            String name = t.getName();
            if (name == null) name = "Native crash with no name...";
            String message = t.getReason();
            if (message == null) message = "Native crash with no reason...";
            MapController.features.print("ErrorReporter", "Submitting native iOS error report for " + t.getName());
            //Flurry.logEvent(name + ": " + message, params);

            logToMyServer(version, device, ios, name + ": " + message, stacktrace);

            MapController.features.print("ErrorReporter", "Successfully submitted native iOS error report for " + t.getClass().getName());
            //Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public static void logException(Throwable t) {
        logException(t, null);
    }

    public static void logExceptionThreaded(Throwable t) {
        logExceptionThreaded(t, null);
    }

    public static void logException(NSException t) {
        logException(t, null);
    }

    public static void logExceptionThreaded(NSException t) {
        logExceptionThreaded(t, null);
    }

    public static void logToMyServer(final String version, final String device, final String ios, final String name, final String stacktrace) {
        String params = "";
        try {
            HttpWrapper http = new HttpWrapper();
            params = "?version=" + URLEncoder.encode(version, "UTF-8") + "&device=" + URLEncoder.encode(device, "UTF-8")  + "&ios=" + URLEncoder.encode(ios, "UTF-8")  + "&name=" + URLEncoder.encode(name, "UTF-8")  + "&stacktrace=" + URLEncoder.encode(stacktrace, "UTF-8");
            if (!params.equals("")) FileUtils.writeStringToFile(Gdx.files.local(LEFTOVERS).file(), params + "\n", true);
            //http.get(SERVER + params);
            //MapController.features.print("ErrorReporter", "Http response: " + http.getHtml());
        } catch (Exception e) {
            e.printStackTrace();
            /*try {
                if (!params.equals("")) FileUtils.writeStringToFile(Gdx.files.local(LEFTOVERS).file(), params + "\n", true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }*/
        }
    }

    public static void trySendingLeftovers() {
        Thread thread = new Thread() {
            public void run() {
                try {
                    MapController.features.print("ErrorReporter", "Looking for leftovers...");

                    if (!Gdx.files.local(LEFTOVERS).exists()) {
                        MapController.features.print("ErrorReporter", "No leftovers file found! That's a good sign :)");
                        return;
                    }

                    ArrayList<String> removables = new ArrayList<>();
                    ArrayList<String> leftovers = new ArrayList<>(Arrays.asList(FileUtils.readFileToString(Gdx.files.local(LEFTOVERS).file()).split("\\n")));

                    MapController.features.print("ErrorReporter", "Found " + leftovers.size() + " leftovers...");

                    if (leftovers.isEmpty()) return;

                    removables.add("");
                    int counter = 0;
                    for (String leftover : leftovers) {
                        try {
                            counter++;
                            if (leftover.equals("")) continue;
                            MapController.features.print("ErrorReporter", "Sending leftover " + counter + " of " + leftovers.size());
                            HttpWrapper http = new HttpWrapper();
                            http.get(SERVER + leftover);
                            MapController.features.print("ErrorReporter", "Http response: " + http.getHtml());
                            removables.add(leftover);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (counter >= MAX_LEFTOVERS_PER_SEND) break;
                    }

                    leftovers.removeAll(removables);

                    if (leftovers.isEmpty()) {
                        MapController.features.print("ErrorReporter", "Sent all leftovers successfully!");
                        Gdx.files.local(LEFTOVERS).delete();
                        return;
                    } else {
                        MapController.features.print("ErrorReporter", "Failed to send " + leftovers.size() + " leftovers. Saving for next time...");
                    }

                    String leftoverString = "";
                    for (String leftover : leftovers) {
                        leftoverString += leftover + "\n";
                    }

                    if (!leftoverString.equals("")) FileUtils.writeStringToFile(Gdx.files.local(LEFTOVERS).file(), leftoverString);
                    else Gdx.files.local(LEFTOVERS).delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public static String getDeviceModel() {
        String platform = HWMachine.getMachineString();
        
        /* iPhone */

        if (platform.equals("iPhone1,1")) return "iPhone 2G";
        if (platform.equals("iPhone1,2")) return "iPhone 3G";
        if (platform.equals("iPhone2,1")) return "iPhone 3GS";
        if (platform.equals("iPhone3,1")) return "iPhone 4 (GSM)";
        if (platform.equals("iPhone3,2")) return "iPhone 4 (GSM/2012)";
        if (platform.equals("iPhone3,3")) return "iPhone 4 (CDMA)";
        if (platform.equals("iPhone4,1")) return "iPhone 4S";
        if (platform.equals("iPhone5,1")) return "iPhone 5 (GSM)";
        if (platform.equals("iPhone5,2")) return "iPhone 5 (Global)";
        if (platform.equals("iPhone5,3")) return "iPhone 5c (GSM)";
        if (platform.equals("iPhone5,4")) return "iPhone 5c (Global)";
        if (platform.equals("iPhone6,1")) return "iPhone 5s (GSM)";
        if (platform.equals("iPhone6,2")) return "iPhone 5s (Global)";
        if (platform.equals("iPhone7,2")) return "iPhone 6";
        if (platform.equals("iPhone7,1")) return "iPhone 6 Plus";
        if (platform.equals("iPhone8,1")) return "iPhone 6s";
        if (platform.equals("iPhone8,2")) return "iPhone 6s Plus";
        if (platform.equals("iPhone8,4")) return "iPhone SE";
        if (platform.equals("iPhone9,1")) return "iPhone 7 (Global)";
        if (platform.equals("iPhone9,3")) return "iPhone 7 (GSM)";
        if (platform.equals("iPhone9,2")) return "iPhone 7 Plus (Global)";
        if (platform.equals("iPhone9,4")) return "iPhone 7 Plus (GSM)";

        /* iPad */

        if (platform.equals("iPad1,1")) return "iPad 1";
        if (platform.equals("iPad2,1")) return "iPad 2 (WiFi)";
        if (platform.equals("iPad2,2")) return "iPad 2 (GSM)";
        if (platform.equals("iPad2,3")) return "iPad 2 (CDMA)";
        if (platform.equals("iPad2,4")) return "iPad 2 (Mid 2012)";
        if (platform.equals("iPad3,1")) return "iPad 3 (WiFi)";
        if (platform.equals("iPad3,2")) return "iPad 3 (CDMA)";
        if (platform.equals("iPad3,3")) return "iPad 3 (GSM)";
        if (platform.equals("iPad3,4")) return "iPad 4 (WiFi)";
        if (platform.equals("iPad3,5")) return "iPad 4 (GSM)";
        if (platform.equals("iPad3,6")) return "iPad 4 (Global)";

        /* iPad Air */

        if (platform.equals("iPad4,1")) return "iPad Air (WiFi)";
        if (platform.equals("iPad4,2")) return "iPad Air (Cellular)";
        if (platform.equals("iPad4,3")) return "iPad Air (China)";
        if (platform.equals("iPad5,3")) return "iPad Air 2 (WiFi)";
        if (platform.equals("iPad5,4")) return "iPad Air 2 (Cellular)";

        /* iPad Mini */

        if (platform.equals("iPad2,5")) return "iPad Mini (WiFi)";
        if (platform.equals("iPad2,6")) return "iPad Mini (GSM)";
        if (platform.equals("iPad2,7")) return "iPad Mini (Global)";
        if (platform.equals("iPad4,4")) return "iPad Mini 2 (WiFi)";
        if (platform.equals("iPad4,5")) return "iPad Mini 2 (Cellular)";
        if (platform.equals("iPad4,6")) return "iPad Mini 2 (China)";
        if (platform.equals("iPad4,7")) return "iPad Mini 3 (WiFi)";
        if (platform.equals("iPad4,8")) return "iPad Mini 3 (Cellular)";
        if (platform.equals("iPad4,9")) return "iPad Mini 3 (China)";
        if (platform.equals("iPad5,1")) return "iPad Mini 4 (WiFi)";
        if (platform.equals("iPad5,2")) return "iPad Mini 4 (Cellular)";

        /* iPad Pro */

        if (platform.equals("iPad6,3")) return "iPad Pro (9.7 inch/WiFi)";
        if (platform.equals("iPad6,4")) return "iPad Pro (9.7 inch/Cellular)";

        if (platform.equals("iPad6,7")) return "iPad Pro (12.9 inch/WiFi)";
        if (platform.equals("iPad6,8")) return "iPad Pro (12.9 inch/Cellular)";

        /* iPod */

        if (platform.equals("iPod1,1")) return "iPod Touch 1G";
        if (platform.equals("iPod2,1")) return "iPod Touch 2G";
        if (platform.equals("iPod3,1")) return "iPod Touch 3G";
        if (platform.equals("iPod4,1")) return "iPod Touch 4G";
        if (platform.equals("iPod5,1")) return "iPod Touch 5G";
        if (platform.equals("iPod7,1")) return "iPod Touch 6G";

        /* Simulator */

        if (platform.equals("i386")) return "Simulator";
        if (platform.equals("x86_64")) return "Simulator";

        return platform;
    }
}
