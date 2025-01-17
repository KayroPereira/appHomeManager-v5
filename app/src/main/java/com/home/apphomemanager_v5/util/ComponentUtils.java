package com.home.apphomemanager_v5.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.viewbinding.ViewBinding;

import com.home.apphomemanager_v5.inteface.EventClick;

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
            }else if(child instanceof  ImageView){
                changeValueComponent(child, false);
            }
        }
    }

    public static void setComponentEnabledAll(ViewBinding binding, Map<Integer, String> componentsActivity, boolean isEnabled){

        componentsActivity.forEach((key, value) -> {

            setComponentEnabled(binding, key, isEnabled);
        });
    }

    public static void setComponentEnabled(ViewBinding binding, int componentId, boolean isEnabled) {

        View component = binding.getRoot().findViewById(componentId);

        if (component != null) {
            component.setEnabled(isEnabled);

            if (component instanceof ImageView) {
                ImageView imageView = (ImageView) component;
                if (isEnabled) {
                    removerImageViewTomDeCinza(imageView);
                } else {
                    imageViewTomDeCinza(imageView);
                }
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

    public static void changeValueComponent(View child, Object... value){

        if(child instanceof SwitchCompat){

            SwitchCompat switchComponent = (SwitchCompat) child;
            switchComponent.setChecked((boolean) value[0]);

        }else if(child instanceof TextView){

            TextView textViewComponent = (TextView) child;
            textViewComponent.setText(value[0].toString());

        }else if(child instanceof ImageView){

            ImageView imageView = (ImageView) child;

            if(imageView.getTag() != null) {

                Pair<String, String> parametros = getStatusAndImagemImageViewByTag(imageView);

                imageView.setTag(parametros.first + "_" + getOnOff((boolean) value[0]));
                imageView.setImageResource(getIdImage(imageView.getContext(), imageView.getTag().toString()));
            }
        }
    }

    public static Pair<String, String> getStatusAndImagemImageViewByTag(ImageView imageView){
        String[] tag = imageView.getTag().toString().split("_");

        Pair<String, String> parametros = new Pair<>(tag[0], tag[1]);

        return parametros;
    }

    public static <T> void setSwitchCheckedChangeListener(SwitchCompat switchCompat, Map<Integer, String> componentsActivity, T clazz){

        setSwitchCheckedChangeListener(switchCompat, componentsActivity, clazz, null);
    }

    public static <T> void setSwitchCheckedChangeListener(SwitchCompat switchCompat, Map<Integer, String> componentsActivity, T clazz, String pathPai){
        switchCompat.setOnCheckedChangeListener((compoundButton, b) -> {

            Integer idComponent = compoundButton.getId();
            String path = componentsActivity.get(idComponent);
            Pair<String, String> parametros = AtributoUtils.getParamentrosField(clazz, path);

            String pathFinal = pathPai != null ? pathPai : parametros.first;

            FirebaseUtils.sendSimpleData(pathFinal, parametros.second, b);
        });
    }

    public static <T> void setImageViewToggleListener(ImageView imageView, Map<Integer, String> componentsActivity, T clazz){

        setImageViewToggleListener(imageView,componentsActivity, clazz, null);
    }

    public static <T> void setImageViewToggleListener(ImageView imageView, Map<Integer, String> componentsActivity, T clazz, String pathPai){

        imageView.setOnClickListener((compoundButton) -> {

            ImageView im = (ImageView) compoundButton;

            Integer idComponent = compoundButton.getId();
            String path = componentsActivity.get(idComponent);
            Pair<String, String> parametros = AtributoUtils.getParamentrosField(clazz, path);

            Pair<String, String> statusComponent = getStatusAndImagemImageViewByTag(im);

            String valorStatusToggle = toggleStatusString(statusComponent.second);

            im.setTag(statusComponent.first+"_"+valorStatusToggle);
            im.setImageResource(getIdImage(im.getContext(), im.getTag().toString()));

            String pathFinal = pathPai != null ? pathPai : parametros.first;

            FirebaseUtils.sendSimpleData(pathFinal, parametros.second, getTrueFalse(valorStatusToggle));
        });
    }

    public static <T extends  View> void setEventClickGeneric(T imageView, EventClick method){

        imageView.setOnClickListener(event -> method.execute(event));
    }

    public static int getIdImage(Context context, String nome){

        return getIdImage(context, "drawable", nome);
    }

    public static int getIdImage(Context context, String defType, String nome){

        Resources resources = context.getResources();

        return resources.getIdentifier(nome, defType, context.getPackageName());
    }

    public static String toggleStatusString(boolean statusComponent){
        return "on".equalsIgnoreCase(getOnOff(statusComponent)) ? "off" : "on";
    }

    public static String toggleStatusString(String statusComponent){
        return "on".equalsIgnoreCase(statusComponent) ? "off" : "on";
    }

    public static String getOnOff(boolean status){
        return status ? "on" : "off";
    }

    public static boolean getTrueFalse(String status){
        return ("on".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status));
    }

    public static void imageViewTomDeCinza(ImageView imageView){

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        imageView.setColorFilter(filter);
    }

    public static void removerImageViewTomDeCinza(ImageView imageView){
        imageView.clearColorFilter();
    }
}