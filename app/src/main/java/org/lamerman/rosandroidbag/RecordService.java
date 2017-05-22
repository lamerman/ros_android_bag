package org.lamerman.rosandroidbag;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class RecordService extends Service implements RecordLauncher.RecordStateListener {

    private RecordLauncher recordLauncher;

    private volatile boolean isRecordStarted = false;
    private volatile boolean isDestroyed = false;
    private volatile boolean isDestroyRequestedByRecorder = false;

    @Override
    public void onCreate() {
        super.onCreate();

        recordLauncher = new RecordLauncher(this, this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new RecordServiceBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        synchronized (this) {
            if (!isRecordStarted) {
                recordLauncher.startRecord(intent);
                isRecordStarted = true;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isDestroyed = true;

        synchronized (this) {
            if (isRecordStarted && !isDestroyRequestedByRecorder) {
                recordLauncher.stopRecord();
            }

            isRecordStarted = false;
        }
    }

    @Override
    public void onRecordExit() {
        if (!isDestroyed) {
            isDestroyRequestedByRecorder = true;
            stopSelf();
        }
    }

    public class RecordServiceBinder extends Binder {
        public String getLogs() {
            return recordLauncher.getLogs();
        }
    }
}
