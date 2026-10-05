package com.home.apphomemanager_v5;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.home.apphomemanager_v5.comodo.Comodo;
import com.home.apphomemanager_v5.comodo.ComodoActivity;
import com.home.apphomemanager_v5.comodo.ComodoRepository;
import com.home.apphomemanager_v5.comodo.DashboardDados;
import com.home.apphomemanager_v5.comodo.DashboardDados.ResumoComodo;
import com.home.apphomemanager_v5.comodo.LeituraDispositivo;
import com.home.apphomemanager_v5.comodo.LeituraDispositivo.EstadoAlarme;
import com.home.apphomemanager_v5.commons.AppConstants;
import com.home.apphomemanager_v5.commons.StatusDispositivo;
import com.home.apphomemanager_v5.databinding.ActivityDashBoardMetricasBinding;
import com.home.apphomemanager_v5.databinding.ItemDashComodoBinding;
import com.home.apphomemanager_v5.databinding.ItemDashLinhaBinding;
import com.home.apphomemanager_v5.databinding.ItemDashReservatorioBinding;
import com.home.apphomemanager_v5.databinding.ItemDashTileBinding;
import com.home.apphomemanager_v5.model.firebase.FirebaseEntity;
import com.home.apphomemanager_v5.model.reservatorio.CaixaDagua;
import com.home.apphomemanager_v5.model.reservatorio.Cisterna;
import com.home.apphomemanager_v5.model.reservatorio.Reservatorio;
import com.home.apphomemanager_v5.tuya.TuyaRepository;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.util.ComponentUtils;
import com.home.apphomemanager_v5.util.JsonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Painel só de leitura da casa: resumo geral, seções fixas (segurança, climatização e manutenção) e um
 * cartão por cômodo. Os aparelhos vêm do último estado da Tuya (reconsultado ao abrir, a cada 2 minutos
 * com a tela visível e no botão de atualizar) e os reservatórios do Firebase, ao vivo.
 */
public class DashBoardMetricasActivity extends AppCompatActivity {

    private static final String TAG = "DashBoardMetricas";

    /** A cota de chamadas da Tuya é mensal e cada aparelho custa uma chamada: o painel não precisa ser mais rápido que isso. */
    private static final long INTERVALO_ATUALIZACAO_MS = 2 * 60 * 1000L;

    private static final String PATH_CISTERNA = "cisterna";
    private static final String PATH_CAIXA = "cx1";

    /** Abaixo desta fração o reservatório é sinalizado como baixo. */
    private static final float NIVEL_BAIXO = 0.2f;

    private ActivityDashBoardMetricasBinding binding;

    private ComodoRepository comodoRepository;

    private TuyaRepository tuyaRepository;

    private List<Comodo> comodos = new ArrayList<>();

    private final FirebaseEntity firebaseCisterna = new FirebaseEntity();
    private final FirebaseEntity firebaseCaixa = new FirebaseEntity();

    private Cisterna cisterna;
    private CaixaDagua caixa;

    private final StatusDispositivo statusDispositivo = new StatusDispositivo();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable atualizacaoPeriodica = new Runnable() {
        @Override
        public void run() {
            carregaEstados();
            handler.postDelayed(this, INTERVALO_ATUALIZACAO_MS);
        }
    };

    private ObjectAnimator animacaoAtualizar;

    private boolean carregando = false;

    /** Falso até a primeira consulta à Tuya da visita: a primeira lista de cômodos a dispara se o ciclo chegou antes dela. */
    private boolean estadosPedidos = false;

    private DashboardDados ultimosDados;

    /** Horário (relógio do aparelho) da última resposta da Tuya; zero enquanto não houve. */
    private long atualizadoEm = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        comodoRepository = new ComodoRepository(this);

        binding = ActivityDashBoardMetricasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ComponentUtils.setEventClickGeneric(binding.ivMetVoltar, event -> finish());
        ComponentUtils.setEventClickGeneric(binding.ivMetAtualizar, event -> carregaEstados());

        if (TuyaRepository.estaConfigurado()) {
            tuyaRepository = TuyaRepository.getInstance();
        }

        firebaseCisterna.FirebaseInicialize(PATH_CISTERNA);
        firebaseCaixa.FirebaseInicialize(PATH_CAIXA);

        exibe();
    }

    @Override
    protected void onStart() {

        super.onStart();

        // Primeiro a cópia local dos cômodos e o último estado em cache: a tela abre preenchida.
        comodoRepository.inicia(new ComodoRepository.Listener() {
            @Override
            public void aoAtualizar(List<Comodo> lista) {

                comodos = lista;
                exibe();

                if (!estadosPedidos) {
                    carregaEstados();
                }
            }

            @Override
            public void aoFalhar(DatabaseError erro) {

                Log.w(TAG, "Erro ao ler os cômodos: " + erro.getMessage());
                Toast.makeText(DashBoardMetricasActivity.this, R.string.erroLerComodos, Toast.LENGTH_LONG).show();
            }
        });

        firebaseCisterna.addValueEventListener(ouvinteReservatorio(Cisterna.class, leitura -> cisterna = leitura));
        firebaseCaixa.addValueEventListener(ouvinteReservatorio(CaixaDagua.class, leitura -> caixa = leitura));

        // O "online" dos reservatórios depende do relógio: reavalia de tempos em tempos mesmo sem dado novo.
        statusDispositivo.inicializaSchedulerStatusDispositivo(() -> {
            if (ultimosDados != null) {
                exibeManutencao(ultimosDados);
            }
        }, AppConstants.DELAY_VERIFICACAO_STATUS_MS);

        handler.post(atualizacaoPeriodica);
    }

    @Override
    protected void onStop() {

        super.onStop();

        handler.removeCallbacks(atualizacaoPeriodica);
        estadosPedidos = false;
        statusDispositivo.paraSchedulerStatusDispositivo();
        comodoRepository.para();
        firebaseCisterna.disconnect();
        firebaseCaixa.disconnect();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        mostraAtualizando(false);
    }

    private interface Receptor<T> {
        void recebe(T leitura);
    }

    private <T extends Reservatorio> ValueEventListener ouvinteReservatorio(Class<T> tipo, Receptor<T> receptor) {

        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                try {
                    receptor.recebe(snapshot.getValue() == null ? null : JsonUtils.fromJson(tipo, new Gson().toJson(snapshot.getValue())));
                } catch (RuntimeException e) {
                    Log.w(TAG, "Dado inválido do reservatório " + tipo.getSimpleName(), e);
                    receptor.recebe(null);
                }
                exibe();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Erro ao ler " + tipo.getSimpleName() + ": " + error.getMessage());
            }
        };
    }

    private void carregaEstados() {

        Set<String> ids = DashboardDados.idsTuya(comodos);

        if (tuyaRepository == null || carregando || ids.isEmpty()) {
            return;
        }

        carregando = true;
        estadosPedidos = true;
        mostraAtualizando(true);
        exibe();

        tuyaRepository.carregaEstados(ids, new TuyaRepository.Resultado<java.util.Map<String, TuyaDevice>>() {
            @Override
            public void aoConcluir(java.util.Map<String, TuyaDevice> dispositivos) {

                carregando = false;

                if (isDestroyed()) {
                    return;
                }

                atualizadoEm = System.currentTimeMillis();
                mostraAtualizando(false);
                exibe();
            }

            @Override
            public void aoFalhar(String mensagem) {

                carregando = false;

                if (isDestroyed()) {
                    return;
                }

                Log.w(TAG, "Estado da Tuya indisponível: " + mensagem);
                Toast.makeText(DashBoardMetricasActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();

                mostraAtualizando(false);
                exibe();
            }
        });
    }

    /** Gira o ícone de atualizar e o desabilita enquanto a consulta à Tuya está em andamento. */
    private void mostraAtualizando(boolean atualizando) {

        if (binding == null) {
            return;
        }

        binding.ivMetAtualizar.setEnabled(!atualizando);
        binding.ivMetAtualizar.setAlpha(atualizando ? 0.6f : 1f);

        if (atualizando) {
            if (animacaoAtualizar == null) {
                animacaoAtualizar = ObjectAnimator.ofFloat(binding.ivMetAtualizar, View.ROTATION, 0f, 360f);
                animacaoAtualizar.setDuration(900);
                animacaoAtualizar.setInterpolator(new LinearInterpolator());
                animacaoAtualizar.setRepeatCount(ObjectAnimator.INFINITE);
            }
            if (!animacaoAtualizar.isRunning()) {
                animacaoAtualizar.start();
            }
        } else {
            if (animacaoAtualizar != null) {
                animacaoAtualizar.cancel();
            }
            binding.ivMetAtualizar.setRotation(0f);
        }
    }

    // ---------------------------------------------------------------- montagem da tela

    private void exibe() {

        if (binding == null || isDestroyed()) {
            return;
        }

        DashboardDados dados = DashboardDados.calcula(comodos, TuyaRepository::estadoEmCache);
        ultimosDados = dados;

        exibeCabecalho();
        exibeResumo(dados);
        exibeSeguranca(dados);
        exibeClimatizacao(dados);
        exibeManutencao(dados);
        exibeComodos(dados);
    }

    private void exibeCabecalho() {

        if (carregando) {
            binding.tvMetAtualizado.setText(R.string.dashAtualizando);
        } else if (atualizadoEm > 0) {
            binding.tvMetAtualizado.setText(getString(R.string.dashAtualizadoAs,
                    android.text.format.DateFormat.getTimeFormat(this).format(new Date(atualizadoEm))));
        } else {
            binding.tvMetAtualizado.setText("");
        }

        if (tuyaRepository == null) {
            binding.tvMetAviso.setText(R.string.tuyaNaoConfiguradaCurto);
            binding.tvMetAviso.setVisibility(View.VISIBLE);
        } else if (comodos.isEmpty()) {
            binding.tvMetAviso.setText(R.string.dashSemComodos);
            binding.tvMetAviso.setVisibility(View.VISIBLE);
        } else {
            binding.tvMetAviso.setVisibility(View.GONE);
        }
    }

    private void exibeResumo(DashboardDados dados) {

        Locale locale = Locale.getDefault();
        List<ItemDashTileBinding> tiles = new ArrayList<>();

        tiles.add(tile(R.string.dashTileLampadas, String.valueOf(dados.lampadasAcesas), dados.lampadasAcesas > 0 ? R.color.corLuz : 0,
                getString(R.string.dashDeTotal, dados.lampadas)));

        tiles.add(tile(R.string.dashTileTomadas, String.valueOf(dados.tomadasLigadas), dados.tomadasLigadas > 0 ? R.color.corTomada : 0,
                getString(R.string.dashDeTotal, dados.tomadas)));

        tiles.add(tile(R.string.dashTileConsumo, LeituraDispositivo.potencia(dados.potencia, locale), dados.potencia != null ? R.color.corEnergia : 0,
                dados.potencia != null ? "" : getString(R.string.dashSemMedidor)));

        boolean temOffline = !dados.foraDoAr.isEmpty();

        tiles.add(tile(R.string.dashTileDispositivos, getString(R.string.dashFracao, dados.dispositivosOnline, dados.dispositivos),
                temOffline ? R.color.offLine : dados.dispositivos > 0 ? R.color.onLine : 0,
                temOffline ? getString(R.string.dashForaDoArContagem, dados.foraDoAr.size()) : getString(R.string.dashTodosOnline)));

        tiles.add(tile(R.string.dashTileTemperatura, LeituraDispositivo.temperatura(dados.temperaturaMedia, locale),
                dados.temperaturaMedia != null ? R.color.corTemperatura : 0,
                dados.umidadeMedia != null ? getString(R.string.dashUmidade, LeituraDispositivo.umidade(dados.umidadeMedia, locale)) : ""));

        EstadoAlarme geral = dados.alarmeGeral();

        tiles.add(tile(R.string.dashTileSeguranca, geral == null ? "--" : textoAlarme(geral), geral == null ? 0 : corAlarme(geral),
                geral == null ? getString(R.string.dashSemAlarme) : ""));

        binding.llMetResumo.removeAllViews();

        LinearLayout linha = null;

        for (int i = 0; i < tiles.size(); i++) {

            if (i % 2 == 0) {
                linha = new LinearLayout(this);
                linha.setOrientation(LinearLayout.HORIZONTAL);
                binding.llMetResumo.addView(linha);
            }
            linha.addView(tiles.get(i).getRoot());
        }
    }

    private ItemDashTileBinding tile(int titulo, String valor, @ColorRes int cor, String apoio) {

        ItemDashTileBinding tile = ItemDashTileBinding.inflate(LayoutInflater.from(this), binding.llMetResumo, false);

        tile.tvDtTitulo.setText(titulo);
        tile.tvDtValor.setText(valor);

        if (cor != 0) {
            tile.tvDtValor.setTextColor(getColor(cor));
        }

        tile.tvDtApoio.setText(apoio);

        return tile;
    }

    private void exibeSeguranca(DashboardDados dados) {

        LinearLayout cartao = limpa(binding.llMetSeguranca);

        for (DashboardDados.Alarme alarme : dados.alarmes) {

            String estado = alarme.foraDoAr ? getString(R.string.estadoForaDoAr) : textoAlarme(alarme.estado);
            int cor = alarme.foraDoAr ? R.color.offLine : corAlarme(alarme.estado);

            adicionaLinha(cartao, R.drawable.ic_alarme, cor, alarme.nome, alarme.comodo, estado, cor, null);
        }

        for (DashboardDados.SensorChuva sensor : dados.sensoresChuva) {

            Boolean chovendo = sensor.chovendo;
            int cor = chovendo == null ? R.color.textoSecundario : chovendo ? R.color.corAgua : R.color.textoSecundario;
            int texto = chovendo == null ? R.string.estadoSemLeitura : chovendo ? R.string.estadoChovendo : R.string.estadoSemChuva;

            adicionaLinha(cartao, R.drawable.ic_chuva, R.color.corAgua, sensor.nome, sensor.comodo, getString(texto), cor, null);
        }

        if (cartao.getChildCount() == 0) {
            adicionaAviso(cartao, getString(R.string.dashSemSeguranca));
        }
    }

    private void exibeClimatizacao(DashboardDados dados) {

        LinearLayout cartao = limpa(binding.llMetClimatizacao);
        Locale locale = Locale.getDefault();

        for (DashboardDados.Termometro termometro : dados.termometros) {

            Double graus = termometro.temperatura;

            adicionaLinha(cartao, R.drawable.ic_termometro, R.color.corTemperatura, termometro.nome, termometro.comodo,
                    LeituraDispositivo.temperatura(graus, locale), graus == null ? R.color.textoSecundario : R.color.corTemperatura,
                    termometro.umidade != null ? LeituraDispositivo.umidade(termometro.umidade, locale) : null);
        }

        if (dados.cortinas > 0) {
            adicionaLinha(cartao, R.drawable.ic_cortina, R.color.corCortina, getString(R.string.dashCortinas), null,
                    getString(R.string.dashFracao, dados.cortinasAbertas, dados.cortinas), R.color.corCortina,
                    getString(R.string.dashCortinasAbertas, dados.cortinasAbertas, dados.cortinas));
        }

        if (cartao.getChildCount() == 0) {
            adicionaAviso(cartao, getString(R.string.dashSemClima));
        }
    }

    private void exibeManutencao(DashboardDados dados) {

        LinearLayout cartao = limpa(binding.llMetManutencao);

        adicionaReservatorio(cartao, getString(R.string.cisterna), cisterna, cisterna == null ? null : infoCisterna());
        adicionaReservatorio(cartao, getString(R.string.dashCaixaDagua), caixa, caixa == null ? null : infoCaixa());

        if (dados.foraDoAr.isEmpty()) {
            if (dados.dispositivos > 0) {
                adicionaLinha(cartao, R.drawable.ic_tomada, R.color.onLine, getString(R.string.dashTodosDispositivosOnline), null, "", R.color.onLine, null);
            }
        } else {
            for (String dispositivo : dados.foraDoAr) {
                adicionaLinha(cartao, R.drawable.ic_tomada, R.color.offLine, dispositivo, null, getString(R.string.estadoForaDoAr), R.color.offLine, null);
            }
        }
    }

    private String infoCisterna() {

        List<String> partes = new ArrayList<>();

        partes.add(modo(cisterna));
        partes.add(getString(Boolean.TRUE.equals(cisterna.getPump()) ? R.string.dashBombaLigada : R.string.dashBombaDesligada));

        return String.join(" · ", partes);
    }

    private String infoCaixa() {

        boolean enchendo = Boolean.TRUE.equals(caixa.getOnOff())
                && (Boolean.TRUE.equals(caixa.getVlep()) || Boolean.TRUE.equals(caixa.getVles()));

        return enchendo ? modo(caixa) + " · " + getString(R.string.dashEnchendo) : modo(caixa);
    }

    private String modo(Reservatorio reservatorio) {

        if (!Boolean.TRUE.equals(reservatorio.getOnOff())) {
            return getString(R.string.estadoDesligado);
        }
        return getString(Boolean.TRUE.equals(reservatorio.getAutoManual()) ? R.string.dashModoAutomatico : R.string.dashModoManual);
    }

    private void adicionaReservatorio(LinearLayout cartao, String nome, Reservatorio reservatorio, String info) {

        ItemDashReservatorioBinding item = ItemDashReservatorioBinding.inflate(LayoutInflater.from(this), cartao, false);

        boolean online = reservatorio != null && statusDispositivo.isOnline(reservatorio.getStatus(), AppConstants.PERIODO_2_MINUTO_S);
        float fracao = reservatorio != null ? reservatorio.calculaFracaoNivel() : -1f;
        boolean baixo = online && fracao >= 0 && fracao < NIVEL_BAIXO;

        item.ivDrIcone.setImageResource(R.drawable.caixa_on);
        item.tvDrNome.setText(nome);
        item.tvDrStatus.setText(online ? R.string.online : R.string.offline);
        item.tvDrStatus.setTextColor(getColor(online ? R.color.onLine : R.color.offLine));

        int percentual = fracao < 0 ? 0 : Math.round(fracao * 100);

        item.pbDrNivel.setProgress(percentual);
        item.pbDrNivel.setProgressTintList(android.content.res.ColorStateList.valueOf(getColor(baixo ? R.color.offLine : R.color.corAgua)));
        item.tvDrNivel.setText(fracao < 0 ? "--" : getString(R.string.dashPercentual, percentual));

        // Sem leitura (ou aparelho fora do ar) o nível é o último conhecido: esmaece para não enganar.
        item.llDrConteudo.setAlpha(online && fracao >= 0 ? 1f : 0.55f);

        if (fracao < 0) {
            item.tvDrInfo.setText(R.string.dashSemLeituraNivel);
        } else {
            item.tvDrInfo.setText(baixo ? getString(R.string.dashNivelBaixo) + " · " + info : info);
        }

        adicionaItem(cartao, item.getRoot(), item.vDrDivisor);
    }

    private void exibeComodos(DashboardDados dados) {

        Locale locale = Locale.getDefault();

        binding.llMetComodos.removeAllViews();

        for (ResumoComodo resumo : dados.comodos) {

            ItemDashComodoBinding item = ItemDashComodoBinding.inflate(LayoutInflater.from(this), binding.llMetComodos, false);
            Comodo comodo = resumo.comodo;

            item.ivDcIcone.setImageResource(comodo.drawableIcone());
            item.tvDcNome.setText(comodo.nome);

            if (!comodo.habilitado) {
                item.tvDcAviso.setText(R.string.dashComodoDesabilitado);
                item.tvDcAviso.setTextColor(getColor(R.color.textoSecundario));
            } else if (resumo.foraDoAr > 0) {
                item.tvDcAviso.setText(getString(R.string.dashForaDoArContagem, resumo.foraDoAr));
                item.tvDcAviso.setTextColor(getColor(R.color.offLine));
            } else {
                item.tvDcAviso.setText("");
            }

            indicador(item.llDcLuzes, item.tvDcLuzes, resumo.lampadas > 0,
                    getString(R.string.dashFracao, resumo.lampadasAcesas, resumo.lampadas), resumo.lampadasAcesas > 0 ? R.color.corLuz : R.color.textoSecundario);

            indicador(item.llDcTomadas, item.tvDcTomadas, resumo.tomadas > 0,
                    getString(R.string.dashFracao, resumo.tomadasLigadas, resumo.tomadas), resumo.tomadasLigadas > 0 ? R.color.corTomada : R.color.textoSecundario);

            indicador(item.llDcTemperatura, item.tvDcTemperatura, resumo.temTermometro(),
                    LeituraDispositivo.temperatura(resumo.temperatura, locale), resumo.temperatura != null ? R.color.corTemperatura : R.color.textoSecundario);

            indicador(item.llDcConsumo, item.tvDcConsumo, resumo.temMedidor(),
                    LeituraDispositivo.potencia(resumo.potencia, locale), resumo.potencia != null ? R.color.corEnergia : R.color.textoSecundario);

            boolean semIndicadores = resumo.lampadas == 0 && resumo.tomadas == 0 && !resumo.temTermometro() && !resumo.temMedidor();

            // Alarmes, sensores de chuva e cortinas não têm coluna própria: viram uma linha de texto.
            String extras = extrasDoComodo(resumo);

            item.llDcIndicadores.setVisibility(semIndicadores ? View.GONE : View.VISIBLE);

            item.tvDcVazio.setText(!extras.isEmpty() ? extras : getString(R.string.dashSemDispositivos));
            item.tvDcVazio.setVisibility(semIndicadores || !extras.isEmpty() ? View.VISIBLE : View.GONE);

            // Com indicadores acima, a linha de extras não precisa da margem larga do aviso de cômodo vazio.
            ((LinearLayout.LayoutParams) item.tvDcVazio.getLayoutParams()).topMargin =
                    Math.round(getResources().getDisplayMetrics().density * (semIndicadores ? 8 : 4));

            item.llDcCartao.setAlpha(comodo.habilitado ? 1f : 0.55f);
            item.llDcCartao.setOnClickListener(view -> abreComodo(comodo));

            binding.llMetComodos.addView(item.getRoot());
        }
    }

    /** "Alarme: Ativado · Sem chuva · 1 de 2 cortinas abertas", só com o que o cômodo tem; vazio se não tem nenhum. */
    private String extrasDoComodo(ResumoComodo resumo) {

        List<String> partes = new ArrayList<>();

        if (resumo.alarmes > 0) {
            partes.add(getString(R.string.dashAlarmeComodo, resumo.alarme != null ? textoAlarme(resumo.alarme) : getString(R.string.alarmeDesconhecido)));
        }
        if (resumo.sensoresChuva > 0) {
            partes.add(getString(resumo.chovendo == null ? R.string.estadoSemLeitura : resumo.chovendo ? R.string.estadoChovendo : R.string.estadoSemChuva));
        }
        if (resumo.cortinas > 0) {
            partes.add(getString(R.string.dashCortinasComodo, resumo.cortinasAbertas, resumo.cortinas));
        }
        return String.join(" · ", partes);
    }

    private void indicador(View bloco, android.widget.TextView valor, boolean existe, String texto, @ColorRes int cor) {

        bloco.setVisibility(existe ? View.VISIBLE : View.GONE);
        valor.setText(texto);
        valor.setTextColor(getColor(cor));
    }

    private void abreComodo(Comodo comodo) {

        Intent intent = new Intent(this, ComodoActivity.class);
        intent.putExtra(ComodoActivity.EXTRA_ID, comodo.id);

        startActivity(intent);
    }

    // ---------------------------------------------------------------- linhas dos cartões

    private static LinearLayout limpa(LinearLayout cartao) {

        cartao.removeAllViews();
        cartao.setTag(null);

        return cartao;
    }

    /** O divisor entre linhas só aparece depois da linha seguinte: a última do cartão fica sem. */
    private static void adicionaItem(LinearLayout cartao, View raiz, View divisor) {

        if (cartao.getTag() instanceof View) {
            ((View) cartao.getTag()).setVisibility(View.VISIBLE);
        }

        divisor.setVisibility(View.GONE);
        cartao.setTag(divisor);
        cartao.addView(raiz);
    }

    private void adicionaLinha(LinearLayout cartao, @DrawableRes int icone, @ColorRes int corIcone, String titulo, String subtitulo,
                               String valor, @ColorRes int corValor, String apoio) {

        ItemDashLinhaBinding linha = ItemDashLinhaBinding.inflate(LayoutInflater.from(this), cartao, false);

        linha.ivDlIcone.setImageResource(icone);
        linha.ivDlIcone.setColorFilter(getColor(corIcone));
        linha.tvDlTitulo.setText(titulo);
        linha.tvDlSubtitulo.setText(subtitulo);
        linha.tvDlSubtitulo.setVisibility(subtitulo == null || subtitulo.isEmpty() ? View.GONE : View.VISIBLE);
        linha.tvDlValor.setText(valor);
        linha.tvDlValor.setTextColor(getColor(corValor));
        linha.tvDlApoio.setText(apoio);
        linha.tvDlApoio.setVisibility(apoio == null || apoio.isEmpty() ? View.GONE : View.VISIBLE);

        adicionaItem(cartao, linha.getRoot(), linha.vDlDivisor);
    }

    private void adicionaAviso(LinearLayout cartao, String mensagem) {

        ItemDashLinhaBinding linha = ItemDashLinhaBinding.inflate(LayoutInflater.from(this), cartao, false);

        linha.ivDlIcone.setVisibility(View.GONE);
        linha.tvDlTitulo.setText(mensagem);
        linha.tvDlTitulo.setTextColor(ContextCompat.getColor(this, R.color.textoSecundario));
        linha.tvDlTitulo.setTextSize(14);
        linha.tvDlSubtitulo.setVisibility(View.GONE);
        linha.tvDlValor.setVisibility(View.GONE);
        linha.tvDlApoio.setVisibility(View.GONE);

        adicionaItem(cartao, linha.getRoot(), linha.vDlDivisor);
    }

    private String textoAlarme(EstadoAlarme estado) {

        switch (estado) {
            case ATIVADO:
                return getString(R.string.alarmeAtivado);
            case EM_CASA:
                return getString(R.string.alarmeEmCasa);
            case DISPARADO:
                return getString(R.string.alarmeDisparado);
            case DESATIVADO:
                return getString(R.string.alarmeDesativado);
            default:
                return getString(R.string.alarmeDesconhecido);
        }
    }

    @ColorRes
    private static int corAlarme(EstadoAlarme estado) {

        switch (estado) {
            case ATIVADO:
                return R.color.corAlarmeOk;
            case EM_CASA:
                return R.color.corLuz;
            case DISPARADO:
                return R.color.corAlarme;
            default:
                return R.color.textoSecundario;
        }
    }
}
