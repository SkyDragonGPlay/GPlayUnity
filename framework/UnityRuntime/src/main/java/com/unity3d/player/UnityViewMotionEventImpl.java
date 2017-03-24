package com.unity3d.player;

import android.view.MotionEvent;
import android.view.View;

public class UnityViewMotionEventImpl implements UnityViewMotionEvent {
    @Override
    public final boolean registerMotionEvent(View view, MotionEvent motionEvent) {
        return view.dispatchGenericMotionEvent(motionEvent);
    }
}

