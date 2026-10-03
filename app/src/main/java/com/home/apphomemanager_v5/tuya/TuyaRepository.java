package com.home.apphomemanager_v5.tuya;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.home.apphomemanager_v5.BuildConfig;
import com.home.apphomemanager_v5.tuya.model.TuyaCommands;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaDeviceInfo;
import com.home.apphomemanager_v5.tuya.model.TuyaDevicesPage;
import com.home.apphomemanager_v5.tuya.model.TuyaFuncao;
import com.home.apphomemanager_v5.tuya.model.TuyaResponse;
import com.home.apphomemanager_v5.tuya.model.TuyaScene;
import com.home.apphomemanager_v5.tuya.model.TuyaSpec;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Lista dispositivos e envia comandos pela Tuya Cloud. Os callbacks chegam na main thread. */
public class TuyaRepository {

    public interface Resultado<T> {
        void aoConcluir(T dado);

        void aoFalhar(String mensagem);
    }

    /** Cena do Smart Life que o botão da porta do dashboard executa. */
    public static final String CENA_PORTA = "Abrir Entrada";

    private static final int TAMANHO_PAGINA = 50;

    private static final long VALIDADE_CENAS_MS = 10 * 60_000;

    /** Cenas de acionamento manual da última consulta feita por {@link #executaCenaPorNome}. */
    private List<TuyaScene> cenasEmCache;

    private long cenasEm = -VALIDADE_CENAS_MS;

    private final TuyaService service;

    /** Id da casa, descoberto uma vez e reaproveitado. */
    private String homeId;

    /** Especificações mudam raramente: buscadas uma vez por dispositivo. */
    private final Map<String, TuyaSpec> especificacoes = new HashMap<>();

    private static TuyaRepository instancia;

    /** Último estado conhecido de cada aparelho consultado por id (só acessado na main thread). */
    private static final Map<String, TuyaDevice> CACHE_ESTADO = new HashMap<>();

    /**
     * Instância compartilhada: reaproveita o token e a conexão já abertos, o que tira o handshake
     * e o pedido de token do caminho de cada tela.
     */
    public static synchronized TuyaRepository getInstance() {

        if (instancia == null) {
            instancia = new TuyaRepository();
        }
        return instancia;
    }

    /** Último estado conhecido do aparelho, ou nulo se ainda não foi consultado. */
    public static TuyaDevice estadoEmCache(String deviceId) {
        return CACHE_ESTADO.get(deviceId);
    }

    public TuyaRepository() {

        OkHttpClient cliente = new OkHttpClient.Builder()
                .addInterceptor(new TuyaInterceptor(BuildConfig.TUYA_ACCESS_ID, BuildConfig.TUYA_ACCESS_SECRET))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        service = new Retrofit.Builder()
                .baseUrl(BuildConfig.TUYA_BASE_URL)
                .client(cliente)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TuyaService.class);
    }

    public static boolean estaConfigurado() {
        return !BuildConfig.TUYA_ACCESS_ID.isEmpty() && !BuildConfig.TUYA_ACCESS_SECRET.isEmpty();
    }

    /** Todos os dispositivos, já com o estado atual e o esquema de cada ponto de dados. */
    public void carregaDispositivos(Resultado<List<TuyaDispositivo>> resultado) {

        carregaPagina(null, new ArrayList<>(), new Resultado<List<TuyaDevice>>() {
            @Override
            public void aoConcluir(List<TuyaDevice> dispositivos) {

                List<TuyaDispositivo> convertidos = new ArrayList<>();

                for (TuyaDevice dispositivo : dispositivos) {
                    convertidos.add(converte(dispositivo, especificacoes.get(dispositivo.id)));
                }
                resultado.aoConcluir(convertidos);
            }

            @Override
            public void aoFalhar(String mensagem) {
                resultado.aoFalhar(mensagem);
            }
        });
    }

    /**
     * Estado atual só dos aparelhos pedidos (uma chamada por aparelho, em paralelo). Os que responderem
     * ficam também em cache; se nenhum responder, falha. Muito mais leve que {@link #carregaDispositivos}.
     */
    public void carregaEstados(Collection<String> deviceIds, Resultado<Map<String, TuyaDevice>> resultado) {

        Map<String, TuyaDevice> obtidos = new HashMap<>();

        if (deviceIds.isEmpty()) {
            resultado.aoConcluir(obtidos);
            return;
        }

        int[] pendentes = {deviceIds.size()};
        String[] ultimoErro = {null};

        for (String id : deviceIds) {

            service.getDevice(id).enqueue(new Callback<TuyaResponse<TuyaDevice>>() {
                @Override
                public void onResponse(@NonNull Call<TuyaResponse<TuyaDevice>> call, @NonNull Response<TuyaResponse<TuyaDevice>> resposta) {

                    TuyaResponse<TuyaDevice> corpo = resposta.body();

                    if (corpo == null || !corpo.success || corpo.result == null) {
                        ultimoErro[0] = erro(corpo, resposta.code());
                        terminou();
                        return;
                    }

                    TuyaDevice dispositivo = corpo.result;
                    dispositivo.id = id;

                    if (dispositivo.status != null) {
                        guarda(dispositivo);
                        terminou();
                        return;
                    }

                    // Algumas contas não devolvem o status junto com o aparelho.
                    service.getStatus(id).enqueue(new Callback<TuyaResponse<List<TuyaStatus>>>() {
                        @Override
                        public void onResponse(@NonNull Call<TuyaResponse<List<TuyaStatus>>> c, @NonNull Response<TuyaResponse<List<TuyaStatus>>> r2) {

                            TuyaResponse<List<TuyaStatus>> b2 = r2.body();

                            if (b2 != null && b2.success && b2.result != null) {
                                dispositivo.status = b2.result;
                                guarda(dispositivo);
                            } else {
                                ultimoErro[0] = erro(b2, r2.code());
                            }
                            terminou();
                        }

                        @Override
                        public void onFailure(@NonNull Call<TuyaResponse<List<TuyaStatus>>> c, @NonNull Throwable t) {
                            ultimoErro[0] = t.getMessage();
                            terminou();
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call<TuyaResponse<TuyaDevice>> call, @NonNull Throwable t) {
                    ultimoErro[0] = t.getMessage();
                    terminou();
                }

                private void guarda(TuyaDevice dispositivo) {
                    obtidos.put(id, dispositivo);
                    CACHE_ESTADO.put(id, dispositivo);
                }

                // Callbacks do Retrofit rodam na main thread: o contador não precisa de sincronização.
                private void terminou() {
                    if (--pendentes[0] == 0) {
                        if (obtidos.isEmpty()) {
                            resultado.aoFalhar(ultimoErro[0] != null ? ultimoErro[0] : "sem resposta da Tuya");
                        } else {
                            resultado.aoConcluir(obtidos);
                        }
                    }
                }
            });
        }
    }

    /**
     * Cenas de acionamento manual da casa. O id da casa vem de {@code TUYA_HOME_ID} (local.properties)
     * ou, se vazio, do {@code owner_id} do aparelho de referência.
     */
    public void carregaCenas(String deviceIdReferencia, Resultado<List<TuyaScene>> resultado) {

        resolveCasa(deviceIdReferencia, new Resultado<String>() {
            @Override
            public void aoConcluir(String casa) {

                service.getScenes(casa).enqueue(new Callback<TuyaResponse<List<TuyaScene>>>() {
                    @Override
                    public void onResponse(@NonNull Call<TuyaResponse<List<TuyaScene>>> call, @NonNull Response<TuyaResponse<List<TuyaScene>>> resposta) {

                        TuyaResponse<List<TuyaScene>> corpo = resposta.body();

                        if (corpo != null && corpo.success) {
                            resultado.aoConcluir(soAcionamentoManual(corpo.result));
                        } else {
                            resultado.aoFalhar(erro(corpo, resposta.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TuyaResponse<List<TuyaScene>>> call, @NonNull Throwable t) {
                        resultado.aoFalhar(t.getMessage());
                    }
                });
            }

            @Override
            public void aoFalhar(String mensagem) {
                resultado.aoFalhar(mensagem);
            }
        });
    }

    /** Id da casa descoberto por {@link #carregaCenas}, ou nulo se ainda não foi. */
    public String getHomeId() {
        return homeId;
    }

    /** Usa uma casa já conhecida (guardada de uma execução anterior), sem precisar consultar as cenas. */
    public void usaCasa(String casaId) {
        this.homeId = casaId;
    }

    /** Executa a cena. Só funciona depois de {@link #carregaCenas} ou {@link #usaCasa}. */
    public void disparaCena(String sceneId, Resultado<Void> resultado) {

        if (homeId == null) {
            resultado.aoFalhar("casa não identificada");
            return;
        }

        service.triggerScene(homeId, sceneId).enqueue(new Callback<TuyaResponse<Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<TuyaResponse<Boolean>> call, @NonNull Response<TuyaResponse<Boolean>> resposta) {

                TuyaResponse<Boolean> corpo = resposta.body();

                if (corpo != null && corpo.success && !Boolean.FALSE.equals(corpo.result)) {
                    resultado.aoConcluir(null);
                } else {
                    resultado.aoFalhar(erro(corpo, resposta.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<TuyaResponse<Boolean>> call, @NonNull Throwable t) {
                resultado.aoFalhar(t.getMessage());
            }
        });
    }

    private void resolveCasa(String deviceIdReferencia, Resultado<String> resultado) {

        if (homeId != null) {
            resultado.aoConcluir(homeId);
            return;
        }

        if (!BuildConfig.TUYA_HOME_ID.isEmpty()) {
            homeId = BuildConfig.TUYA_HOME_ID;
            resultado.aoConcluir(homeId);
            return;
        }

        if (deviceIdReferencia == null) {
            resolveCasaPeloPrimeiroAparelho(resultado);
            return;
        }

        service.getDeviceInfo(deviceIdReferencia).enqueue(new Callback<TuyaResponse<TuyaDeviceInfo>>() {
            @Override
            public void onResponse(@NonNull Call<TuyaResponse<TuyaDeviceInfo>> call, @NonNull Response<TuyaResponse<TuyaDeviceInfo>> resposta) {

                TuyaResponse<TuyaDeviceInfo> corpo = resposta.body();

                if (corpo != null && corpo.success && corpo.result != null && corpo.result.ownerId != null && !corpo.result.ownerId.isEmpty()) {
                    homeId = corpo.result.ownerId;
                    resultado.aoConcluir(homeId);
                } else if (corpo != null && !corpo.success) {
                    resultado.aoFalhar(erro(corpo, resposta.code()));
                } else {
                    resultado.aoFalhar("a Tuya não informou o id da casa; defina TUYA_HOME_ID no local.properties");
                }
            }

            @Override
            public void onFailure(@NonNull Call<TuyaResponse<TuyaDeviceInfo>> call, @NonNull Throwable t) {
                resultado.aoFalhar(t.getMessage());
            }
        });
    }

    /** Sem aparelho de referência (ex.: botão do dashboard): pega o primeiro da conta só para descobrir a casa. */
    private void resolveCasaPeloPrimeiroAparelho(Resultado<String> resultado) {

        service.getDevices(1, null).enqueue(new Callback<TuyaResponse<TuyaDevicesPage>>() {
            @Override
            public void onResponse(@NonNull Call<TuyaResponse<TuyaDevicesPage>> call, @NonNull Response<TuyaResponse<TuyaDevicesPage>> resposta) {

                TuyaResponse<TuyaDevicesPage> corpo = resposta.body();

                if (corpo == null || !corpo.success) {
                    resultado.aoFalhar(erro(corpo, resposta.code()));
                } else if (corpo.result == null || corpo.result.devices == null || corpo.result.devices.isEmpty()) {
                    resultado.aoFalhar("nenhum aparelho na conta para identificar a casa; defina TUYA_HOME_ID no local.properties");
                } else {
                    resolveCasa(corpo.result.devices.get(0).id, resultado);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TuyaResponse<TuyaDevicesPage>> call, @NonNull Throwable t) {
                resultado.aoFalhar(t.getMessage());
            }
        });
    }

    /**
     * Localiza a cena de acionamento manual pelo nome (ignora maiúsculas e espaços nas pontas) e a executa.
     * Buscar pelo nome a cada uso evita depender de um id que muda se a cena for recriada.
     */
    public void executaCenaPorNome(String nome, Resultado<Void> resultado) {

        // A lista de cenas fica em cache: cada toque custaria 2 chamadas (listar + executar) em vez de 1.
        TuyaScene emCache = SystemClock.elapsedRealtime() - cenasEm < VALIDADE_CENAS_MS ? achaCena(cenasEmCache, nome) : null;

        if (emCache != null && homeId != null) {
            disparaCena(emCache.sceneId, new Resultado<Void>() {
                @Override
                public void aoConcluir(Void dado) {
                    resultado.aoConcluir(null);
                }

                @Override
                public void aoFalhar(String mensagem) {
                    // O id guardado pode ter ficado velho (cena recriada): confere na lista atual uma vez.
                    // Só quando a Tuya recusou (mensagem termina com o código): falha de rede não repete,
                    // para uma cena que já tenha rodado não rodar duas vezes.
                    if (mensagem != null && mensagem.matches("(?s).*\\(\\d+\\)$")) {
                        cenasEm = -VALIDADE_CENAS_MS;
                        executaCenaPorNomeSemCache(nome, resultado);
                    } else {
                        resultado.aoFalhar(mensagem);
                    }
                }
            });
            return;
        }

        executaCenaPorNomeSemCache(nome, resultado);
    }

    private void executaCenaPorNomeSemCache(String nome, Resultado<Void> resultado) {

        carregaCenas(null, new Resultado<List<TuyaScene>>() {
            @Override
            public void aoConcluir(List<TuyaScene> cenas) {

                cenasEmCache = cenas;
                cenasEm = SystemClock.elapsedRealtime();

                TuyaScene cena = achaCena(cenas, nome);

                if (cena == null) {
                    resultado.aoFalhar("cena \"" + nome + "\" não encontrada");
                    return;
                }

                disparaCena(cena.sceneId, resultado);
            }

            @Override
            public void aoFalhar(String mensagem) {
                resultado.aoFalhar(mensagem);
            }
        });
    }

    static TuyaScene achaCena(List<TuyaScene> cenas, String nome) {

        if (cenas == null || nome == null) {
            return null;
        }

        for (TuyaScene cena : cenas) {
            if (cena.name != null && cena.name.trim().equalsIgnoreCase(nome.trim())) {
                return cena;
            }
        }
        return null;
    }

    /** A consulta traz também automações; só as cenas sem condição são de acionamento manual. */
    static List<TuyaScene> soAcionamentoManual(List<TuyaScene> cenas) {

        List<TuyaScene> manuais = new ArrayList<>();

        if (cenas == null) {
            return manuais;
        }

        for (TuyaScene cena : cenas) {
            boolean semCondicao = cena.conditions == null || cena.conditions.isEmpty();

            if (cena.sceneId != null && cena.name != null && semCondicao) {
                manuais.add(cena);
            }
        }
        return manuais;
    }

    /** Especificações já baixadas, por id de dispositivo (usado no diagnóstico em debug). */
    public Map<String, TuyaSpec> getEspecificacoes() {
        return especificacoes;
    }

    /**
     * Baixa a lista de cenas antecipadamente: o primeiro toque numa cena só precisa listá-las se não houver
     * cópia recente, e listar leva alguns segundos. Sem efeito se a cópia ainda é válida.
     */
    public void preaqueceCenas() {

        if (SystemClock.elapsedRealtime() - cenasEm < VALIDADE_CENAS_MS && cenasEmCache != null && homeId != null) {
            return;
        }

        carregaCenas(null, new Resultado<List<TuyaScene>>() {
            @Override
            public void aoConcluir(List<TuyaScene> cenas) {
                cenasEmCache = cenas;
                cenasEm = SystemClock.elapsedRealtime();
            }

            @Override
            public void aoFalhar(String mensagem) {
                // Nada a fazer: o toque tenta de novo e mostra o erro.
            }
        });
    }

    public void enviaComando(String deviceId, String code, Object valor, Resultado<Void> resultado) {

        service.sendCommands(deviceId, new TuyaCommands(code, valor)).enqueue(new Callback<TuyaResponse<Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<TuyaResponse<Boolean>> call, @NonNull Response<TuyaResponse<Boolean>> resposta) {

                TuyaResponse<Boolean> corpo = resposta.body();

                if (corpo != null && corpo.success) {
                    resultado.aoConcluir(null);
                } else {
                    resultado.aoFalhar(erro(corpo, resposta.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<TuyaResponse<Boolean>> call, @NonNull Throwable t) {
                resultado.aoFalhar(t.getMessage());
            }
        });
    }

    /**
     * Junta o estado do dispositivo ao esquema de cada ponto de dados (faixa, escala, unidade).
     * Sem especificação, os pontos seguem sem esquema.
     */
    static TuyaDispositivo converte(TuyaDevice dispositivo, TuyaSpec spec) {

        List<TuyaPonto> pontos = new ArrayList<>();

        if (dispositivo.status != null) {
            for (TuyaStatus status : dispositivo.status) {
                if (status.code != null && status.value != null) {
                    pontos.add(new TuyaPonto(status.code, status.value, esquema(spec, status.code)));
                }
            }
        }
        return new TuyaDispositivo(dispositivo.id, dispositivo.name, dispositivo.online, pontos);
    }

    private static String esquema(TuyaSpec spec, String code) {

        if (spec == null) {
            return null;
        }

        String doComando = procura(spec.functions, code);

        return doComando != null ? doComando : procura(spec.status, code);
    }

    private static String procura(List<TuyaFuncao> funcoes, String code) {

        if (funcoes == null) {
            return null;
        }

        for (TuyaFuncao funcao : funcoes) {
            if (code.equals(funcao.code)) {
                return funcao.values;
            }
        }
        return null;
    }

    private void carregaPagina(String ultimaLinha, List<TuyaDevice> acumulados, Resultado<List<TuyaDevice>> resultado) {

        service.getDevices(TAMANHO_PAGINA, ultimaLinha).enqueue(new Callback<TuyaResponse<TuyaDevicesPage>>() {
            @Override
            public void onResponse(@NonNull Call<TuyaResponse<TuyaDevicesPage>> call, @NonNull Response<TuyaResponse<TuyaDevicesPage>> resposta) {

                TuyaResponse<TuyaDevicesPage> corpo = resposta.body();

                if (corpo == null || !corpo.success || corpo.result == null) {
                    resultado.aoFalhar(erro(corpo, resposta.code()));
                    return;
                }

                if (corpo.result.devices != null) {
                    acumulados.addAll(corpo.result.devices);
                }

                if (corpo.result.hasMore && corpo.result.lastRowKey != null) {
                    carregaPagina(corpo.result.lastRowKey, acumulados, resultado);
                } else {
                    carregaStatus(acumulados, resultado);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TuyaResponse<TuyaDevicesPage>> call, @NonNull Throwable t) {
                resultado.aoFalhar(t.getMessage());
            }
        });
    }

    /** A listagem não traz o status dos pontos de dados; consulta um a um. */
    private void carregaStatus(List<TuyaDevice> dispositivos, Resultado<List<TuyaDevice>> resultado) {

        if (dispositivos.isEmpty()) {
            resultado.aoConcluir(dispositivos);
            return;
        }

        int[] pendentes = {dispositivos.size()};

        for (TuyaDevice dispositivo : dispositivos) {

            service.getStatus(dispositivo.id).enqueue(new Callback<TuyaResponse<List<TuyaStatus>>>() {
                @Override
                public void onResponse(@NonNull Call<TuyaResponse<List<TuyaStatus>>> call, @NonNull Response<TuyaResponse<List<TuyaStatus>>> resposta) {

                    TuyaResponse<List<TuyaStatus>> corpo = resposta.body();

                    if (corpo != null && corpo.success && corpo.result != null) {
                        dispositivo.status = corpo.result;
                    }
                    terminou();
                }

                @Override
                public void onFailure(@NonNull Call<TuyaResponse<List<TuyaStatus>>> call, @NonNull Throwable t) {
                    terminou();
                }

                // Callbacks do Retrofit rodam na main thread: o contador não precisa de sincronização.
                private void terminou() {
                    if (--pendentes[0] == 0) {
                        carregaEspecificacoes(dispositivos, resultado);
                    }
                }
            });
        }
    }

    /**
     * Busca a especificação dos dispositivos que ainda não a têm. Se falhar, o dispositivo segue
     * sem ela (continua com os interruptores) e a busca é tentada de novo na próxima atualização.
     */
    private void carregaEspecificacoes(List<TuyaDevice> dispositivos, Resultado<List<TuyaDevice>> resultado) {

        List<TuyaDevice> faltando = new ArrayList<>();

        for (TuyaDevice dispositivo : dispositivos) {
            if (!especificacoes.containsKey(dispositivo.id)) {
                faltando.add(dispositivo);
            }
        }

        if (faltando.isEmpty()) {
            resultado.aoConcluir(dispositivos);
            return;
        }

        int[] pendentes = {faltando.size()};

        for (TuyaDevice dispositivo : faltando) {

            service.getSpecifications(dispositivo.id).enqueue(new Callback<TuyaResponse<TuyaSpec>>() {
                @Override
                public void onResponse(@NonNull Call<TuyaResponse<TuyaSpec>> call, @NonNull Response<TuyaResponse<TuyaSpec>> resposta) {

                    TuyaResponse<TuyaSpec> corpo = resposta.body();

                    if (corpo != null && corpo.success && corpo.result != null) {
                        especificacoes.put(dispositivo.id, corpo.result);
                    }
                    terminou();
                }

                @Override
                public void onFailure(@NonNull Call<TuyaResponse<TuyaSpec>> call, @NonNull Throwable t) {
                    terminou();
                }

                private void terminou() {
                    if (--pendentes[0] == 0) {
                        resultado.aoConcluir(dispositivos);
                    }
                }
            });
        }
    }

    /** Mensagens da Tuya vêm em inglês; os códigos mais comuns ganham texto em português. */
    static String traduz(int codigo) {

        switch (codigo) {
            case 60001001:
                return "a cota de dispositivos controláveis do projeto na Tuya acabou; libere vagas ou amplie o plano na Tuya IoT Platform";
            case 2001:
                return "dispositivo offline";
            case 2008:
                return "o dispositivo não aceita este comando";
            case 1004:
                return "assinatura da requisição inválida; confira as credenciais da Tuya";
            case 1010:
                return "token da Tuya expirado";
            case 1106:
                return "sem permissão para acessar este recurso na Tuya";
            case 28841002:
                return "esta API não está assinada no projeto da Tuya";
            default:
                return null;
        }
    }

    private static String erro(TuyaResponse<?> corpo, int codigoHttp) {

        if (corpo != null) {
            String traduzida = traduz(corpo.code);

            return (traduzida != null ? traduzida : corpo.msg) + " (" + corpo.code + ")";
        }
        return "HTTP " + codigoHttp;
    }
}
