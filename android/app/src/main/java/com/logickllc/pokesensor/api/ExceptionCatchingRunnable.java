package com.logickllc.pokesensor.api;


import com.crashlytics.android.Crashlytics;

public class ExceptionCatchingRunnable implements Runnable {
    private Runnable runnable;

    public ExceptionCatchingRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            //ErrorReporter.logException(e);
        }
    }
}
