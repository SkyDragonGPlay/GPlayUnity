package com.unity3d.player;

import android.app.Presentation;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public final class UnityPresentation implements UnityDisplayListener {
    private Object lockObject = new Object[0];
    private Presentation mPresentation;
    private DisplayManager.DisplayListener mDisplayListener;

    @Override
    public void registerDisplayListener(final UnityPlayer unityPlayer, final Context context) {
        DisplayManager dm = (DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) return;
        DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener(){

            public final void onDisplayAdded(int displayId) {
                unityPlayer.displayChanged(-1, null);
            }

            public final void onDisplayRemoved(int displayId) {
                unityPlayer.displayChanged(-1, null);
            }

            public final void onDisplayChanged(int displayId) {
                unityPlayer.displayChanged(-1, null);
            }
        };
        this.mDisplayListener = listener;
        dm.registerDisplayListener(listener, null);
    }

    @Override
    public void unregisterDisplayListener(Context context) {
        if (this.mDisplayListener == null) {
            return;
        }
        DisplayManager dm = (DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) {
            return;
        }
        dm.unregisterDisplayListener(mDisplayListener);
    }

    @Override
    public boolean show(final UnityPlayer unityPlayer, final Context context, int displayId) {
        synchronized (lockObject) {
            Display display;
            if (this.mPresentation != null
                    && this.mPresentation.isShowing()
                    && (display = this.mPresentation.getDisplay()) != null
                    && display.getDisplayId() == displayId) {
                return true;
            }
            DisplayManager displayManager = (DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
            if (displayManager == null) {
                return false;
            }
            final Display localDisplay = displayManager.getDisplay(displayId);
            if (localDisplay == null) {
                return false;
            }
            unityPlayer.runOnUiThread(new Runnable(){

                @Override
                public final void run() {
                    synchronized (lockObject) {
                        if (mPresentation != null) {
                            mPresentation.dismiss();
                        }
                        mPresentation = new Presentation(context, localDisplay){

                            protected final void onCreate(Bundle bundle) {
                                SurfaceView surfaceView = new SurfaceView(context);
                                surfaceView.getHolder().addCallback(new SurfaceHolder.Callback(){

                                    public final void surfaceCreated(SurfaceHolder surfaceHolder) {
                                        unityPlayer.displayChanged(1, surfaceHolder.getSurface());
                                    }

                                    public final void surfaceChanged(SurfaceHolder holder, int format, int width,
                                                                     int height) {
                                        unityPlayer.displayChanged(1, holder.getSurface());
                                    }

                                    public final void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                                        unityPlayer.displayChanged(1, null);
                                    }
                                });
                                this.setContentView(surfaceView);
                            }

                            public final void onDisplayRemoved() {
                                this.dismiss();
                                synchronized (lockObject) {
                                    mPresentation = null;
                                    return;
                                }
                            }

                        };
                        mPresentation.show();
                        return;
                    }
                }

            });
            return true;
        }
    }

}

