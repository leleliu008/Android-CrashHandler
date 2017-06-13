package com.fpliu.newton.crash;


import android.content.Context;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.widget.Toast;

import com.fpliu.newton.log.Logger;

import java.io.File;

/**
 * 未捕获异常处理器
 *
 * @author 792793182@qq.com 2015-06-11
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    /**
     * 崩溃日志文件名称
     */
    private static final String FILE_UNCAUGHT_EXCEPTION_LOG = "UncaughtException.log";

    private Thread.UncaughtExceptionHandler otherUncaughtExceptionHandler;

    private Context appContext;

    private CrashHandler(Context appContext) {
        this.appContext = appContext;
        otherUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void init(Context appContext) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(appContext));
    }

    @Override
    public void uncaughtException(Thread thread, final Throwable ex) {
        Logger.e("DebugLog", "uncaughtException() " + thread.getName(), ex);

        // 使用 Toast 来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(appContext, "程序异常，正要退出", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();

        // 保存堆栈信息
        Logger.syncSaveFile(getUncaughtExceptionFile(appContext), ex);
        // 等待2s
        SystemClock.sleep(2000);

        if (otherUncaughtExceptionHandler != null) {
            otherUncaughtExceptionHandler.uncaughtException(thread, ex);
        }

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
}