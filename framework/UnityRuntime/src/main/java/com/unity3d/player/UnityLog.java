package com.unity3d.player;

import android.util.Log;

final class UnityLog {
    static boolean bshowLog = false;

    private static final String TAG = "Unity";

    public static final int WARN = 5;
    public static final int ERROR = 6;
    protected static void Log(int level, String msg) {
        if (bshowLog) {
            return;
        }
        if (level == ERROR) {
            Log.e(TAG, msg);
        }
        if (level == WARN) {
            Log.w(TAG, msg);
        }
    }
}

