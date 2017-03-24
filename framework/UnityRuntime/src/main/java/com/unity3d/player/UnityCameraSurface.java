package com.unity3d.player;

import android.app.Activity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

abstract class UnityCameraSurface implements SurfaceHolder.Callback {
    private final Activity mActivity = (Activity) UnitySurfaceViewManager._instance.getContext();
    private final int mSurfaceType = SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS;
    private SurfaceView mSurfaceView;

    UnityCameraSurface() {

    }

    final void create() {
        this.mActivity.runOnUiThread(new Runnable(){
            @Override
            public final void run() {
                if (mSurfaceView == null) {
                    mSurfaceView = new SurfaceView(UnitySurfaceViewManager._instance.getContext());
                    mSurfaceView.getHolder().setType(mSurfaceType);
                    mSurfaceView.getHolder().addCallback(UnityCameraSurface.this);
                    UnitySurfaceViewManager._instance.addCameraSurfaceView(mSurfaceView);
                    mSurfaceView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    final void destroy() {
        this.mActivity.runOnUiThread(new Runnable(){

            @Override
            public final void run() {
                if (mSurfaceView != null) {
                    UnitySurfaceViewManager._instance.removeCameraSurfaceView(mSurfaceView);
                }
                mSurfaceView = null;
            }
        });
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {

    }

}

