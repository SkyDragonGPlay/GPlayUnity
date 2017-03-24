package com.unity3d.player;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class UnityCamera {
    private final Object[] lockObject = new Object[0];
    private final int cameraId;
    private final int width;
    private final int height;
    private final int defaultFps;
    Camera camera;
    Camera.Parameters cameraParameters;
    Camera.Size propPreviewSize;
    int bitsPerPixel;
    int[] propPreviewFpsRange;
    UnityCameraSurface mUnitySurface;

    public UnityCamera(int cameraId, int width, int height, int fps) {
        this.cameraId = cameraId;
        this.width = optInt(width, 640);
        this.height = optInt(height, 480);
        this.defaultFps = optInt(fps, 24);
    }

    private void init(final onCamearFrameCallback callback) {
        synchronized (this.lockObject) {
            this.camera = Camera.open((int)this.cameraId);
            this.cameraParameters = this.camera.getParameters();
            this.propPreviewSize = this.getPropPreviewSize();
            this.propPreviewFpsRange = this.getPropPreViewFpsRange();
            this.bitsPerPixel = this.getBitsPerPixel();
            initParameters(cameraParameters);
            this.cameraParameters.setPreviewSize(this.propPreviewSize.width, this.propPreviewSize.height);
            this.cameraParameters.setPreviewFpsRange(this.propPreviewFpsRange[0], this.propPreviewFpsRange[1]);
            this.camera.setParameters(this.cameraParameters);
            Camera.PreviewCallback previewCallback = new Camera.PreviewCallback(){
                public final void onPreviewFrame(byte[] arrby, Camera paramCamera) {
                    if (camera != paramCamera) {
                        return;
                    }
                    callback.onCameraFrame(UnityCamera.this, arrby);
                }
            };
            int bufferSize = this.propPreviewSize.width * this.propPreviewSize.height * this.bitsPerPixel / 8 + 4096;
            this.camera.addCallbackBuffer(new byte[bufferSize]);
            this.camera.addCallbackBuffer(new byte[bufferSize]);
            this.camera.setPreviewCallbackWithBuffer(previewCallback);
            return;
        }
    }

    private static void initParameters(Camera.Parameters parameters) {
        if (parameters.getSupportedColorEffects() != null) {
            parameters.setColorEffect("none");
        }
        if (parameters.getSupportedFocusModes().contains("continuous-video")) {
            parameters.setFocusMode("continuous-video");
        }
    }

    public int getCameraId() {
        return this.cameraId;
    }

    public Camera.Size getPreviewSize() {
        return this.propPreviewSize;
    }

    public final void setCallbackBuffer(onCamearFrameCallback callback) {
        synchronized (this.lockObject) {
            if (this.camera == null) {
                this.init(callback);
            }
            if (UnityConstants.IS_GE_SDK_VERSION_11 && UnityConstants.sUnityUiController.setDefaultSurfaceTexture(this.camera)) {
                this.camera.startPreview();
                return;
            }
            if (this.mUnitySurface == null) {
                this.mUnitySurface = new UnityCameraSurface(){
                    Camera localCamera = camera;

                    public final void surfaceCreated(SurfaceHolder surfaceHolder) {
                        synchronized (lockObject) {
                            if (localCamera != camera) {
                                return;
                            }
                            try {
                                localCamera.setPreviewDisplay(surfaceHolder);
                                localCamera.startPreview();
                            }
                            catch (Exception var1_2) {
                                UnityLog.Log(6, "Unable to initialize webcam data stream: " + var1_2.getMessage());
                            }
                            return;
                        }
                    }

                    @Override
                    public final void surfaceDestroyed(SurfaceHolder arrobject) {
                        synchronized (UnityCamera.this.lockObject) {
                            if (localCamera != camera) {
                                return;
                            }
                            localCamera.stopPreview();
                            return;
                        }
                    }
                };
                this.mUnitySurface.create();
            }
            return;
        }
    }

    public void setCallbackBuffer(byte[] arrby) {
        synchronized (this.lockObject) {
            if (this.camera != null) {
                this.camera.addCallbackBuffer(arrby);
            }
            return;
        }
    }

    public void close() {
        synchronized (this.lockObject) {
            if (this.camera != null) {
                this.camera.setPreviewCallbackWithBuffer(null);
                this.camera.stopPreview();
                this.camera.release();
                this.camera = null;
            }
            if (this.mUnitySurface != null) {
                this.mUnitySurface.destroy();
                this.mUnitySurface = null;
            }
            return;
        }
    }

    private int getBitsPerPixel() {
        this.cameraParameters.setPreviewFormat(ImageFormat.NV21);
        return ImageFormat.getBitsPerPixel(ImageFormat.NV21);
    }

    private int[] getPropPreViewFpsRange() {
        double sysFps = this.defaultFps * 1000;
        List<int[]> listFps = this.cameraParameters.getSupportedPreviewFpsRange();
        if (listFps == null) {
            listFps = new ArrayList();
        }
        int[] arrn = new int[]{this.defaultFps * 1000, this.defaultFps * 1000};
        double initValue = Double.MAX_VALUE;
        for (int[] fptss : listFps) {
            double fps = Math.abs(Math.log(sysFps / (double)fptss[0])) + Math.abs(Math.log(sysFps / (double)fptss[1]));
            if (fps >= initValue) continue;
            initValue = fps;
            arrn = fptss;
        }
        return arrn;
    }

    private Camera.Size getPropPreviewSize() {
        double dWidth = this.width;
        double dHeight = this.height;
        List<Camera.Size> listSizes = this.cameraParameters.getSupportedPreviewSizes();
        Camera.Size size = null;
        double initSize = Double.MAX_VALUE;
        Iterator<Camera.Size> iter= listSizes.iterator();
        while (iter.hasNext()) {
            Camera.Size previewSize = (Camera.Size)iter.next();
            double dSize = Math.abs(Math.log(dWidth / (double)previewSize.width)) + Math.abs(Math.log(dHeight / (double)previewSize.height));
            if (dSize >= initSize) continue;
            initSize = dSize;
            size = previewSize;
        }
        return size;
    }

    private static int optInt(int value, int defaultValue) {
        if (value != 0) {
            return value;
        }
        return defaultValue;
    }

    static interface onCamearFrameCallback {
        public void onCameraFrame(UnityCamera object, byte[] arrby);
    }

}

