package com.home.apphomemanager_v5.util;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseUtils {

    public static void sendSimpleData(String path, String propriedade, Object value) {

        try {
            FirebaseDatabase dtBase = FirebaseDatabase.getInstance();
            DatabaseReference reference = dtBase.getReference(path.replace(".", "/"));

            Map<String, Object> campo = new HashMap<>();

            campo.put(propriedade, value);

            reference.updateChildren(campo);
        }catch(Exception e){
            DebuggerUtils.log("sendSimpleData", e.getMessage());
        }
    }

    public static void updateMultipleFields(Object obj, List<String> campos){

        String[] nameClass = obj.getClass().getName().split("\\.");
        String classPai = (nameClass[nameClass.length-1]).toLowerCase();

        for(String campo : campos) {

            String atributo = AtributoUtils.getAtributo(campo);

            String pathTarget = classPai + AtributoUtils.getPath(campo);

            Object value = AtributoUtils.obterValorCampo(obj, campo);

            sendSimpleData(pathTarget, atributo, value);
        }
    }
}