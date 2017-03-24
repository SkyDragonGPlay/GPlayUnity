
package com.unity3d.player;

public interface UnityMotionEvent {
    void triggerMotionEvent(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, float[] arrf, int metaState, float xPrecision, float yPrecision, int deviceId, int edgeFlags, int source, int flags, int endIndex, long[] arrEventTime, float[] allPointerCoordValues);
}

