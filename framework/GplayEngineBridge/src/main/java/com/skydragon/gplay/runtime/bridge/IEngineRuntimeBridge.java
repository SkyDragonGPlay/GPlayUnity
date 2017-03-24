package com.skydragon.gplay.runtime.bridge;

import android.app.Activity;
import android.content.Intent;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by zhangjunfei on 15/12/17.
 */
public interface IEngineRuntimeBridge extends IBridge {
    void init(Activity activity);
    void initRuntimeJNI();
    String getRuntimeVersion();
    void loadSharedLibrary(List<String> listSo);
    void startGame();
    void quitGame();
    void onStart();
    void onResume();
    void onPause();
    void onStop();
    void onDestroy();
    void onNewIntent(Intent intent);
    void onActivityResult(int requestCode, int resultCode, Intent data);
    void onWindowFocusChanged(boolean hasFocus);
    void notifyOnLoadSharedLibrary();
    ViewGroup getEngineLayout();
    void runOnGLThread(Runnable r);
    boolean isRunOnEngineContext();

}
