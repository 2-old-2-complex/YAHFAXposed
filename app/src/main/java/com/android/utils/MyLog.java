package com.android.utils;

import android.os.Process;
import android.util.Log;

public class MyLog {

    private static Boolean isDebug=true;
    public static void d(String tag,String msg)
    {
        if(isDebug) {
            Log.e(tag, "["+ Process.myPid() +"]"+msg);
        }
    }

    public static void e(String tag,String msg)
    {
        if(isDebug) {
            Log.e(tag, "["+ Process.myPid() +"]"+msg);
        }
    }
}
