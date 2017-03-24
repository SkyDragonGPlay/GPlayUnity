package com.unity3d.player;

import android.util.Log;
import android.view.Choreographer;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UnityVSYNCtimingImpl implements UnityVSYNCtiming {
    private Choreographer mChoreographer = null;
    private long b = 0;
    private Choreographer.FrameCallback mFrameCallback;
    private Lock mLock = new ReentrantLock();

    @Override
    public void startVSYNCtiming(final UnityPlayer unityPlayer) {
        this.mLock.lock();
        if (this.mChoreographer == null) {
            this.mChoreographer = Choreographer.getInstance();
            if (this.mChoreographer != null) {
                UnityLog.Log(Log.INFO, "Choreographer available: Enabling VSYNC timing");
                this.mFrameCallback = new Choreographer.FrameCallback(){

                    public final void doFrame(long frameTimeNanos) {
                        UnityPlayer.lockNativeAccess();
                        if (UnityEnvironment.isRuntimeLibraryLoaded()) {
                            unityPlayer.nativeAddVSyncTime(frameTimeNanos);
                        }
                        UnityPlayer.unlockNativeAccess();
                        UnityVSYNCtimingImpl.this.mLock.lock();
                        if (mChoreographer != null) {
                            mChoreographer.postFrameCallback(UnityVSYNCtimingImpl.this.mFrameCallback);
                        }
                        UnityVSYNCtimingImpl.this.mLock.unlock();
                    }
                };
                this.mChoreographer.postFrameCallback(this.mFrameCallback);
            }
        }
        this.mLock.unlock();
    }

    @Override
    public void stopVSYNCtiming() {
        this.mLock.lock();
        if (this.mChoreographer != null) {
            this.mChoreographer.removeFrameCallback(this.mFrameCallback);
        }
        this.mChoreographer = null;
        this.mLock.unlock();
    }

}

