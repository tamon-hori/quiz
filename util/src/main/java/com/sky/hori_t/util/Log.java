package com.sky.hori_t.util;

import android.support.annotation.Nullable;

public class Log {
    // タグ
    private static final String TAG = "クイズアプリ";
    // ログ出力ON/OFF
    private static final boolean OUTPUT_LOG_ENABLED = true;
    // スレッド名出力ON/OFF
    private static final boolean OUTPUT_THREAD_NAME = false;
    private static final LogLevel OUTPUT_LOG_LEVEL = LogLevel.Verbose;

    private static final String logPrefixDebug = "[DEBUG]";
    private static final String logPrefixInfo = "[INFO ]";
    private static final String logPrefixWarn = "[WARN ]";
    private static final String logPrefixError = "[ERROR]";
    private static final String logPrefixMethodIn = "[IN   ]";
    private static final String logPrefixMethodOut = "[OUT  ]";

    private static final int stackDepth = 5;

    public static void d(String msg) {
        outputLog(LogLevel.Debug, logPrefixDebug, msg);
    }

    public static void vMethodIn() {
        outputLog(LogLevel.Verbose, logPrefixMethodIn, "");
    }

    public static void vMethodIn(String msg) {
        outputLog(LogLevel.Verbose, logPrefixMethodIn, msg);
    }

    public static void vMethodOut() {
        outputLog(LogLevel.Verbose, logPrefixMethodOut, "");
    }

    public static void vMethodOut(String msg) {
        outputLog(LogLevel.Verbose, logPrefixMethodOut, msg);
    }

    private static void outputLog(LogLevel logLevel, String prefix, String msg) {
        outputLog(logLevel, prefix + " " + methodNameString(stackDepth) + " " + msg);
    }

    private static void outputLog(LogLevel logLevel, String msg) {
        outputLog(logLevel, TAG, OUTPUT_THREAD_NAME, msg);
    }

    private static void outputLog(
            @Nullable LogLevel logLevel,
            @Nullable String tag,
            boolean addThreadName,
            @Nullable String msg) {
        if (!OUTPUT_LOG_ENABLED) {
            return;
        }
        logLevel = (null == logLevel) ? LogLevel.Error : logLevel;
        if (OUTPUT_LOG_LEVEL.ordinal() > logLevel.ordinal()) {
            return;
        }
        String threadName = addThreadName ? "[ThreadName: " + Thread.currentThread().getName() + "]" : "";
        tag = null == tag ? TAG :tag;
        msg = threadName + msg;
        switch (logLevel) {
            case Debug:
                android.util.Log.d(tag, msg);
                break;
            case Info:
                android.util.Log.i(tag, msg);
                break;
            case Warn:
                android.util.Log.w(tag, msg);
                break;
            case Error:
                android.util.Log.e(tag, msg);
                break;
            default:
                android.util.Log.v(tag, msg);
                break;
        }
    }

    protected static String methodNameString(int stackDepth) {
        final StackTraceElement element = Thread.currentThread().getStackTrace()[stackDepth];
        final String fullClassName = element.getClassName();
        final String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String methodName = element.getMethodName();
        final int lineNumber = element.getLineNumber();
        return simpleClassName + "#" + methodName + ":" + lineNumber;
    }

    private enum LogLevel {
        Verbose,
        Debug,
        Info,
        Warn,
        Error
    }
}
