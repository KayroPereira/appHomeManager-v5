package com.home.apphomemanager_v4.util;

import android.util.Log;
import android.util.Pair;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AtributoUtils {

    public static String getPath(String pathCampo){
        return getPath(pathCampo, "\\.");
    }

    public static String getPath(String pathCampo, String separador){
        List<String> atributo = Arrays.stream(pathCampo.split(separador)).collect(Collectors.toList());
        int ultimoElemento = atributo.size() - 1;

        if (ultimoElemento > 0) {
            atributo.remove(ultimoElemento);
        }else{
            return "";
        }
        return "." + String.join(".", atributo);
    }

    public static String getAtributo(String pathCampo){
        return getAtributo(pathCampo, "\\.");
    }

    public static String getAtributo(String pathCampo, String separador){
        String[] atributo = pathCampo.split(separador);

        return atributo[atributo.length-1];
    }

    public static Pair<String, String> getParamentrosField(Object obj, String campo){

        String[] nameClass = obj.getClass().getName().split("\\.");
        String classPai = (nameClass[nameClass.length-1]).toLowerCase();

        String atributo = AtributoUtils.getAtributo(campo);

        String pathTarget = classPai + AtributoUtils.getPath(campo);

        return new Pair<>(pathTarget, atributo);
    }

    public static <K, V> K buscarChavePorValor(Map<K, V> mapa, V valor) {
        for (Map.Entry<K, V> entry : mapa.entrySet()) {
            if (valor.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void transferirValoresEntreObjetos(Object sourceObj, Object targetObj, List<String> fields) {
        fields.stream().forEach(field -> {

            Object sourceValue = obterValorCampo(sourceObj, field);

            if(sourceValue != null) {
                setValor(targetObj, field, sourceValue);
            }
        });
    }

    public static void atributosAlterados(Object oldObj, Object newObj, List<String> fields) {
        atributosAlterados(oldObj, newObj, fields, "");
    }

    public static void atributosAlterados(Object oldObj, Object newObj, List<String> fields, String path) {
        Class<?> oldClasse = oldObj.getClass();
        Class<?> newClasse = newObj.getClass();

        while (oldClasse != null) {
            Field[] oldAtributos = oldClasse.getDeclaredFields();
            Field[] newAtributos = newClasse.getDeclaredFields();

            if(!oldClasse.getName().startsWith("java.lang.Object")){

                Iterator<Field> oldIterator = Arrays.stream(oldAtributos).iterator();
                Iterator<Field> newIterator = Arrays.stream(newAtributos).iterator();

                while (oldIterator.hasNext() && newIterator.hasNext()){

                    Field oldAtt = oldIterator.next();
                    Field newAtt = newIterator.next();

                    oldAtt.setAccessible(true);
                    newAtt.setAccessible(true);

                    try {
                        Object oldValue = oldAtt.get(oldObj);
                        Object newValue = newAtt.get(newObj);

                        if (oldValue != null && !oldAtt.getType().isPrimitive() && !oldAtt.getType().getName().startsWith("java.")) {
                            atributosAlterados(oldValue, newValue, fields, path + oldAtt.getName() + ".");
                        } else {
                            if (oldValue == null || !oldValue.equals(newValue)) {
                                fields.add(path + oldAtt.getName());
                            }
                        }
                    } catch (IllegalAccessException e) {
                        Log.d("AtributoUtils", "Erro ao acessar o campo " + oldAtt.getName());
                    }
                }
            }
            oldClasse = oldClasse.getSuperclass();
        }
    }

    public static void setValor(Object objectMain, String fullPath, Object value) {

        String[] pathSplit = fullPath.split("\\.");

        Object objectTarget = objectMain;
        Field field;

        for (String path : pathSplit) {
            try {
                field = getField(objectTarget.getClass(), path);
                field.setAccessible(true);

                if(!field.getType().getCanonicalName().startsWith("java.")) {
                    objectTarget = field.get(objectTarget);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return;
            }
        }

        try {
            field = getField(objectTarget.getClass(), pathSplit[pathSplit.length - 1]);
            field.setAccessible(true);
            field.set(objectTarget, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Log.d("AtributoUtils", "Erro: " + e.getMessage());
        }
    }

    public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    public static Object obterValorCampo(Object objectMain, String fullPath) {
        String[] pathSplit = fullPath.split("\\.");

        Object objectTarget = objectMain;
        for (String path : pathSplit) {
            try {
                Field field = getField(objectTarget.getClass(), path);
                field.setAccessible(true);
                objectTarget = field.get(objectTarget);

            } catch (NoSuchFieldException | IllegalAccessException e) {
                objectTarget = null;
            }
        }
        return objectTarget;
    }

    public static void obterTodosAtributos(Object obj, List<String> atributos, boolean incluirClassPai) {
        obterTodosAtributos(obj, atributos, incluirClassPai, "", "");
    }

    public static void obterTodosAtributos(Object obj, List<String> atributos, boolean incluirClassPai, String path, String classPai) {
        Class<?> objClasse = obj.getClass();

        if("".equals(classPai)){
            String[] nameClass = obj.getClass().getName().split("\\.");
            classPai = (nameClass[nameClass.length-1]+".").toLowerCase();
        }

        while (objClasse != null) {
            Field[] fields = objClasse.getDeclaredFields();

            if(!objClasse.getName().startsWith("java.lang.Object")){

                Iterator<Field> iterator = Arrays.stream(fields).iterator();

                while (iterator.hasNext()){

                    Field att = iterator.next();

                    att.setAccessible(true);

                    try {
                        Object value = att.get(obj);

                        if(!att.getType().isPrimitive() && !att.getType().getName().startsWith("java.")){
                            obterTodosAtributos(value, atributos, incluirClassPai, path+att.getName()+".", classPai);
                        }else {
                            atributos.add(incluirClassPai ? classPai+path+att.getName() : path+att.getName());
                        }
                    } catch (IllegalAccessException e) {
                        Log.d("AtributoUtils", "Erro ao acessar o campo " + att.getName());
                    }
                }
            }
            objClasse = objClasse.getSuperclass();
        }
    }

    public static <T> boolean compare(Class<T> obj1, Class<T> obj2){
        return obj1.equals(obj2);
    }

    public static void getAtributos(List<Field> fields, Object obj){
        for(Field f: fields){
            f.setAccessible(true);

            try {
                Object o = f.get(obj);
            } catch (IllegalAccessException e) {
                Log.d("AtributoUtils", "Erro ao acessar o campo " + f.getName());
            }
        }
    }
}