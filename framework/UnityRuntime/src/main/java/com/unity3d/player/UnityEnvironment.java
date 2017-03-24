package com.unity3d.player;

final class UnityEnvironment {
    private static boolean isRuntimeLibraryLoaded = false;
    private boolean isSDKVersionLT23 = !UnityConstants.IS_GE_SDK_VERSION_23;
    private boolean hasWindowFocus = false;
    private boolean isPreparedEnvironment = false;
    private boolean isPause = true;

    UnityEnvironment() {
    }

    static void setRuntimeLibraryLoadFinished() {
        isRuntimeLibraryLoaded = true;
    }

    static void setRuntimeLibraryUnload() {
        isRuntimeLibraryLoaded = false;
    }

    static boolean isRuntimeLibraryLoaded() {
        return isRuntimeLibraryLoaded;
    }

    final void setSDKVersionLT23() {
        this.isSDKVersionLT23 = true;
    }

    final void setWindowFocus(boolean hasFoucus) {
        this.hasWindowFocus = hasFoucus;
    }

    final void setPaused(boolean paused) {
        this.isPause = paused;
    }

    final boolean isPaused() {
        return this.isPause;
    }

    final void setIsPreparedEnvironment(boolean bl) {
        this.isPreparedEnvironment = bl;
    }

    final boolean isRunning() {

        if (isRuntimeLibraryLoaded && this.hasWindowFocus && this.isSDKVersionLT23 && !this.isPause && !this.isPreparedEnvironment) {
            return true;
        }
        return false;
    }

    final boolean hasPreparedEnvironment() {
        return this.isPreparedEnvironment;
    }

    public final String toString() {
        return super.toString();
    }
}

