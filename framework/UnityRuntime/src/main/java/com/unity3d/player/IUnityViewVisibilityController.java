package com.unity3d.player;

import android.hardware.Camera;
import android.view.View;

public interface IUnityViewVisibilityController {

    boolean setDefaultSurfaceTexture(Camera camera);

    void setViewUIVisibility(View view, boolean isUseDefaultUiFlags);

    void registerSystemUiVisibilityChangeListener(View view);

    void updateViewUiVisibility(View view);
}

