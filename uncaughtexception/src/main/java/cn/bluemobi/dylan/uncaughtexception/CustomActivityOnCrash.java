package cn.bluemobi.dylan.uncaughtexception;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cat.ereza.customactivityoncrash.R;
import cn.bluemobi.dylan.uncaughtexception.activity.DefaultErrorActivity;
import cn.bluemobi.dylan.uncaughtexception.email.ReportByEmail;


@SuppressLint({"NewApi"})
public final class CustomActivityOnCrash {
    private static final String INTENT_ACTION_ERROR_ACTIVITY = "cat.ereza.customactivityoncrash.ERROR";
    private static final String INTENT_ACTION_RESTART_ACTIVITY = "cat.ereza.customactivityoncrash.RESTART";
    private static Application application;
    private static WeakReference<Activity> lastActivityCreated = new WeakReference(null);
    private static boolean isInBackground = false;

    private static boolean launchErrorActivityWhenInBackground = true;
    private static boolean showErrorDetails = true;
    private static boolean enableAppRestart = true;
    private static int defaultErrorActivityDrawableId = R.drawable.customactivityoncrash_error_image;
    private static Class<? extends Activity> errorActivityClass = null;
    private static Class<? extends Activity> restartActivityClass = null;
    private static boolean debugModel = true;
    private static String[] toArr;
    private static String dingtalkUrl;

    public static void setDingtalkUrl(String dingtalkUrl) {
        CustomActivityOnCrash.dingtalkUrl = dingtalkUrl;
    }

    public static String getDingtalkUrl() {
        return dingtalkUrl;
    }

    public static void setEmailTo(String[] toAddress) {
        toArr = toAddress;
    }

    public static String[] getEmailTo() {
        return toArr;
    }

    public static void setDebugMode(boolean debugMode) {
        debugModel = debugMode;
    }


    public static void install(final Context context) {
        try {
            if (context == null) {
                Log.e("CustomActivityOnCrash", "Install failed: context is null!");
            } else {
                if (VERSION.SDK_INT < 14) {
                    Log.w("CustomActivityOnCrash", "CustomActivityOnCrash will be installed, but may not be reliable in API lower than 14");
                }


                Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

                if ((oldHandler != null) && (oldHandler.getClass().getName().startsWith("cat.ereza.customactivityoncrash"))) {
                    Log.e("CustomActivityOnCrash", "You have already installed CustomActivityOnCrash, doing nothing!");
                } else {
                    if ((oldHandler != null) && (!oldHandler.getClass().getName().startsWith("com.android.internal.os"))) {
                        Log.e("CustomActivityOnCrash", "IMPORTANT WARNING! You already have an UncaughtExceptionHandler, are you sure this is correct? If you use ACRA, Crashlytics or similar libraries, you must initialize them AFTER CustomActivityOnCrash! Installing anyway, but your original handler will not be called.");
                    }

                    application = (Application) context.getApplicationContext();


                    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        public void uncaughtException(Thread thread, final Throwable throwable) {
                            Log.e("CustomActivityOnCrash", "App has crashed, executing CustomActivityOnCrash's UncaughtExceptionHandler", throwable);
                            if (!CustomActivityOnCrash.debugModel) {
                                new Thread() {
                                    public void run() {
                                        super.run();
                                        StringWriter sw = new StringWriter();
                                        PrintWriter pw = new PrintWriter(sw);
                                        throwable.printStackTrace(pw);
                                        String stackTraceString = sw.toString();


                                        if (stackTraceString.length() > 131071) {
                                            String disclaimer = " [stack trace too large]";
                                            stackTraceString = stackTraceString.substring(0, 131071 - disclaimer.length()) + disclaimer;
                                        }

                                        final String finalStackTraceString = stackTraceString;
                                        if (!TextUtils.isEmpty(CustomActivityOnCrash.getDingtalkUrl())) {
                                            String url = CustomActivityOnCrash.getDingtalkUrl();
                                            String json = "{ \"msgtype\": \"text\", \"text\": {\"content\": \"" + CustomActivityOnCrash.getAllErrorDetailsFromContext(context, finalStackTraceString) + "\"}}";
                                            requestPost(url, json);
                                        }
                                        ReportByEmail.sendEmail2(CustomActivityOnCrash.getApplicationName(context), CustomActivityOnCrash.getAllErrorDetailsFromContext(context, finalStackTraceString));
                                        // 退出程序
                                        //干掉当前的程序
                                        CustomActivityOnCrash.killCurrentProcess();
                                        // 关闭虚拟机，彻底释放内存空间
                                        System.exit(0);


                                    }

                                }.start();
                                return;
                            }

                            if (CustomActivityOnCrash.errorActivityClass == null) {
                                errorActivityClass = guessErrorActivityClass(application);
                            }

                            if (CustomActivityOnCrash.isStackTraceLikelyConflictive(throwable, CustomActivityOnCrash.errorActivityClass)) {
                                Log.e("CustomActivityOnCrash", "Your application class or your error activity have crashed, the custom activity will not be launched!");
                            } else if ((CustomActivityOnCrash.launchErrorActivityWhenInBackground) || (!CustomActivityOnCrash.isInBackground)) {
                                Intent intent = new Intent(CustomActivityOnCrash.application, CustomActivityOnCrash.errorActivityClass);
                                StringWriter sw = new StringWriter();
                                PrintWriter pw = new PrintWriter(sw);
                                throwable.printStackTrace(pw);
                                String stackTraceString = sw.toString();


                                if (stackTraceString.length() > 131071) {
                                    String disclaimer = " [stack trace too large]";
                                    stackTraceString = stackTraceString.substring(0, 131071 - disclaimer.length()) + disclaimer;
                                }

                                if ((CustomActivityOnCrash.enableAppRestart) && (CustomActivityOnCrash.restartActivityClass == null)) {

                                    restartActivityClass = guessRestartActivityClass(application);
                                } else if (!CustomActivityOnCrash.enableAppRestart) {
                                    restartActivityClass = null;
                                }

                                intent.putExtra("cat.ereza.customactivityoncrash.EXTRA_STACK_TRACE", stackTraceString);
                                intent.putExtra("cat.ereza.customactivityoncrash.EXTRA_RESTART_ACTIVITY_CLASS", CustomActivityOnCrash.restartActivityClass);
                                intent.putExtra("cat.ereza.customactivityoncrash.EXTRA_SHOW_ERROR_DETAILS", CustomActivityOnCrash.showErrorDetails);
                                intent.putExtra("cat.ereza.customactivityoncrash.EXTRA_IMAGE_DRAWABLE_ID", CustomActivityOnCrash.defaultErrorActivityDrawableId);
                                intent.setFlags(268468224);
                                CustomActivityOnCrash.application.startActivity(intent);
                            }

                            Activity lastActivity = (Activity) CustomActivityOnCrash.lastActivityCreated.get();
                            if (lastActivity != null) {


                                lastActivity.finish();
                                CustomActivityOnCrash.lastActivityCreated.clear();
                            }
                            killCurrentProcess();
                        }
                    });
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                            int currentlyStartedActivities = 0;

                            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                                if (activity.getClass() != CustomActivityOnCrash.errorActivityClass) {


                                    lastActivityCreated = new WeakReference(activity);
                                }
                            }

                            public void onActivityStarted(Activity activity) {
                                currentlyStartedActivities++;
                                isInBackground = (currentlyStartedActivities == 0);
                            }


                            public void onActivityResumed(Activity activity) {
                            }


                            public void onActivityPaused(Activity activity) {
                            }


                            public void onActivityStopped(Activity activity) {
                                currentlyStartedActivities--;
                                isInBackground = (currentlyStartedActivities == 0);
                            }


                            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                            }


                            public void onActivityDestroyed(Activity activity) {
                            }
                        });
                    }


                    Log.i("CustomActivityOnCrash", "CustomActivityOnCrash has been installed.");
                }
            }
        } catch (Throwable t) {
            Log.e("CustomActivityOnCrash", "An unknown error occurred while installing CustomActivityOnCrash, it may not have been properly initialized. Please report this as a bug if needed.", t);
        }
    }


    public static boolean isShowErrorDetailsFromIntent(Intent intent) {
        return intent.getBooleanExtra("cat.ereza.customactivityoncrash.EXTRA_SHOW_ERROR_DETAILS", true);
    }


    public static int getDefaultErrorActivityDrawableIdFromIntent(Intent intent) {
        return intent.getIntExtra("cat.ereza.customactivityoncrash.EXTRA_IMAGE_DRAWABLE_ID", R.drawable.customactivityoncrash_error_image);
    }


    public static String getStackTraceFromIntent(Intent intent) {
        return intent.getStringExtra("cat.ereza.customactivityoncrash.EXTRA_STACK_TRACE");
    }


    public static String getAllErrorDetailsFromIntent(Context context, Intent intent) {
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);


        String buildDateAsString = getAppDate(context, dateFormat);


        String versionName = getVersionName(context);

        String errorDetails = "";

        errorDetails = errorDetails + "ApplicationName: " + getApplicationName(context) + " \n";
        errorDetails = errorDetails + "Build version: " + versionName + " \n";
        errorDetails = errorDetails + "Build date: " + buildDateAsString + " \n";
        errorDetails = errorDetails + "Current date: " + dateFormat.format(currentDate) + " \n";
        errorDetails = errorDetails + "Device: " + getDeviceModelName() + " \n";
        errorDetails = errorDetails + "SupportedAbis: " + getSupportedAbis() + " \n\n";
        errorDetails = errorDetails + "Stack trace:  \n";
        errorDetails = errorDetails + getStackTraceFromIntent(intent);
        return errorDetails;
    }


    public static String getAllErrorDetailsFromContext(Context context, String stackTraceString) {
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);


        String buildDateAsString = getAppDate(context, dateFormat);


        String versionName = getVersionName(context);

        String errorDetails = "";

        errorDetails = errorDetails + "ApplicationName: " + getApplicationName(context) + " \n";
        errorDetails = errorDetails + "Build version: " + versionName + " \n";
        errorDetails = errorDetails + "Build date: " + buildDateAsString + " \n";
        errorDetails = errorDetails + "Current date: " + dateFormat.format(currentDate) + " \n";
        errorDetails = errorDetails + "Device: " + getDeviceModelName() + " \n";
        errorDetails = errorDetails + "SupportedAbis: " + getSupportedAbis() + " \n\n";
        errorDetails = errorDetails + "Stack trace:  \n";
        errorDetails = errorDetails + stackTraceString;
        return errorDetails;
    }

    public static String getApplicationName(Context context) {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            applicationInfo = null;
        }

        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }


    public static Class<? extends Activity> getRestartActivityClassFromIntent(Intent intent) {
        Serializable serializedClass = intent.getSerializableExtra("cat.ereza.customactivityoncrash.EXTRA_RESTART_ACTIVITY_CLASS");

        if ((serializedClass != null) && ((serializedClass instanceof Class))) {
            return (Class) serializedClass;
        }
        return null;
    }


    public static void restartApplicationWithIntent(Activity activity, Intent intent) {
        intent.addFlags(268468224);
        activity.finish();
        activity.startActivity(intent);
        killCurrentProcess();
    }


    public static void closeApplication(Activity activity) {
        activity.finish();
        killCurrentProcess();
    }


    public static boolean isLaunchErrorActivityWhenInBackground() {
        return launchErrorActivityWhenInBackground;
    }


    public static void setLaunchErrorActivityWhenInBackground(boolean launchErrorActivityWhenInBackground) {
        launchErrorActivityWhenInBackground = launchErrorActivityWhenInBackground;
    }


    public static boolean isShowErrorDetails() {
        return showErrorDetails;
    }


    public static void setShowErrorDetails(boolean showErrorDetails) {
        showErrorDetails = showErrorDetails;
    }


    public static int getDefaultErrorActivityDrawable() {
        return defaultErrorActivityDrawableId;
    }


    public static void setDefaultErrorActivityDrawable(int defaultErrorActivityDrawableId) {
        defaultErrorActivityDrawableId = defaultErrorActivityDrawableId;
    }


    public static boolean isEnableAppRestart() {
        return enableAppRestart;
    }


    public static void setEnableAppRestart(boolean enableAppRestart) {
        enableAppRestart = enableAppRestart;
    }


    public static Class<? extends Activity> getErrorActivityClass() {
        return errorActivityClass;
    }


    public static void setErrorActivityClass(Class<? extends Activity> errorActivityClass) {
        errorActivityClass = errorActivityClass;
    }


    public static Class<? extends Activity> getRestartActivityClass() {
        return restartActivityClass;
    }


    public static void setRestartActivityClass(Class<? extends Activity> restartActivityClass) {
        restartActivityClass = restartActivityClass;
    }


    private static boolean isStackTraceLikelyConflictive(Throwable throwable, Class<? extends Activity> activityClass) {
        do {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if (((element.getClassName().equals("android.app.ActivityThread")) && (element.getMethodName().equals("handleBindApplication"))) || (element.getClassName().equals(activityClass.getName()))) {
                    return true;
                }
            }
        } while ((throwable = throwable.getCause()) != null);
        return false;
    }

    public static String getAppDate(Context context, DateFormat dateFormat) {
        long lastUpdateTime = 0;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return dateFormat.format(new Date(lastUpdateTime));
    }

    private static String getBuildDateAsString(Context context, DateFormat dateFormat) {
        String buildDate;


        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            long time = ze.getTime();
            buildDate = dateFormat.format(new Date(time));
            zf.close();
        } catch (Exception e) {
            buildDate = "Unknown";
        }
        return buildDate;
    }


    private static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
        }
        return "Unknown";
    }


    private static String getDeviceModelName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String getSupportedAbis() {
        String[] supportedAbis = Build.SUPPORTED_ABIS;

        return Arrays.toString(supportedAbis);
    }


    private static String capitalize(String s) {
        if ((s == null) || (s.length() == 0)) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        }
        return Character.toUpperCase(first) + s.substring(1);
    }


    private static Class<? extends Activity> guessRestartActivityClass(Context context) {
        Class<? extends Activity> resolvedActivityClass = getRestartActivityClassWithIntentFilter(context);


        if (resolvedActivityClass == null) {
            resolvedActivityClass = getLauncherActivity(context);
        }

        return resolvedActivityClass;
    }


    private static Class<? extends Activity> getRestartActivityClassWithIntentFilter(Context context) {
        Intent searchedIntent = new Intent().setAction(INTENT_ACTION_RESTART_ACTIVITY).setPackage(context.getPackageName());
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(searchedIntent,
                PackageManager.GET_RESOLVED_FILTER);
        if ((resolveInfos != null) && (resolveInfos.size() > 0)) {
            ResolveInfo resolveInfo = (ResolveInfo) resolveInfos.get(0);
            try {
                return (Class<? extends Activity>) Class.forName(resolveInfo.activityInfo.name);
            } catch (ClassNotFoundException e) {
                Log.e("CustomActivityOnCrash", "Failed when resolving the restart activity class via intent filter, stack trace follows!", e);
            }
        }

        return null;
    }


    private static Class<? extends Activity> getLauncherActivity(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            try {
                return (Class<? extends Activity>) Class.forName(intent.getComponent().getClassName());
            } catch (ClassNotFoundException e) {
                Log.e("CustomActivityOnCrash", "Failed when resolving the restart activity class via getLaunchIntentForPackage, stack trace follows!", e);
            }
        }

        return null;
    }


    private static Class<? extends Activity> guessErrorActivityClass(Context context) {
        Class<? extends Activity> resolvedActivityClass = getErrorActivityClassWithIntentFilter(context);


        if (resolvedActivityClass == null) {
            resolvedActivityClass = DefaultErrorActivity.class;
        }

        return resolvedActivityClass;
    }

    private static Class<? extends Activity> getErrorActivityClassWithIntentFilter(Context context) {
        Intent searchedIntent = new Intent().setAction(INTENT_ACTION_ERROR_ACTIVITY).setPackage(context.getPackageName());
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(searchedIntent,
                PackageManager.GET_RESOLVED_FILTER);

        if ((resolveInfos != null) && (resolveInfos.size() > 0)) {
            ResolveInfo resolveInfo = (ResolveInfo) resolveInfos.get(0);
            try {
                return (Class<? extends Activity>) Class.forName(resolveInfo.activityInfo.name);
            } catch (ClassNotFoundException e) {
                Log.e("CustomActivityOnCrash", "Failed when resolving the error activity class via intent filter, stack trace follows!", e);
            }
        }

        return null;
    }


    private static void killCurrentProcess() {
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    /**
     * Post请求
     *
     * @param httpUrl
     * @param json
     */
    private static void requestPost(String httpUrl, String json) {
        try {
            String baseUrl = httpUrl;
            //合成参数
            StringBuilder tempParams = new StringBuilder();
//            int pos = 0;
//            for (String key : paramsMap.keySet()) {
//                if (pos > 0) {
//                    tempParams.append("&");
//                }
//                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
//                pos++;
//            }
//            String params = tempParams.toString();
            System.out.println("Post方式请求地址httpUrl--->" + httpUrl);
//            System.out.println("Post方式请求参数params--->" + params);
            // 请求的参数转换为byte数组
//            byte[] postData = params.getBytes();
            // 新建一个URL对象
            URL url = new URL(baseUrl);
            // 打开一个HttpURLConnection连接
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            // 设置连接超时时间
            urlConn.setConnectTimeout(5 * 1000);
            //设置从主机读取数据超时
            urlConn.setReadTimeout(5 * 1000);
            // Post请求必须设置允许输出 默认false
            urlConn.setDoOutput(true);
            //设置请求允许输入 默认是true
            urlConn.setDoInput(true);
            // Post请求不能使用缓存
            urlConn.setUseCaches(false);
            // 设置为Post请求
            urlConn.setRequestMethod("POST");
            //设置本次连接是否自动处理重定向
            urlConn.setInstanceFollowRedirects(true);
            // 配置请求Content-Type
            //设置请求头里面的数据，以下设置用于解决http请求code415的问题
            urlConn.setRequestProperty("Content-Type",
                    "application/json");

//            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            // 开始连接
            urlConn.connect();
            // 发送请求参数
            DataOutputStream dos = new DataOutputStream(urlConn.getOutputStream());
            dos.write(json.getBytes());
            dos.flush();
            dos.close();
            // 判断请求是否成功
            if (urlConn.getResponseCode() == 200) {
                // 获取返回的数据
                String result = streamToString(urlConn.getInputStream());
                System.out.println("Post方式请求成功，result--->" + result);
            } else {
                System.out.println("Post方式请求失败");
            }
            // 关闭连接
            urlConn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将输入流转换成字符串
     *
     * @param is 从网络获取的输入流
     * @return
     */
    public static String streamToString(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            baos.close();
            is.close();
            byte[] byteArray = baos.toByteArray();
            return new String(byteArray);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

