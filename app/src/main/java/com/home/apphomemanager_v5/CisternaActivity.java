package com.home.apphomemanager_v5;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

    private final Map<Integer, String> componentsActivity = new HashMap<>();

    private boolean wasFirstUpdate = true;

    private boolean online = false;

    private final StatusDispositivo statusDispositivo = new StatusDispositivo();


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

        ComponentUtils.inicializaElementos(binding);

        ComponentUtils.setImageViewToggleListener(binding.ivCisOnOffMain, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisAutoManual, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisValvulaEntrada, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisValvulaControle, componentsActivity, cisterna);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCisBomba, componentsActivity, cisterna);

        setParametrosDefault();

        // Até o primeiro dado chegar o dispositivo é tratado como off-line.
        atualizaHabilitacaoComponentes();

        listenerFirebase();
    }

    @Override
    protected void onStart() {

        super.onStart();

        statusDispositivo.inicializaSchedulerStatusDispositivo(this::verificaStatusDispositivo, AppConstants.DELAY_VERIFICACAO_STATUS_MS);
    }

    @Override
    protected void onStop() {

        super.onStop();

        statusDispositivo.paraSchedulerStatusDispositivo();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        statusDispositivo.paraSchedulerStatusDispositivo();

        firebaseEntity.disconnect();
    }

    private void verificaStatusDispositivo() {

        online = statusDispositivo.isOnline(cisterna.getStatus(), AppConstants.PERIODO_2_MINUTO_S);

        binding.tvCisStatus.setText(online ? R.string.online : R.string.offline);
        binding.tvCisStatus.setTextColor(getColor(online ? R.color.onLine : R.color.offLine));

        atualizaHabilitacaoComponentes();
    }

    private void setParametrosDefault() {

        binding.tvCisMain.setText(ACTIVITY_NAME);
        binding.tvCisStatus.setText(R.string.offline);
        binding.tvCisStatus.setTextColor(getColor(R.color.offLine));
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

        // Executa uma única vez, quando a SeekBar já tem largura para posicionar o rótulo do nível.
        binding.skbCisNivel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.skbCisNivel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                defineNivelSuperior();
                ajustaPosicaoNivelAtual();
            }
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
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.getValue() == null) {
                    Log.w("Err" + ACTIVITY_NAME, "Nó '" + PATH_ROOT_FIREBASE + "' inexistente no Firebase");
                    return;
                }

                try {
                    Cisterna cisternaFirebase = JsonUtils.fromJson(Cisterna.class, new Gson().toJson(dataSnapshot.getValue()));

                    List<String> atributosAlterados = new ArrayList<>();

                    if(wasFirstUpdate){

                        wasFirstUpdate = false;
                        AtributoUtils.obterTodosAtributos(cisterna, atributosAlterados, false);

                        // Só publica os valores padrão dos campos que ainda não existem no Firebase;
                        // reenviar o que acabou de ser lido poderia sobrescrever uma leitura mais nova do dispositivo.
                        List<String> camposAusentes = AtributoUtils.camposAusentes(cisternaFirebase, atributosAlterados);
                        FirebaseUtils.updateMultipleFields(cisterna, camposAusentes, PATH_ROOT_FIREBASE);
                    }else{
                        AtributoUtils.atributosAlterados(cisternaFirebase, cisterna, atributosAlterados);
                    }

                    AtributoUtils.transferirValoresEntreObjetos(cisternaFirebase, cisterna, atributosAlterados);

                    ComponentUtils.atualizaComponents(cisterna, atributosAlterados, componentsActivity, binding);

                    controleComponentes(atributosAlterados);

                } catch (Exception e) {
                    Log.e("Err" + ACTIVITY_NAME, "Erro ao processar os dados", e);
                    Toast.makeText(CisternaActivity.this, "Erro ao processar os dados: " + e, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CisternaActivity.this, "Erro ao receber os dados: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.w("Err"+ACTIVITY_NAME, "Erro ao receber os dados", databaseError.toException());
            }
        };
        firebaseEntity.addValueEventListener(postListener);
    }

    private void controleComponentes(List<String> atributosAlterados){

        boolean atualizaHabilitacao = false;
        boolean atualizaNivel = false;
        boolean atualizaNivelSuperior = false;
        boolean atualizaCaixas = false;
        boolean atualizaStatus = false;
        boolean atualizaFluxo = false;

        for (String att : atributosAlterados) {

            switch (att){
                case "onOff":
                case "autoManual":
                    atualizaHabilitacao = true;
                    break;

                case "nsc":
                case "nic":
                    atualizaNivelSuperior = true;
                    atualizaNivel = true;
                    break;

                case "na":
                case "ni":
                case "ns":
                    atualizaNivel = true;
                    break;

                case "cx1":
                case "cx2":
                case "cx3":
                    atualizaCaixas = true;
                    break;

                case "vle":
                case "pump":
                    atualizaFluxo = true;
                    break;

                case "status":
                    atualizaStatus = true;
                    break;
            }
        }

        if (atualizaNivelSuperior) {
            defineNivelSuperior();
        }

        if (atualizaNivel) {
            ajustaPosicaoNivelAtual();

            binding.ivCisReservatorio.setNivel(cisterna.calculaFracaoNivel());
        }

        if (atualizaFluxo) {
            atualizaFluxoAgua();
        }

        if (atualizaCaixas) {
            ComponentUtils.changeValueComponent(binding.ivCisCx1, cisterna.getCx1());
            ComponentUtils.changeValueComponent(binding.ivCisCx2, cisterna.getCx2());
            ComponentUtils.changeValueComponent(binding.ivCisCx3, cisterna.getCx3());
        }

        // verificaStatusDispositivo já reaplica a habilitação dos componentes.
        if (atualizaStatus) {
            verificaStatusDispositivo();
        } else if (atualizaHabilitacao) {
            atualizaHabilitacaoComponentes();
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
        // O rótulo está ancorado ao início da SeekBar: centraliza sobre o thumb.
        return px - binding.skbCisNivel.getThumbOffset() + binding.skbCisNivel.getPaddingLeft() - binding.tvCisNivelAtual.getWidth() / 2;
    }

    /**
     * Regra única de habilitação:
     * - off-line: tudo desabilitado (e em tons de cinza);
     * - on-line e desligado: só o botão liga/desliga;
     * - on-line e ligado: modo auto/manual e válvula de controle;
     * - on-line, ligado e manual: também válvula de entrada e bomba.
     */
    private void atualizaHabilitacaoComponentes(){

        boolean ligado = online && Boolean.TRUE.equals(cisterna.getOnOff());
        boolean manual = ligado && !Boolean.TRUE.equals(cisterna.getAutoManual());

        ComponentUtils.setComponentEnabled(binding, binding.ivCisOnOffMain.getId(), online);
        ComponentUtils.setComponentEnabled(binding, binding.ivCisReservatorio.getId(), online);

        binding.swCisAutoManual.setEnabled(ligado);
        binding.swCisValvulaControle.setEnabled(ligado);

        binding.swCisValvulaEntrada.setEnabled(manual);
        binding.swCisBomba.setEnabled(manual);

        atualizaFluxoAgua();
    }

    /** Bolhas na entrada de água e gotas na saída (bomba) só com o dispositivo on-line e ligado. */
    private void atualizaFluxoAgua() {

        boolean ligado = online && Boolean.TRUE.equals(cisterna.getOnOff());

        binding.ivCisReservatorio.setEnchendo(ligado && Boolean.TRUE.equals(cisterna.getVle()));
        binding.ivCisReservatorio.setEsvaziando(ligado && Boolean.TRUE.equals(cisterna.getPump()));
    }

    private void voltar(Object event){

        finish();
    }
}
