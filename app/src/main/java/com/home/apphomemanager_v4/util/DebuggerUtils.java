package com.home.apphomemanager_v4.util;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.List;

public class DebuggerUtils {

    private DebuggerUtils(){}

    public static <T> void printObj(List<T> obj, String text){
        obj.stream().forEach(o -> printObj(o));
    }

    public static void log(String msn){
        Log.d("DebuggerUtils", "\n\n<------------- " + msn + " ------------------>");
    }

    public static void log(String tag, String msn){
        Log.d(tag, "\n\n<------------- " + msn + " ------------------>");
    }

    public static void printObj(Object obj){

        Class<?> classe = obj.getClass();

        while (classe != null) {
            Field[] atributos = classe.getDeclaredFields();

            if(!classe.getName().startsWith("java.lang.Object")){
                Log.d("DebuggerUtils", "\n\n<------------- printObj - class: " + classe.getCanonicalName() + " ------------------>");

                for (Field atributo : atributos) {
                    atributo.setAccessible(true);

                    try {
                        Object o = atributo.get(obj);

                        if(!atributo.getType().isPrimitive() && !atributo.getType().getName().startsWith("java.")){
                            printObj(o);
                        }else {
                            Log.d("DebuggerUtils", atributo.getName() + " = " + o);
                        }
                    } catch (IllegalAccessException e) {
                        Log.d("DebuggerUtils", "Erro ao acessar o campo " + atributo.getName());
                    }
                }
            }
            classe = classe.getSuperclass();
        }
    }
}