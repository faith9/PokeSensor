package com.logickllc.pokesensor.api;



import com.crashlytics.android.Crashlytics;

import java.util.concurrent.ThreadFactory;

// Solution given by Mark Peters in a Stack Overflow question at
// http://stackoverflow.com/questions/3875739/exception-handling-in-threadpools/3875784#3875784
public class ExceptionCatchingThreadFactory implements ThreadFactory {
    private final ThreadFactory delegate;

    public ExceptionCatchingThreadFactory(ThreadFactory delegate) {
        this.delegate = delegate;
    }

    public Thread newThread(final Runnable r) {
        Thread t = delegate.newThread(r);
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
        });
        return t;
    }
}
