package com.fpliu.newton.crash;


import android.content.Context;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import com.fpliu.newton.log.Logger;

import java.io.File;

/**
 * 未捕获异常处理器
 *
 * @author 792793182@qq.com 2015-06-11
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = CrashHandler.class.getSimpleName();

    /**
     * 崩溃日志文件名称
     */
    private static final String FILE_UNCAUGHT_EXCEPTION_LOG = "UncaughtException.log";

    private Thread.UncaughtExceptionHandler otherUncaughtExceptionHandler;

    private Context appContext;

    private TipCallback tipCallback;

    private CrashHandler(Context appContext, TipCallback tipCallback) {
        this.appContext = appContext;
        this.tipCallback = tipCallback;
        otherUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void init(Context appContext, TipCallback tipCallback) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(appContext, tipCallback));
    }

    @Override
    public void uncaughtException(Thread thread, final Throwable throwable) {
        Logger.e(TAG, "uncaughtException() " + thread.getName(), throwable);

        if (tipCallback != null) {
            // 在界面上显示异常信息
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    tipCallback.onTip(thread, throwable);
                    Looper.loop();
                }
            }.start();
        }

        // 保存堆栈信息
        Logger.syncSaveFile(getUncaughtExceptionFile(appContext), throwable);

        if (otherUncaughtExceptionHandler != null) {
            otherUncaughtExceptionHandler.uncaughtException(thread, throwable);
        }

        // 等待2s
        SystemClock.sleep(2000);

        // 杀死进程
        Process.killProcess(Process.myPid());
    }

    /**
     * 获取保存日志的路径
     */
    public File getUncaughtExceptionFile(Context context) {
        File logDir = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getPackageName() + "/log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        return new File(logDir, FILE_UNCAUGHT_EXCEPTION_LOG);
    }

    public interface TipCallback {
        void onTip(Thread thread, final Throwable throwable);
    }
}