package com.unity3d.player;

import android.content.Context;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.util.HashSet;
import java.util.Set;

final class UnitySurfaceViewManager {
    public static UnitySurfaceViewManager _instance;
    private ViewGroup mRootView;
    private Set<View> mViews = new HashSet<View>();
    private SurfaceView mGameSurfaceView;

    UnitySurfaceViewManager(ViewGroup rootView) {
        this.mRootView = rootView;
        _instance = this;
    }

    public final Context getContext() {
        return this.mRootView.getContext();
    }

    public final void addGameSurfaceView(SurfaceView glView) {
        if (this.mGameSurfaceView != glView) {
            this.mGameSurfaceView = glView;
            this.mRootView.addView(glView);
            for (View view : this.mViews) {
                this.addView(view);
            }
        }
    }

    public final void removeGameSurfaceView(View glView) {
        if (this.mGameSurfaceView == glView) {
            for (View view : this.mViews) {
                this.removeView(view);
            }
            this.mRootView.removeView(glView);
            this.mGameSurfaceView = null;
        }
    }

    public final void addCameraSurfaceView(View view) {
        this.mViews.add(view);
        if (this.mGameSurfaceView != null) {
            this.addView(view);
        }
    }

    public final void removeCameraSurfaceView(View view) {
        this.mViews.remove(view);
        if (this.mGameSurfaceView != null) {
            this.removeView(view);
        }
    }


    private void addView(View view) {
        this.mRootView.addView(view, this.mRootView.getChildCount());
    }

    private void removeView(View view) {
        this.mRootView.removeView(view);
        this.mRootView.requestLayout();
    }
}

