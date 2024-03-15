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
import com.home.apphomemanager_v4.util.FirebaseUtils;
import com.home.apphomemanager_v4.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Pair<String, String> parametros = AtributoUtils.getParamentrosField(churrasqueira, path);

            FirebaseUtils.sendSimpleData(parametros.first, parametros.second, b);
        });
    }

    private void mapeametoComponenteToFirebase(){
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

                try {
                    Churrasqueira churrasqueiraFirebase = JsonUtils.fromJson(Churrasqueira.class, new Gson().toJson(dataSnapshot.getValue()));

                    List<String> atributosAlterados = new ArrayList<>();

                    AtributoUtils.atributosAlterados(churrasqueiraFirebase, churrasqueira, atributosAlterados);

                    AtributoUtils.transferirValoresEntreObjetos(churrasqueiraFirebase, churrasqueira, atributosAlterados);

                    FirebaseUtils.updateMultipleFields(churrasqueira, atributosAlterados);

                    StringBuilder sb = new StringBuilder();

                    sb.append("\n------------------------------------------");
                    sb.append(new Gson().toJson(dataSnapshot.getValue()));
                    sb.append("\n\n----------------- churrasqueira.toString() -------------------------\n");
                    sb.append(churrasqueira.toString());
                    sb.append("\n\n----------------- atributosAlterados -------------------------\n");
                    sb.append(atributosAlterados);
                    sb.append("\n\n------------------------------------------\n");

                    binding.tvDFBInfo.setText(sb.toString());

                    ComponentUtils.atualizaComponents(churrasqueira, atributosAlterados, componentActivity, binding);
                } catch (Exception e) {
                    Toast.makeText(DadosFirebaseActivity.this, "Erro ao adicionar o json ao textview: " + e, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(DadosFirebaseActivity.this, "Erro Firebase update", Toast.LENGTH_SHORT).show();
                Log.w("Erro leitura", "loadPost:onCancelled", databaseError.toException());
            }
        };
        mDatabase.addValueEventListener(postListener);
    }
}