package com.logickllc.pokesensor.api;

import com.logickllc.pokesensor.ErrorReporter;
import com.logickllc.pokesensor.IOSLauncher;
import com.logickllc.pokesensor.MapController;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


public class CustomLock extends ReentrantLock {
    public String trace = "";
    public Exception exception = null;

    @Override
    public void lock() {
        super.lock();

        try {
            int n = 0;
            int x = 5 / n;
        } catch (Exception e) {
            exception = e;
        }
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        boolean result = super.tryLock(timeout, unit);
        if (!result) {
            //Flurry.logError(exception.getClass().getName(), exception.getMessage(), exception);
            ErrorReporter.logException(exception);
            exception.printStackTrace(System.out);
            trace = MapController.features.getStackTraceString(exception);
            if (IOSLauncher.IS_AD_TESTING) MapController.features.superLongMessage(trace);
        } else {
            try {
                int n = 0;
                int x = 5 / n;
            } catch (Exception e) {
                exception = e;
            }
        }
        return result;
    }
}
