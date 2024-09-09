package com.home.apphomemanager_v5;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.home.apphomemanager_v5.commons.AppConstants;
import com.home.apphomemanager_v5.commons.StatusDispositivo;
import com.home.apphomemanager_v5.databinding.ActivityCisternaBinding;
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

    private Boolean wasFirstUpdate = true;

    private StatusDispositivo statusDispositivo = new StatusDispositivo();


    private static final String PATH_ROOT_FIREBASE = "cisterna";
    private static final String ACTIVITY_NAME = "Cisterna";

    private static final int QUANTIDADE_IMAGENS_CISTENA = 20;



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

        ComponentUtils.setImageViewToggleListener(binding.ivCisOnOffMain, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisAutoManual, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisValvulaEntrada, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisValvulaControle, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisBomba, componentsActivity, cisterna);

        setParametrosDefault();

        statusDispositivo.inicializaSchedulerStatusDispositivo(this::verificaStatusDispositivo, AppConstants.DELAY_2_MINUTO_MS);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        statusDispositivo.paraSchedulerStatusDispositivo();

        firebaseEntity.disconnect();
    }

    private void verificaStatusDispositivo() {

        boolean status = statusDispositivo.isOnline(cisterna.getStatus(), AppConstants.PERIODO_2_MINUTO_S);

        binding.tvCisStatus.setText(status ? R.string.online : R.string.offline);
        binding.tvCisStatus.setTextColor(getString(R.string.online).equals(binding.tvCisStatus.getText()) ? getColor(R.color.onLine) : getColor(R.color.offLine));

        ComponentUtils.setComponentEnabledAll(binding, componentsActivity, status);
        ComponentUtils.setComponentEnabled(binding, binding.ivCisReservatorio.getId(), status);
    }

    private void setParametrosDefault() {

        binding.tvCisMain.setText(ACTIVITY_NAME);
        binding.tvCisStatus.setText(R.string.online);
        binding.tvCisAutoManual.setText(R.string.autoManual);
        binding.tvCisValveEntrada.setText(R.string.valveEntrada);
        binding.tvCisValveControle.setText(R.string.valveControle);
        binding.tvCisValveBomba.setText(R.string.bomba);
        binding.tvCisCx1.setText(R.string.caixa1);
        binding.tvCisCx2.setText(R.string.caixa2);
        binding.tvCisCx3.setText(R.string.caixa3);
        binding.tvCisNivelInferior.setText(R.string.zeroNum);

        ComponentUtils.setEventClickGeneric(binding.ivCisBackMain, this::voltar);

        ComponentUtils.changeValueComponent(binding.ivCisCx1, false);
        ComponentUtils.changeValueComponent(binding.ivCisCx2, false);
        ComponentUtils.changeValueComponent(binding.ivCisCx3, false);

        binding.skbCisNivel.setEnabled(false);

        binding.skbCisNivel.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            defineNivelSuperior();
            ajustaPosicaoNivelAtual();
        });
    }

    private void mapeametoComponenteToFirebase(){

        componentsActivity.put(binding.ivCisOnOffMain.getId(), "onOff");
        componentsActivity.put(binding.swCisAutoManual.getId(), "autoManual");
        componentsActivity.put(binding.swCisValvulaEntrada.getId(), "vle");
        componentsActivity.put(binding.swCisValvulaControle.getId(), "vlc");
        componentsActivity.put(binding.swCisBomba.getId(), "pump");
    }

    private void listenerFirebase(){

        ValueEventListener postListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                try {
                    Cisterna cisternaFirebase = JsonUtils.fromJson(Cisterna.class, new Gson().toJson(dataSnapshot.getValue()));

                    List<String> atributosAlterados = new ArrayList<>();

                    if(wasFirstUpdate){

                        wasFirstUpdate = false;
                        atributosAlterados.clear();
                        AtributoUtils.obterTodosAtributos(cisterna, atributosAlterados, false);
                    }else{
                        AtributoUtils.atributosAlterados(cisternaFirebase, cisterna, atributosAlterados);
                    }

                    AtributoUtils.transferirValoresEntreObjetos(cisternaFirebase, cisterna, atributosAlterados);

                    FirebaseUtils.updateMultipleFields(cisterna, atributosAlterados, PATH_ROOT_FIREBASE);

                    ComponentUtils.atualizaComponents(cisterna, atributosAlterados, componentsActivity, binding);

                    controleComponentes(atributosAlterados);

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

    private void controleComponentes(List<String> atributosAlterados){

        for (String att : atributosAlterados) {

            switch (att){
                case "onOff":

                    binding.swCisAutoManual.setEnabled(cisterna.getOnOff());
                    binding.swCisValvulaControle.setEnabled(cisterna.getOnOff());
                case "autoManual":

                    controleEquipamentosAutoManual();
                    break;

                case "nsc":
                case "nic":
                    defineNivelSuperior();
                case "na":
                case "ni":
                case "ns":

                    ajustaPosicaoNivelAtual();

                    @SuppressLint("DiscouragedApi") int resourceIdImagem = getResources().getIdentifier("ct" + cisterna.getImageLevel(QUANTIDADE_IMAGENS_CISTENA), "drawable", getPackageName());
                    binding.ivCisReservatorio.setImageResource(resourceIdImagem != 0 ? resourceIdImagem : R.drawable.ct0);
                    break;

                case "cx1":
                case "cx2":
                case "cx3":

                    ComponentUtils.changeValueComponent(binding.ivCisCx1, cisterna.getCx1());
                    ComponentUtils.changeValueComponent(binding.ivCisCx2, cisterna.getCx2());
                    ComponentUtils.changeValueComponent(binding.ivCisCx3, cisterna.getCx3());
                    break;

                case "status":
                    verificaStatusDispositivo();
                    break;
            }
        }
    }

    private void defineNivelSuperior() {

        binding.skbCisNivel.setMax(cisterna.getNivelSuperiorRelativo());
        binding.tvCisNivelSuperior.setText(String.valueOf(cisterna.getNivelSuperiorRelativo()));
    }

    private void ajustaPosicaoNivelAtual() {

        binding.skbCisNivel.setProgress(cisterna.getNivelAtualRelativo());
        binding.tvCisNivelAtual.setText(String.valueOf(cisterna.getNivelAtualRelativo()));
        binding.tvCisNivelAtual.setTranslationX(getPosicaoNivelAtualX());
    }

    private int getPosicaoNivelAtualX(){

        int px = binding.skbCisNivel.getThumb().getBounds().centerX();
        int seekBarInicioX = binding.skbCisNivel.getLeft();

        return seekBarInicioX + px;
    }

    private void controleEquipamentosAutoManual(){

        Boolean status = cisterna.getOnOff() && !cisterna.getAutoManual();

        binding.swCisValvulaEntrada.setEnabled(status);
        binding.swCisBomba.setEnabled(status);
    }

    private void voltar(Object event){

        finish();
    }
}