package com.home.apphomemanager_v4;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.home.apphomemanager_v4.databinding.ActivityDadosFirebaseBinding;
import com.home.apphomemanager_v4.model.churrasqueira.Churrasqueira;
import com.home.apphomemanager_v4.util.AtributoUtils;
import com.home.apphomemanager_v4.util.ComponentUtils;
import com.home.apphomemanager_v4.util.DebuggerUtils;
import com.home.apphomemanager_v4.util.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DadosFirebaseActivity extends AppCompatActivity {

    private ActivityDadosFirebaseBinding binding;

    private FirebaseDatabase database;

    private DatabaseReference mDatabase;

    private Churrasqueira churrasqueira;

    private Map<Integer, String> componentActivity = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        churrasqueira = new Churrasqueira();
        churrasqueira.inicializa();

        binding = ActivityDadosFirebaseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference().child("churrasqueira");

        mapeametoComponenteToFirebase();
        ComponentUtils.inicializaElementos(binding);

        ouvinteFirebase();

        setSwitchCheckedChangeListener(binding.swOnOffChurrasqueiraGeral);
        setSwitchCheckedChangeListener(binding.swMDirecaoChurrasqueira);
        setSwitchCheckedChangeListener(binding.swMHabilitadoChurrasqueira);
        setSwitchCheckedChangeListener(binding.swMOnOffChurrasqueira);
        setSwitchCheckedChangeListener(binding.swOAguaChurrasqueira);
        setSwitchCheckedChangeListener(binding.swOExauChurrasqueira);
        setSwitchCheckedChangeListener(binding.swOLampadaChurrasqueira);
        setSwitchCheckedChangeListener(binding.swOSopradorChurrasqueira);
        setSwitchCheckedChangeListener(binding.swSFc1Churrasqueira);
        setSwitchCheckedChangeListener(binding.swSFc2Churrasqueira);
    }

    private void setSwitchCheckedChangeListener(SwitchCompat switchCompat){
        switchCompat.setOnCheckedChangeListener((compoundButton, b) -> {

            Integer idComponent = compoundButton.getId();
            String path = componentActivity.get(idComponent);
            Pair<String, String> parametros = getParamentrosField(churrasqueira, path);

            sendSimpleData(parametros.first, parametros.second, b);
        });
    }

    private void sendSimpleData(String path, String propriedade, Object value) {

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

    private Pair<String, String> getParamentrosField(Object obj, String campo){

        DebuggerUtils.log("updateMultipleFields", "----------------- updateMultipleFields --------------------");

        String[] nameClass = obj.getClass().getName().split("\\.");
        String classPai = (nameClass[nameClass.length-1]).toLowerCase();

        String atributo = getAtributo(campo);

        String pathTarget = classPai + getPath(campo);

        DebuggerUtils.log("updateMultipleFields", "pathTarget: " + pathTarget + " atributo: " + atributo);

        return new Pair<>(pathTarget, atributo);
    }

    private void updateMultipleFields(Object obj, List<String> campos){

        DebuggerUtils.log("updateMultipleFields", "----------------- updateMultipleFields --------------------");

        String[] nameClass = obj.getClass().getName().split("\\.");
        String classPai = (nameClass[nameClass.length-1]).toLowerCase();

        for(String campo : campos) {

            String atributo = getAtributo(campo);

            String pathTarget = classPai + getPath(campo);

            DebuggerUtils.log("updateMultipleFields", "pathTarget: " + pathTarget + " atributo: " + atributo);

            Object value = AtributoUtils.obterValorCampo(obj, campo);

            sendSimpleData(pathTarget, atributo, value);
        }
    }

    private String getPath(String pathCampo){
        return getPath(pathCampo, "\\.");
    }

    private String getPath(String pathCampo, String separador){
        List<String> atributo = Arrays.stream(pathCampo.split(separador)).collect(Collectors.toList());
        int ultimoElemento = atributo.size() - 1;

        if (ultimoElemento > 0) {
            atributo.remove(ultimoElemento);
        }else{
            return "";
        }
        return "." + String.join(".", atributo);
    }

    private String getAtributo(String pathCampo){
        return getAtributo(pathCampo, "\\.");
    }

    private String getAtributo(String pathCampo, String separador){
        String[] atributo = pathCampo.split(separador);

        return atributo[atributo.length-1];
    }

    private void updateMultipleFields(Object obj){

        List<String> todosAtributos = new ArrayList<>();
        AtributoUtils.obterTodosAtributos(obj, todosAtributos, true);
    }

    private void mapeametoComponenteToFirebase(){
//        componentActivity.put("onOff", binding.swOnOffChurrasqueiraGeral.getId());
//        componentActivity.put("parametros.motor.direcao", binding.swMDirecaoChurrasqueira.getId());
//        componentActivity.put("parametros.motor.habilitado", binding.swMHabilitadoChurrasqueira.getId());
//        componentActivity.put("parametros.motor.onOff", binding.swMOnOffChurrasqueira.getId());
//        componentActivity.put("parametros.output.agua", binding.swOAguaChurrasqueira.getId());
//        componentActivity.put("parametros.output.exaustor", binding.swOExauChurrasqueira.getId());
//        componentActivity.put("parametros.output.lampada", binding.swOLampadaChurrasqueira.getId());
//        componentActivity.put("parametros.output.soprador", binding.swOSopradorChurrasqueira.getId());
//        componentActivity.put("parametros.sensor.fc1", binding.swSFc1Churrasqueira.getId());
//        componentActivity.put("parametros.sensor.fc2", binding.swSFc2Churrasqueira.getId());
//        componentActivity.put("update", binding.tvDFBUpdate.getId());

        componentActivity.put(binding.swOnOffChurrasqueiraGeral.getId(), "onOff");
        componentActivity.put(binding.swMDirecaoChurrasqueira.getId(), "parametros.motor.direcao");
        componentActivity.put(binding.swMHabilitadoChurrasqueira.getId(), "parametros.motor.habilitado");
        componentActivity.put(binding.swMOnOffChurrasqueira.getId(), "parametros.motor.onOff");
        componentActivity.put(binding.swOAguaChurrasqueira.getId(), "parametros.output.agua");
        componentActivity.put(binding.swOExauChurrasqueira.getId(), "parametros.output.exaustor");
        componentActivity.put(binding.swOLampadaChurrasqueira.getId(), "parametros.output.lampada");
        componentActivity.put(binding.swOSopradorChurrasqueira.getId(), "parametros.output.soprador");
        componentActivity.put(binding.swSFc1Churrasqueira.getId(), "parametros.sensor.fc1");
        componentActivity.put(binding.swSFc2Churrasqueira.getId(), "parametros.sensor.fc2");
        componentActivity.put(binding.tvDFBUpdate.getId(), "update");
    }

    private void ouvinteFirebase(){
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                DebuggerUtils.log("DadosFirebaseActivity", "onDataChange");

                boolean valueOnOff = dataSnapshot.child("onOff").getValue(boolean.class);
                try {
                    Churrasqueira churrasqueiraFirebase = JsonUtils.fromJson(Churrasqueira.class, new Gson().toJson(dataSnapshot.getValue()));

                    DebuggerUtils.printObj(churrasqueiraFirebase);

                    List<String> atributosAlterados = new ArrayList<>();

                    AtributoUtils.atributosAlterados(churrasqueiraFirebase, churrasqueira, atributosAlterados);

                    AtributoUtils.transferirValoresEntreObjetos(churrasqueiraFirebase, churrasqueira, atributosAlterados);

                    updateMultipleFields(churrasqueira, atributosAlterados);

                    StringBuilder sb = new StringBuilder();

                    sb.append("\n------------------------------------------");
                    sb.append(new Gson().toJson(dataSnapshot.getValue()));
                    sb.append("\n\n----------------- churrasqueira.toString() -------------------------\n");
                    sb.append(churrasqueira.toString());
                    sb.append("\n\n----------------- atributosAlterados -------------------------\n");
                    sb.append(atributosAlterados);
                    sb.append("\n\n------------------------------------------\n");

                    DebuggerUtils.log("DadosFirebaseActivity", sb.toString());

                    binding.tvDFBInfo.setText(sb.toString());

                    ComponentUtils.atualizaComponents(churrasqueira, atributosAlterados, componentActivity, binding);
                } catch (Exception e) {
                    Toast.makeText(DadosFirebaseActivity.this, "Erro ao adicionar o json ao textview: " + e, Toast.LENGTH_SHORT).show();
                }
                binding.swOnOffChurrasqueiraGeral.setChecked(valueOnOff);
                Toast.makeText(DadosFirebaseActivity.this, "value firebase: "+(valueOnOff ? "true" : "false"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Toast.makeText(DadosFirebaseActivity.this, "Erro Firebase update", Toast.LENGTH_SHORT).show();
                Log.w("Erro leitura", "loadPost:onCancelled", databaseError.toException());
            }
        };
        mDatabase.addValueEventListener(postListener);
    }
}