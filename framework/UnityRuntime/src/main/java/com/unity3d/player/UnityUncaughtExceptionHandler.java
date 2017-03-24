package com.unity3d.player;

import android.os.Build;

final class UnityUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private volatile Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    UnityUncaughtExceptionHandler() {
    }

    final synchronized boolean init() {
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (uncaughtExceptionHandler == this) {
            return false;
        }
        this.mDefaultUncaughtExceptionHandler = uncaughtExceptionHandler;
        Thread.setDefaultUncaughtExceptionHandler(this);
        return true;
    }

    @Override
    public synchronized void uncaughtException(Thread thread, Throwable throwable) {
        try {
            if(null != throwable) {
                throwable.printStackTrace();
            }
            Error error = new Error(String.format("FATAL EXCEPTION [%s]\n", thread.getName()) + String.format("Unity version     : %s\n", "5.3.1f1") + String.format("Device model      : %s %s\n", Build.MANUFACTURER, Build.MODEL) + String.format("Device fingerprint: %s\n", Build.FINGERPRINT));
            error.setStackTrace(new StackTraceElement[0]);
            error.initCause(throwable);
            this.mDefaultUncaughtExceptionHandler.uncaughtException(thread, error);
            return;
        }
        catch (Throwable t) {
            this.mDefaultUncaughtExceptionHandler.uncaughtException(thread, throwable);
        }
    }
}

