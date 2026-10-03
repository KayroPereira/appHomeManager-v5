package com.home.apphomemanager_v5.tuya;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.home.apphomemanager_v5.BuildConfig;
import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ActivityTuyaDevicesBinding;
import com.home.apphomemanager_v5.tuya.model.TuyaFuncao;
import com.home.apphomemanager_v5.tuya.model.TuyaScene;
import com.home.apphomemanager_v5.tuya.model.TuyaSpec;
import com.home.apphomemanager_v5.util.ComponentUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controle dos dispositivos Tuya do Smart Life. Carrega ao abrir e na atualização manual: listar todos os
 * aparelhos custa uma chamada por aparelho, o que esgotaria a franquia mensal de chamadas da Tuya se fosse
 * repetido sozinho.
 */
public class TuyaDevicesActivity extends AppCompatActivity {

    private static final String TAG = "TuyaDevices";

    /** Ao voltar para a tela, só recarrega se a última carga for mais velha que isto. */
    private static final long VALIDADE_CARGA_MS = 60_000;

    /** Logo após um comando a Tuya ainda devolve o estado antigo: a atualização espera para não "desfazer" o toque. */
    private static final long CARENCIA_APOS_COMANDO_MS = 4_000;

    private ActivityTuyaDevicesBinding binding;

    private TuyaRepository repository;

    private TuyaControleAdapter adapter;

    private long ultimaCargaEm = -VALIDADE_CARGA_MS;

    private long ultimoComandoEm = -CARENCIA_APOS_COMANDO_MS;

    private boolean carregando = false;

    private List<TuyaDispositivo> ultimosDispositivos = new ArrayList<>();

    private List<TuyaScene> cenas = new ArrayList<>();

    private boolean cenasConsultadas = false;

    /** O log de diagnóstico sai na primeira carga e a cada atualização manual. */
    private boolean jaRegistrouNoLog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityTuyaDevicesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ComponentUtils.setEventClickGeneric(binding.ivTuyaBack, event -> finish());
        ComponentUtils.setEventClickGeneric(binding.ivTuyaRefresh, event -> carrega(false));

        adapter = new TuyaControleAdapter(this::enviaComando);

        binding.rvTuya.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTuya.setAdapter(adapter);

        if (!TuyaRepository.estaConfigurado()) {
            exibeMensagem(getString(R.string.tuyaNaoConfigurado));
            return;
        }

        repository = TuyaRepository.getInstance();
    }

    @Override
    protected void onStart() {

        super.onStart();

        if (repository != null && SystemClock.elapsedRealtime() - ultimaCargaEm >= VALIDADE_CARGA_MS) {
            carrega(true);
        }
    }

    private boolean podeAtualizarSilenciosamente() {
        return !adapter.emInteracao() && SystemClock.elapsedRealtime() - ultimoComandoEm >= CARENCIA_APOS_COMANDO_MS;
    }

    /** @param silencioso atualização automática: sem indicador de carregamento e sem apagar a lista em caso de erro. */
    private void carrega(boolean silencioso) {

        if (repository == null || carregando || (silencioso && !podeAtualizarSilenciosamente())) {
            return;
        }

        carregando = true;
        ultimaCargaEm = SystemClock.elapsedRealtime();

        if (!silencioso) {
            binding.pbTuya.setVisibility(View.VISIBLE);
            binding.tvTuyaMensagem.setVisibility(View.GONE);
        }

        repository.carregaDispositivos(new TuyaRepository.Resultado<List<TuyaDispositivo>>() {
            @Override
            public void aoConcluir(List<TuyaDispositivo> dispositivos) {

                carregando = false;

                if (isDestroyed()) {
                    return;
                }

                binding.pbTuya.setVisibility(View.GONE);

                // O usuário pode ter começado a mexer enquanto a consulta estava na rede.
                if (silencioso && !podeAtualizarSilenciosamente()) {
                    return;
                }

                if (BuildConfig.DEBUG && (!silencioso || !jaRegistrouNoLog)) {
                    jaRegistrouNoLog = true;
                    registraNoLog(dispositivos);
                    registraComandosNoLog(dispositivos, repository.getEspecificacoes());
                }

                ultimosDispositivos = dispositivos;

                mostra(!silencioso);

                // Cenas mudam pouco: carregadas na primeira vez e a cada atualização manual.
                if (!silencioso || !cenasConsultadas) {
                    carregaCenas(dispositivos, !silencioso);
                }
            }

            @Override
            public void aoFalhar(String mensagem) {

                carregando = false;

                if (isDestroyed()) {
                    return;
                }

                binding.pbTuya.setVisibility(View.GONE);

                if (!silencioso) {
                    adapter.atualiza(Collections.emptyList(), true);
                    exibeMensagem(getString(R.string.tuyaErro, mensagem));
                }
            }
        });
    }

    /** Monta a lista da tela: aparelhos e, no fim, as cenas de acionamento manual. */
    private void mostra(boolean forcar) {

        List<TuyaControle> controles = TuyaControle.extrai(ultimosDispositivos);

        for (TuyaScene cena : cenas) {
            controles.add(TuyaControle.cena(cena.sceneId, cena.name));
        }

        adapter.atualiza(controles, forcar);

        binding.tvTuyaMensagem.setVisibility(View.GONE);

        if (controles.isEmpty()) {
            exibeMensagem(getString(R.string.tuyaSemDispositivos));
        }
    }

    private void carregaCenas(List<TuyaDispositivo> dispositivos, boolean avisaErro) {

        cenasConsultadas = true;

        String referencia = dispositivos.isEmpty() ? null : dispositivos.get(0).id;

        repository.carregaCenas(referencia, new TuyaRepository.Resultado<List<TuyaScene>>() {
            @Override
            public void aoConcluir(List<TuyaScene> lista) {

                if (isDestroyed()) {
                    return;
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "=== " + lista.size() + " cena(s) de acionamento manual ===");
                    for (TuyaScene cena : lista) {
                        Log.d(TAG, "    " + cena.name + " id=" + cena.sceneId);
                    }
                }

                cenas = lista;
                mostra(true);
            }

            @Override
            public void aoFalhar(String mensagem) {

                Log.w(TAG, "Cenas indisponíveis: " + mensagem);

                if (avisaErro && !isDestroyed()) {
                    Toast.makeText(TuyaDevicesActivity.this, getString(R.string.tuyaCenasIndisponiveis, mensagem), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void enviaComando(TuyaControle controle, Object valor) {

        if (controle.tipo == TuyaControle.Tipo.CENA) {
            confirmaCena(controle);
            return;
        }

        ultimoComandoEm = SystemClock.elapsedRealtime();

        repository.enviaComando(controle.deviceId, controle.code, valor, new TuyaRepository.Resultado<Void>() {
            @Override
            public void aoConcluir(Void dado) {
            }

            @Override
            public void aoFalhar(String mensagem) {

                if (isDestroyed()) {
                    return;
                }

                Toast.makeText(TuyaDevicesActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();

                // Recarrega o estado real em vez de adivinhar o valor anterior.
                ultimoComandoEm = -CARENCIA_APOS_COMANDO_MS;
                carregando = false;
                carrega(false);
            }
        });
    }

    /** Confirmação obrigatória: uma cena pode abrir uma porta. */
    private void confirmaCena(TuyaControle cena) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.tuyaExecutarCenaTitulo)
                .setMessage(getString(R.string.tuyaExecutarCenaMensagem, cena.rotulo))
                .setPositiveButton(R.string.sim, (dialog, which) -> executaCena(cena))
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void executaCena(TuyaControle cena) {

        repository.disparaCena(cena.code, new TuyaRepository.Resultado<Void>() {
            @Override
            public void aoConcluir(Void dado) {

                if (!isDestroyed()) {
                    Toast.makeText(TuyaDevicesActivity.this, getString(R.string.tuyaCenaExecutada, cena.rotulo), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void aoFalhar(String mensagem) {

                if (!isDestroyed()) {
                    Toast.makeText(TuyaDevicesActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /** Só em debug: despeja cada aparelho e seus pontos de dados para diagnóstico (filtro do Logcat: TuyaDevices). */
    private static void registraNoLog(List<TuyaDispositivo> dispositivos) {

        Log.d(TAG, "=== " + dispositivos.size() + " dispositivo(s) ===");

        for (TuyaDispositivo dispositivo : dispositivos) {

            Log.d(TAG, "[" + dispositivo.nome + "] id=" + dispositivo.id + " online=" + dispositivo.online);

            for (TuyaPonto ponto : dispositivo.pontos) {
                Log.d(TAG, "    " + ponto.code + " = " + ponto.valor + (ponto.propriedade != null ? "  esquema=" + ponto.propriedade : ""));
            }
        }
    }

    /**
     * Só em debug: pontos de dados que o aparelho aceita como comando (functions da especificação).
     * Botões como "abrir a porta" costumam existir só aqui, sem aparecer no status.
     */
    private static void registraComandosNoLog(List<TuyaDispositivo> dispositivos, Map<String, TuyaSpec> especificacoes) {

        for (TuyaDispositivo dispositivo : dispositivos) {

            TuyaSpec spec = especificacoes.get(dispositivo.id);

            if (spec == null) {
                Log.d(TAG, "[" + dispositivo.nome + "] comandos: sem especificação");
                continue;
            }

            Log.d(TAG, "[" + dispositivo.nome + "] comandos aceitos (functions):");

            if (spec.functions != null) {
                for (TuyaFuncao funcao : spec.functions) {
                    Log.d(TAG, "    " + funcao.code + " tipo=" + funcao.type + " valores=" + funcao.values);
                }
            }
        }
    }

    private void exibeMensagem(String mensagem) {

        binding.tvTuyaMensagem.setText(mensagem);
        binding.tvTuyaMensagem.setVisibility(View.VISIBLE);
    }
}
