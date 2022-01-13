package com.android.utils;

import android.net.Uri;

import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class UrlParse {

    /**
     * 获得解析后的URL参数
     * @param url url对象
     * @return URL参数map集合
     */
    public static Map<String, String> getUrlParams(String url){
        final Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        URL mUrl = stringToURL(url);
        if(mUrl == null)
        {
            return query_pairs;
        }
        try {
            String query = mUrl.getQuery();
            if(query==null){
                return query_pairs;
            }
            //判断是否包含url=,如果是url=后面的内容不用解析
            if(query.contains("url=")){
                int index = query.indexOf("url=");
                String urlValue = query.substring(index + 4);
                query_pairs.put("url", URLDecoder.decode(urlValue, "UTF-8"));
                query = query.substring(0, index);
            }
            //除url之外的参数进行解析
            if(query.length()>0) {
                final String[] pairs = query.split("&");
                for (String pair : pairs) {
                    final int idx = pair.indexOf("=");
                    //如果等号存在且不在字符串两端，取出key、value
                    if (idx > 0 && idx < pair.length() - 1) {
                        final String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                        final String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                        query_pairs.put(key, value);
                    }
                }
            }
        }catch (Exception ex){

        }
        return query_pairs;
    }

    /**
     * 获得Url参数字符串
     * @param url url地址
     * @return 参数字符串
     */
    public static String getUrlParamStr(String url){
        URL mUrl = stringToURL(url);
        if(mUrl == null){
            return "";
        }
        try {
            String query = mUrl.getQuery();
            return query;
        }catch (Exception ex){

        }
        return "";
    }

    /**
     * 获得url的协议+域+路径（即url路径问号左侧的内容）
     * @param url url地址
     * @return url的协议+域+路径
     */
    public static String getUrlHostAndPath(String url){
        if (url.contains("?")) {
            return url.substring(0, url.indexOf("?"));
        }
        return url;
    }

    /**
     * 获得Uri参数值
     * @param uri uri
     * @param paramKey 参数名称
     * @return 参数值
     */
    public static String getUriParam(Uri uri, String paramKey){
        if(uri == null || paramKey==null || paramKey.length() == 0){
            return "";
        }
        String paramValue = uri.getQueryParameter(paramKey);
        if(paramValue==null){
            paramValue = "";
        }
        return paramValue;
    }

    /**
     * 获得Uri参数值
     * @param uri uri
     * @param paramKey 参数名称
     * @return 参数值
     */
    public static int getIntUriParam(Uri uri, String paramKey){
        if(uri == null || paramKey==null || paramKey.length() == 0){
            return 0;
        }
        try {
            String paramValue = uri.getQueryParameter(paramKey);
            if (paramValue == null || paramValue.length() == 0) {
                return 0;
            }
            return Integer.parseInt(paramValue);
        }catch (Exception ex){

        }
        return 0;
    }

    /**
     * 字符串转为URL对象
     * @param url url字符串
     * @return url对象
     */
    private static URL stringToURL(String url){
        if(url==null || url.length() == 0 || !url.contains("://")){
            return null;
        }
        try {
            StringBuilder sbUrl = new StringBuilder("http");
            sbUrl.append(url.substring(url.indexOf("://")));
            URL mUrl = new URL(sbUrl.toString());
            return mUrl;
        }catch (Exception ex){

            return null;
        }
    }
}
