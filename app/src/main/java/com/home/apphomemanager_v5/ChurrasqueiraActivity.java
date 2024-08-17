package com.home.apphomemanager_v5;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.home.apphomemanager_v5.databinding.ActivityChurrasqueiraBinding;
import com.home.apphomemanager_v5.model.churrasqueira.Churrasqueira;
import com.home.apphomemanager_v5.model.firebase.FirebaseEntity;
import com.home.apphomemanager_v5.util.AtributoUtils;
import com.home.apphomemanager_v5.util.ComponentUtils;
import com.home.apphomemanager_v5.util.FirebaseUtils;
import com.home.apphomemanager_v5.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChurrasqueiraActivity extends AppCompatActivity {

    private ActivityChurrasqueiraBinding binding;

    private FirebaseEntity firebaseEntity;

    private Churrasqueira churrasqueira;

    private Map<Integer, String> componentsActivity = new HashMap<>();

    private static final String PATH_ROOT_FIREBASE = "churrasqueira";
    private static final String ACTIVITY_NAME = "Churrasqueira";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseEntity = new FirebaseEntity();
        firebaseEntity.FirebaseInicialize(PATH_ROOT_FIREBASE);

        churrasqueira = new Churrasqueira();
        churrasqueira.inicializa();

        binding = ActivityChurrasqueiraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        mapeametoComponenteToFirebase();

        listenerFirebase();

        binding.tvChMain.setText(ACTIVITY_NAME);

        ComponentUtils.inicializaElementos(binding);
        ComponentUtils.setImageViewToggleListener(binding.ivChOnOffMain, componentsActivity, churrasqueira);
    }

    private void mapeametoComponenteToFirebase(){
        componentsActivity.put(binding.ivChOnOffMain.getId(), "onOff");
//        componentsActivity.put(binding.swMDirecaoChurrasqueira.getId(), "parametros.motor.direcao");
//        componentsActivity.put(binding.swMHabilitadoChurrasqueira.getId(), "parametros.motor.habilitado");
//        componentsActivity.put(binding.swMOnOffChurrasqueira.getId(), "parametros.motor.onOff");
//        componentsActivity.put(binding.swOAguaChurrasqueira.getId(), "parametros.output.agua");
//        componentsActivity.put(binding.swOExauChurrasqueira.getId(), "parametros.output.exaustor");
//        componentsActivity.put(binding.swOLampadaChurrasqueira.getId(), "parametros.output.lampada");
//        componentsActivity.put(binding.swOSopradorChurrasqueira.getId(), "parametros.output.soprador");
//        componentsActivity.put(binding.swSFc1Churrasqueira.getId(), "parametros.sensor.fc1");
//        componentsActivity.put(binding.swSFc2Churrasqueira.getId(), "parametros.sensor.fc2");
//        componentsActivity.put(binding.tvDFBUpdate.getId(), "update");
    }

    private void listenerFirebase(){
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                try {
                    Churrasqueira churrasqueiraFirebase = JsonUtils.fromJson(Churrasqueira.class, new Gson().toJson(dataSnapshot.getValue()));

                    List<String> atributosAlterados = new ArrayList<>();

                    AtributoUtils.atributosAlterados(churrasqueiraFirebase, churrasqueira, atributosAlterados);

                    AtributoUtils.transferirValoresEntreObjetos(churrasqueiraFirebase, churrasqueira, atributosAlterados);

                    FirebaseUtils.updateMultipleFields(churrasqueira, atributosAlterados, PATH_ROOT_FIREBASE);

                    ComponentUtils.atualizaComponents(churrasqueira, atributosAlterados, componentsActivity, binding);
                } catch (Exception e) {
                    Toast.makeText(ChurrasqueiraActivity.this, "Erro ao processar os dados: " + e, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChurrasqueiraActivity.this, "Erro ao receber os dados: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.w("Err"+ACTIVITY_NAME, "Erro ao receber os dados", databaseError.toException());
            }
        };
        firebaseEntity.getmDatabase().addValueEventListener(postListener);
    }
}