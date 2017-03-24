package com.unity3d.player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;

import java.io.FileInputStream;
import java.io.IOException;

public final class UnityVedioPlayer extends FrameLayout implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback, MediaController.MediaPlayerControl {
    private static boolean bShowLog = false;
    private final UnityPlayer mUnityPlayer;
    private final Context mContext;
    private final SurfaceView mSurfaceView;
    private final SurfaceHolder mSurfaceHolder;
    private final String mFileName;
    private final int mControlMode;
    private final int mScalingMode;
    private final boolean mIsURL;
    private final long mVideoOffset;
    private final long mVideoLength;
    private final FrameLayout mInstance;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mVideoWidth;
    private int mVideoHeight;
    private MediaPlayer mMediaPlayer;
    private MediaController mMediaController;
    private boolean mIsVedioSizePrepared = false;
    private boolean mIsMediaPlayerPrepared = false;
    private int mPercent = 0;
    private boolean mIsPaused = false;
    private int mCurrentPosition = 0;
    private boolean mIsStart;

    private static void showLog(String msg) {
        Log.v("Video", "VideoPlayer: " + msg);
    }

    UnityVedioPlayer(UnityPlayer unityPlayer, Context context, String fileName, int backgroundColor, int controlMode, int scalingMode, boolean isUrl, long videoOffset, long videoLength) {
        super(context);
        this.mUnityPlayer = unityPlayer;
        this.mContext = context;
        this.mInstance = this;
        this.mSurfaceView = new SurfaceView(context);
        this.mSurfaceHolder = this.mSurfaceView.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.mInstance.setBackgroundColor(backgroundColor);
        this.mInstance.addView(mSurfaceView);
        this.mFileName = fileName;
        this.mControlMode = controlMode;
        this.mScalingMode = scalingMode;
        this.mIsURL = isUrl;
        this.mVideoOffset = videoOffset;
        this.mVideoLength = videoLength;
        if (bShowLog) {
            showLog("fileName: " + this.mFileName);
        }
        if (bShowLog) {
            showLog("backgroundColor: " + backgroundColor);
        }
        if (bShowLog) {
            showLog("controlMode: " + this.mControlMode);
        }
        if (bShowLog) {
            showLog("scalingMode: " + this.mScalingMode);
        }
        if (bShowLog) {
            showLog("isURL: " + this.mIsURL);
        }
        if (bShowLog) {
            showLog("videoOffset: " + this.mVideoOffset);
        }
        if (bShowLog) {
            showLog("videoLength: " + this.mVideoLength);
        }
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
        this.mIsStart = true;
    }

    public void onControllerHide() {

    }

    void onPause() {
        if (bShowLog) {
            showLog("onPause called");
        }
        if (!this.mIsPaused) {
            this.pause();
            this.mIsPaused = false;
        }
        if (this.mMediaPlayer != null) {
            this.mCurrentPosition = this.mMediaPlayer.getCurrentPosition();
        }
        this.mIsStart = false;
    }

    void onResume() {
        if (bShowLog) {
            showLog("onResume called");
        }
        if (!this.mIsStart && !this.mIsPaused) {
            this.start();
        }
        this.mIsStart = true;
    }

    void onDestroy() {
        this.onPause();
        this.doCleanUp();
        UnityPlayer.runOnNewThread(new Runnable(){

            @Override
            public final void run() {
                mUnityPlayer.hideVideoPlayer();
            }
        });
    }

    private void init() {
        this.doCleanUp();
        try {
            this.mMediaPlayer = new MediaPlayer();
            if (this.mIsURL) {
                this.mMediaPlayer.setDataSource(this.mContext, Uri.parse(this.mFileName));
            } else if (this.mVideoLength != 0) {
                FileInputStream fileInputStream = new FileInputStream(this.mFileName);
                this.mMediaPlayer.setDataSource(fileInputStream.getFD(), this.mVideoOffset, this.mVideoLength);
                fileInputStream.close();
            } else {
                AssetManager assetManager = this.getResources().getAssets();
                try {
                    AssetFileDescriptor fileDescriptor = assetManager.openFd(this.mFileName);
                    this.mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
                    fileDescriptor.close();
                }
                catch (IOException v0) {
                    FileInputStream is = new FileInputStream(this.mFileName);
                    this.mMediaPlayer.setDataSource(is.getFD());
                    is.close();
                }
            }
            this.mMediaPlayer.setDisplay(this.mSurfaceHolder);
            this.mMediaPlayer.setScreenOnWhilePlaying(true);
            this.mMediaPlayer.setOnBufferingUpdateListener(this);
            this.mMediaPlayer.setOnCompletionListener(this);
            this.mMediaPlayer.setOnPreparedListener(this);
            this.mMediaPlayer.setOnVideoSizeChangedListener(this);
            this.mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            this.mMediaPlayer.prepare();
            if (this.mControlMode == 0 || this.mControlMode == 1) {
                this.mMediaController = new MediaController(this.mContext);
                this.mMediaController.setMediaPlayer(this);
                this.mMediaController.setAnchorView(this);
                this.mMediaController.setEnabled(true);
                this.mMediaController.show();
            }
            return;
        }
        catch (Exception exception) {
            if (bShowLog) {
                showLog("error: " + exception.getMessage() + exception);
            }
            this.onDestroy();
            return;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK || this.mControlMode == 2 && keyCode != 0 && !keyEvent.isSystem()) {
            this.onDestroy();
            return true;
        }
        if (this.mMediaController != null) {
            return this.mMediaController.onKeyDown(keyCode, keyEvent);
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int n2 = motionEvent.getAction() & 0xFF;
        if (this.mControlMode == 2 && n2 == 0) {
            this.onDestroy();
            return true;
        }
        if (this.mMediaController != null) {
            return this.mMediaController.onTouchEvent(motionEvent);
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        if (bShowLog) {
            showLog("onBufferingUpdate percent:" + percent);
        }
        this.mPercent = percent;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (bShowLog) {
            showLog("onCompletion called");
        }
        this.onDestroy();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        if (bShowLog) {
            showLog("onVideoSizeChanged called " + width + "mCurrentPosition" + height);
        }
        if (width == 0 || height == 0) {
            if (bShowLog) {
                showLog("invalid video width(" + width + ") or height(" + height + ")");
            }
            return;
        }
        this.mIsVedioSizePrepared = true;
        this.mVideoWidth = width;
        this.mVideoHeight = height;
        if (this.mIsMediaPlayerPrepared && this.mIsVedioSizePrepared) {
            this.startVideoPlayback();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (bShowLog) {
            showLog("onPrepared called");
        }
        this.mIsMediaPlayerPrepared = true;
        if (this.mIsMediaPlayerPrepared && this.mIsVedioSizePrepared) {
            this.startVideoPlayback();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        if (bShowLog) {
            showLog("surfaceChanged called " + format + " " + width + "mCurrentPosition" + height);
        }
        if (this.mDisplayWidth != width || this.mDisplayHeight != height) {
            this.mDisplayWidth = width;
            this.mDisplayHeight = height;
            this.updateVideoLayout();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (bShowLog) {
            showLog("surfaceDestroyed called");
        }
        this.doCleanUp();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (bShowLog) {
            showLog("surfaceCreated called");
        }
        this.init();
        this.seekTo(this.mCurrentPosition);
    }

    protected void doCleanUp() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }
        this.mVideoWidth = 0;
        this.mVideoHeight = 0;
        this.mIsMediaPlayerPrepared = false;
        this.mIsVedioSizePrepared = false;
    }

    private void startVideoPlayback() {
        if (this.isPlaying()) {
            return;
        }
        if (bShowLog) {
            showLog("startVideoPlayback");
        }
        this.updateVideoLayout();
        if (!this.mIsPaused) {
            this.start();
        }
    }

    void updateVideoLayout() {
        if (bShowLog) {
            showLog("updateVideoLayout");
        }
        if (this.mDisplayWidth == 0 || this.mDisplayHeight == 0) {
            WindowManager windowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
            this.mDisplayWidth = windowManager.getDefaultDisplay().getWidth();
            this.mDisplayHeight = windowManager.getDefaultDisplay().getHeight();
        }
        int displayWidth = this.mDisplayWidth;
        int displayHeight = this.mDisplayHeight;
        float f2 = (float)this.mVideoWidth / (float)this.mVideoHeight;
        float f3 = (float)this.mDisplayWidth / (float)this.mDisplayHeight;
        if (this.mScalingMode == 1) {
            if (f3 <= f2) {
                displayHeight = (int)((float)this.mDisplayWidth / f2);
            } else {
                displayWidth = (int)((float)this.mDisplayHeight * f2);
            }
        } else if (this.mScalingMode == 2) {
            if (f3 >= f2) {
                displayHeight = (int)((float)this.mDisplayWidth / f2);
            } else {
                displayWidth = (int)((float)this.mDisplayHeight * f2);
            }
        } else if (this.mScalingMode == 0) {
            displayWidth = this.mVideoWidth;
            displayHeight = this.mVideoHeight;
        }
        if (bShowLog) {
            showLog("frameWidth = " + displayWidth + "; frameHeight = " + displayHeight);
        }
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(displayWidth, displayHeight, 17);
        this.mInstance.updateViewLayout(this.mSurfaceView, layoutParams);
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getBufferPercentage() {
        if (this.mIsURL) {
            return this.mPercent;
        }
        return 100;
    }

    @Override
    public int getCurrentPosition() {
        if (this.mMediaPlayer == null) {
            return 0;
        }
        return this.mMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        if (this.mMediaPlayer == null) {
            return 0;
        }
        return this.mMediaPlayer.getDuration();
    }

    @Override
    public  boolean isPlaying() {
        boolean isPrepared = this.mIsMediaPlayerPrepared && this.mIsVedioSizePrepared;
        if (this.mMediaPlayer == null) {
            if (!isPrepared) {
                return true;
            }
            return false;
        }
        if (this.mMediaPlayer.isPlaying() || !isPrepared) {
            return true;
        }
        return false;
    }

    @Override
    public void pause() {
        if (this.mMediaPlayer == null) {
            return;
        }
        this.mMediaPlayer.pause();
        this.mIsPaused = true;
    }

    @Override
    public void seekTo(int position) {
        if (this.mMediaPlayer == null) {
            return;
        }
        this.mMediaPlayer.seekTo(position);
    }

    @Override
    public void start() {
        if (this.mMediaPlayer == null) {
            return;
        }
        this.mMediaPlayer.start();
        this.mIsPaused = false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}

