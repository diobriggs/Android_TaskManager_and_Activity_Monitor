package com.example.myapplication;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.Locale;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final int SORT_BY_NAME = 1;
    private static final int SORT_BY_SCREEN_TIME = 2;
    private static final int SORT_BY_STORAGE = 3;
    private int currentSortingCriteria = SORT_BY_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for and request usage stats permission if not granted
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
            // You might want to inform the user or take further action here
        } else {
            // Proceed with displaying the list of running tasks
            displayRunningTasks(currentSortingCriteria);
        }

        Button sortButton = findViewById(R.id.sortButton);
        updateSortButtonText(sortButton);  // Set initial text
        sortButton.setOnClickListener(v -> {
            // Toggle between SORT_BY_NAME, SORT_BY_SCREEN_TIME, and SORT_BY_STORAGE on each click
            switch (currentSortingCriteria) {
                case SORT_BY_NAME:
                    currentSortingCriteria = SORT_BY_SCREEN_TIME;
                    break;
                case SORT_BY_SCREEN_TIME:
                    currentSortingCriteria = SORT_BY_STORAGE;
                    break;
                case SORT_BY_STORAGE:
                default:
                    currentSortingCriteria = SORT_BY_NAME;
                    break;
            }
            displayRunningTasks(currentSortingCriteria);
            updateSortButtonText(sortButton);  // Update button text after sorting
        });
    }

    private void updateSortButtonText(Button sortButton) {
        // Set button text based on the current sorting criteria
        switch (currentSortingCriteria) {
            case SORT_BY_NAME:
                sortButton.setText(R.string.sort_by_name);
                break;
            case SORT_BY_SCREEN_TIME:
                sortButton.setText(R.string.sort_by_screen_time);
                break;
            case SORT_BY_STORAGE:
                sortButton.setText(R.string.sort_by_storage);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void displayRunningTasks(int sortingCriteria) {
        ListView listView = findViewById(R.id.taskListView);

        // Get a list of currently running tasks
        List<String> runningTasks = getRunningTasks();

        // Create a list of strings to display in the ListView
        List<String> taskList = new ArrayList<>();
        for (String packageName : runningTasks) {
            String appName = getAppName(packageName);
            String versionName = getVersionName(packageName);
            String screenTime = getFormattedScreenTime(packageName);
            long storageData = getStorageData(packageName);

            // Format the information as a string and add it to the list
            String taskInfoString = String.format(
                    Locale.getDefault(),
                    "%s\nPackage: %s\nVersion: %s\nScreen Time: %s\nStorage Data: %d MB",
                    appName, packageName, versionName, screenTime, storageData
            );
            taskList.add(taskInfoString);
        }

        // Sort the taskList based on the specified criteria
        sortTaskList(taskList, sortingCriteria);

        // Create an ArrayAdapter to display the sorted list in the ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, taskList);
        listView.setAdapter(adapter);

        // Set an OnItemClickListener for the ListView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Retrieve the selected package name
            String selectedPackageName = runningTasks.get(position);

            // Stop the selected app or task
            stopRunningTask(selectedPackageName);

            // Refresh the list to reflect the changes
            displayRunningTasks(sortingCriteria);
        });
    }

    private void stopRunningTask(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        startActivity(intent);
    }

    private List<String> getInstalledApps() {
        List<String> installedApps = new ArrayList<>();

        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            installedApps.add(app.packageName);
        }

        return installedApps;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<String> getRunningTasks() {
        List<String> runningTasks = new ArrayList<>();

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            // Get running tasks using ActivityManager
            List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                String packageName = processInfo.processName;
                runningTasks.add(packageName);
            }

            // Add installed apps to the list
            List<String> installedApps = getInstalledApps();
            runningTasks.addAll(installedApps);
        }

        return runningTasks;
    }

    private String getAppName(String packageName) {
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private String getVersionName(String packageName) {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "N/A";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String getFormattedScreenTime(String packageName) {
        long screenTimeMillis = getScreenTime(packageName);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(screenTimeMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(screenTimeMillis) -
                TimeUnit.MINUTES.toSeconds(minutes);

        return String.format("%d min, %d sec", minutes, seconds);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private long getScreenTime(String packageName) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        if (usageStatsManager != null) {
            long endTime = System.currentTimeMillis();
            long startTime = endTime - 24 * 60 * 60 * 1000; // Look back 24 hours

            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST, startTime, endTime);

            for (UsageStats usageStats : usageStatsList) {
                if (usageStats.getPackageName().equals(packageName)) {
                    return usageStats.getTotalTimeInForeground();
                }
            }
        }

        // If unable to retrieve accurate data, return 0
        return 0;
    }

    private long getStorageData(String packageName) {
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);

            // Calculate the size of the APK file
            String appSourceDir = applicationInfo.sourceDir;
            File appFile = new File(appSourceDir);
            long apkSize = appFile.length();

            // Calculate the size of the app's data directory
            File dataDir = new File(applicationInfo.dataDir);
            long dataSize = getDirSize(dataDir);

            // Convert the total storage data from bytes to megabytes
            return (apkSize + dataSize) / (1024 * 1024);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // If unable to retrieve accurate data, return 0
        return 0;
    }

    // Helper method to calculate the size of a directory
    private long getDirSize(File dir) {
        long size = 0;

        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    size += getDirSize(file);
                }
            }
        } else {
            size = dir.length();
        }

        return size;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }

    private void sortTaskList(List<String> taskList, int sortingCriteria) {
        // Implement sorting logic based on the specified criteria
        switch (sortingCriteria) {
            case SORT_BY_NAME:
                sortTaskListByName(taskList);
                break;
            case SORT_BY_SCREEN_TIME:
                sortTaskListByScreenTime(taskList);
                break;
            case SORT_BY_STORAGE:
                sortTaskListByStorage(taskList);
                break;
            // Add more cases for additional sorting criteria if needed
        }
    }

    private void sortTaskListByName(List<String> taskList) {
        Collections.sort(taskList, new Comparator<String>() {
            @Override
            public int compare(String task1, String task2) {
                // Extract app names from the formatted strings and compare
                String appName1 = task1.split("\n")[0];
                String appName2 = task2.split("\n")[0];
                return appName1.compareToIgnoreCase(appName2);
            }
        });
    }

    private void sortTaskListByScreenTime(List<String> taskList) {
        Collections.sort(taskList, new Comparator<String>() {
            @Override
            public int compare(String task1, String task2) {
                // Extract screen times from the formatted strings and compare
                String screenTime1 = task1.split("\n")[3].replace("Screen Time: ", "");
                String screenTime2 = task2.split("\n")[3].replace("Screen Time: ", "");

                long time1 = parseScreenTime(screenTime1);
                long time2 = parseScreenTime(screenTime2);

                return Long.compare(time2, time1);  // Sort in descending order
            }
        });
    }

    private void sortTaskListByStorage(List<String> taskList) {
        Collections.sort(taskList, new Comparator<String>() {
            @Override
            public int compare(String task1, String task2) {
                // Extract storage data from the formatted strings and compare
                String storageData1 = task1.split("\n")[4].replace("Storage Data: ", "").replace(" MB", "");
                String storageData2 = task2.split("\n")[4].replace("Storage Data: ", "").replace(" MB", "");

                long size1 = Long.parseLong(storageData1);
                long size2 = Long.parseLong(storageData2);

                return Long.compare(size2, size1);  // Sort in descending order
            }
        });
    }

    private long parseScreenTime(String screenTime) {
        // Parse the formatted screen time (e.g., "5 min, 30 sec") into milliseconds
        String[] timeComponents = screenTime.split(", ");
        long minutes = Long.parseLong(timeComponents[0].replace(" min", ""));
        long seconds = Long.parseLong(timeComponents[1].replace(" sec", ""));
        return TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds);
    }

}