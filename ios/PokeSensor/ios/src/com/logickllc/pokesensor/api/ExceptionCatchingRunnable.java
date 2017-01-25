package com.logickllc.pokesensor.api;


import com.logickllc.pokesensor.ErrorReporter;

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
            e.getStackTrace();
            ErrorReporter.logException(e);
        }
    }
}
