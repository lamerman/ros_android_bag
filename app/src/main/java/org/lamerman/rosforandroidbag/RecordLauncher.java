package org.lamerman.rosforandroidbag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;

public class RecordLauncher {
    public interface RecordStateListener {
        void onRecordExit();
    }

    private volatile boolean mIsRecordProcessLaunched = false;

    private volatile boolean mIsStopRequested = false;

    private Thread mRunProcessThread;

    private RecordStateListener mListener;

    private StringBuffer mApplicationLogs = new StringBuffer();

    private final Context mContext;

    RecordLauncher(Context context, RecordStateListener listener) {
        if (listener == null) throw new NullPointerException("Listener must not be null");

        this.mContext = context;
        this.mListener = listener;
    }

    synchronized public void startRecord(final Intent intent) {
        if (mIsRecordProcessLaunched) {
            throw new IllegalStateException("Record already launched");
        } else {
            mIsRecordProcessLaunched = true;

            mRunProcessThread = new Thread() {
                @Override
                public void run() {
                    runRecordProcess(mContext, intent);
                }
            };

            mRunProcessThread.start();
        }
    }

    synchronized public void stopRecord() {
        if (!mIsRecordProcessLaunched) {
            throw new IllegalStateException("Record has not been launched yet");
        } else {
            mIsStopRequested = true;
            mRunProcessThread.interrupt();

            while (mIsRecordProcessLaunched) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    // normal workflow
                }
            }
        }
    }

    public String getLogs() {
        return mApplicationLogs.toString();
    }

    private void runRecordProcess(Context context, Intent intent) {
        InputStream recordResource = context.getResources().openRawResource(R.raw.record);

        try {
            File recordFile = copyRecordExecutableToFs(context, recordResource);

            String myExec = recordFile.toString();

            String command = MessageFormat.format("{0} {1}", myExec, intent.getStringExtra(Common.KEY_ARGUMENTS));

            File pwd = context.getExternalCacheDir();

            String logMessage = "The bags will be available at this path (selectable text): " + pwd;
            Log.i(Common.LOG_TAG, logMessage);
            mApplicationLogs.append(logMessage).append("\n\n");

            String[] defaultEnv = new String[] {
                "ROS_MASTER_URI=" + intent.getStringExtra(Common.KEY_MASTER_URL),
                "HOME=" + pwd.toString()
            };

            String[] env = (String[]) ArrayUtils.addAll(defaultEnv,
                    extractEnvironmentVariables(intent.getStringExtra(Common.KEY_ENVIRONMENT_VARIABLES)));

            logMessage = MessageFormat.format("Running the command\n{0}\nwith the following environment " +
                    "variables set\n{1}", command, Arrays.toString(env));
            mApplicationLogs.append(logMessage).append("\n\n");
            Log.i(Common.LOG_TAG, logMessage);

            Process process = Runtime.getRuntime().exec(command, env, pwd);

            DataInputStream stderr = new DataInputStream(process.getErrorStream());
            DataInputStream stdout = new DataInputStream(process.getInputStream());

            Integer returnCode = null;
            while (mIsStopRequested == false && returnCode == null) {
                try {
                    returnCode = process.exitValue();

                    logStream(stdout, "stdout");
                    logStream(stderr, "stderr");

                    break;
                } catch (IllegalThreadStateException ex) {
                    // this exception will be thrown if the process is running, so it's not an error
                }

                logStream(stdout, "stdout");
                logStream(stderr, "stderr");

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    // normal workflow
                }
            }

            if (returnCode == null) {
                int pid = Common.getPid(process);

                boolean killed = Common.killWithSigint(pid);
                if (killed) {
                    try {
                        returnCode = process.waitFor();
                    } catch (InterruptedException e) {
                        process.destroy();
                        // normal workflow
                    }
                } else {
                    Log.w(Common.LOG_TAG, "Could not shut down the record process gracefully, killing forcibly");
                    process.destroy();
                }
            }

            SharedPreferences sharedPref = context.getSharedPreferences(Common.STORAGE_KEY, Context.MODE_PRIVATE);
            sharedPref.edit().putString(Common.LOGS_STORAGE_KEY, getLogs()).commit();

            mListener.onRecordExit();

            stderr.close();
            stdout.close(); // TODO: close streams even on exception

            Log.w(Common.LOG_TAG, "Return code: " + String.valueOf(returnCode));
        } catch (IOException e) {
            Log.e(Common.LOG_TAG, "Could not run executable", e);
        }

        mIsRecordProcessLaunched = false;
        mIsStopRequested = false;
    }

    private File copyRecordExecutableToFs(Context context, InputStream recordResource) throws IOException {
        byte[] recordBytes = IOUtils.toByteArray(recordResource);
        File recordFile = new File(context.getCacheDir(), "ros_android_bag_record");

        if (recordFile.exists()) recordFile.delete();

        FileUtils.writeByteArrayToFile(recordFile, recordBytes);

        boolean result = recordFile.setExecutable(true);
        if (result == false) {
            String errorText = "Could not set executable flag to " + recordFile.toString();
            Log.e(Common.LOG_TAG, errorText);
            throw new IllegalStateException(errorText);
        }
        return recordFile;
    }

    private void logStream(DataInputStream stream, String streamName) throws IOException {
        if (stream.available() > 0) {
            String logLine = streamName + ": " + new String(IOUtils.readFully(stream, stream.available()), "UTF-8");
            mApplicationLogs.append(logLine).append("\n\n");
            Log.w(Common.LOG_TAG, logLine);
        }
    }

    static String[] extractEnvironmentVariables(String environmentVariablesStr) {
        String environmentVariablesStrEscaped = environmentVariablesStr.replace("\\,", Common.ESCAPED_COMMA_REPLACEMENT);

        String[] keyValuePairs = environmentVariablesStrEscaped.split(",");

        for (int i = 0; i < keyValuePairs.length; i++) {
            keyValuePairs[i] = keyValuePairs[i].replace(Common.ESCAPED_COMMA_REPLACEMENT, ",");
        }

        return keyValuePairs;
    }
}
