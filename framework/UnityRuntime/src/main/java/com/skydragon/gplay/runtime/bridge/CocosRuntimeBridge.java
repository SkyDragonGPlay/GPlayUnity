package com.skydragon.gplay.runtime.bridge;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.ViewGroup;

import com.skydragon.gplay.runtime.callback.IActivityCallback;
import com.skydragon.gplay.unitsdk.nativewrapper.NativeWrapper;
import com.unity3d.player.UnityPlayerActivity;
import com.unity3d.player.WWW;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by zhangjunfei on 16/8/18.
 */
public class CocosRuntimeBridge implements IEngineRuntimeBridge {

    public static final String TAG = "UnityRuntimeBridge";

    private static final String VERSION = "0.5.1Beta";
    private static final int VERSION_CODE = 6;

    public static final String KEY_CAPTURE_SCREEN_METHOD = "CAPTURE_SCREEN";
    public static final String KEY_CAPTURE_SCREEN_FILE_NAME = "FILE_NAME";
    public static final String KEY_CAPTURE_SCREEN_QUALITY = "QUALITY";
    public static final String KEY_CAPTURE_SCREEN_RESULT_CODE = "result_code";
    public static final String KEY_CAPTURE_SCREEN_RESULT_MSG = "result_msg";

    private Activity mActivity = null;
    private IActivityCallback mActivityCallback = null;

    private IBridgeProxy mBridgeProxy = null;

    private static CocosRuntimeBridge sInstance = null;

    private HashMap<String, Object> mOptionMap = new HashMap<>();
    private boolean isNativeLibraryLoaded;

    private static int mGameRunMode = 1;

    private String mDefaultResourceRootPath = "";

    static {
        System.loadLibrary("gplay");
    }

    public String getDefaultResourceRootPath() {
        return mDefaultResourceRootPath;
    }

    @Override
    public Object invokeMethodSync(String method, Map<String, Object> args) {
        System.out.println("zjf@ CocosRuntimeBridge invokeMethodSync");
        if (method == null) {
            Log.e(TAG, "invokeMethodSync method is null");
            return null;
        }
        else {
            Log.d(TAG, "invokeMethodSync method:" + method);
        }

        try {
            switch (method) {
                case "preloadResponse": {
                    nativePreloadResponse((String) args.get("responseJson"), (Boolean) args.get("isDone"), (Long) args.get("ext"));
                    break;
                }
                case "preloadResponse2": {
                    nativePreloadResponse2((Boolean) args.get("isDone"),
                            (Boolean) args.get("isFailed"), (String) args.get("errorCode"),
                            (Float) args.get("percent"), (Float) args.get("downloadSpeed"), (String) args.get("groupName"));
                    break;
                }
                case "onAsyncActionResult": {
                    int code = (Integer)args.get("ret");
                    String msg = (String)args.get("msg");
                    String callbackId = (String)args.get("callbackid");
                    NativeWrapper.onAsyncActionResult(code, msg, callbackId);
                    break;
                }
                case "downloadRemoteFileCallback": {
                    nativeDownloadFileCallback((String) args.get("responseJson"), (Long) args.get("ext"));
                    break;
                }
                case "setRuntimeConfig": {
                    String jsonStr = (String) args.get("jsonStr");
                    nativeSetRuntimeConfig(jsonStr);
                    break;
                }
                case "nativeExtensionAPI": {
                    nativeExtensionAPI((String) args.get("method"), (String) args.get("string"),
                            (Integer) args.get("int"), (Double) args.get("double"));
                    break;
                }
                default:
                    Log.e(TAG, "CocosBridge.invokeMethodSync doesn't support ( " + method + " )");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void invokeMethodAsync(final String method, final Map<String, Object> args, final ICallback callback) {
        if (method != null)
            Log.d(TAG,  "invokeMethodAsync method:" + method);
        else {
            Log.e(TAG,  "invokeMethodAsync method is null");
            return;
        }

        switch (method) {
            case KEY_CAPTURE_SCREEN_METHOD: {
                String pictureSaveFile = (String)args.get(KEY_CAPTURE_SCREEN_FILE_NAME);
                int quality = (int)args.get(KEY_CAPTURE_SCREEN_QUALITY);
                doCaptureScreen(pictureSaveFile, quality, callback);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void setOption(String key, Object value) {
        mOptionMap.put(key, value);
    }

    @Override
    public Object getOption(String key) {
        return mOptionMap.get(key);
    }

    @Override
    public void setBridgeProxy(IBridgeProxy proxy) {
        mBridgeProxy = proxy;
        Object runMode = proxy.invokeMethodSync("getGameRunMode", null);
        if (runMode != null)
            mGameRunMode = (Integer)runMode;
        else
            mGameRunMode = 1;
    }

    @Override
    public IBridgeProxy getBridgeProxy() {
        return mBridgeProxy;
    }

    public static CocosRuntimeBridge getInstance() {
        if (sInstance == null) {
            Log.e(TAG, "UnityRuntimeBridge has not been constructed!");
        }
        return sInstance;
    }

    public CocosRuntimeBridge() {
        sInstance = this;
    }

    @Override
    public String getRuntimeVersion() {
        return VERSION;
    }


    @Override
    public void loadSharedLibrary(List<String> listSo) {

    }

    @Override
    public void notifyOnLoadSharedLibrary() {
        try {
            if(isNativeLibraryLoaded) {
                return;
            }
            isNativeLibraryLoaded = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startGame() {
        Log.d(TAG, "startGame ...");
        String sResourceDir = (String)mBridgeProxy.invokeMethodSync("getGameResourceDir",null);
        WWW.setIsGPlayRuntime(true);
        WWW.setResourceRootPath(sResourceDir);

        mBridgeProxy.invokeMethodSync("notifyOnPrepareEngineFinished", null);
        mActivityCallback.onActivityWindowFocusChanged(true);
        mActivityCallback.onActivityResume();
    }

    @Override
    public void quitGame() {

    }

    @Override
    public void init(Activity activity) {
        String sResourceDir = (String)mBridgeProxy.invokeMethodSync("getGameResourceDir",null);
        nativeAddSearchPath(sResourceDir);
        String sScriptResourceDir = (String)mBridgeProxy.invokeMethodSync("getRuntimeResourceDir", null);
        nativeAddSearchPath(sScriptResourceDir);
        nativeSetDefaultResourceRootPath(sResourceDir);
        mActivity = activity;
        if (mActivityCallback == null) {
            mActivityCallback = new UnityPlayerActivity();
        }
        mActivityCallback.onActivityCreate(mActivity);
    }

    @Override
    public void initRuntimeJNI() {
        nativeInitRuntimeJNI();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause ...");
        mActivityCallback.onActivityPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume ...");
        mActivityCallback.onActivityResume();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop ...");
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent ...");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy ...");
        mActivityCallback.onActivityDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode:" + resultCode + ", data:" + data);
        mActivityCallback.onActivityResultCallback(requestCode, resultCode, data);
    }

    @Override
    public void runOnGLThread(Runnable runnable) {
        mActivityCallback.onRunOnGLThread(runnable);
    }

    @Override
    public boolean isRunOnEngineContext() {
        boolean isGLThread = Thread.currentThread().getName().contains("GL");
        if (!isGLThread) {
            Log.d(TAG, "Oops, makeSureIsInGLThread failed, it wasn't invoked from GL thread!");
        }
        return isGLThread;
    }

    @Override
    public ViewGroup getEngineLayout() {
        return mActivityCallback.getContainer();
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        mActivityCallback.onActivityWindowFocusChanged(hasFocus);
    }


    public String getGameResourceDir() {
        return (String)mBridgeProxy.invokeMethodSync("getGameResourceDir",null);
    }

    public String getSharedLibraryPath() {
        return (String)mBridgeProxy.invokeMethodSync("getGameSharedLibrariesDir",null);
    }

    public static int getGameRunMode() {
        return mGameRunMode;
    }


    private static native void nativeCaptureScreen(String pictureSaveFile, int quality);

    private static native void nativeInitRuntimeJNI();

    private static native void nativeAddSearchPath(String path);

    private static native void nativePreloadResponse(String responseJson, boolean isDone, long ext);

    private static native void nativePreloadResponse2(boolean isDone, boolean isFailed, String errorCode, float percent, float downloadSpeed, String groupName);

    private static native void nativeDownloadFileCallback(String responseJson, long ext);

    private static native void nativeSetRuntimeConfig(String responseJson);

    public static native void nativeExtensionAPI(String method, String stringArg, int intArg, double doubleArg);

    private static native void nativeSetDefaultResourceRootPath(String resRootPath);


    private static List<CaptureScreenWrapper> listCaptureScreenWrapper = new ArrayList<>();

    private void doCaptureScreen(final String picureSaveFile, final int quality, ICallback callback) {
        Log.d(TAG,  "doCaptureScreen picureSaveFile:" + picureSaveFile + ",quality:" + quality);
        runOnGLThread(new Runnable() {
            @Override
            public void run() {
                nativeCaptureScreen(picureSaveFile, quality);
            }
        });
        CaptureScreenWrapper wrapper = new CaptureScreenWrapper(picureSaveFile, callback);
        listCaptureScreenWrapper.add(wrapper);
    }

    static String getWritablePath() {
        String sGameDir= (String)sInstance.mBridgeProxy.invokeMethodSync("getGameDir",null);
        File fWritablePath = new File(sGameDir, "writable");
        if(!fWritablePath.exists()) {
            fWritablePath.mkdirs();
        }
        return fWritablePath.getAbsolutePath() + "/";
    }

    static void captureScreenCallback(String pictureSaveFile) {
        CaptureScreenWrapper selfWrapper = null;
        for(CaptureScreenWrapper wrapper : listCaptureScreenWrapper ) {
            if(wrapper.mPictureSaveFile.equalsIgnoreCase(pictureSaveFile)) {
                wrapper.callback();
                selfWrapper = wrapper;
                break;
            }
        }

        if(null != selfWrapper) {
            listCaptureScreenWrapper.remove(selfWrapper);
        }

    }


    class CaptureScreenWrapper {
        private String mPictureSaveFile;
        private ICallback mCallback;
        CaptureScreenWrapper(String pictureSaveFile, ICallback callback) {
            mPictureSaveFile = pictureSaveFile;
            mCallback = callback;
        }

        void callback() {
            if(null != mCallback) {
                Map<String,Object> params = new HashMap<>();
                params.put(KEY_CAPTURE_SCREEN_FILE_NAME, mPictureSaveFile);
                mCallback.onCallback(KEY_CAPTURE_SCREEN_METHOD, params);
            }
        }
    }

}

