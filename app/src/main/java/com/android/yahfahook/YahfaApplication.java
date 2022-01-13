package com.android.yahfahook;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.lsposed.lspd.core.Main;
import org.lsposed.lspd.yahfa.hooker.YahfaHooker;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class YahfaApplication extends Application {



    private static final String  TAG=YahfaApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();


        //加载so
        System.loadLibrary("yahfaxp");
        //初始化xposed 框架的一些参数
        XposedInit.init(this,this.getPackageName());
        //正式hook方法
        XposedHelpers.findAndHookMethod(MainActivity.class, "getMsg", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Log.d(TAG,"############ Hook Success!!!!!!!!!!!!!!");
                param.setResult("hook success!0000000000000000000000000"+count++);
            }
        });

    }
    static int count=0;
}
