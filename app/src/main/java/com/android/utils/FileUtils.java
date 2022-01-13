package com.android.utils;

import java.io.FileOutputStream;

public class FileUtils {

    public static  void  saveFile(String filePath,String textMsg)
    {
        try{
            FileOutputStream fileOutputStream=new FileOutputStream(filePath,true);
            fileOutputStream.write(textMsg.getBytes("utf-8"));
            fileOutputStream.flush();
            fileOutputStream.close();
        }catch (Exception eeeee)
        {

        }
    }

    public static  void  saveFile(String filePath,String textMsg,boolean isAppend)
    {
        try{
            FileOutputStream fileOutputStream=new FileOutputStream(filePath,isAppend);
            fileOutputStream.write(textMsg.getBytes("utf-8"));
            fileOutputStream.flush();
            fileOutputStream.close();
        }catch (Exception eeeee)
        {

        }
    }
}
