package org.fmod;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.nio.ByteBuffer;

public class FMODAudioDevice implements Runnable {
    private volatile Thread mThreadFMOAudio = null;
    private volatile boolean isRunning = false;
    private AudioTrack mAudioTrack = null;
    private boolean d = false;
    private ByteBuffer mByteBuffer = null;
    private byte[] f = null;
    private volatile UnityAudioRecord mUnityAudioRecord;
    private static int h = 0;
    private static int i = 1;
    private static int j = 2;
    private static int k = 3;

    public synchronized void start() {
        if (this.mThreadFMOAudio != null) {
            this.stop();
        }
        this.mThreadFMOAudio = new Thread(this, "FMODAudioDevice");
        this.mThreadFMOAudio.setPriority(10);
        this.isRunning = true;
        System.out.println("zjf@ FMODAudioDevice thread start!!!!");
        this.mThreadFMOAudio.start();
        if (this.mUnityAudioRecord != null) {
            this.mUnityAudioRecord.startAudioRecord();
        }
    }

    public synchronized void stop() {
        while (this.mThreadFMOAudio != null) {
            this.isRunning = false;
            try {
                this.mThreadFMOAudio.join();
                this.mThreadFMOAudio = null;
            }
            catch (InterruptedException v0) {}
        }
        if (this.mUnityAudioRecord != null) {
            this.mUnityAudioRecord.stopAudioRecord();
        }
    }

    public synchronized void close() {
        this.stop();
    }

    public boolean isRunning() {
        if (this.mThreadFMOAudio != null && this.mThreadFMOAudio.isAlive()) {
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        System.out.println("zjf@ FMODAudioDevice thread running!!!!");
        int streamType = AudioManager.STREAM_MUSIC;
        while (this.isRunning) {
            if (!this.d && streamType > 0) {
                this.releaseAudioTrack();
                int sampleRateInHz = this.fmodGetInfo(h);
                int bufferSizeInBytes = Math.round((float)AudioTrack.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO,  AudioFormat.ENCODING_PCM_16BIT) * 1.1f) & -4;
                int n5 = this.fmodGetInfo(i);
                int n6 = this.fmodGetInfo(j);
                if (n5 * n6 * 4 > bufferSizeInBytes) {
                    bufferSizeInBytes = n5 * n6 * 4;
                }

                this.mAudioTrack = new AudioTrack(streamType, sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes, AudioTrack.MODE_STREAM);
                this.d = this.mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED;
                if (this.d) {
                    streamType = 3;
                    this.mByteBuffer = ByteBuffer.allocateDirect(n5 * 2 * 2);
                    this.f = new byte[this.mByteBuffer.capacity()];
                    this.mAudioTrack.play();
                } else {
                    Log.e("FMOD", ("AudioTrack failed to initialize (status " + this.mAudioTrack.getState() + ")"));
                    this.releaseAudioTrack();
                    --streamType;
                }
            }
            if (!this.d) continue;
            if (this.fmodGetInfo(k) == 1) {
                this.fmodProcess(this.mByteBuffer);
                this.mByteBuffer.get(this.f, 0, this.mByteBuffer.capacity());
                this.mAudioTrack.write(this.f, 0, this.mByteBuffer.capacity());
                this.mByteBuffer.position(0);
                continue;
            }
            this.releaseAudioTrack();
        }
        this.releaseAudioTrack();
    }

    private void releaseAudioTrack() {
        if (this.mAudioTrack != null) {
            if (this.mAudioTrack.getState() == 1) {
                this.mAudioTrack.stop();
            }
            this.mAudioTrack.release();
            this.mAudioTrack = null;
        }
        this.mByteBuffer = null;
        this.f = null;
        this.d = false;
    }

    public synchronized int startAudioRecord(int n2, int n3, int n4) {
        if (this.mUnityAudioRecord == null) {
            this.mUnityAudioRecord = new UnityAudioRecord(this, n2, n3);
            this.mUnityAudioRecord.startAudioRecord();
        }
        return this.mUnityAudioRecord.getAudioRecordCapacity();
    }

    public synchronized void stopAudioRecord() {
        if (this.mUnityAudioRecord != null) {
            this.mUnityAudioRecord.stopAudioRecord();
            this.mUnityAudioRecord = null;
        }
    }

    private native int fmodGetInfo(int var1);

    private native int fmodProcess(ByteBuffer data);

    native int fmodProcessMicData(ByteBuffer data, int var2);
}

