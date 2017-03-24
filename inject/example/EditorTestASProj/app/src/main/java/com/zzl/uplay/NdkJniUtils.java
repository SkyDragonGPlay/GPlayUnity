package com.zzl.uplay;

/**
 * Created by zzl on 2016/7/28.
 */
public class NdkJniUtils {
    static{
        System.loadLibrary("gplay");
    }

    public native void OpenFile(String filePath);
    public native void Start(String libPath);
    public native void HookFopen();
    public native void UnHookFopen();
    public native String StringFromJNI();
}
