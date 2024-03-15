package com.home.apphomemanager_v4.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.viewbinding.ViewBinding;

import java.util.List;
import java.util.Map;

public class ComponentUtils {

    public static void inicializaElementos(ViewBinding binding){
        ViewGroup layout = (ViewGroup) binding.getRoot();

        for(int i = 0; i < layout.getChildCount(); i++){

            View child = layout.getChildAt(i);

            if(child instanceof SwitchCompat){
                changeValueComponent(child, false);
            }else if(child instanceof TextView){
                changeValueComponent(child, "");
            }
        }
    }

    public static void atualizaComponents(Object source, List<String> fields, Map<Integer, String> componentActivity, ViewBinding binding){

        fields.stream().forEach(att -> {
            Object value = AtributoUtils.obterValorCampo(source, att);
            View child = getComponent(binding, componentActivity, att);

            changeValueComponent(child, value);
        });
    }

    public static View getComponent(ViewBinding binding, Map<Integer, String> componentActivity, String atributo){
        Integer idComponent = AtributoUtils.buscarChavePorValor(componentActivity, atributo);

        return idComponent != null ? binding.getRoot().findViewById(idComponent) : null;
    }

    public static void changeValueComponent(View child, Object value){
        if(child instanceof SwitchCompat){
            SwitchCompat switchComponent = (SwitchCompat) child;
            switchComponent.setChecked((boolean) value);
        }else if(child instanceof TextView){
            TextView textViewComponent = (TextView) child;
            textViewComponent.setText(value.toString());
        }
    }
}