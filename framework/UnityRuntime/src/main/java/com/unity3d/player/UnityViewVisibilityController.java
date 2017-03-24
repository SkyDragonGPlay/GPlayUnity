package com.unity3d.player;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.view.View;

public class UnityViewVisibilityController implements IUnityViewVisibilityController {
    private static final SurfaceTexture sDefaultSurfaceTexture = new SurfaceTexture(-1);
    private static final int sSystemUIFlag = UnityConstants.IS_GE_SDK_VERSION_19 ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION : View.SYSTEM_UI_FLAG_LOW_PROFILE;
    private volatile boolean mIsContainDefaultUIFlags;

    @Override
    public boolean setDefaultSurfaceTexture(Camera camera) {
        try {
            camera.setPreviewTexture(sDefaultSurfaceTexture);
            return true;
        }
        catch (Exception exception) {
            return false;
        }
    }

    @Override
    public void setViewUIVisibility(View view, boolean bUiFlags) {
        this.mIsContainDefaultUIFlags = bUiFlags;
        view.setSystemUiVisibility(this.mIsContainDefaultUIFlags ? view.getSystemUiVisibility() | sSystemUIFlag : view.getSystemUiVisibility() & ~sSystemUIFlag);
    }

    @Override
    public void registerSystemUiVisibilityChangeListener(final View view) {
        if (UnityConstants.IS_GE_SDK_VERSION_19) {
            return;
        }
        view.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener(){

            public final void onSystemUiVisibilityChange(int visibility) {
                updateUiVisibility(view, 1000);
            }
        });
    }

    @Override
    public void updateViewUiVisibility(View view) {
        if (!UnityConstants.IS_GE_SDK_VERSION_19 && this.mIsContainDefaultUIFlags) {
            this.setViewUIVisibility(view, false);
            this.mIsContainDefaultUIFlags = true;
        }
        this.updateUiVisibility(view, 1000);
    }

    private void updateUiVisibility(final View view, int delayTimes) {
        Handler handler = view.getHandler();
        if (handler == null) {
            this.setViewUIVisibility(view, this.mIsContainDefaultUIFlags);
            return;
        }
        handler.postDelayed(new Runnable(){

            @Override
            public final void run() {
                setViewUIVisibility(view, mIsContainDefaultUIFlags);
            }
        }, delayTimes);
    }
}

