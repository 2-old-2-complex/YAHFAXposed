package com.android.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyUtils {

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        return result;
    }

    /**
     * byte[]转int
     * @param bytes 需要转换成int的数组
     * @return int值
     */
    public static int byteArrayToInt(byte[] bytes) {
        int value=0;
        for(int i = 0; i < 4; i++) {
            int shift= (3-i) * 8;
            value +=(bytes[i] & 0xFF) << shift;
        }
        return value;
    }



    /**
     * 获取当前时间戳
     * @param timeStamp
     * @return
     */
    public static String timeStamp2Date(String timeStamp) {
        String format = "yyyyMMddHHmm";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        if(timeStamp.length()==10) {
            return sdf.format(new Date(Long.valueOf(timeStamp + "000")));
        }
        return  sdf.format(new Date(Long.valueOf(timeStamp)));
    }

    public static  String  getCurrentTime()
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");//设置日期格式
        String timeString=df.format(new Date());// new Date()为获取当前系统时间
        return timeString;
    }

    public static  String  getCurrentYMDHMS()
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-ddHHmmss");//设置日期格式
        String timeString=df.format(new Date());// new Date()为获取当前系统时间
        return timeString;
    }

    public static  String  getCurrentYMD()
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");//设置日期格式
        String timeString=df.format(new Date());// new Date()为获取当前系统时间
        return timeString;
    }

    public static  String  getCurrentYMDHour()
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHH");//设置日期格式
        String timeString=df.format(new Date());// new Date()为获取当前系统时间
        return timeString;
    }

    /**
     * 获取app版本
     * @param context
     * @return
     */
    public static String  getAppVersion(Context context,String pkgName)
    {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo applicationInfo = packageManager.getPackageInfo(pkgName, 0);
            return applicationInfo.versionName;
        } catch (Exception eeeee)
        {

        }
        return "";
    }


    public static String  getProcessName()
    {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/proc/self/cmdline"))));
            String line=bufferedReader.readLine().trim();
            bufferedReader.close();
            return line.trim();
        }catch (Exception eeee)
        {

        }
        return "";
    }

    public static Boolean isGranted(Context context)
    {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method isGrantedMethod=connectivityManager.getClass().getDeclaredMethod("isGranted");
            isGrantedMethod.setAccessible(true);
            return (boolean)isGrantedMethod.invoke(connectivityManager);
        }catch (Exception eee)
        {

        }
        return false;

    }

    /***
     * 设置
     * @param context
     * @param packageName
     * @param isHide
     */
    public static   void   hideApp(Context context,String packageName,boolean isHide) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method setKeyMethod=connectivityManager.getClass().getDeclaredMethod("hideApp",String.class,boolean.class);
            setKeyMethod.setAccessible(true);
            setKeyMethod.invoke(connectivityManager,packageName,isHide);
        }catch (Exception eee)
        {

        }
    }


    public static  void   hidePluginApp(Context context,String packageName,boolean isHide){
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method setKeyMethod=connectivityManager.getClass().getDeclaredMethod("hidePluginApp",String.class,boolean.class);
            setKeyMethod.setAccessible(true);
            setKeyMethod.invoke(connectivityManager,packageName,isHide);
        }catch (Exception eee)
        {

        }
    }

    public static  String getOsType(Context context)
    {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method setKeyMethod=connectivityManager.getClass().getDeclaredMethod("getOsType");
            setKeyMethod.setAccessible(true);
            return (String)setKeyMethod.invoke(connectivityManager);
        }catch (Exception eee)
        {

        }
        return "";
    }

}
