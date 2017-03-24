
package org.fmod;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import java.nio.ByteBuffer;

final class UnityAudioRecord implements Runnable {
    private final FMODAudioDevice mAudioDevice;
    private final ByteBuffer mAudioData;
    private final int mSampleRateInHz;
    private final int mChannelConfig;
    private final int mAudioFormat;
    private volatile Thread mThread;
    private volatile boolean g;
    private AudioRecord mAudioRecord;
    private boolean mIsAudioRecordInit;

    UnityAudioRecord(FMODAudioDevice fMODAudioDevice, int sampleRateInHz, int channelConfig) {
        this.mAudioDevice = fMODAudioDevice;
        this.mSampleRateInHz = sampleRateInHz;
        this.mChannelConfig = channelConfig;
        this.mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
        this.mAudioData = ByteBuffer.allocateDirect(AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT));
    }

    public int getAudioRecordCapacity() {
        return this.mAudioData.capacity();
    }

    public void startAudioRecord() {
        if (this.mThread != null) {
            this.stopAudioRecord();
        }
        this.g = true;
        this.mThread = new Thread(this);
        System.out.println("zjf@ UnityAudioRecord startAudioRecord thread starting!!!");
        this.mThread.start();
    }

    public void stopAudioRecord() {
        while (this.mThread != null) {
            this.g = false;
            try {
                this.mThread.join();
                this.mThread = null;
            }
            catch (InterruptedException v0) {}
        }
    }

    @Override
    public void run() {
        System.out.println("zjf@ AudioRecordThread run!!!");
        int stateRecording = AudioRecord.RECORDSTATE_RECORDING;
        while (this.g) {
            if (!this.mIsAudioRecordInit && stateRecording > 0) {
                this.stop();
                this.mAudioRecord = new AudioRecord(1, this.mSampleRateInHz, this.mChannelConfig, this.mAudioFormat, this.mAudioData.capacity());
                this.mIsAudioRecordInit = this.mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED;
                if (this.mIsAudioRecordInit) {
                    stateRecording = AudioRecord.RECORDSTATE_RECORDING;
                    this.mAudioData.position(0);
                    this.mAudioRecord.startRecording();
                } else {
                    Log.e("FMOD", "AudioRecord failed to initialize (status " + this.mAudioRecord.getState() + ")");
                    --stateRecording;
                    this.stop();
                }
            }
            if (!this.mIsAudioRecordInit || this.mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) continue;
            int size = this.mAudioRecord.read(this.mAudioData, this.mAudioData.capacity());
            this.mAudioDevice.fmodProcessMicData(this.mAudioData, size);
            this.mAudioData.position(0);
        }
        this.stop();
    }

    private void stop() {
        if (this.mAudioRecord != null) {
            if (this.mAudioRecord.getState() == 1) {
                this.mAudioRecord.stop();
            }
            this.mAudioRecord.release();
            this.mAudioRecord = null;
        }
        this.mAudioData.position(0);
        this.mIsAudioRecordInit = false;
    }

}

