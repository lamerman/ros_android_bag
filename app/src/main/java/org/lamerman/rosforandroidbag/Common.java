package org.lamerman.rosforandroidbag;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Vector;

public class Common {
    public static final String LOG_TAG = "ROS_ANDROID_BAG";

    public static final String STORAGE_KEY = "STORAGE";
    public static final String LOGS_STORAGE_KEY = "LOGS";

    public static final String KEY_ARGUMENTS = "arguments";
    public static final String KEY_ENVIRONMENT_VARIABLES = "environment_variables";
    public static final String KEY_MASTER_URL = "master_url";
    public static final String KEY_RECORD_ALL_TOPICS = "record_all_topics";
    public static final String KEY_TOPICS_TO_RECORD = "topics_to_record";

    public static final String ESCAPED_COMMA_REPLACEMENT = "<<escaped_comma>>";

    /** Get the pid of the process using reflection
     *
     * @return pid of the process or -1 if could not get it
     */
    public static int getPid(Process p) {
        int pid = -1;

        try {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getInt(p);
            f.setAccessible(false);
        } catch (Throwable e) {
            Log.e(LOG_TAG, "Failed to get pid of process", e);
            pid = -1;
        }
        return pid;
    }

    public static boolean killWithSigint(int pid) {
        try {
            int killReturnCode = Runtime.getRuntime().exec("kill -SIGINT " + String.valueOf(pid)).waitFor();
            if (killReturnCode == 0) return true;
        } catch (InterruptedException ex) {
            // normal workflow
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException while killing a process", e);
        }

        return false;
    }

    public static boolean isServiceRunning(Class<?> serviceClass, Context context){
        final ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        Vector<String> serviceNames = new Vector<>();
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            serviceNames.add(runningServiceInfo.service.getClassName());
            if (runningServiceInfo.service.getClassName().equals(serviceClass.getName())){
                return true;
            }
        }
        return false;
    }
}
