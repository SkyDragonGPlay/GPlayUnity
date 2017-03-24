
package com.unity3d.player;

import android.app.Activity;

public interface ITaskExecutor {
    public void executeTaskByCheckPermission(Activity activity, Runnable task);
}

