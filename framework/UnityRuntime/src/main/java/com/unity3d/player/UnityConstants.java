package com.unity3d.player;

import android.os.Build;

public class UnityConstants {
    static final boolean IS_GE_SDK_VERSION_11 = Build.VERSION.SDK_INT >= 11;
    static final boolean IS_GE_SDK_VERSION_12 = Build.VERSION.SDK_INT >= 12;
    static final boolean IS_GE_SDK_VERSION_14 = Build.VERSION.SDK_INT >= 14;
    static final boolean IS_GE_SDK_VERSION_16 = Build.VERSION.SDK_INT >= 16;
    static final boolean IS_GE_SDK_VERSION_17 = Build.VERSION.SDK_INT >= 17;
    static final boolean IS_GE_SDK_VERSION_19 = Build.VERSION.SDK_INT >= 19;
    static final boolean IS_GE_SDK_VERSION_21 = Build.VERSION.SDK_INT >= 21;
    static final boolean IS_GE_SDK_VERSION_23 = Build.VERSION.SDK_INT >= 23;
    static final IUnityViewVisibilityController sUnityUiController = IS_GE_SDK_VERSION_11 ? new UnityViewVisibilityController() : null;
    static final UnityViewMotionEvent sUnityViewMotionEvent = IS_GE_SDK_VERSION_12 ? new UnityViewMotionEventImpl() : null;
    static final UnityVSYNCtiming sUnityAnimation = IS_GE_SDK_VERSION_16 ? new UnityVSYNCtimingImpl() : null;
    static final UnityDisplayListener sUnityDisplay = IS_GE_SDK_VERSION_17 ? new UnityPresentation() : null;
    static final ITaskExecutor sTaskExecutor = IS_GE_SDK_VERSION_23 ? new UnityTaskExecutor() : null;
}

