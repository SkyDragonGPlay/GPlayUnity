package com.unity3d.player;

import android.content.Context;

public interface UnityDisplayListener {

    void registerDisplayListener(UnityPlayer unityPlayer, Context context);

    void unregisterDisplayListener(Context context);

    boolean show(UnityPlayer unityPlayer, Context context, int displayId);
}

