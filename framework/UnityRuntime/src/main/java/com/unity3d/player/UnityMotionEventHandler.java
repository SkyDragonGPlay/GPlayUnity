package com.unity3d.player;

import android.app.Activity;
import android.content.Context;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UnityMotionEventHandler implements UnityMotionEvent {
    private final Queue<MotionEvent> mQueueMotionEvent = new ConcurrentLinkedQueue<MotionEvent>();
    private final Activity mActivity;
    private Runnable mRunnbale;

    public UnityMotionEventHandler(Context context) {
        this.mRunnbale = new Runnable(){

            private void safeRegisterMotionEvent(View view, MotionEvent motionEvent) {
                if (UnityConstants.IS_GE_SDK_VERSION_12) {
                    UnityConstants.sUnityViewMotionEvent.registerMotionEvent(view, motionEvent);
                }
            }

            @Override
            public final void run() {
                MotionEvent motionEvent;
                while ((motionEvent = mQueueMotionEvent.poll()) != null) {
                    View view = mActivity.getWindow().getDecorView();
                    int source = motionEvent.getSource();
                    if ((source & InputDevice.SOURCE_CLASS_POINTER) != 0) {
                        switch (motionEvent.getAction() & 255) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_MOVE:
                            case MotionEvent.ACTION_CANCEL:
                            case MotionEvent.ACTION_OUTSIDE:
                            case MotionEvent.ACTION_POINTER_DOWN:
                            case MotionEvent.ACTION_POINTER_UP: {
                                view.dispatchTouchEvent(motionEvent);
                                break;
                            }
                        }
                        safeRegisterMotionEvent(view, motionEvent);
                        continue;
                    }
                    if ((source & InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {
                        view.dispatchTrackballEvent(motionEvent);
                        continue;
                    }
                    safeRegisterMotionEvent(view, motionEvent);
                }
            }
        };
        this.mActivity = (Activity)context;
    }

    private static MotionEvent.PointerCoords[] createPointerCoords(int pointerCount, float[] allPointerCoordValues) {
        MotionEvent.PointerCoords[] arrpointerCoords = new MotionEvent.PointerCoords[pointerCount];
        assemblePointerCoords(arrpointerCoords, allPointerCoordValues, 0);
        return arrpointerCoords;
    }

    private static int assemblePointerCoords(MotionEvent.PointerCoords[] arrpointerCoords, float[] allPointerCoordValues, int position) {
        for (int i = 0; i < arrpointerCoords.length; ++i) {
            MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
            pointerCoords.orientation = allPointerCoordValues[position++];
            pointerCoords.pressure = allPointerCoordValues[position++];
            pointerCoords.size = allPointerCoordValues[position++];
            pointerCoords.toolMajor = allPointerCoordValues[position++];
            pointerCoords.toolMinor = allPointerCoordValues[position++];
            pointerCoords.touchMajor = allPointerCoordValues[position++];
            pointerCoords.touchMinor = allPointerCoordValues[position++];
            pointerCoords.x = allPointerCoordValues[position++];
            pointerCoords.y = allPointerCoordValues[position++];
            arrpointerCoords[i] = pointerCoords;
        }
        return position;
    }

    @Override
    public void triggerMotionEvent(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, float[] arrf, int metaState, float xPrecision, float yPrecision, int deviceId, int edgeFlags, int source, int flags, int endIndex, long[] arrEventTime, float[] allPointerCoordValues) {
        if (this.mActivity != null) {
            MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime, action, pointerCount, pointerIds, createPointerCoords(pointerCount, arrf), metaState, xPrecision, yPrecision, deviceId, edgeFlags, source, flags);
            int position = 0;
            for (int n = 0; n < endIndex; ++n) {
                MotionEvent.PointerCoords[] arrpointerCoords = new MotionEvent.PointerCoords[pointerCount];
                position = assemblePointerCoords(arrpointerCoords, allPointerCoordValues, position);
                motionEvent.addBatch(arrEventTime[n], arrpointerCoords, metaState);
            }
            this.mQueueMotionEvent.add(motionEvent);
            this.mActivity.runOnUiThread(this.mRunnbale);
        }
    }

}

