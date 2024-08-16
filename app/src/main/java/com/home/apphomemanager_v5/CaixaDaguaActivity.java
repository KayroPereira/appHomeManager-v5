package com.home.apphomemanager_v5;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.home.apphomemanager_v5.databinding.ActivityCaixaDaguaBinding;
import com.home.apphomemanager_v5.databinding.ActivityCisternaBinding;
import com.home.apphomemanager_v5.model.firebase.FirebaseEntity;
import com.home.apphomemanager_v5.model.reservatorio.CaixaDagua;
import com.home.apphomemanager_v5.model.reservatorio.Cisterna;
import com.home.apphomemanager_v5.util.AtributoUtils;
import com.home.apphomemanager_v5.util.ComponentUtils;
import com.home.apphomemanager_v5.util.FirebaseUtils;
import com.home.apphomemanager_v5.util.JsonUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaixaDaguaActivity extends AppCompatActivity {

    private ActivityCaixaDaguaBinding binding;

    private FirebaseEntity firebaseEntity;

    private CaixaDagua caixaDagua;

    private Map<Integer, String> componentsActivity = new HashMap<>();

    private Boolean wasFirstUpdate = true;

    private Handler handler;
    private Runnable runnable;


    private String PATH_ROOT_CISTERNA_FIREBASE = "cisterna";
    private String PATH_ROOT_FIREBASE = "cx";
    private String ACTIVITY_NAME = "Caixa D'água - ";

    private static final int QUANTIDADE_IMAGENS_CISTENA = 20;

    private static final int DELAY_2_MINUTO_MS = 1000 * 60 * 2;
    private static final int PERIODO_2_MINUTO_S = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String path = intent.getStringExtra("path");

        PATH_ROOT_FIREBASE += path;
        ACTIVITY_NAME += path;

        firebaseEntity = new FirebaseEntity();
        firebaseEntity.FirebaseInicialize(PATH_ROOT_FIREBASE);

        caixaDagua = new CaixaDagua();
        caixaDagua.inicializa();

        binding = ActivityCaixaDaguaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapeametoComponenteToFirebase();

        listenerFirebase();

        ComponentUtils.inicializaElementos(binding);

        ComponentUtils.setImageViewToggleListener(binding.btCxdOnOffMain, componentsActivity, caixaDagua, PATH_ROOT_FIREBASE);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCxdAutoManual, componentsActivity, caixaDagua, PATH_ROOT_FIREBASE);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCxdValvulaEntradaPrincipal, componentsActivity, caixaDagua, PATH_ROOT_FIREBASE);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCxdValvulaEntradaSecundaria, componentsActivity, caixaDagua, PATH_ROOT_FIREBASE);

        setParametrosDefault();

        inicializaSchedulerStatusDispositivo();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        handler.removeCallbacks(runnable);
    }

    private void inicializaSchedulerStatusDispositivo() {

        handler = new Handler(Looper.myLooper());

        runnable = new Runnable() {
            @Override
            public void run() {

                verificaStatusDispositivo();

                handler.postDelayed(this, DELAY_2_MINUTO_MS);
            }
        };

        handler.post(runnable);
    }

    private void verificaStatusDispositivo() {

        Instant dataAtual = Instant.now();

        binding.tvCxdStatus.setText(dataAtual.getEpochSecond() - caixaDagua.getStatus() > PERIODO_2_MINUTO_S ? R.string.offline : R.string.online);
        binding.tvCxdStatus.setTextColor(getString(R.string.online).equals(binding.tvCxdStatus.getText()) ? getColor(R.color.onLine) : getColor(R.color.offLine));
    }

    private void setParametrosDefault() {

        binding.tvCxdMain.setText(ACTIVITY_NAME);
        binding.tvCxdStatus.setText(R.string.online);
        binding.tvCxdAutoManual.setText(R.string.autoManual);
        binding.tvCxdValveEntradaPrincipal.setText(R.string.valveEntradaPrincipal);
        binding.tvCxdValveEntradaSecundaria.setText(R.string.valveEntradaSecundaria);
    }

    private void mapeametoComponenteToFirebase(){

        componentsActivity.put(binding.btCxdOnOffMain.getId(), "onOff");
        componentsActivity.put(binding.swCxdAutoManual.getId(), "autoManual");
        componentsActivity.put(binding.swCxdValvulaEntradaPrincipal.getId(), "vlep");
        componentsActivity.put(binding.swCxdValvulaEntradaSecundaria.getId(), "vles");
    }

    private void listenerFirebase(){

        ValueEventListener postListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                try {
                    CaixaDagua caixaDaguaFirebase = JsonUtils.fromJson(CaixaDagua.class, new Gson().toJson(dataSnapshot.getValue()));

                    List<String> atributosAlterados = new ArrayList<>();

                    if(wasFirstUpdate){

                        wasFirstUpdate = false;
                        atributosAlterados.clear();
                        AtributoUtils.obterTodosAtributos(caixaDagua, atributosAlterados, false);
                    }else{
                        AtributoUtils.atributosAlterados(caixaDaguaFirebase, caixaDagua, atributosAlterados);
                    }

                    AtributoUtils.transferirValoresEntreObjetos(caixaDaguaFirebase, caixaDagua, atributosAlterados);

                    FirebaseUtils.updateMultipleFields(caixaDagua, atributosAlterados, PATH_ROOT_FIREBASE);

                    ComponentUtils.atualizaComponents(caixaDagua, atributosAlterados, componentsActivity, binding);

                    controleComponentes(atributosAlterados);

                } catch (Exception e) {
                    Toast.makeText(CaixaDaguaActivity.this, "Erro ao processar os dados: " + e, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(CaixaDaguaActivity.this, "Erro ao receber os dados: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.w("Err"+ACTIVITY_NAME, "Erro ao receber os dados", databaseError.toException());
            }
        };
        firebaseEntity.getmDatabase().addValueEventListener(postListener);
    }

    private void controleComponentes(List<String> atributosAlterados){

        for (String att : atributosAlterados) {

            switch (att){
                case "onOff":

                    binding.swCxdAutoManual.setEnabled(caixaDagua.getOnOff());
                case "autoManual":

                    controleEquipamentosAutoManual();
                    break;

                case "na":
                case "ni":
                case "ns":

                    @SuppressLint("DiscouragedApi") int resourceIdImagem = getResources().getIdentifier("wt" + caixaDagua.getImageLevel(QUANTIDADE_IMAGENS_CISTENA), "drawable", getPackageName());
                    binding.ivCxdReservatorio.setImageResource(resourceIdImagem != 0 ? resourceIdImagem : R.drawable.wt0);
                    break;

                case "status":
                    verificaStatusDispositivo();
                    break;
            }
        }
    }

    private void controleEquipamentosAutoManual(){

        Boolean status = caixaDagua.getOnOff() && !caixaDagua.getAutoManual();

        binding.swCxdValvulaEntradaPrincipal.setEnabled(status);
        binding.swCxdValvulaEntradaSecundaria.setEnabled(status);
    }
}