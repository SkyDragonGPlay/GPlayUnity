package com.skydragon.gplay.unitsdk.nativewrapper;

/**
 * Created by zhangjunfei on 16/1/30.
 */
public final class NativeWrapper {
    private static native void nativeAsyncActionResult(int ret, String msg, String callbackId);

    public static void onAsyncActionResult(int ret, String msg, String callbackId) {
        nativeAsyncActionResult(ret, msg, callbackId);
    }

}
