package org.lamerman.rosforandroidbag;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

public class LogsActivity extends AppCompatActivity {

    private volatile boolean mServiceConnected = false;
    private volatile boolean mServiceBound = false;
    private volatile RecordService.RecordServiceBinder mBinder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean serviceRunning = Common.isServiceRunning(RecordService.class, this);
        if (serviceRunning) {
            Intent intent = new Intent(getBaseContext(), RecordService.class);
            mServiceBound = bindService(intent, mConnection, 0);
        }

        if (!serviceRunning || !mServiceBound) {
            SharedPreferences sharedPref = getSharedPreferences(Common.STORAGE_KEY, Context.MODE_PRIVATE);
            String logs = sharedPref.getString(Common.LOGS_STORAGE_KEY, "");
            setLogsOnTheScreen(logs);
        }
    }

    @Override
    protected void onDestroy() {
        if (mServiceBound) {
            unbindService(mConnection);
            mServiceBound = false;
        }

        mServiceConnected = false;

        super.onDestroy();
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBinder = (RecordService.RecordServiceBinder) service;
            mServiceConnected = true;

            new Thread() {
                @Override
                public void run() {
                    while (mServiceConnected) {
                        try {
                            String logs = mBinder.getLogs();
                            setLogsOnTheScreen(logs);
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceConnected = false;
        }
    };

    private void setLogsOnTheScreen(final String logs) {
        final TextView textViewLogs = (TextView)findViewById(R.id.textViewLogs);

        LogsActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewLogs.setText(logs);
            }
        });
    }
}
