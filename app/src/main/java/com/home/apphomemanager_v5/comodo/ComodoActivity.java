package com.home.apphomemanager_v5.comodo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DatabaseError;
import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.comodo.CelulaAdapter.Celula;
import com.home.apphomemanager_v5.databinding.ActivityComodoBinding;
import com.home.apphomemanager_v5.tuya.TuyaRepository;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;
import com.home.apphomemanager_v5.util.IconeClima;
import com.home.apphomemanager_v5.util.ComponentUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tela de um cômodo cadastrado: lâmpadas e tomadas, ligadas/desligadas pelos dispositivos Tuya associados.
 * Só consulta os aparelhos deste cômodo e mostra um indicador de carregamento até todos responderem.
 */
public class ComodoActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "comodoId";

    private static final String TAG = "ComodoActivity";

    /**
     * A franquia de chamadas da Tuya é mensal e cada aparelho custa uma chamada por consulta, então o
     * intervalo cresce com o tempo sem atividade: rápido logo após abrir, tocar ou ver uma mudança
     * (inclusive feita por fora, como no app da Tuya), e espaçado quando a tela fica parada.
     */
    private static final long INTERVALO_ATIVO_MS = 10_000;
    private static final long INTERVALO_MORNO_MS = 30_000;
    private static final long INTERVALO_OCIOSO_MS = 60_000;

    private static final long ANTECIPACAO_APOS_TOQUE_MS = 5_000;

    /** Tempo sem atividade para passar de ativo a morno e de morno a ocioso. */
    private static final long LIMITE_ATIVO_MS = 60_000;
    private static final long LIMITE_MORNO_MS = 5 * 60_000;

    /** Logo após um comando a Tuya ainda devolve o estado antigo: a atualização espera para não "desfazer" o toque. */
    private static final long CARENCIA_APOS_COMANDO_MS = 4_000;

    private ActivityComodoBinding binding;

    private ComodoRepository comodoRepository;

    private TuyaRepository tuyaRepository;

    private CelulaAdapter adapterLampadas;
    private CelulaAdapter adapterTomadas;
    private MedidorAdapter adapterMedidores;
    private TermometroAdapter adapterTermometros;
    private CortinaAdapter adapterCortinas;
    private ChuvaAdapter adapterChuva;
    private AlarmeAdapter adapterAlarmes;
    private LuzAdapter adapterLuzes;

    private String comodoId;
    private Comodo comodo;

    /** Verdadeiro até a primeira resposta da Tuya, quando ainda não há estado de todos os aparelhos. */
    private boolean aguardando = true;

    private boolean primeiraLeitura = true;

    private boolean atualizando = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable atualizacaoPeriodica = new Runnable() {
        @Override
        public void run() {
            carregaEstados();
            handler.postDelayed(this, intervaloAtual());
        }
    };

    /** Consulta extra pouco depois de um comando de alarme, para mostrar o novo modo sem esperar o ciclo normal. */
    private final Runnable atualizacaoAposComando = this::carregaEstados;

    /**
     * Quando cada motor de cortina recebeu o último comando desta tela. A Tuya só informa a posição quando
     * o motor para; até lá o cartão mostra "Abrindo" ou "Fechando".
     */
    private final Map<String, ComandoCortina> comandoDaCortina = new java.util.HashMap<>();

    /** Último comando dado a uma cortina nesta tela: dá retorno imediato no cartão antes de a Tuya confirmar. */
    private static final class ComandoCortina {
        final long em = SystemClock.elapsedRealtime();
        /** 0 = subir, 1 = descer, 2 = parar. */
        final int comando;
        /** Verdadeiro até a Tuya responder ao pedido da cena. */
        boolean enviando = true;
        /** Posição que a Tuya informava ao tocar em parar: o cartão espera ela mudar para mostrar a nova. */
        Double posicaoAoParar;

        ComandoCortina(int comando) {
            this.comando = comando;
        }
    }

    private String chaveDaCortina(int posicao) {
        return comodo.id + "/" + posicao;
    }

    private static final long TEMPO_DE_MOVIMENTO_MS = 90_000;

    /** Tempo máximo esperando a Tuya informar a posição depois de parar. */
    private static final long TEMPO_ESPERANDO_PARADA_MS = 20_000;

    /** Último toque, comando ou mudança de estado vista; base do intervalo adaptativo. */
    private long ultimaAtividadeEm = 0;

    /** Resumo do último estado recebido, para notar mudanças feitas fora do app. */
    private String assinaturaEstados;

    private long ultimoComandoEm = -CARENCIA_APOS_COMANDO_MS;

    private boolean carregando = false;

    private boolean temAlgumaCena() {

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            for (ItemComodo item : comodo.itens(grupo)) {
                for (int comando = 0; comando < 7; comando++) {
                    if (ItemComodo.preenchida(item.cena(comando))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        comodoRepository = new ComodoRepository(this);

        binding = ActivityComodoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        comodoId = getIntent().getStringExtra(EXTRA_ID);

        ComponentUtils.setEventClickGeneric(binding.ivComodoBack, event -> finish());
        ComponentUtils.setEventClickGeneric(binding.ivComodoOnOff, event -> alternaHabilitado());

        adapterLampadas = new CelulaAdapter(celula -> aoClicar(comodo.lampadas, celula));
        adapterTomadas = new CelulaAdapter(celula -> aoClicar(comodo.tomadas, celula));
        adapterMedidores = new MedidorAdapter(medidor -> aoClicarMedidor(medidor));
        adapterTermometros = new TermometroAdapter();
        adapterCortinas = new CortinaAdapter(this::aoClicarCortina, this::mostraDadosCortina);
        adapterChuva = new ChuvaAdapter();
        adapterAlarmes = new AlarmeAdapter(this::aoClicarAlarme);
        adapterLuzes = new LuzAdapter(this::aoClicarLuz);

        binding.rvComodoLampadas.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvComodoLampadas.setAdapter(adapterLampadas);
        binding.rvComodoTomadas.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvComodoTomadas.setAdapter(adapterTomadas);
        binding.rvComodoMedidores.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComodoMedidores.setAdapter(adapterMedidores);
        binding.rvComodoTermometros.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComodoTermometros.setAdapter(adapterTermometros);
        binding.rvComodoCortinas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComodoCortinas.setAdapter(adapterCortinas);
        binding.rvComodoChuva.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComodoChuva.setAdapter(adapterChuva);
        binding.rvComodoAlarmes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComodoAlarmes.setAdapter(adapterAlarmes);
        binding.rvComodoLuzes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComodoLuzes.setAdapter(adapterLuzes);

        if (TuyaRepository.estaConfigurado()) {
            tuyaRepository = TuyaRepository.getInstance();
        }

        exibeCarregando(true);
    }

    @Override
    protected void onStart() {

        super.onStart();

        comodoRepository.inicia(new ComodoRepository.Listener() {
            @Override
            public void aoAtualizar(List<Comodo> comodos) {

                comodo = null;

                for (Comodo candidato : comodos) {
                    if (candidato.id.equals(comodoId)) {
                        comodo = candidato;
                    }
                }

                if (comodo == null) {
                    // Cômodo excluído (por outro aparelho, por exemplo).
                    finish();
                    return;
                }

                if (primeiraLeitura) {
                    primeiraLeitura = false;

                    // O primeiro toque numa cena fica mais rápido com a lista já baixada.
                    if (tuyaRepository != null && temAlgumaCena()) {
                        tuyaRepository.preaqueceCenas();
                    }
                    // Com o estado de todos os aparelhos já em cache (visita anterior) a tela abre direto.
                    aguardando = tuyaRepository != null && !idsTuya().isEmpty() && !todosEmCache();
                }

                exibe();
                iniciaAtualizacao();
            }

            @Override
            public void aoFalhar(DatabaseError erro) {

                Log.w(TAG, "Erro ao ler o cômodo: " + erro.getMessage());
                Toast.makeText(ComodoActivity.this, R.string.erroLerComodos, Toast.LENGTH_LONG).show();
                exibeCarregando(false);
            }
        });
    }

    @Override
    protected void onStop() {

        super.onStop();

        comodoRepository.para();
        handler.removeCallbacks(atualizacaoPeriodica);
        handler.removeCallbacks(atualizacaoAposComando);
        atualizando = false;
        primeiraLeitura = true;
        assinaturaEstados = null;
    }

    @Override
    public void onUserInteraction() {

        super.onUserInteraction();

        boolean estavaParado = SystemClock.elapsedRealtime() - ultimaAtividadeEm >= LIMITE_ATIVO_MS;

        // Qualquer toque na tela volta ao ritmo rápido.
        ultimaAtividadeEm = SystemClock.elapsedRealtime();

        // Saindo da ociosidade a próxima consulta poderia estar a um minuto: antecipa para logo depois da carência do comando.
        if (estavaParado && atualizando) {
            handler.removeCallbacks(atualizacaoPeriodica);
            handler.postDelayed(atualizacaoPeriodica, ANTECIPACAO_APOS_TOQUE_MS);
        }
    }

    private long intervaloAtual() {

        long parado = SystemClock.elapsedRealtime() - ultimaAtividadeEm;

        if (parado < LIMITE_ATIVO_MS) {
            return INTERVALO_ATIVO_MS;
        }
        return parado < LIMITE_MORNO_MS ? INTERVALO_MORNO_MS : INTERVALO_OCIOSO_MS;
    }

    private static String assinatura(Map<String, TuyaDevice> dispositivos) {

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, TuyaDevice> entrada : new java.util.TreeMap<>(dispositivos).entrySet()) {

            TuyaDevice dispositivo = entrada.getValue();

            sb.append(entrada.getKey()).append(dispositivo.online);

            if (dispositivo.status != null) {
                for (TuyaStatus status : dispositivo.status) {
                    sb.append(status.code).append('=').append(status.value).append(';');
                }
            }
        }
        return sb.toString();
    }

    /** Aparelhos distintos associados a este cômodo (um aparelho de várias tomadas conta uma vez). */
    private Set<String> idsTuya() {

        Set<String> ids = new LinkedHashSet<>();

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            for (ItemComodo item : comodo.itens(grupo)) {
                if (item.associado()) {
                    ids.add(item.tuyaDeviceId);
                }
            }
        }
        return ids;
    }

    private boolean todosEmCache() {

        for (String id : idsTuya()) {
            if (TuyaRepository.estadoEmCache(id) == null) {
                return false;
            }
        }
        return true;
    }

    /** A atualização periódica só roda com a tela visível e havendo algo associado. */
    private void iniciaAtualizacao() {

        if (atualizando) {
            return;
        }

        if (tuyaRepository == null || idsTuya().isEmpty()) {
            aguardando = false;
            exibe();
            return;
        }

        atualizando = true;
        ultimaAtividadeEm = SystemClock.elapsedRealtime();
        handler.post(atualizacaoPeriodica);
    }

    private void carregaEstados() {

        if (comodo == null || carregando || SystemClock.elapsedRealtime() - ultimoComandoEm < CARENCIA_APOS_COMANDO_MS) {
            return;
        }

        carregando = true;

        tuyaRepository.carregaEstados(idsTuya(), new TuyaRepository.Resultado<Map<String, TuyaDevice>>() {
            @Override
            public void aoConcluir(Map<String, TuyaDevice> dispositivos) {

                carregando = false;

                if (isDestroyed()) {
                    return;
                }

                // O usuário pode ter tocado num botão enquanto a consulta estava na rede.
                if (SystemClock.elapsedRealtime() - ultimoComandoEm < CARENCIA_APOS_COMANDO_MS) {
                    return;
                }

                String nova = assinatura(dispositivos);

                // Mudança vinda de fora (app da Tuya, interruptor físico): acompanha de perto por um tempo.
                if (assinaturaEstados != null && !nova.equals(assinaturaEstados)) {
                    ultimaAtividadeEm = SystemClock.elapsedRealtime();
                }
                assinaturaEstados = nova;

                aguardando = false;
                exibe();
            }

            @Override
            public void aoFalhar(String mensagem) {

                carregando = false;

                if (isDestroyed()) {
                    return;
                }

                Log.w(TAG, "Estado da Tuya indisponível: " + mensagem);

                if (aguardando) {
                    Toast.makeText(ComodoActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();
                }

                aguardando = false;
                exibe();
            }
        });
    }

    private void exibeCarregando(boolean carregandoTela) {

        binding.llComodoCarregando.setVisibility(carregandoTela ? View.VISIBLE : View.GONE);
        binding.svComodo.setVisibility(carregandoTela ? View.INVISIBLE : View.VISIBLE);
    }

    private void exibe() {

        if (comodo == null) {
            return;
        }

        binding.tvComodoTitulo.setText(comodo.nome);

        binding.ivComodoOnOff.setEnabled(true);
        binding.ivComodoOnOff.setImageResource(comodo.habilitado ? R.drawable.bt_on : R.drawable.bt_off);

        exibeCarregando(aguardando);

        adapterLampadas.atualiza(celulas(comodo.lampadas, R.drawable.ic_lampada, R.color.corLuz, "l"));
        adapterTomadas.atualiza(celulas(comodo.tomadas, R.drawable.ic_tomada, R.color.corTomada, "t"));

        adapterMedidores.atualiza(medidores());
        adapterTermometros.atualiza(termometros());
        adapterCortinas.atualiza(cortinas());
        adapterChuva.atualiza(sensoresChuva());
        adapterAlarmes.atualiza(alarmes());
        adapterLuzes.atualiza(luzes());

        binding.tvComodoLampadas.setVisibility(comodo.lampadas.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvComodoTomadas.setVisibility(comodo.tomadas.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvComodoMedidores.setVisibility(comodo.medidores.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvComodoTermometros.setVisibility(comodo.termometros.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvComodoCortinas.setVisibility(comodo.cortinas.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvComodoChuva.setVisibility(comodo.sensoresChuva.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvComodoAlarmes.setVisibility(comodo.alarmes.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvComodoLuzes.setVisibility(comodo.luzes.isEmpty() ? View.GONE : View.VISIBLE);

        binding.tvComodoMensagem.setText(R.string.comodoSemItens);
        binding.tvComodoMensagem.setVisibility(comodo.totalItens() == 0 ? View.VISIBLE : View.GONE);
    }

    private List<MedidorAdapter.Medidor> medidores() {

        List<MedidorAdapter.Medidor> medidores = new ArrayList<>();
        Locale locale = Locale.getDefault();

        for (int i = 0; i < comodo.medidores.size(); i++) {

            ItemComodo item = comodo.medidores.get(i);
            TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;
            TuyaStatus status = statusDe(dispositivo, item);

            boolean ligado = status != null && Boolean.TRUE.equals(status.value);
            boolean foraDoAr = dispositivo != null && !dispositivo.online;

            int estado = !item.associado() ? R.string.estadoSemDispositivo
                    : foraDoAr ? R.string.estadoForaDoAr
                    : ligado ? R.string.estadoLigado : R.string.estadoDesligado;

            medidores.add(new MedidorAdapter.Medidor(i, item.nome, ligado,
                    !comodo.habilitado || status == null || foraDoAr, getString(estado),
                    LeituraDispositivo.potencia(LeituraDispositivo.valor(dispositivo, item, LeituraDispositivo.POTENCIA), locale),
                    LeituraDispositivo.corrente(LeituraDispositivo.valor(dispositivo, item, LeituraDispositivo.CORRENTE), locale),
                    LeituraDispositivo.tensao(LeituraDispositivo.valor(dispositivo, item, LeituraDispositivo.TENSAO), locale)));
        }
        return medidores;
    }

    private List<TermometroAdapter.Termometro> termometros() {

        List<TermometroAdapter.Termometro> termometros = new ArrayList<>();
        Locale locale = Locale.getDefault();

        for (ItemComodo item : comodo.termometros) {

            TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;
            Double temperatura = LeituraDispositivo.valor(dispositivo, item, LeituraDispositivo.TEMPERATURA);
            Double umidade = LeituraDispositivo.valor(dispositivo, item, LeituraDispositivo.UMIDADE);

            // O termômetro segue a mesma regra de faixas do dashboard.
            int icone = IconeClima.termometro(temperatura != null ? Math.round(temperatura) : null);

            termometros.add(new TermometroAdapter.Termometro(item.nome, icone,
                    LeituraDispositivo.temperatura(temperatura, locale),
                    getString(R.string.umidadeValor, LeituraDispositivo.umidade(umidade, locale)),
                    !comodo.habilitado || temperatura == null || !dispositivo.online));
        }
        return termometros;
    }

    /** O id da célula é "prefixo + posição", para achar o item ao tocar. */
    private List<Celula> celulas(List<ItemComodo> itens, int icone, int corLigado, String prefixo) {

        List<Celula> celulas = new ArrayList<>();

        for (int i = 0; i < itens.size(); i++) {

            ItemComodo item = itens.get(i);
            TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;
            TuyaStatus status = statusDe(dispositivo, item);

            boolean ligado = status != null && Boolean.TRUE.equals(status.value);
            // Cômodo desabilitado, item sem associação ou aparelho desconhecido/fora do ar ficam esmaecidos.
            boolean esmaecido = !comodo.habilitado || status == null || !dispositivo.online;

            celulas.add(new Celula(prefixo + i, item.nome, icone, esmaecido,
                    ligado ? getColor(corLigado) : 0x99FFFFFF, ligado));
        }
        return celulas;
    }

    private static TuyaStatus statusDe(TuyaDevice dispositivo, ItemComodo item) {

        if (dispositivo == null || dispositivo.status == null) {
            return null;
        }

        for (TuyaStatus status : dispositivo.status) {
            if (item.tuyaCode.equals(status.code) && status.value instanceof Boolean) {
                return status;
            }
        }
        return null;
    }

    private List<CortinaAdapter.Cortina> cortinas() {

        List<CortinaAdapter.Cortina> cortinas = new ArrayList<>();

        for (int i = 0; i < comodo.cortinas.size(); i++) {

            ItemComodo item = comodo.cortinas.get(i);

            // A associação é opcional: sem ela só há os botões, sem leitura de posição.
            TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;

            String estado = "";
            LeituraDispositivo.EstadoCortina leituraCortina = null;
            boolean foraDoAr = dispositivo != null && !dispositivo.online;

            ComandoCortina local = comandoDaCortina.get(chaveDaCortina(i));

            // Comandos vencidos saem da lista (movimento passou de 90s ou a parada esperou demais).
            if (local != null && !local.enviando && SystemClock.elapsedRealtime() - local.em
                    >= (local.comando == 2 ? TEMPO_ESPERANDO_PARADA_MS : TEMPO_DE_MOVIMENTO_MS)) {
                comandoDaCortina.remove(chaveDaCortina(i));
                local = null;
            }

            // Botão em destaque: enquanto o pedido é enviado (todos) e durante o movimento (subir e descer).
            boolean emAcao = local != null && !foraDoAr
                    && (local.enviando || (local.comando != 2 && SystemClock.elapsedRealtime() - local.em < TEMPO_DE_MOVIMENTO_MS));

            if (item.associado()) {
                leituraCortina = foraDoAr ? null : LeituraDispositivo.estadoCortina(dispositivo, item, false);
            }

            // O motor já chegou onde o comando mandava (posição confirmada): encerra o retorno imediato.
            if (emAcao && local.comando != 2 && leituraCortina != null && leituraCortina.percentual != null
                    && ((local.comando == 0 && leituraCortina.posicao == LeituraDispositivo.PosicaoCortina.ABERTA)
                    || (local.comando == 1 && leituraCortina.posicao == LeituraDispositivo.PosicaoCortina.FECHADA))
                    && !local.enviando) {
                comandoDaCortina.remove(chaveDaCortina(i));
                local = null;
                emAcao = false;
            }

            // Parou: a Tuya só informa a posição alguns segundos depois; até lá o cartão avisa que está atualizando.
            boolean aguardandoPosicao = false;

            if (local != null && local.comando == 2 && !foraDoAr && item.associado()) {

                Double atual = LeituraDispositivo.posicaoCortina(dispositivo, item);
                boolean mudou = !java.util.Objects.equals(atual, local.posicaoAoParar);

                if (mudou) {
                    // A posição nova chegou: encerra a espera.
                    comandoDaCortina.remove(chaveDaCortina(i));
                    local = null;
                } else {
                    aguardandoPosicao = true;
                }
            }

            if (foraDoAr) {
                estado = getString(R.string.estadoForaDoAr);
            } else if (aguardandoPosicao) {
                estado = getString(R.string.cortinaAtualizando);
                leituraCortina = null;
            } else if (emAcao && local.comando != 2) {
                // Retorno imediato: não espera a Tuya atualizar para mostrar que a cortina está andando.
                estado = getString(local.comando == 0 ? R.string.cortinaAbrindo : R.string.cortinaFechando);
                leituraCortina = null;
            } else if (item.associado()) {
                if (leituraCortina == null) {
                    estado = getString(R.string.estadoSemLeitura);
                } else if (leituraCortina.posicao == LeituraDispositivo.PosicaoCortina.ABERTA) {
                    estado = getString(R.string.cortinaAberta);
                } else if (leituraCortina.posicao == LeituraDispositivo.PosicaoCortina.FECHADA) {
                    estado = getString(R.string.cortinaFechada);
                } else {
                    estado = leituraCortina.percentual != null
                            ? getString(R.string.cortinaParcial, leituraCortina.percentual)
                            : getString(R.string.cortinaParcialSemPercentual);
                }
            }

            // A barra só aparece com um percentual intermediário informado pelo motor; "Aberta" e "Fechada" dispensam a barra.
            int percentualAberto = -1;

            if (leituraCortina != null && leituraCortina.posicao == LeituraDispositivo.PosicaoCortina.PARCIAL && leituraCortina.percentual != null) {
                percentualAberto = leituraCortina.percentual;
            }

            int acaoAtiva = emAcao && local != null ? local.comando : -1;

            cortinas.add(new CortinaAdapter.Cortina(i, item.nome, estado, percentualAberto, acaoAtiva, ItemComodo.preenchida(item.tuyaCena),
                    ItemComodo.preenchida(item.tuyaCenaParar), ItemComodo.preenchida(item.tuyaCenaDesliga),
                    !comodo.habilitado || foraDoAr));
        }
        return cortinas;
    }

    private List<ChuvaAdapter.Chuva> sensoresChuva() {

        List<ChuvaAdapter.Chuva> sensores = new ArrayList<>();

        // Sem informação de sol ou nuvem no sensor, o horário do aparelho decide entre sol e lua.
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean dia = hora >= 6 && hora < 18;

        for (ItemComodo item : comodo.sensoresChuva) {

            TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;
            Boolean chovendo = LeituraDispositivo.chovendo(dispositivo, item);

            // Mesmos ícones do clima do dashboard: 61 = chuva, 0 = céu limpo, nulo = sem informação.
            int icone = IconeClima.condicao(chovendo == null ? null : chovendo ? 61 : 0, dia);
            int estado = chovendo == null ? R.string.estadoSemLeitura : chovendo ? R.string.estadoChovendo : R.string.estadoSemChuva;

            sensores.add(new ChuvaAdapter.Chuva(item.nome, icone, getString(estado),
                    !comodo.habilitado || chovendo == null || !dispositivo.online));
        }
        return sensores;
    }

    private List<AlarmeAdapter.Alarme> alarmes() {

        List<AlarmeAdapter.Alarme> alarmes = new ArrayList<>();

        for (int i = 0; i < comodo.alarmes.size(); i++) {

            ItemComodo item = comodo.alarmes.get(i);
            TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;
            LeituraDispositivo.EstadoAlarme estado = LeituraDispositivo.estadoAlarme(dispositivo, item);

            int texto;
            int cor;

            switch (estado) {
                case ATIVADO:
                    texto = R.string.alarmeAtivado;
                    cor = getColor(R.color.corAlarmeOk);
                    break;
                case EM_CASA:
                    texto = R.string.alarmeEmCasa;
                    cor = getColor(R.color.corLuz);
                    break;
                case DISPARADO:
                    texto = R.string.alarmeDisparado;
                    cor = getColor(R.color.corAlarme);
                    break;
                case DESATIVADO:
                    texto = R.string.alarmeDesativado;
                    cor = getColor(R.color.textoSecundario);
                    break;
                default:
                    texto = R.string.alarmeDesconhecido;
                    cor = getColor(R.color.textoSecundario);
                    break;
            }

            // Comandar só depende das cenas; o aparelho serve para mostrar o status.
            boolean foraDoAr = dispositivo != null && !dispositivo.online;

            boolean[] temCena = new boolean[5];

            for (int comando = 0; comando < temCena.length; comando++) {
                temCena[comando] = ItemComodo.preenchida(item.cena(comando));
            }

            // O botão do modo em vigor ganha contorno.
            int emVigor = estado == LeituraDispositivo.EstadoAlarme.ATIVADO ? AlarmeAdapter.TOTAL
                    : estado == LeituraDispositivo.EstadoAlarme.EM_CASA ? AlarmeAdapter.PARCIAL
                    : estado == LeituraDispositivo.EstadoAlarme.DESATIVADO ? AlarmeAdapter.DESATIVAR
                    : estado == LeituraDispositivo.EstadoAlarme.DISPARADO ? AlarmeAdapter.SOS : -1;

            alarmes.add(new AlarmeAdapter.Alarme(i, item.nome, getString(foraDoAr ? R.string.estadoForaDoAr : texto), cor,
                    temCena, foraDoAr ? -1 : emVigor, !comodo.habilitado || foraDoAr));
        }
        return alarmes;
    }

    /** Último degrau acionado de cada controle da luz ajustável, guardado no aparelho (as cenas não informam estado). */
    private android.content.SharedPreferences degraus() {
        return getSharedPreferences("luz_ajustavel", MODE_PRIVATE);
    }

    private String chaveDoDegrau(int posicao, boolean brilho) {
        return comodo.id + "/" + posicao + (brilho ? "/b" : "/t");
    }

    /** Depois de um toque numa luz, o degrau tocado vale até a Tuya refletir a mudança (alguns segundos). */
    private static final long VALIDADE_DO_TOQUE_MS = 20_000;

    private final Map<String, Long> luzTocadaEm = new java.util.HashMap<>();

    private List<LuzAdapter.Luz> luzes() {

        List<LuzAdapter.Luz> luzes = new ArrayList<>();

        for (int i = 0; i < comodo.luzes.size(); i++) {

            ItemComodo item = comodo.luzes.get(i);
            boolean[] temCena = new boolean[LuzAdapter.TEMPERATURAS + LuzAdapter.BRILHOS];

            for (int comando = 0; comando < temCena.length; comando++) {
                temCena[comando] = ItemComodo.preenchida(item.cena(comando));
            }

            // Último degrau tocado neste aparelho; só vale sozinho sem dispositivo ou logo após o toque.
            int temperatura = degraus().getInt(chaveDoDegrau(i, false), -1);
            int brilho = degraus().getInt(chaveDoDegrau(i, true), -1);

            String estado = "";
            boolean foraDoAr = false;

            TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;

            if (item.associado()) {

                Long tocadaEm = luzTocadaEm.get(chaveDoDegrau(i, false));
                boolean toqueRecente = tocadaEm != null && SystemClock.elapsedRealtime() - tocadaEm < VALIDADE_DO_TOQUE_MS;

                LeituraDispositivo.EstadoLuz leitura = LeituraDispositivo.estadoLuz(dispositivo, item);
                foraDoAr = dispositivo != null && !dispositivo.online;

                if (foraDoAr) {
                    estado = getString(R.string.estadoForaDoAr);
                } else if (leitura == null) {
                    estado = getString(R.string.estadoSemLeitura);
                } else {
                    if (Boolean.FALSE.equals(leitura.ligada)) {
                        estado = getString(R.string.luzDesligada);
                    } else if (leitura.brilho != null) {
                        estado = getString(R.string.luzLigada, leitura.brilho);
                    } else {
                        estado = getString(R.string.luzLigadaSemBrilho);
                    }

                    // O estado real do aparelho manda; o toque recente só cobre o intervalo até a Tuya atualizar.
                    if (!toqueRecente) {
                        temperatura = leitura.temperatura != null ? LeituraDispositivo.degrauDeTemperatura(leitura.temperatura) : -1;
                        brilho = leitura.brilho != null ? LeituraDispositivo.degrauDeBrilho(leitura.brilho) : -1;

                        // Desligada: nenhum degrau em vigor.
                        if (Boolean.FALSE.equals(leitura.ligada)) {
                            temperatura = -1;
                            brilho = -1;
                        }
                    }
                }
            }

            luzes.add(new LuzAdapter.Luz(i, item.nome, estado, temCena, temperatura, brilho, !comodo.habilitado || foraDoAr));
        }
        return luzes;
    }

    private void aoClicarLuz(LuzAdapter.Luz luz, int comando) {

        if (luz.posicao >= comodo.luzes.size()) {
            return;
        }

        ItemComodo item = comodo.luzes.get(luz.posicao);

        // Só marca o degrau se há cena para disparar e o cômodo está habilitado.
        if (comodo.habilitado && ItemComodo.preenchida(item.cena(comando))) {

            boolean brilho = comando >= LuzAdapter.TEMPERATURAS;

            degraus().edit().putInt(chaveDoDegrau(luz.posicao, brilho), brilho ? comando - LuzAdapter.TEMPERATURAS : comando).apply();
            luzTocadaEm.put(chaveDoDegrau(luz.posicao, false), SystemClock.elapsedRealtime());
            exibe();
        }

        // Com dispositivo associado, confere o estado logo depois para mostrar o valor real.
        executaCenaDoItem(item, comando, item.associado());
    }

    /** Lista cada ponto de dados do aparelho da cortina com o valor atual (código = valor). */
    private void mostraDadosCortina(CortinaAdapter.Cortina cortina) {

        if (cortina.posicao >= comodo.cortinas.size()) {
            return;
        }

        ItemComodo item = comodo.cortinas.get(cortina.posicao);
        TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;

        StringBuilder texto = new StringBuilder();

        if (dispositivo != null && dispositivo.status != null) {
            for (TuyaStatus status : dispositivo.status) {
                texto.append(status.code).append(" = ").append(status.value).append('\n');
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dadosDispositivoTitulo)
                .setMessage(texto.length() == 0 ? getString(R.string.dadosDispositivoVazio) : texto.toString().trim())
                .setPositiveButton(R.string.fechar, null)
                .show();
    }

    private void aoClicarCortina(CortinaAdapter.Cortina cortina, int comando) {

        if (cortina.posicao >= comodo.cortinas.size()) {
            return;
        }

        ItemComodo alvo = comodo.cortinas.get(cortina.posicao);
        String chave = chaveDaCortina(cortina.posicao);

        // Sem cena escolhida (ou cômodo desabilitado) não há ação: só a mensagem de sempre.
        if (!comodo.habilitado || !ItemComodo.preenchida(alvo.cena(comando))) {
            executaCenaDoItem(alvo, comando, false);
            return;
        }

        // Retorno imediato: o botão muda de cor e o estado já mostra "Abrindo…" ou "Fechando…".
        ComandoCortina enviado = new ComandoCortina(comando);

        if (comando == 2) {
            enviado.posicaoAoParar = LeituraDispositivo.posicaoCortina(
                    alvo.associado() ? TuyaRepository.estadoEmCache(alvo.tuyaDeviceId) : null, alvo);
        }

        comandoDaCortina.put(chave, enviado);
        exibe();

        executaCenaDoItem(alvo, comando, true, () -> {

            enviado.enviando = false;

            // Um pedido que falhou desfaz o retorno imediato; parar mantém a espera pela posição nova.
            if (falhouNoEnvio) {
                comandoDaCortina.remove(chave);
            }
            exibe();
        });
    }

    private void aoClicarAlarme(AlarmeAdapter.Alarme alarme, int comando) {

        if (alarme.posicao >= comodo.alarmes.size()) {
            return;
        }

        ItemComodo item = comodo.alarmes.get(alarme.posicao);

        // Desativar tira a proteção da casa e SOS faz a sirene tocar: os dois pedem confirmação.
        if (comando == AlarmeAdapter.DESATIVAR) {
            confirmaComandoDoAlarme(item, R.string.desativarAlarmeTitulo, R.string.desativarAlarmeMensagem, R.string.desativar, comando);
            return;
        }
        if (comando == AlarmeAdapter.SOS) {
            confirmaComandoDoAlarme(item, R.string.acionarSosTitulo, R.string.acionarSosMensagem, R.string.acionar, comando);
            return;
        }

        executaCenaDoItem(item, comando, true);
    }

    private void confirmaComandoDoAlarme(ItemComodo item, int titulo, int mensagem, int botao, int comando) {

        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(getString(mensagem, item.nome))
                .setPositiveButton(botao, (dialog, which) -> executaCenaDoItem(item, comando, true))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    /**
     * Dispara a cena do comando do item (0 a 4, ver {@link ItemComodo#cena(int)}). Cortinas e alarmes
     * são sempre comandados por cenas, que não gastam a cota de aparelhos controláveis da Tuya.
     */
    private void executaCenaDoItem(ItemComodo item, int comando, boolean atualizaEstadoDepois) {
        executaCenaDoItem(item, comando, atualizaEstadoDepois, null);
    }

    /** Verdadeiro se o último pedido de cena falhou (lido pelo término do pedido, na main thread). */
    private boolean falhouNoEnvio = false;

    private void executaCenaDoItem(ItemComodo item, int comando, boolean atualizaEstadoDepois, Runnable aoTerminar) {

        if (!comodo.habilitado) {
            Toast.makeText(this, R.string.comodoDesabilitado, Toast.LENGTH_SHORT).show();
            return;
        }

        String cena = item.cena(comando);

        if (!ItemComodo.preenchida(cena)) {
            Toast.makeText(this, R.string.semCenaConfigurada, Toast.LENGTH_SHORT).show();
            return;
        }

        if (tuyaRepository == null) {
            Toast.makeText(this, R.string.tuyaNaoConfiguradaCurto, Toast.LENGTH_SHORT).show();
            return;
        }

        ultimaAtividadeEm = SystemClock.elapsedRealtime();

        tuyaRepository.executaCenaPorNome(cena, new TuyaRepository.Resultado<Void>() {
            @Override
            public void aoConcluir(Void dado) {

                if (isDestroyed()) {
                    return;
                }

                // O estado leva alguns segundos para refletir o comando: confere várias vezes, começando cedo.
                if (atualizaEstadoDepois && atualizando) {
                    handler.removeCallbacks(atualizacaoAposComando);
                    handler.postDelayed(atualizacaoAposComando, 3_000);
                    handler.postDelayed(atualizacaoAposComando, 8_000);
                    handler.postDelayed(atualizacaoAposComando, 15_000);
                    handler.postDelayed(atualizacaoAposComando, 30_000);
                }

                falhouNoEnvio = false;

                if (aoTerminar != null) {
                    aoTerminar.run();
                }
            }

            @Override
            public void aoFalhar(String mensagem) {

                if (!isDestroyed()) {
                    Toast.makeText(ComodoActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();

                    falhouNoEnvio = true;

                    if (aoTerminar != null) {
                        aoTerminar.run();
                    }
                }
            }
        });
    }

    private void alternaHabilitado() {

        if (comodo != null) {
            comodoRepository.defineHabilitado(comodo.id, !comodo.habilitado);
        }
    }

    /**
     * Comanda o item por uma cena do Smart Life (não consome a cota de aparelhos controláveis).
     * O estado continua vindo do interruptor associado: ele define o sentido do toque e a cor do ícone.
     */
    private void aoClicarComCena(ItemComodo item) {

        if (tuyaRepository == null) {
            Toast.makeText(this, R.string.tuyaNaoConfiguradaCurto, Toast.LENGTH_SHORT).show();
            return;
        }

        TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;
        TuyaStatus status = statusDe(dispositivo, item);

        if (status != null && !dispositivo.online) {
            Toast.makeText(this, R.string.dispositivoOffline, Toast.LENGTH_SHORT).show();
            return;
        }

        // Sem estado conhecido não há como inverter: assume que está desligado.
        boolean ligar = status == null || !Boolean.TRUE.equals(status.value);

        if (status != null) {
            status.value = ligar;
            exibe();
        }

        ultimoComandoEm = SystemClock.elapsedRealtime();

        String cena = item.cenaPara(ligar);

        tuyaRepository.executaCenaPorNome(cena, new TuyaRepository.Resultado<Void>() {
            @Override
            public void aoConcluir(Void dado) {
            }

            @Override
            public void aoFalhar(String mensagem) {

                if (isDestroyed()) {
                    return;
                }

                Toast.makeText(ComodoActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();

                ultimoComandoEm = -CARENCIA_APOS_COMANDO_MS;
                carregaEstados();
            }
        });
    }

    /** O id da célula é "prefixo + posição" (ver {@link #celulas}). */
    private void aoClicar(List<ItemComodo> itens, Celula celula) {
        aciona(itens, Integer.parseInt(celula.id.substring(1)), false);
    }

    private void aoClicarMedidor(MedidorAdapter.Medidor medidor) {
        aciona(comodo.medidores, medidor.posicao, true);
    }

    /**
     * Liga ou desliga o item (por cena ou direto no aparelho). Em medidores de energia, que costumam
     * alimentar algum equipamento, desligar pede confirmação antes.
     */
    private void aciona(List<ItemComodo> itens, int posicao, boolean confirmaDesligar) {

        if (!comodo.habilitado) {
            Toast.makeText(this, R.string.comodoDesabilitado, Toast.LENGTH_SHORT).show();
            return;
        }

        if (posicao >= itens.size()) {
            return;
        }

        ItemComodo item = itens.get(posicao);

        if (confirmaDesligar && estaLigado(item)) {

            new AlertDialog.Builder(this)
                    .setTitle(R.string.desligarTitulo)
                    .setMessage(getString(R.string.desligarMensagem, item.nome))
                    .setPositiveButton(R.string.desligar, (dialog, which) -> {
                        // O aparelho pode ter sido desligado por fora enquanto o diálogo estava aberto: não o religa.
                        if (estaLigado(item)) {
                            executa(item);
                        }
                    })
                    .setNegativeButton(R.string.cancelar, null)
                    .show();
            return;
        }

        executa(item);
    }

    private static boolean estaLigado(ItemComodo item) {

        TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;
        TuyaStatus status = statusDe(dispositivo, item);

        return status != null && Boolean.TRUE.equals(status.value);
    }

    private void executa(ItemComodo item) {

        if (item.temCena()) {
            aoClicarComCena(item);
            return;
        }

        if (!item.associado()) {
            Toast.makeText(this, R.string.semDispositivoAssociado, Toast.LENGTH_SHORT).show();
            return;
        }

        if (tuyaRepository == null) {
            Toast.makeText(this, R.string.tuyaNaoConfiguradaCurto, Toast.LENGTH_SHORT).show();
            return;
        }

        TuyaDevice dispositivo = TuyaRepository.estadoEmCache(item.tuyaDeviceId);
        TuyaStatus status = statusDe(dispositivo, item);

        if (status == null) {
            Toast.makeText(this, R.string.dispositivoIndisponivel, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!dispositivo.online) {
            Toast.makeText(this, R.string.dispositivoOffline, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean novoValor = !Boolean.TRUE.equals(status.value);

        // Atualiza a tela já; se o comando falhar o estado real é recarregado.
        status.value = novoValor;
        ultimoComandoEm = SystemClock.elapsedRealtime();
        exibe();

        tuyaRepository.enviaComando(item.tuyaDeviceId, item.tuyaCode, novoValor, new TuyaRepository.Resultado<Void>() {
            @Override
            public void aoConcluir(Void dado) {
            }

            @Override
            public void aoFalhar(String mensagem) {

                if (isDestroyed()) {
                    return;
                }

                Toast.makeText(ComodoActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();

                ultimoComandoEm = -CARENCIA_APOS_COMANDO_MS;
                carregaEstados();
            }
        });
    }
}
