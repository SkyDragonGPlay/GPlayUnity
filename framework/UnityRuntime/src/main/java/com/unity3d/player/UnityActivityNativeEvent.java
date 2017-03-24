package com.unity3d.player;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public final class UnityActivityNativeEvent {
    private final Bundle mBundle;

    public UnityActivityNativeEvent(Activity activity) {
        Bundle bundleMeta = Bundle.EMPTY;
        PackageManager packageManager = activity.getPackageManager();
        ComponentName comp = activity.getComponentName();
        try {
            ActivityInfo info = packageManager.getActivityInfo(comp, PackageManager.GET_META_DATA);
            if (packageManager != null && info.metaData != null) {
                bundleMeta = info.metaData;
            }
        }
        catch (PackageManager.NameNotFoundException exception) {
            UnityLog.Log(Log.ERROR, "Unable to retreive meta data for activity '" + comp + "'");
        }
        this.mBundle = new Bundle(bundleMeta);
    }

    public final boolean isSendNativeEventToDalvik() {
        return this.mBundle.getBoolean(formatMessage("ForwardNativeEventsToDalvik"));
    }

    private static String formatMessage(String string) {
        return String.format("%s.%s", "unityplayer", string);
    }
}

