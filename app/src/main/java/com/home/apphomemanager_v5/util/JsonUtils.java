package com.home.apphomemanager_v5.util;

import com.google.gson.Gson;
import org.json.JSONObject;

public class JsonUtils {

    public static <T> T fromJson(Class<T> clazz, JSONObject json){
        return (T) fromJson(clazz, json.toString());
    }

    public static <T> T fromJson(Class<T> clazz, String json){
        return new Gson().fromJson(json, clazz);
    }

    public static  <T> String toJson(T objeto){
        return new Gson().toJson((T) objeto);
    }
}