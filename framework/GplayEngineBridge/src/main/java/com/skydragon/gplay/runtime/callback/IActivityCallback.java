package com.skydragon.gplay.runtime.callback;

import android.app.Activity;
import android.content.Intent;
import android.view.ViewGroup;

import org.json.JSONObject;

/**
 * Created by zhangjunfei on 15/12/16.
 */
public interface IActivityCallback {

    public void onActivityCreate(Activity activity);

    public void onActivityPause();

    public void onActivityResume();

    public void onActivityDestroy();

    public void onActivityResultCallback(int requestCode, int resultCode, Intent data);

    public void onRunOnGLThread(Runnable runnable);

    public void onActivityWindowFocusChanged(boolean hasFocus);

    public ViewGroup getContainer();
}
