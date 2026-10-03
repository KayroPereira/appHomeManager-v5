package com.home.apphomemanager_v5;

import android.content.Intent;
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
import com.home.apphomemanager_v5.databinding.ActivityCaixaDaguaBinding;
import com.home.apphomemanager_v5.model.firebase.FirebaseEntity;
import com.home.apphomemanager_v5.model.reservatorio.CaixaDagua;
import com.home.apphomemanager_v5.util.AtributoUtils;
import com.home.apphomemanager_v5.util.ComponentUtils;
import com.home.apphomemanager_v5.util.FirebaseUtils;
import com.home.apphomemanager_v5.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaixaDaguaActivity extends AppCompatActivity {

    public static final String EXTRA_PATH = "path";

    private static final String PATH_PADRAO = "1";

    private ActivityCaixaDaguaBinding binding;

    private FirebaseEntity firebaseEntity;

    private CaixaDagua caixaDagua;

    private final Map<Integer, String> componentsActivity = new HashMap<>();

    private boolean wasFirstUpdate = true;

    private boolean online = false;

    private final StatusDispositivo statusDispositivo = new StatusDispositivo();

    private String pathRootFirebase;
    private String activityName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String path = intent.getStringExtra(EXTRA_PATH);

        if (path == null || path.trim().isEmpty()) {
            path = PATH_PADRAO;
        }

        pathRootFirebase = "cx" + path;
        activityName = "Caixa D'água - " + path;

        firebaseEntity = new FirebaseEntity();
        firebaseEntity.FirebaseInicialize(pathRootFirebase);

        caixaDagua = new CaixaDagua();
        caixaDagua.inicializa();

        binding = ActivityCaixaDaguaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapeametoComponenteToFirebase();

        ComponentUtils.inicializaElementos(binding);

        ComponentUtils.setEventClickGeneric(binding.ivCxdBackMain, this::voltar);

        ComponentUtils.setImageViewToggleListener(binding.ivCxdOnOffMain, componentsActivity, caixaDagua, pathRootFirebase);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCxdAutoManual, componentsActivity, caixaDagua, pathRootFirebase);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCxdValvulaEntradaPrincipal, componentsActivity, caixaDagua, pathRootFirebase);
        ComponentUtils.setSwitchCheckedChangeListener(binding.swCxdValvulaEntradaSecundaria, componentsActivity, caixaDagua, pathRootFirebase);

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

        online = statusDispositivo.isOnline(caixaDagua.getStatus(), AppConstants.PERIODO_2_MINUTO_S);

        binding.tvCxdStatus.setText(online ? R.string.online : R.string.offline);
        binding.tvCxdStatus.setTextColor(getColor(online ? R.color.onLine : R.color.offLine));

        atualizaHabilitacaoComponentes();
    }

    private void setParametrosDefault() {

        binding.tvCxdMain.setText(activityName);
        binding.tvCxdStatus.setText(R.string.offline);
        binding.tvCxdStatus.setTextColor(getColor(R.color.offLine));
        binding.tvCxdAutoManual.setText(R.string.autoManual);
        binding.tvCxdValveEntradaPrincipal.setText(R.string.valveEntradaPrincipal);
        binding.tvCxdValveEntradaSecundaria.setText(R.string.valveEntradaSecundaria);
        binding.tvCxdNivelInferior.setText(R.string.zeroNum);

        binding.skbCxdNivel.setEnabled(false);

        // Executa uma única vez, quando a SeekBar já tem largura para posicionar o rótulo do nível.
        binding.skbCxdNivel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.skbCxdNivel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                defineNivelSuperior();
                ajustaPosicaoNivelAtual();
            }
        });
    }

    private void mapeametoComponenteToFirebase(){

        componentsActivity.put(binding.ivCxdOnOffMain.getId(), "onOff");
        componentsActivity.put(binding.swCxdAutoManual.getId(), "autoManual");
        componentsActivity.put(binding.swCxdValvulaEntradaPrincipal.getId(), "vlep");
        componentsActivity.put(binding.swCxdValvulaEntradaSecundaria.getId(), "vles");
    }

    private void listenerFirebase(){

        ValueEventListener postListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.getValue() == null) {
                    Log.w("Err" + activityName, "Nó '" + pathRootFirebase + "' inexistente no Firebase");
                    return;
                }

                try {
                    CaixaDagua caixaDaguaFirebase = JsonUtils.fromJson(CaixaDagua.class, new Gson().toJson(dataSnapshot.getValue()));

                    List<String> atributosAlterados = new ArrayList<>();

                    if(wasFirstUpdate){

                        wasFirstUpdate = false;
                        AtributoUtils.obterTodosAtributos(caixaDagua, atributosAlterados, false);

                        // Só publica os valores padrão dos campos que ainda não existem no Firebase;
                        // reenviar o que acabou de ser lido poderia sobrescrever uma leitura mais nova do dispositivo.
                        List<String> camposAusentes = AtributoUtils.camposAusentes(caixaDaguaFirebase, atributosAlterados);
                        FirebaseUtils.updateMultipleFields(caixaDagua, camposAusentes, pathRootFirebase);
                    }else{
                        AtributoUtils.atributosAlterados(caixaDaguaFirebase, caixaDagua, atributosAlterados);
                    }

                    AtributoUtils.transferirValoresEntreObjetos(caixaDaguaFirebase, caixaDagua, atributosAlterados);

                    ComponentUtils.atualizaComponents(caixaDagua, atributosAlterados, componentsActivity, binding);

                    controleComponentes(atributosAlterados);

                } catch (Exception e) {
                    Log.e("Err" + activityName, "Erro ao processar os dados", e);
                    Toast.makeText(CaixaDaguaActivity.this, "Erro ao processar os dados: " + e, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CaixaDaguaActivity.this, "Erro ao receber os dados: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.w("Err"+activityName, "Erro ao receber os dados", databaseError.toException());
            }
        };
        firebaseEntity.addValueEventListener(postListener);
    }

    private void controleComponentes(List<String> atributosAlterados){

        boolean atualizaHabilitacao = false;
        boolean atualizaNivel = false;
        boolean atualizaNivelSuperior = false;
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

                case "vlep":
                case "vles":
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

            binding.ivCxdReservatorio.setNivel(caixaDagua.calculaFracaoNivel());
        }

        if (atualizaFluxo) {
            atualizaFluxoAgua();
        }

        // verificaStatusDispositivo já reaplica a habilitação dos componentes.
        if (atualizaStatus) {
            verificaStatusDispositivo();
        } else if (atualizaHabilitacao) {
            atualizaHabilitacaoComponentes();
        }
    }

    private void defineNivelSuperior() {

        binding.skbCxdNivel.setMax(caixaDagua.getNivelSuperiorRelativo());
        binding.tvCxdNivelSuperior.setText(String.valueOf(caixaDagua.getNivelSuperiorRelativo()));
    }

    private void ajustaPosicaoNivelAtual() {

        binding.skbCxdNivel.setProgress(caixaDagua.getNivelAtualRelativo());
        binding.tvCxdNivelAtual.setText(String.valueOf(caixaDagua.getNivelAtualRelativo()));
        binding.tvCxdNivelAtual.setTranslationX(getPosicaoNivelAtualX());
    }

    private int getPosicaoNivelAtualX(){

        int px = binding.skbCxdNivel.getThumb().getBounds().centerX();
        // O rótulo está ancorado ao início da SeekBar: centraliza sobre o thumb.
        return px - binding.skbCxdNivel.getThumbOffset() + binding.skbCxdNivel.getPaddingLeft() - binding.tvCxdNivelAtual.getWidth() / 2;
    }

    /**
     * Regra única de habilitação:
     * - off-line: tudo desabilitado (e em tons de cinza);
     * - on-line e desligado: só o botão liga/desliga;
     * - on-line e ligado: modo auto/manual;
     * - on-line, ligado e manual: também as válvulas de entrada.
     */
    private void atualizaHabilitacaoComponentes(){

        boolean ligado = online && Boolean.TRUE.equals(caixaDagua.getOnOff());
        boolean manual = ligado && !Boolean.TRUE.equals(caixaDagua.getAutoManual());

        ComponentUtils.setComponentEnabled(binding, binding.ivCxdOnOffMain.getId(), online);
        ComponentUtils.setComponentEnabled(binding, binding.ivCxdReservatorio.getId(), online);

        binding.swCxdAutoManual.setEnabled(ligado);

        binding.swCxdValvulaEntradaPrincipal.setEnabled(manual);
        binding.swCxdValvulaEntradaSecundaria.setEnabled(manual);

        atualizaFluxoAgua();
    }

    /** Bolhas e gotas no tanque só quando o dispositivo está on-line, ligado e com alguma válvula de entrada aberta. */
    private void atualizaFluxoAgua() {

        boolean ligado = online && Boolean.TRUE.equals(caixaDagua.getOnOff());
        boolean entrada = Boolean.TRUE.equals(caixaDagua.getVlep()) || Boolean.TRUE.equals(caixaDagua.getVles());

        binding.ivCxdReservatorio.setEnchendo(ligado && entrada);
    }

    private void voltar(Object event){

        finish();
    }
}
