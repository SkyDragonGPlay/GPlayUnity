package com.unity3d.player;

import android.view.MotionEvent;
import android.view.View;

public interface UnityViewMotionEvent {

    public boolean registerMotionEvent(View view, MotionEvent motionEvent);
}

