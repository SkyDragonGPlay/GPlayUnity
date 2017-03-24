package com.unity3d.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NativeActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.text.method.MultiTapKeyListener;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UnityPlayer extends FrameLayout implements UnityCamera.onCamearFrameCallback {
    private static final String TAG = "UnitPlayer";
    public static Activity currentActivity = null;
    private boolean hasSurfaceCreated = false;
    private boolean mIsGplayRuntime;
    private UnityMotionEvent mUnityMotionEventHandler;
    private UnitySurfaceViewManager mUnityViewManager;
    private UnityEnvironment mUnityEnvironment = new UnityEnvironment();
    private ConcurrentLinkedQueue mPendingTasks = new ConcurrentLinkedQueue();
    private boolean isNativeEventPassedToDalvik = false;
    private String mShareLibraryPath;
    private String mResourceDefaultRootPath;
    GLThread glThread;
    private Context mContext;
    private SurfaceView mSurfaceView;
    private static boolean isLoadedMainLibrary;
    private boolean isPaused;
    private UnityLocation mUnityLocation;
    private Bundle bundle;
    private List<UnityCamera> mListUnityCameras;
    private UnityVedioPlayer mUnityVedioPlayer;
    UnityEditTextDialog mEditTextDialog;
    private ProgressBar mProgressBar;
    private Runnable rShowProgress;
    private Runnable rCloseProgress;
    private static Lock sLock;

    public UnityPlayer(Context context ) {
        super(context);
        initRuntime(context);
    }

    public UnityPlayer(Context context, String resourceRootPath, String shareLibraryPath) {
        super(context);
        mResourceDefaultRootPath = resourceRootPath;
        mShareLibraryPath = shareLibraryPath;
        mIsGplayRuntime = true;
        initRuntime(context);
    }

    public void initRuntime(Context context) {
        mContext = context;
        if(context instanceof  Activity) {
            currentActivity = (Activity)context;
        }
        this.glThread = new GLThread();
        this.bundle = new Bundle();
        this.mListUnityCameras = new ArrayList();
        this.mEditTextDialog = null;
        this.mProgressBar = null;
        this.rShowProgress = new Runnable(){

            @Override
            public final void run() {
                int defStyle = nativeActivityIndicatorStyle();
                if (defStyle >= 0) {
                    if (mProgressBar == null) {
                        int[] arrn = new int[]{android.R.attr.progressBarStyleLarge, android.R.attr.progressBarStyleLargeInverse,
                                android.R.attr.progressBarStyleSmall, android.R.attr.progressBarStyleSmallInverse};
                        mProgressBar = new ProgressBar(mContext, null, arrn[defStyle]);
                        mProgressBar.setIndeterminate(true);
                        mProgressBar.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT, 51));
                        addView(mProgressBar);
                    }
                    mProgressBar.setVisibility(View.VISIBLE);
                    bringChildToFront(mProgressBar);
                }
            }
        };
        this.rCloseProgress = new Runnable(){

            @Override
            public final void run() {
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.GONE);
                    removeView(mProgressBar);
                    mProgressBar = null;
                }
            }
        };

        this.mUnityViewManager = new UnitySurfaceViewManager(this);

        this.mUnityMotionEventHandler = context instanceof Activity ? new UnityMotionEventHandler(context) : null;

        this.mUnityLocation = new UnityLocation(mContext, this);

        this.loadSettings();
        if (UnityConstants.IS_GE_SDK_VERSION_11) {
            UnityConstants.sUnityUiController.registerSystemUiVisibilityChangeListener(this);
        }

        this.setFullscreen(true);
        if(mIsGplayRuntime) {
            loadRuntimeLibrary(mShareLibraryPath);
        } else {
            loadRuntimeLibrary(this.mContext.getApplicationInfo());
        }

        if (!UnityEnvironment.isRuntimeLibraryLoaded()) {
            AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle("Failure to initialize!").setPositiveButton("OK", new DialogInterface.OnClickListener(){

                public final void onClick(DialogInterface dialogInterface, int n2) {
                    closeGame();
                }
            }).setMessage("Your hardware does not support this application, sorry!").create();
            dialog.setCancelable(false);
            dialog.show();
            return;
        }
        this.initJni(mContext);
        this.nativeFile(this.mContext.getPackageCodePath());
        this.checkUseObbFile();

        this.mSurfaceView = new SurfaceView(context);
        this.mSurfaceView.getHolder().setFormat(PixelFormat.RGBX_8888);
        this.mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback(){

            public final void surfaceCreated(SurfaceHolder holder) {
                checkCreateNativeGLSurfaceView(0, holder.getSurface());
            }

            public final void surfaceChanged(SurfaceHolder holder, int format, int width,
                                             int height) {
                checkCreateNativeGLSurfaceView(0, holder.getSurface());
            }

            public final void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                checkCreateNativeGLSurfaceView(0, null);
            }
        });
        this.mSurfaceView.setFocusable(true);
        this.mSurfaceView.setFocusableInTouchMode(true);
        this.mUnityViewManager.addGameSurfaceView(this.mSurfaceView);
        this.isPaused = false;
        this.initNativeEvent();
        this.nativeInitWWW(WWW.class);
        this.nativeInitWebRequest(UnityWebRequest.class);
        if (UnityConstants.IS_GE_SDK_VERSION_17) {
            UnityConstants.sUnityDisplay.registerDisplayListener(this, mContext);
        }
        if (UnityConstants.IS_GE_SDK_VERSION_23 && currentActivity != null) {
            UnityConstants.sTaskExecutor.executeTaskByCheckPermission(currentActivity, new Runnable(){

                @Override
                public final void run() {
                    runOnUiThread(new Runnable(){

                        @Override
                        public final void run() {
                            mUnityEnvironment.setSDKVersionLT23();
                            prepareEnvironment();
                        }
                    });
                }

            });
        }
        if (UnityConstants.IS_GE_SDK_VERSION_16) {
            UnityConstants.sUnityAnimation.startVSYNCtiming(this);
        }
        this.enforeFullScreen();
        this.glThread.start();
    }

    private void checkCreateNativeGLSurfaceView(int code, Surface surface) {
        if (this.hasSurfaceCreated) {
            return;
        }
        this.createNativeGLSurfaceView(code, surface);
    }

    private boolean createNativeGLSurfaceView(int code, Surface surface) {
        if (!UnityEnvironment.isRuntimeLibraryLoaded()) {
            return false;
        }
        this.nativeRecreateGfxState(code, surface);
        return true;
    }

    public boolean displayChanged(int code, Surface surface) {
        if (code == 0) {
            this.hasSurfaceCreated = surface != null;
            this.runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    if (hasSurfaceCreated) {
                        mUnityViewManager.removeGameSurfaceView(mSurfaceView);
                        return;
                    }
                    mUnityViewManager.addGameSurfaceView(mSurfaceView);
                }
            });
        }
        return this.createNativeGLSurfaceView(code, surface);
    }

    public boolean installPresentationDisplay(int code) {
        if (UnityConstants.IS_GE_SDK_VERSION_17) {
            return UnityConstants.sUnityDisplay.show(this, this.mContext, code);
        }
        return false;
    }

    private void loadSettings() {
        try {
            File fSetXml = null;
            if(mIsGplayRuntime) {
                fSetXml = new File(mResourceDefaultRootPath, "assets/bin/Data/settings.xml");
            } else {
                fSetXml = new File(this.mContext.getPackageCodePath(), "assets/bin/Data/settings.xml");
            }
            InputStream is = fSetXml.exists() ? new FileInputStream(fSetXml) : mContext.getAssets().open("bin/Data/settings.xml");
            XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
            xmlPullParserFactory.setNamespaceAware(true);
            XmlPullParser parser = xmlPullParserFactory.newPullParser();
            parser.setInput(is, null);
            String attrValue = null;
            int eventType = parser.getEventType();
            String sName = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    sName = parser.getName();
                    int n = 0;
                    for ( n = 0; n < parser.getAttributeCount(); n++) {
                        if (!parser.getAttributeName(n).equalsIgnoreCase("name")) continue;
                        attrValue = parser.getAttributeValue(n);
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    sName = null;
                } else if (eventType == XmlPullParser.TEXT && attrValue != null) {
                    if (sName.equalsIgnoreCase("integer")) {
                        this.bundle.putInt(attrValue, Integer.parseInt(parser.getText()));
                    } else if (sName.equalsIgnoreCase("attrValue")) {
                        this.bundle.putString(attrValue, parser.getText());
                    } else if (sName.equalsIgnoreCase("bool")) {
                        this.bundle.putBoolean(attrValue, Boolean.parseBoolean(parser.getText()));
                    } else if (sName.equalsIgnoreCase("float")) {
                        this.bundle.putFloat(attrValue, Float.parseFloat(parser.getText()));
                    }
                    attrValue = null;
                }
                eventType = parser.next();
            }
            return;
        }
        catch (Exception var1_2) {
            var1_2.printStackTrace();;
            UnityLog.Log(Log.ERROR, "Unable to locate player settings. " + var1_2.getLocalizedMessage());
            if(!mIsGplayRuntime) {
                this.closeGame();
            }
        }
    }

    public Bundle getSettings() {
        return this.bundle;
    }


    private void closeGame() {
        if(mIsGplayRuntime) return;
        if (mContext instanceof Activity && !((Activity)mContext).isFinishing()) {
            ((Activity)mContext).finish();
        }
    }

    static void runOnNewThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    final void runOnUiThread(Runnable runnable) {
        if (mContext instanceof Activity) {
            ((Activity)mContext).runOnUiThread(runnable);
            return;
        }
        UnityLog.Log(Log.WARN, "Not running Unity from an Activity; ignored...");
    }

    public void init(int n2, boolean bl) {

    }

    public View getView() {
        return this;
    }

    private void initNativeEvent() {
        UnityActivityNativeEvent eventChecker = new UnityActivityNativeEvent((Activity)mContext);
        if (mContext instanceof NativeActivity) {
            this.isNativeEventPassedToDalvik = eventChecker.isSendNativeEventToDalvik();
            this.nativeForwardEventsToDalvik(this.isNativeEventPassedToDalvik);
        }
    }

    public void kill() {
        Process.killProcess(Process.myPid());
    }

    public void quit() {
        this.isPaused = true;
        if (!this.mUnityEnvironment.isPaused()) {
            this.pause();
        }
        this.glThread.notifyQuit();
        try {
            this.glThread.join(4000);
        }
        catch (InterruptedException v0) {
            this.glThread.interrupt();
        }
        if (UnityEnvironment.isRuntimeLibraryLoaded()) {
            this.removeAllViews();
        }
        if (UnityConstants.IS_GE_SDK_VERSION_17) {
            UnityConstants.sUnityDisplay.unregisterDisplayListener(mContext);
        }
        if (UnityConstants.IS_GE_SDK_VERSION_16) {
            UnityConstants.sUnityAnimation.stopVSYNCtiming();
        }
        if(!mIsGplayRuntime) {
            this.kill();
        }
        UnityPlayer.unloadRuntimeLibrary();
    }

    private void closeCamera() {
        Iterator<UnityCamera> iterator = this.mListUnityCameras.iterator();
        while (iterator.hasNext()) {
            iterator.next().close();
        }
    }

    private void initCamera() {
        for (UnityCamera unityCamera : this.mListUnityCameras) {
            try {
                unityCamera.setCallbackBuffer(this);
            }
            catch (Exception var3_4) {
                String string = "Unable to initialize camera: " + var3_4.getMessage();
                UnityLog.Log(Log.ERROR, string);
                unityCamera.close();
            }
        }
    }

    public void pause() {
        if (this.mUnityVedioPlayer != null) {
            this.mUnityVedioPlayer.onPause();
            return;
        }
        this.reportSoftInputStr(null, 1, true);
        if (!this.mUnityEnvironment.hasPreparedEnvironment()) {
            return;
        }
        if (UnityEnvironment.isRuntimeLibraryLoaded()) {
            final Semaphore semaphore = new Semaphore(0);
            if (this.isFinishing()) {
                this.queueEvent(new Runnable(){

                    @Override
                    public final void run() {
                        stop();
                        semaphore.release();
                    }
                });
            } else {
                this.queueEvent(new Runnable(){

                    @Override
                    public final void run() {
                        if (nativePause()) {
                            isPaused = true;
                            stop();
                            semaphore.release(2);
                            return;
                        }
                        semaphore.release();
                    }
                });
            }
            try {
                if (!semaphore.tryAcquire(4, TimeUnit.SECONDS)) {
                    UnityLog.Log(Log.WARN, "Timeout while trying to pause the Unity Engine.");
                }
            }
            catch (InterruptedException v0) {
                UnityLog.Log(Log.WARN, "UI thread got interrupted while trying to pause the Unity Engine.");
            }
            if (semaphore.drainPermits() > 0) {
                this.quit();
            }
        }
        this.mUnityEnvironment.setIsPreparedEnvironment(false);
        this.mUnityEnvironment.setPaused(true);
        this.closeCamera();
        this.glThread.notifyPause();
        this.mUnityLocation.safeClose();
    }

    public void resume() {
        if (UnityConstants.IS_GE_SDK_VERSION_11) {
            UnityConstants.sUnityUiController.updateViewUiVisibility(this);
        }
        this.mUnityEnvironment.setPaused(false);
        this.prepareEnvironment();
    }

    private void stop() {
        this.nativeDone();
    }

    private void prepareEnvironment() {
        if (!this.mUnityEnvironment.isRunning()) {
            return;
        }

        if (this.mUnityVedioPlayer != null) {
            this.mUnityVedioPlayer.onResume();
            return;
        }

        this.mUnityEnvironment.setIsPreparedEnvironment(true);
        this.initCamera();
        this.mUnityLocation.safeStartLocation();
        if (UnityEnvironment.isRuntimeLibraryLoaded()) {
            this.checkUseObbFile();
        }
        this.queueEvent(new Runnable(){

            @Override
            public final void run() {
                nativeResume();
            }
        });

        this.glThread.notifyResume();
    }

    public void configurationChanged(Configuration configuration) {
        if (this.mSurfaceView instanceof SurfaceView) {
            this.mSurfaceView.getHolder().setSizeFromLayout();
        }
        if (this.mUnityVedioPlayer != null) {
            this.mUnityVedioPlayer.updateVideoLayout();
        }
    }

    public void windowFocusChanged(final boolean hasFocus) {
        mUnityEnvironment.setWindowFocus(hasFocus);
        if (hasFocus && this.mEditTextDialog != null) {
            this.reportSoftInputStr(null, 1, false);
        }
        if (UnityConstants.IS_GE_SDK_VERSION_11 && hasFocus) {
            UnityConstants.sUnityUiController.updateViewUiVisibility(this);
        }
        this.queueEvent(new Runnable(){

            @Override
            public final void run() {
                nativeFocusChanged(hasFocus);
            }
        });
        this.glThread.notifyFocusChanged(hasFocus);
        this.prepareEnvironment();
    }

    public static boolean loadStatic(String string) {
        try {
            System.load(string);
        }
        catch (UnsatisfiedLinkError v0) {
            UnityLog.Log(Log.ERROR, "Unable to find " + string);
            return false;
        }
        catch (Exception var0_1) {
            UnityLog.Log(Log.ERROR, "Unknown error " + var0_1);
            return false;
        }
        return true;
    }

    public static boolean loadLibraryStatic(String string) {
        try {
            System.loadLibrary(string);
        }
        catch (UnsatisfiedLinkError v0) {
            UnityLog.Log(Log.ERROR, "Unable to find " + string);
            return false;
        }
        catch (Exception var0_1) {
            UnityLog.Log(Log.ERROR, "Unknown error " + var0_1);
            return false;
        }
        return true;
    }

    public boolean loadLibrary(String library) {
        return UnityPlayer.loadLibraryStatic(library);
    }

    public void startActivityIndicator() {
        this.runOnUiThread(this.rShowProgress);
    }

    public void stopActivityIndicator() {
        this.runOnUiThread(this.rCloseProgress);
    }

    public static void lockNativeAccess() {
        sLock.lock();
    }

    public static void unlockNativeAccess() {
        sLock.unlock();
    }

    private static void loadRuntimeLibrary(String sharelibraryPath) {
        Log.w(TAG, "loadRuntimeLibrary sharelibraryPath:" + sharelibraryPath);
        if (isLoadedMainLibrary && NativeLoader.load(sharelibraryPath)) {
            UnityEnvironment.setRuntimeLibraryLoadFinished();
        }
        UnityEnvironment.setRuntimeLibraryLoadFinished();
    }

    private static void loadRuntimeLibrary(ApplicationInfo applicationInfo) {
        if (isLoadedMainLibrary && NativeLoader.load(applicationInfo.nativeLibraryDir)) {
            UnityEnvironment.setRuntimeLibraryLoadFinished();
        }
        UnityEnvironment.setRuntimeLibraryLoadFinished();
    }

    private static void unloadRuntimeLibrary() {
        if (!UnityEnvironment.isRuntimeLibraryLoaded()) {
            return;
        }
        UnityPlayer.lockNativeAccess();
        if (!NativeLoader.unload()) {
            UnityPlayer.unlockNativeAccess();
            throw new UnsatisfiedLinkError("Unable to unload libraries from libmain.so");
        }
        UnityEnvironment.setRuntimeLibraryUnload();
        UnityPlayer.unlockNativeAccess();
    }

    public void forwardMotionEventToDalvik(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, float[] arrf, int metaState, float xPrecision, float yPrecision, int deviceId, int edgeFlags, int source, int flags, int endIndex, long[] arrEventTime, float[] allPointerCoordValues) {
        mUnityMotionEventHandler.triggerMotionEvent(downTime, eventTime, action, pointerCount, pointerIds, arrf, metaState, xPrecision, yPrecision, deviceId, edgeFlags, source, flags, endIndex, arrEventTime, allPointerCoordValues);
    }

    public void setFullscreen(final boolean hasSystemDefaultProperties) {
        if (UnityConstants.IS_GE_SDK_VERSION_11) {
            this.runOnUiThread(new Runnable(){
                @Override
                public final void run() {
                    UnityConstants.sUnityUiController.setViewUIVisibility(UnityPlayer.this, hasSystemDefaultProperties);
                }
            });
        }
    }

    public void showSoftInput(final String content, final int index, final boolean isAutoCorrect, final boolean isSupportMutiLines, final boolean isValidPassword, final boolean bl4, final String hint) {
        final UnityPlayer unityPlayer = this;
        this.runOnUiThread(new Runnable(){

            @Override
            public final void run() {
                mEditTextDialog = new UnityEditTextDialog(mContext, unityPlayer, content, index, isAutoCorrect, isSupportMutiLines, isValidPassword, hint);
                mEditTextDialog.show();
            }
        });
    }

    public void hideSoftInput() {
        final Runnable runnable = new Runnable(){

            @Override
            public final void run() {
                if (mEditTextDialog != null) {
                    mEditTextDialog.dismiss();
                    mEditTextDialog = null;
                }
            }
        };
        if (UnityConstants.IS_GE_SDK_VERSION_21) {
            this.captureScreen(new UnityRecordVideoTask(){

                @Override
                public final void onRecordVideoUpdate() {
                    runOnUiThread(runnable);
                }
            });
            return;
        }
        this.runOnUiThread(runnable);
    }

    public void setSoftInputStr(final String string) {
        this.runOnUiThread(new Runnable(){

            @Override
            public final void run() {
                if (mEditTextDialog != null && string != null) {
                    mEditTextDialog.selectText(string);
                }
            }
        });
    }

    public void reportSoftInputStr(final String string, final int n2, final boolean bl) {
        if (n2 == 1) {
            this.hideSoftInput();
        }
        this.captureScreen(new UnityRecordVideoTask(){

            @Override
            public void onRecordVideoUpdate() {
                if (bl) {
                    nativeSetInputCanceled(true);
                } else if (string != null) {
                    nativeSetInputString(string);
                }
                if (n2 == 1) {
                    nativeSoftInputClosed();
                }
            }
        });
    }

    public int[] initCamera(int n2, int n3, int n4, int n5) {
        UnityCamera a2 = new UnityCamera(n2, n3, n4, n5);
        try {
            a2.setCallbackBuffer(this);
            this.mListUnityCameras.add(a2);
            Camera.Size size = a2.getPreviewSize();
            return new int[]{size.width, size.height};
        }
        catch (Exception var2_5) {
            String string = "Unable to initialize camera: " + var2_5.getMessage();
            UnityLog.Log(Log.ERROR, string);
            a2.close();
            return null;
        }
    }

    public void closeCamera(int n2) {
        for (UnityCamera a2 : this.mListUnityCameras) {
            if (a2.getCameraId() != n2) continue;
            a2.close();
            this.mListUnityCameras.remove(a2);
            return;
        }
    }

    public int getNumCameras() {
        if (!this.hasCamera()) {
            return 0;
        }
        return Camera.getNumberOfCameras();
    }

    @Override
    public void onCameraFrame(final UnityCamera camera, final byte[] arrby) {
        final int cameraId = camera.getCameraId();
        final Camera.Size size = camera.getPreviewSize();
        this.captureScreen(new UnityRecordVideoTask(){

            @Override
            public void onRecordVideoUpdate() {
                nativeVideoFrameCallback(cameraId, arrby, size.width, size.height);
                camera.setCallbackBuffer(arrby);
            }
        });
    }

    public boolean isCameraFrontFacing(int n2) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo((int)n2, (Camera.CameraInfo)cameraInfo);
        if (cameraInfo.facing == 1) {
            return true;
        }
        return false;
    }

    public int getCameraOrientation(int n2) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo((int)n2, (Camera.CameraInfo)cameraInfo);
        return cameraInfo.orientation;
    }

    public void showVideoPlayer(final String fileName, final int backgroundColor, final int controlMode, final int scalingMode, final boolean isUrl, final int videoOffset, final int videoLength) {
        this.runOnUiThread(new Runnable(){

            @Override
            public final void run() {
                if (mUnityVedioPlayer != null) {
                    return;
                }
                pause();
                mUnityVedioPlayer = new UnityVedioPlayer(UnityPlayer.this, mContext, fileName, backgroundColor, controlMode, scalingMode, isUrl, videoOffset, videoLength);
                addView(mUnityVedioPlayer);
                mUnityVedioPlayer.requestFocus();
                mUnityViewManager.removeGameSurfaceView(mSurfaceView);
            }
        });
    }

    public void hideVideoPlayer() {
        this.runOnUiThread(new Runnable(){

            @Override
            public final void run() {
                if (mUnityVedioPlayer == null) {
                    return;
                }
                mUnityViewManager.addGameSurfaceView(mSurfaceView);
                removeView(mUnityVedioPlayer);
                mUnityVedioPlayer = null;
                resume();
            }
        });
    }

    public void Location_SetDesiredAccuracy(float desiredAccuracy) {
        this.mUnityLocation.setAccuracy(desiredAccuracy);
    }

    public void Location_SetDistanceFilter(float f2) {
        this.mUnityLocation.setMinDistance(f2);
    }

    public void Location_StartUpdatingLocation() {
        this.mUnityLocation.startLocation();
    }

    public void Location_StopUpdatingLocation() {
        this.mUnityLocation.enforceClose();
    }

    public boolean Location_IsServiceEnabledByUser() {
        return this.mUnityLocation.hasAvailableProvider();
    }

    private boolean hasCamera() {
        if (this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return true;
        }
        return false;
    }

    public int getSplashMode() {
        return this.bundle.getInt("splash_mode");
    }

    public void executeGLThreadJobs() {
        Runnable runnable;
        while ((runnable = (Runnable)this.mPendingTasks.poll()) != null) {
            runnable.run();
        }
    }

    public void disableLogger() {
        UnityLog.bshowLog = true;
    }

    void runOnGLTread(Runnable task) {
        queueEvent(task);
    }

    private void queueEvent(Runnable runnable) {
        if (!UnityEnvironment.isRuntimeLibraryLoaded()) {
            return;
        }
        if (Thread.currentThread() == this.glThread) {
            runnable.run();
            return;
        }
        this.mPendingTasks.add(runnable);
    }

    private void captureScreen(UnityRecordVideoTask captureVideoTask) {
        if (this.isFinishing()) {
            return;
        }
        this.queueEvent(captureVideoTask);
    }

    public boolean isFinishing() {
        if (this.isPaused || (this.mContext instanceof Activity && ((Activity)mContext).isFinishing())) {
            return true;
        }
        return false;
    }

    private void checkUseObbFile() {
        if (!this.bundle.getBoolean("useObb")) {
            return;
        }
        for (String obbFile : UnityPlayer.getObbFiles(mContext)) {
            String encryptData = UnityPlayer.getFileMd5(obbFile);
            if (this.bundle.getBoolean(encryptData)) {
                this.nativeFile(obbFile);
            }
            this.bundle.remove(encryptData);
        }
    }

    private static String[] getObbFiles(Context context) {
        int versionCode;
        String packageName = context.getPackageName();
        Vector<String> vector = new Vector<String>();
        try {
            versionCode = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        }
        catch (PackageManager.NameNotFoundException v0) {
            return new String[0];
        }
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String sObbDir = Environment.getExternalStorageDirectory() + "/Android/obb/" + packageName;
            if ((new File(sObbDir)).exists()) {
                String sVersionObbDir = sObbDir + File.separator + "main." + versionCode + "." + packageName + ".obb";
                if (versionCode > 0 && new File(sVersionObbDir).isFile()) {
                    vector.add(sVersionObbDir);
                }
                String sPatchObbDir = sObbDir + File.separator + "patch." + versionCode + "." + packageName + ".obb";
                if (versionCode > 0 && new File(sPatchObbDir).isFile()) {
                    vector.add(sPatchObbDir);
                }
            }
        }
        String[] object = new String[vector.size()];
        vector.toArray(object);
        return object;
    }

    private static String getFileMd5(String sourceFile) {
        int pos;
        InputStream is = null;
        byte[] arrBytes = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(sourceFile);
            long size = new File(sourceFile).length();
            is.skip(size - Math.min(size, 65558));
            byte[] bs = new byte[1024];
            pos = 0;
            while (pos != -1) {
                pos = is.read(bs);
                messageDigest.update(bs, 0, pos);
            }
            arrBytes = messageDigest.digest();
        }
        catch (FileNotFoundException v0) {}
        catch (IOException v1) {}
        catch (NoSuchAlgorithmException v2) {}
        finally {
            if(null != is ) {
                try {
                    is.close();
                } catch(Exception ex) {
                    ex.printStackTrace();;
                }
            }
        }
        if (arrBytes == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (pos = 0; pos < arrBytes.length; ++pos) {
            sb.append(Integer.toString((arrBytes[pos] & 255) + 256, 16).substring(1));
        }
        return sb.toString();
    }

    private void enforeFullScreen() {
        ((Activity)mContext).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
    }

    public boolean injectEvent(InputEvent inputEvent) {
        return this.nativeInjectEvent(inputEvent);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
        return this.injectEvent(keyEvent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        return this.injectEvent(keyEvent);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return this.injectEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.injectEvent(motionEvent);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return this.injectEvent(motionEvent);
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    static {
        new UnityUncaughtExceptionHandler().init();
        try{
            System.loadLibrary("main");
            isLoadedMainLibrary = true;
        } catch (UnsatisfiedLinkError v0) {
            UnityLog.Log(Log.ERROR, "Unable to find libmain.so");
            isLoadedMainLibrary = false;
        }
        catch (Exception var0_1) {
            UnityLog.Log(Log.ERROR, "Unknown error " + var0_1);
            isLoadedMainLibrary = false;
        }
        sLock = new ReentrantLock();
    }

    private abstract class UnityRecordVideoTask implements Runnable {
        private UnityRecordVideoTask() {

        }

        @Override
        public void run() {
            if (!isFinishing()) {
                onRecordVideoUpdate();
            }
        }

        public abstract void onRecordVideoUpdate();
    }

    private final class GLThread extends Thread {
        ArrayBlockingQueue arrBlockQueue;
        boolean isAlive;

        GLThread() {
            this.isAlive = false;
            this.arrBlockQueue = new ArrayBlockingQueue(32);
        }

        @Override
        public final void run() {
            this.setName("UnityMain");
            try {
                EnumCommand enumCommand;
                while ((enumCommand = (EnumCommand)(this.arrBlockQueue.take())) != EnumCommand.QUIT) {
                    if (enumCommand == EnumCommand.RESUME) {
                        this.isAlive = true;
                    } else if (enumCommand == EnumCommand.PAUSE) {
                        this.isAlive = false;
                        executeGLThreadJobs();
                    } else if (enumCommand == EnumCommand.FOCUS_LOST && !this.isAlive) {
                        executeGLThreadJobs();
                    }
                    if (!this.isAlive) continue;
                    do {
                        executeGLThreadJobs();
                        if (this.arrBlockQueue.peek() != null) break;
                        boolean b = isFinishing();
                        if (b || nativeRender()) continue;
                        notifyResume();
                    } while (!interrupted());
                }
                return;
            }
            catch (InterruptedException v0) {
                return;
            }
        }

        public void notifyQuit() {
            this.addCommand(EnumCommand.QUIT);
        }

        public void notifyResume() {
            this.addCommand(EnumCommand.RESUME);
        }

        public void notifyPause() {
            this.addCommand(EnumCommand.PAUSE);
        }

        public final void notifyFocusChanged(boolean hasFocus) {
            this.addCommand(hasFocus ? EnumCommand.FOCUS_GAINED : EnumCommand.FOCUS_LOST);
        }

        private void addCommand(EnumCommand command) {
            try {
                this.arrBlockQueue.put(command);
                return;
            }
            catch (InterruptedException v0) {
                this.interrupt();
                return;
            }
        }
    }

    static enum EnumCommand {
        PAUSE("PUASE", 0),
        RESUME("RESUME", 1),
        QUIT("QUIT", 2),
        FOCUS_GAINED("FOCUS_GAINED", 3),
        FOCUS_LOST("FOCUS_LOST", 4);

        private String _command;
        private int _code;


        EnumCommand(String command, int code) {
            _command = command;
            _code = code;
        }

        public String getCommand() {
            return _command;
        }

        public int getCode() {
            return _code;
        }

    }

    static native void UnitySendMessage(String gameObject, String method, String content);

    static native void nativeUnitySendMessage(String gameObject, String methodName, String content);

    private native void initJni(Context context);

    private native int nativeActivityIndicatorStyle();

    native void nativeAddVSyncTime(long var1);

    private native void nativeDone();

    private native void nativeFile(String var1);

    private native void nativeFocusChanged(boolean var1);

    native void nativeForwardEventsToDalvik(boolean var1);

    private native void nativeInitWWW(Class var1);

    private native void nativeInitWebRequest(Class var1);

    private native boolean nativeInjectEvent(InputEvent var1);

    private native boolean nativePause();

    private native void nativeRecreateGfxState(int var1, Surface var2);

    private native boolean nativeRender();

    private native void nativeResume();

    private native void nativeSetExtras(Bundle var1);

    private native void nativeSetInputCanceled(boolean var1);

    private native void nativeSetInputString(String var1);

    native void nativeSetLocationStatus(int status);

    native void nativeSetLocation(float var1, float var2, float var3, float var4, double var5, float var7);

    private native void nativeSetTouchDeltaY(float var1);

    private native void nativeSoftInputClosed();

    private native void nativeVideoFrameCallback(int var1, byte[] var2, int var3, int var4);
}

