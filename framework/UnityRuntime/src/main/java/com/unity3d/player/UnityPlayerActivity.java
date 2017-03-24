package com.unity3d.player;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;

import com.skydragon.gplay.runtime.bridge.CocosRuntimeBridge;
import com.skydragon.gplay.runtime.callback.IActivityCallback;

public class UnityPlayerActivity extends Activity implements IActivityCallback {
    private static final String TAG = "UnitPlayerActivity";
    protected UnityPlayer mUnityPlayer;

    @Override
    protected void onCreate(Bundle bundle) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(bundle);
        this.getWindow().setFormat(PixelFormat.RGBX_8888);
        this.mUnityPlayer = new UnityPlayer(this);
        mUnityPlayer.initRuntime(this);
        this.setContentView(this.mUnityPlayer);
        this.mUnityPlayer.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUnityPlayer.resume();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE ) {
            return this.mUnityPlayer.injectEvent(keyEvent);
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return this.mUnityPlayer.injectEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return this.mUnityPlayer.injectEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mUnityPlayer.injectEvent(motionEvent);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return this.mUnityPlayer.injectEvent(motionEvent);
    }

    @Override
    public ViewGroup getContainer() {
        return this.mUnityPlayer;
    }

    @Override
    public void onActivityCreate(Activity activity) {
        String shareLibraryPath = CocosRuntimeBridge.getInstance().getSharedLibraryPath();
        String resourceRootPath = CocosRuntimeBridge.getInstance().getGameResourceDir();
        mUnityPlayer = new UnityPlayer(activity, resourceRootPath, shareLibraryPath);
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onActivityPause() {
        Log.d(TAG, "onPause()");
        this.mUnityPlayer.pause();
    }

    @Override
    public void onActivityResume() {
        Log.d(TAG, "onActivityResume()");
        this.mUnityPlayer.resume();
    }

    @Override
    public void onActivityDestroy() {
        this.mUnityPlayer.quit();
    }

    @Override
    public void onActivityResultCallback(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onActivityWindowFocusChanged(boolean hasFocus) {
        this.mUnityPlayer.windowFocusChanged(hasFocus);
    }

    public void runOnGLThread(final Runnable pRunnable) {
        mUnityPlayer.runOnGLTread(pRunnable);
    }

    @Override
    public void onRunOnGLThread(Runnable runnable) {
        this.runOnGLThread(runnable);
    }
}

