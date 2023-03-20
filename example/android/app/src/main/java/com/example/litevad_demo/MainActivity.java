/*
 * Copyright (C) 2018-2023 luoyun <sysu.zqlong@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.litevad_demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity {
    private static final int LITEVAD_RESULT_ERROR = -1;
    private static final int LITEVAD_RESULT_FRAME_SILENCE = 0;
    private static final int LITEVAD_RESULT_FRAME_ACTIVE = 1;
    private static final int LITEVAD_RESULT_SPEECH_BEGIN = 2;
    private static final int LITEVAD_RESULT_SPEECH_END = 3;

    private final static String TAG = "LiteVadDemo";
    private EventHandler mEventHandler;
    private HandlerThread mHandlerThread;
    private TextView mStatusView;

    private long mVadHandle = 0;
    private AudioRecord mAudioRecord;
    Thread mRecordThread;
    private boolean mRecording = false;
    private boolean mStarted = false;

    private static final int RECORD_SAMPLE_RATE = 16000;
    private static final int RECORD_CHANNEL_COUNT = 1;
    private static final int RECORD_SAMPLE_BITS = 16;

    private static final int PERMISSIONS_REQUEST_CODE_AUDIO = 1;
    private void requestPermissions(Activity activity) {
        // request audio permissions
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSION_AUDIO = { Manifest.permission.RECORD_AUDIO };
            ActivityCompat.requestPermissions(activity, PERMISSION_AUDIO, PERMISSIONS_REQUEST_CODE_AUDIO);
        }
    }
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case PERMISSIONS_REQUEST_CODE_AUDIO:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Failed to request RECORD_AUDIO permission", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(this);

        mStatusView = findViewById(R.id.statusView);
        mStatusView.setText("Idle");

        Looper looper;
        if ((looper = Looper.myLooper()) == null && (looper = Looper.getMainLooper()) == null) {
            mHandlerThread = new HandlerThread("LiteVadEventThread");
            mHandlerThread.start();
            looper = mHandlerThread.getLooper();
        }
        mEventHandler = new EventHandler(looper);

        int bytesPer10Ms = RECORD_SAMPLE_RATE/100*(RECORD_CHANNEL_COUNT*RECORD_SAMPLE_BITS/8);
        int bufferSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        bufferSize = (bufferSize/bytesPer10Ms + 1)*bytesPer10Ms;
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORD_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        mVadHandle = native_create(RECORD_SAMPLE_RATE, RECORD_CHANNEL_COUNT, RECORD_SAMPLE_BITS);
    }

    @Override
    protected void onDestroy() {
        if (mRecording) {
            stopRecording();
            try {
                mRecordThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        if (mVadHandle != 0) {
            native_destroy(mVadHandle);
            mVadHandle = 0;
        }
        if (mHandlerThread != null)
            mHandlerThread.quitSafely();
        super.onDestroy();
    }

    @SuppressLint("SetTextI18n")
    public void onStartClick(View view) {
        if (!mStarted) {
            mStarted = true;
            startRecording();
        }
    }

    @SuppressLint("SetTextI18n")
    public void onStopClick(View view) {
        if (mStarted) {
            stopRecording();
            mStarted = false;
            mStatusView.setText("Idle");
        }
    }

    private class EventHandler extends Handler {

        public EventHandler(Looper looper) {
            super(looper);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case LITEVAD_RESULT_ERROR:
                    mStatusView.setText("ERROR");
                    break;
                case LITEVAD_RESULT_FRAME_ACTIVE:
                    //mStatusView.setText("FRAME ACTIVE");
                    break;
                case LITEVAD_RESULT_FRAME_SILENCE:
                    //mStatusView.setText("FRAME SILENCE");
                    break;
                case LITEVAD_RESULT_SPEECH_BEGIN:
                    mStatusView.setText("SPEECH BEGIN");
                    break;
                case LITEVAD_RESULT_SPEECH_END:
                    mStatusView.setText("SPEECH END");
                    break;
                default:
                    break;
            }
        }
    }

    private void startRecording() {
        mRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int prevResult = LITEVAD_RESULT_FRAME_SILENCE;
                int bufferSize = mAudioRecord.getBufferSizeInFrames()*(RECORD_CHANNEL_COUNT*RECORD_SAMPLE_BITS/8);
                byte[] buffer = new byte[bufferSize];

                native_reset(mVadHandle);

                mAudioRecord.startRecording();
                mRecording = true;

                while (mRecording) {
                    int read = mAudioRecord.read(buffer, 0, bufferSize);
                    if (read > 0) {
                        int result = native_process(mVadHandle, buffer, read);
                        if (result != prevResult) {
                            switch (result) {
                                case LITEVAD_RESULT_ERROR: {
                                    Message m = mEventHandler.obtainMessage(LITEVAD_RESULT_ERROR, 0, 0, null);
                                    mEventHandler.sendMessage(m);
                                    break;
                                }
                                case LITEVAD_RESULT_FRAME_SILENCE: {
                                    Message m = mEventHandler.obtainMessage(LITEVAD_RESULT_FRAME_SILENCE, 0, 0, null);
                                    mEventHandler.sendMessage(m);
                                    break;
                                }
                                case LITEVAD_RESULT_FRAME_ACTIVE: {
                                    Message m = mEventHandler.obtainMessage(LITEVAD_RESULT_FRAME_ACTIVE, 0, 0, null);
                                    mEventHandler.sendMessage(m);
                                    break;
                                }
                                case LITEVAD_RESULT_SPEECH_BEGIN: {
                                    Message m = mEventHandler.obtainMessage(LITEVAD_RESULT_SPEECH_BEGIN, 0, 0, null);
                                    mEventHandler.sendMessage(m);
                                    break;
                                }
                                case LITEVAD_RESULT_SPEECH_END: {
                                    Message m = mEventHandler.obtainMessage(LITEVAD_RESULT_SPEECH_END, 0, 0, null);
                                    mEventHandler.sendMessage(m);
                                    break;
                                }
                                default:
                                    break;
                            }
                            prevResult = result;
                        }
                    }
                }

                mAudioRecord.stop();
                mRecording = false;
            }
        });

        mRecordThread.start();
    }

    public void stopRecording() {
        mRecording = false;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private native long native_create(int sampleRate, int channelCount, int sampleBits);
    private native int  native_process(long handle, byte[] buffer, int size) throws IllegalStateException;
    private native void native_reset(long handle) throws IllegalStateException;
    private native void native_destroy(long handle) throws IllegalStateException;

    // Used to load the 'native-lib' library on application startup.
    static { System.loadLibrary("litevad-jni"); }
}
