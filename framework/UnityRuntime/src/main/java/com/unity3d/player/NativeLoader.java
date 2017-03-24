
package com.unity3d.player;

public class NativeLoader {
    static native boolean load(String shareLibraryPath);

    static native boolean unload();
}

