package com.logickllc.pokesensor.api;


public class Lock {
    private boolean locked = true;
    public Object object = null;

    public synchronized void lock() {
        locked = true;
    }

    public synchronized void unlock() {
        locked = false;
    }

    public synchronized boolean isLocked() {
        return locked;
    }

    public void waitForUnlock() {
        System.out.println("Lock: " + this.toString() + "   Waiting for unlock...");
        while (isLocked()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Lock: " + this.toString() + "   unlocked...");
    }
}
