package com.home.apphomemanager_v5;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.home.apphomemanager_v5.databinding.ActivityCisternaBinding;
import com.home.apphomemanager_v5.model.churrasqueira.Churrasqueira;
import com.home.apphomemanager_v5.model.firebase.FirebaseEntity;
import com.home.apphomemanager_v5.model.reservatorio.Cisterna;
import com.home.apphomemanager_v5.util.AtributoUtils;
import com.home.apphomemanager_v5.util.ComponentUtils;
import com.home.apphomemanager_v5.util.FirebaseUtils;
import com.home.apphomemanager_v5.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CisternaActivity extends AppCompatActivity {

    private ActivityCisternaBinding binding;

    private FirebaseEntity firebaseEntity;

    private Cisterna cisterna;

    private Map<Integer, String> componentsActivity = new HashMap<>();

    private static final String PATH_ROOT_FIREBASE = "cisterna";
    private static final String ACTIVITY_NAME = "Cisterna";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseEntity = new FirebaseEntity();
        firebaseEntity.FirebaseInicialize(PATH_ROOT_FIREBASE);

        cisterna = new Cisterna();
        cisterna.inicializa();

        binding = ActivityCisternaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapeametoComponenteToFirebase();

        listenerFirebase();


        ComponentUtils.inicializaElementos(binding);

        ComponentUtils.setImageViewToggleListener(binding.btCisOnOffMain, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisAutoManual, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisValvulaEntrada, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisValvulaControle, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisBomba, componentsActivity, cisterna);

        binding.tvCisMain.setText(ACTIVITY_NAME);
        binding.tvCisStatus.setText(R.string.online);
        binding.tvCisAutoManual.setText(R.string.autoManual);
        binding.tvCisValveEntrada.setText(R.string.valveEntrada);
        binding.tvCisValveControle.setText(R.string.valveControle);
        binding.tvCisValveBomba.setText(R.string.bomba);
        binding.tvCisCx1.setText(R.string.caixa1);
        binding.tvCisCx2.setText(R.string.caixa2);
        binding.tvCisCx3.setText(R.string.caixa3);
    }

    private void mapeametoComponenteToFirebase(){
        componentsActivity.put(binding.btCisOnOffMain.getId(), "onOff");
        componentsActivity.put(binding.swCisAutoManual.getId(), "autoManual");
        componentsActivity.put(binding.swCisValvulaEntrada.getId(), "vle");
        componentsActivity.put(binding.swCisValvulaControle.getId(), "vlc");
        componentsActivity.put(binding.swCisBomba.getId(), "pump");


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
                    Cisterna cisternaFirebase = JsonUtils.fromJson(Cisterna.class, new Gson().toJson(dataSnapshot.getValue()));

                    List<String> atributosAlterados = new ArrayList<>();

                    AtributoUtils.atributosAlterados(cisternaFirebase, cisterna, atributosAlterados);

                    AtributoUtils.transferirValoresEntreObjetos(cisternaFirebase, cisterna, atributosAlterados);

                    FirebaseUtils.updateMultipleFields(cisterna, atributosAlterados);

                    ComponentUtils.atualizaComponents(cisterna, atributosAlterados, componentsActivity, binding);
                } catch (Exception e) {
                    Toast.makeText(CisternaActivity.this, "Erro ao processar os dados: " + e, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(CisternaActivity.this, "Erro ao receber os dados: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.w("Err"+ACTIVITY_NAME, "Erro ao receber os dados", databaseError.toException());
            }
        };
        firebaseEntity.getmDatabase().addValueEventListener(postListener);
    }
}