package com.skydragon.gplay.unity.simpledemo;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;

import com.skydragon.gplay.runtime.bridge.IBridgeProxy;
import com.skydragon.gplay.runtime.bridge.ICallback;
import com.skydragon.gplay.runtime.bridge.IEngineRuntimeBridge;
import com.skydragon.gplay.runtime.bridge.IEngineRuntimeGetBridge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Map;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final String RUNTIME_GET_BRIDGE_CLASS_NAME = "com.skydragon.gplay.runtime.bridge.RuntimeGetBridge";
    private String mLibrarySavePath;
    private String mDexPath;

    private IEngineRuntimeBridge mRuntimeBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("zjf@ MainActivity onCreate start 1");
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        initRuntime();
        System.out.println("zjf@ MainActivity onCreate start 2");
        mRuntimeBridge.initRuntimeJNI();
        System.out.println("zjf@ MainActivity onCreate start 3");
        mRuntimeBridge.init(this);
        System.out.println("zjf@ MainActivity onCreate start 4");
        mRuntimeBridge.startGame();
        System.out.println("zjf@ MainActivity onCreate start 5");
        this.setContentView( mRuntimeBridge.getEngineLayout());
        System.out.println("zjf@ MainActivity onCreate start 6");
    }

    private void init() {
        String dataDir = this.getApplicationInfo().dataDir;
        File f = new File(dataDir, "gplay");
        if(f.exists()) {
            f.mkdirs();
        }

        File fDex = new File(f.getAbsolutePath(), "dex");
        fDex.mkdirs();
        mDexPath = fDex.getAbsolutePath();
        mLibrarySavePath = f.getAbsolutePath();
    }

    private void initRuntime() {
        init();
        extractSo();
        extractJar();
        loadRuntimeBridge();
    }

    private void loadRuntimeBridge() {
        try {

            ClassLoader parent = this.getClass().getClassLoader();

            File fJarFile = new File(mLibrarySavePath, "libunityruntime.jar");

            //通过指定share library 存放的路径, 就可以通过System.loadLibrary来加载so by zjf 20160831
            DexClassLoader clsLoader = new DexClassLoader(fJarFile.getAbsolutePath(), mDexPath, mLibrarySavePath, parent);

            Class<?> cls = clsLoader.loadClass(RUNTIME_GET_BRIDGE_CLASS_NAME);
            Constructor<?> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            IEngineRuntimeGetBridge bridgeGet = (IEngineRuntimeGetBridge) ctor.newInstance();
            mRuntimeBridge = bridgeGet.getRuntimeBridge();

        } catch (Exception e) {
            e.printStackTrace();;
        }

        if(null == mRuntimeBridge) {
            Log.e(TAG, "runtimeBridge is null!!!");
            return;
        }
        mRuntimeBridge.setBridgeProxy(new IBridgeProxy() {
            @Override
            public Object invokeMethodSync(String method, Map<String, Object> args) {
                if(method.equalsIgnoreCase("getGameResourceDir")) {
                    File resourceDir = new File(Environment.getExternalStorageDirectory(), "unitydemo/resources");
                    return resourceDir.getAbsolutePath() + "/";
                } else if(method.equalsIgnoreCase("getSharedLibraryPath")) {
                    return mLibrarySavePath;
                }
                return null;
            }

            @Override
            public void invokeMethodAsync(String method, Map<String, Object> args, ICallback callback) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRuntimeBridge.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRuntimeBridge.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRuntimeBridge.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mRuntimeBridge.onWindowFocusChanged(hasFocus);
    }

    private void extractSo() {
        AssetManager assetManager = this.getAssets();

        InputStream is = null;
        OutputStream os = null;
        try {
            String[] soFiles = assetManager.list("libs");
            for(String soFile : soFiles) {
                is = assetManager.open("libs/" + soFile);
                os = new FileOutputStream(new File(mLibrarySavePath, soFile));
                byte[] buffer = new byte[4096];
                int size = -1;
                while((size=is.read(buffer)) != -1 ) {
                    os.write(buffer, 0, size);
                }
                os.flush();
            }
            tryClose(is);
            tryClose(os);
        } catch( Exception e ) {
            e.printStackTrace();
            tryClose(is);
            tryClose(os);
        }
    }

    private void extractJar() {
        AssetManager assetManager = this.getAssets();

        InputStream is = null;
        OutputStream os = null;
        try {
            is = assetManager.open("runtime/libunityruntime.jar");
            os = new FileOutputStream(new File(mLibrarySavePath, "libunityruntime.jar"));
            byte[] buffer = new byte[4096];
            int size = -1;
            while((size=is.read(buffer)) != -1 ) {
                os.write(buffer, 0, size);
            }
            os.flush();
        } catch( Exception e ) {
            e.printStackTrace();
        } finally {
            tryClose(is);
            tryClose(os);
        }
    }

    void tryClose(InputStream is) {
        if( null == is ) return;
        try{
            is.close();
        } catch( Exception e) {
            e.printStackTrace();
        }
        is = null;
    }

    void tryClose(OutputStream os) {
        if( null == os ) return;
        try{
            os.close();
        } catch( Exception e) {
            e.printStackTrace();
        }
        os = null;
    }
}
